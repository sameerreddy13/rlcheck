package edu.berkeley.cs.jqf.fuzz.rl;
import edu.berkeley.cs.jqf.fuzz.ei.ZestGuidance;
import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;
import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;
import edu.berkeley.cs.jqf.fuzz.guidance.Result;
import edu.berkeley.cs.jqf.fuzz.guidance.TimeoutException;
import edu.berkeley.cs.jqf.fuzz.util.Coverage;
import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEvent;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Created by clemieux on 6/17/19.
 */
public class RLGuidance implements Guidance {

    private RLGenerator generator;

    // Currently, we only support single-threaded applications
    // This field is used to ensure that
    protected Thread appThread;

    /** A pseudo-random number generator for generating fresh values. */
    protected Random random = new Random();

    /** The name of the test for display purposes. */
    protected final String testName;

    // ------------ ALGORITHM BOOKKEEPING ------------

    /** The max amount of time to run for, in milli-seconds */
    protected final long maxDurationMillis;

    /** The number of trials completed. */
    protected long numTrials = 0;

    /** The number of valid inputs. */
    protected long numValid = 0;

    /** The directory where fuzzing results are written. */
    protected final File outputDirectory;

    /** The directory where saved inputs are written. */
    protected File savedInputsDirectory;

    /** The directory where saved inputs are written. */
    protected File savedFailuresDirectory;

    /** Number of saved inputs.
     *
     * This is usually the same as savedInputs.size(),
     * but we do not really save inputs in TOTALLY_RANDOM mode.
     */
    protected int numSavedInputs = 0;

    /** Coverage statistics for a single run. */
    protected Coverage runCoverage = new Coverage();

    /** Cumulative coverage statistics. */
    protected Coverage totalCoverage = new Coverage();

    /** Cumulative coverage for valid inputs. */
    protected Coverage validCoverage = new Coverage();

//    protected Set<String> uniqueValidInputs = new HashSet<>();
    protected Set<Integer> uniqueValidInputs = new HashSet<>();


    /** Unique paths for valid inputs */
    protected Set<Integer> uniquePaths = new HashSet<>();

    /** Unique branch sets for valid inputs */
    protected Set<Integer> uniqueBranchSets = new HashSet<>();


    /** The set of unique failures found so far. */
    protected Set<List<StackTraceElement>> uniqueFailures = new HashSet<>();

    // ---------- LOGGING / STATS OUTPUT ------------

    /** Whether to print log statements to stderr (debug option; manually edit). */
    protected final boolean verbose = true;

    /** A system console, which is non-null only if STDOUT is a console. */
    protected final Console console = System.console();

    /** Time since this guidance instance was created. */
    protected final Date startTime = new Date();

    /** Time at last stats refresh. */
    protected Date lastRefreshTime = startTime;

    /** Total execs at last stats refresh. */
    protected long lastNumTrials = 0;

    /** Minimum amount of time (in millis) between two stats refreshes. */
    protected static final long STATS_REFRESH_TIME_PERIOD = 300;

    /** The file where log data is written. */
    protected File logFile;

    /** The file where saved plot data is written. */
    protected File statsFile;

    /** The currently executing input (for debugging purposes). */
    protected String currentInput;

    // ------------- TIMEOUT HANDLING ------------

    /** Timeout for an individual run. */
    protected long singleRunTimeoutMillis;

    /** Date when last run was started. */
    protected Date runStart;

    /** Number of conditional jumps since last run was started. */
    protected long branchCount;

    /** Maximum number of trials to run */
    protected Long maxTrials = Long.getLong("jqf.guidance.MAX_TRIALS");

    // ----------- FUZZING HEURISTICS ------------

   /** Whether to use greybox information in rewards **/
    static final boolean USE_GREYBOX = Boolean.getBoolean("rl.guidance.USE_GREYBOX");

    public RLGuidance(RLGenerator g, String testName, Duration duration, File outputDirectory) throws IOException {
        this.generator = g;
        this.testName = testName;
        this.maxDurationMillis = duration != null ? duration.toMillis() : Long.MAX_VALUE;
        this.outputDirectory = outputDirectory;
        prepareOutputDirectory();
    }


