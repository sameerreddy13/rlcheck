#!/bin/bash

if [ "$#" -ne 2 ]; then
	echo "Usage: $0 out_dir num_reps"
	exit 1
fi

if [ -z "$JQF_DIR" ]; then
	echo "Error: JQF_DIR must be set"
	exit 1
fi

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
JQF_DIR=$DIR/../jqf

BASE_OUT_DIR=$1
NUM_REPS=$2
OUT_DIR=$BASE_OUT_DIR/java-data
mkdir -p $OUT_DIR
LOG_FILE=experiments.log
touch $LOG_FILE
echo "Start time: $(date)" > $LOG_FILE
echo "Experiment settings: writing to $OUT_DIR, doing $NUM_REPS repetitions" >> $LOG_FILE

BENCHMARKS=(ant maven closure rhino)
TEST_CLASSES=(ant.ProjectBuilderTest maven.ModelReaderTest closure.CompilerTest rhino.CompilerTest)
TEST_GENS=(edu.berkeley.cs.jqf.examples.xml.XmlRLGenerator edu.berkeley.cs.jqf.examples.xml.XmlRLGenerator edu.berkeley.cs.jqf.examples.js.JavaScriptRLGenerator edu.berkeley.cs.jqf.examples.js.JavaScriptRLGenerator)

TEST_METHOD_ZEST=(testWithInputStreamGenerator testWithInputStreamGenerator testWithGenerator testWithGenerator)


dir_does_not_exist() {
  if [ -d $1 ]; then
  	echo "$1 already exists, I won't re-run this experiment. Delete the directory and re-run the script if you want me to" >> $LOG_FILE
	return 1
   else
	return 0
   fi
}

trap "trap - SIGTERM && killall java && echo 'Terminated' >> $LOG_FILE && exit " SIGINT SIGTERM EXIT

for bench_index in {0..3}; do
	BENCHMARK=${BENCHMARKS[$bench_index]}
	TEST_CLASS=edu.berkeley.cs.jqf.examples.${TEST_CLASSES[$bench_index]}
	TEST_METHOD_RL=testWithInputStream
	TEST_METHOD_ZQC=${TEST_METHOD_ZEST[$bench_index]}
	TEST_GEN=${TEST_GENS[$bench_index]}
	CONFIG_FILE=${BENCHMARK}Config.json
	echo "======= Starting benchmark: $BENCHMARK =======" >> $LOG_FILE
	for REP in $(seq 0 $((NUM_REPS - 1))); do
		echo "----- REP: $REP (started at $(date)) -----" >> $LOG_FILE
		
		# First run the blackbox RLCheck
		DIRNAME=${OUT_DIR}/rl-$BENCHMARK-$REP
		if  dir_does_not_exist $DIRNAME ; then
			NEW_CONFIG=${DIRNAME}-${CONFIG_FILE}
			echo "{\"params\": [ { \"name\":\"seed\", \"type\":\"long\", \"val\": $RANDOM }," > $NEW_CONFIG
			tail -n+2 $JQF_DIR/configFiles/$CONFIG_FILE >> $NEW_CONFIG
			timeout 300 $JQF_DIR/bin/jqf-rl -n -c $($JQF_DIR/scripts/examples_classpath.sh) $TEST_CLASS $TEST_METHOD_RL $TEST_GEN $NEW_CONFIG $DIRNAME &
		        PID=$!
			wait $PID	
			ln -s rl-$BENCHMARK-$REP $OUT_DIR/rl-blackbox-$BENCHMARK-$REP
			echo "[$(date)] Finished regular RLCheck. Staring replay to collect instrumentation data." >> $LOG_FILE
		fi
		
		REPLAYNAME=$DIRNAME-replay
		if  dir_does_not_exist $REPLAYNAME ; then
			NEW_CONFIG=${DIRNAME}-${CONFIG_FILE}
			REPLAYNUM=$(tail -n 1 $DIRNAME/plot_data | awk -F', ' '{print $5}')
			$JQF_DIR/bin/jqf-rl -N $REPLAYNUM -c $($JQF_DIR/scripts/examples_classpath.sh) $TEST_CLASS $TEST_METHOD_RL $TEST_GEN $NEW_CONFIG $REPLAYNAME &

		        PID=$!
			wait $PID	
			ln -s rl-$BENCHMARK-$REP-replay $OUT_DIR/rl-blackbox-$BENCHMARK-$REP-replay
			echo "[$(date)] Finished RLCheck replay." >> $LOG_FILE
		fi

		# Then QuickCheck
		DIRNAME=${OUT_DIR}/quickcheck-$BENCHMARK-$REP
		if  dir_does_not_exist $DIRNAME ; then
			timeout 300 $JQF_DIR/bin/jqf-quickcheck -n -c $($JQF_DIR/scripts/examples_classpath.sh) $TEST_CLASS $TEST_METHOD_ZQC $DIRNAME &
		        PID=$!
			wait $PID	
			echo "[$(date)] Finished regular QuickCheck. Staring replay to collect instrumentation data." >> $LOG_FILE
		fi
		REPLAYNAME=$DIRNAME-replay
		if  dir_does_not_exist $REPLAYNAME ; then
			REPLAYNUM=$(tail -n 1 $DIRNAME/plot_data | awk -F', ' '{print $5}')
			$JQF_DIR/bin/jqf-quickcheck -N $REPLAYNUM -c $($JQF_DIR/scripts/examples_classpath.sh) $TEST_CLASS $TEST_METHOD_ZQC $REPLAYNAME & 
		        PID=$!
			wait $PID	
			echo "[$(date)] Finished QuickCheckCheck replay." >> $LOG_FILE
		fi

		# Finally done with the replaying ones... Let's do Zest..
		DIRNAME=${OUT_DIR}/zest-$BENCHMARK-$REP
		if  dir_does_not_exist $DIRNAME ; then
			timeout 300 $JQF_DIR/bin/jqf-quickcheck -c $($JQF_DIR/scripts/examples_classpath.sh) $TEST_CLASS $TEST_METHOD_ZQC $DIRNAME &
		        PID=$!
			wait $PID	
			echo "[$(date)] Finished Zest. No need to replay." >> $LOG_FILE
		fi

		# Finally, greybox RLCheck
		DIRNAME=${OUT_DIR}/rl-greybox-$BENCHMARK-$REP
		if  dir_does_not_exist $DIRNAME ; then
			CONFIG=$JQF_DIR/configFiles/$CONFIG_FILE
			JVM_OPTS="$JVM_OPTS -Drl.guidance.USE_GREYBOX=true" timeout 300 $JQF_DIR/bin/jqf-rl -c $($JQF_DIR/scripts/examples_classpath.sh) $TEST_CLASS $TEST_METHOD_RL $TEST_GEN $CONFIG $DIRNAME &
		        PID=$!
			wait $PID	
			echo "[$(date)] Finished Greybox RLCheck. No need to replay." >> $LOG_FILE
		fi

	done # Done rep
done # Done bench

echo "======= End of script reached at $(date) =======" >> $LOG_FILE

	