    @Override
    public InputStream getInput() throws IllegalStateException, GuidanceException {
        runCoverage.clear();
        currentInput = generator.generate();
        return new ByteArrayInputStream(currentInput.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public boolean hasInput() {
        if (maxTrials != null){
            if (numTrials >= maxTrials){
                displayStats(true);
            }
            return numTrials < maxTrials;
        } else {
            Date now = new Date();
            long elapsedMilliseconds = now.getTime() - startTime.getTime();
            return elapsedMilliseconds < maxDurationMillis;
        }
    }


    @Override
    public void handleResult(Result result, Throwable error) throws GuidanceException {
        // Stop timeout handling
        this.runStart = null;

        // Increment run count
        this.numTrials++;

        boolean valid = result == Result.SUCCESS;

        if (valid) {
            // Increment valid counter
            numValid++;
        }

        if (result == Result.SUCCESS || result == Result.INVALID) {

            // Coverage before
            int nonZeroBefore = totalCoverage.getNonZeroCount();
            int validNonZeroBefore = validCoverage.getNonZeroCount();

            // Update total coverage
            boolean coverageBitsUpdated = totalCoverage.updateBits(runCoverage);

            int nonZeroAfter = totalCoverage.getNonZeroCount();

            if (valid) {
                validCoverage.updateBits(runCoverage);
                if (!uniqueValidInputs.contains(currentInput.hashCode())){
                    uniqueValidInputs.add(currentInput.hashCode());

                    uniquePaths.add(runCoverage.hashCode());
                    boolean has_new_branches_covered = uniqueBranchSets.add(runCoverage.nonZeroHashCode());
                    
                    if (USE_GREYBOX) {
                      // Greybox: only reward for inputs that cover new branches
                        if (has_new_branches_covered){
                            generator.update(20);
                        } else {
                            generator.update(0);
                        }
                    } else {
                        // Regular behavior: reward for inputs that are unique (see outer if)
                        generator.update(20);
                    }

                } else {
                    // TODO: allow this to be customizable
                    generator.update(0);
                }

            } else {
                generator.update(-1);
            }


            // Coverage after

            int validNonZeroAfter = validCoverage.getNonZeroCount();

            if (nonZeroAfter > nonZeroBefore || validNonZeroAfter > validNonZeroBefore) {
                try {
                    saveCurrentInput(valid);
                } catch (IOException e) {
                    throw new GuidanceException(e);
                }
            }


        } else if (result == Result.FAILURE || result == Result.TIMEOUT) {
            String msg = error.getMessage();

            // Get the root cause of the failure
            Throwable rootCause = error;
            while (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
            }

            // Attempt to add this to the set of unique failures
            if (uniqueFailures.add(Arrays.asList(rootCause.getStackTrace()))) {

                // Save crash to disk
                try {
                    saveCurrentFailure();
                } catch (IOException e) {
                    throw new GuidanceException(e);
                }

            }

        }

        displayStats();

    }


    /// CL Note: Below this is boiler-plate that probably doesn't need to be messed with

    private void prepareOutputDirectory() throws IOException {

        // Create the output directory if it does not exist
        if (!outputDirectory.exists()) {
            if (!outputDirectory.mkdirs()) {
                throw new IOException("Could not create output directory" +
                        outputDirectory.getAbsolutePath());
            }
        }

        // Make sure we can write to output directory
        if (!outputDirectory.isDirectory() || !outputDirectory.canWrite()) {
            throw new IOException("Output directory is not a writable directory: " +
                    outputDirectory.getAbsolutePath());
        }

        // Name files and directories after AFL
        this.savedInputsDirectory = new File(outputDirectory, "corpus");
        this.savedInputsDirectory.mkdirs();
        this.savedFailuresDirectory = new File(outputDirectory, "failures");
        this.savedFailuresDirectory.mkdirs();
        this.statsFile = new File(outputDirectory, "plot_data");
        this.logFile = new File(outputDirectory, "fuzz.log");


        // Delete everything that we may have created in a previous run.
        // Trying to stay away from recursive delete of parent output directory in case there was a
        // typo and that was not a directory we wanted to nuke.
        // We also do not check if the deletes are actually successful.
        statsFile.delete();
        logFile.delete();
        for (File file : savedInputsDirectory.listFiles()) {
            file.delete();
        }
        for (File file : savedFailuresDirectory.listFiles()) {
            file.delete();
        }

        appendLineToFile(statsFile, "# unix_time, unique_crashes, total_cov, valid_cov, total_inputs, valid_inputs, valid_paths, valid_branch_sets, unique_valid_inputs");


    }


    /** Returns the banner to be displayed on the status screen */
    protected String getTitle() {
            return  "RL Fuzzing\n" +
                    "--------------------\n";
    }


    /* Saves an interesting input to the queue. */
    protected void saveCurrentInput(Boolean is_valid) throws IOException {
        String valid_str = is_valid ? "_v" : "";
        // First, save to disk (note: we issue IDs to everyone, but only write to disk  if valid)
        int newInputIdx = numSavedInputs++;
        String saveFileName = String.format("id_%06d%s", newInputIdx, valid_str);
        File saveFile = new File(savedInputsDirectory, saveFileName);
        PrintWriter writer = new PrintWriter(saveFile);
        writer.print(currentInput);
        writer.flush();
    }

    /* Saves an interesting input to the queue. */
    protected void saveCurrentFailure() throws IOException {
        int newInputIdx = uniqueFailures.size();
        String saveFileName = String.format("id_%06d", newInputIdx);
        File saveFile = new File(savedFailuresDirectory, saveFileName);
        PrintWriter writer = new PrintWriter(saveFile);
        writer.print(currentInput);
        writer.flush();
    }

    private void displayStats(){
        displayStats(false);
    }

        // Call only if console exists
    private void displayStats(boolean force) {

        Date now = new Date();
        long intervalMilliseconds = now.getTime() - lastRefreshTime.getTime();
        if (!force && intervalMilliseconds < STATS_REFRESH_TIME_PERIOD) {
            return;
        }
        long interlvalTrials = numTrials - lastNumTrials;
        long intervalExecsPerSec = interlvalTrials * 1000L / intervalMilliseconds;
        double intervalExecsPerSecDouble = interlvalTrials * 1000.0 / intervalMilliseconds;
        lastRefreshTime = now;
        lastNumTrials = numTrials;
        long elapsedMilliseconds = now.getTime() - startTime.getTime();
        long execsPerSec = numTrials * 1000L / elapsedMilliseconds;


        int nonZeroCount = totalCoverage.getNonZeroCount();
        double nonZeroFraction = nonZeroCount * 100.0 / totalCoverage.size();
        int nonZeroValidCount = validCoverage.getNonZeroCount();
        double nonZeroValidFraction = nonZeroValidCount * 100.0 / validCoverage.size();

        if (console != null ){
            console.printf("\033[2J");
            console.printf("\033[H");
            console.printf(this.getTitle() + "\n");
            if (this.testName != null) {
                console.printf("Test name:            %s\n", this.testName);
            }
            console.printf("Results directory:    %s\n", this.outputDirectory.getAbsolutePath());
            console.printf("Elapsed time:         %s (%s)\n", millisToDuration(elapsedMilliseconds),
                    maxDurationMillis == Long.MAX_VALUE ? "no time limit" : ("max " + millisToDuration(maxDurationMillis)));
            console.printf("Number of executions: %,d\n", numTrials);
            console.printf("Valid inputs:         %,d (%.2f%%)\n", numValid, numValid*100.0/numTrials);
            console.printf("Unique failures:      %,d\n", uniqueFailures.size());
            console.printf("Execution speed:      %,d/sec now | %,d/sec overall\n", intervalExecsPerSec, execsPerSec);
            console.printf("Total coverage:       %,d branches (%.2f%% of map)\n", nonZeroCount, nonZeroFraction);
            console.printf("Valid coverage:       %,d branches (%.2f%% of map)\n", nonZeroValidCount, nonZeroValidFraction);
            console.printf("Unique valid inputs:  %,d (%.2f%%)\n", uniqueValidInputs.size(),
                    uniqueValidInputs.size()*100.0/numTrials);
            console.printf("Unique valid paths:   %,d \n", uniquePaths.size());
            console.printf("''  non-zero paths:   %,d \n", uniqueBranchSets.size());
        }

        String plotData = String.format("%d, %d, %d, %d, %d, %d, %d, %d, %d",
                TimeUnit.MILLISECONDS.toSeconds(now.getTime()), uniqueFailures.size(), nonZeroCount, nonZeroValidCount,
                numTrials, numValid, uniquePaths.size(), uniqueBranchSets.size(), uniqueValidInputs.size());
        appendLineToFile(statsFile, plotData);

    }

    private void appendLineToFile(File file, String line) throws GuidanceException {
        try (PrintWriter out = new PrintWriter(new FileWriter(file, true))) {
            out.println(line);
        } catch (IOException e) {
            throw new GuidanceException(e);
        }

    }

    @Override
    public Consumer<TraceEvent> generateCallBack(Thread thread) {
        if (appThread != null) {
            throw new IllegalStateException(ZestGuidance.class +
                    " only supports single-threaded apps at the moment");
        }
        appThread = thread;

        return this::handleEvent;
    }

    /** Handles a trace event generated during test execution */
    protected void handleEvent(TraceEvent e) {
        // Collect totalCoverage
        runCoverage.handleEvent(e);
        // Check for possible timeouts every so often
        if (this.singleRunTimeoutMillis > 0 &&
                this.runStart != null && (++this.branchCount) % 10_000 == 0) {
            long elapsed = new Date().getTime() - runStart.getTime();
            if (elapsed > this.singleRunTimeoutMillis) {
                throw new TimeoutException(elapsed, this.singleRunTimeoutMillis);
            }
        }
    }

    private String millisToDuration(long millis) {
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis % TimeUnit.MINUTES.toMillis(1));
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis % TimeUnit.HOURS.toMillis(1));
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        String result = "";
        if (hours > 0) {
            result = hours + "h ";
        }
        if (hours > 0 || minutes > 0) {
            result += minutes + "m ";
        }
        result += seconds + "s";
        return result;
    }

}
