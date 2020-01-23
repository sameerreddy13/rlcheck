# RLCheck: Replication

This document gives the instructions for running the experiments from the RLCheck paper. 

## Alternative

You can also run the replication directly in a docker container.  You can get Docker CE for Ubuntu here: https://docs.docker.com/install/linux/docker-ce/ubuntu. See links on the sidebar for installation on other platforms.

### Load image

To load the artifact on your system, pull the image from the public repo.
```
docker pull carolemieux/rlcheck-artifact
```

### Run container

Run the following to start a container and get a shell in your terminal:

```
docker run --name rlcheck -it carolemieux/rlcheck-artifact
```
read the `README.md` in the base directory for replication instructions.  

## Filesystem

The default directory in the container, `/rlcheck-artifact`, contains the following contents:
- `REPLICATION.md`: This file. 
- `jqf`: This is the main rlcheck implementation used in the evaluation on top of the the Java fuzzing platform JQF (cloned from https://github.com/rohanpadhye/jqf). 
- `bst_example`: python implementation used for the case studies in Section 4 of the paper.
- `scripts`: Contains various scripts used for running experiments and generating figures from the paper.


## Running fresh experiments

### Section 4 Evaluation 

The evaluation of the data from Section 4 involes experiments with **4 fuzzing techniques** on **1 benchmark program**. You can run the experiments with the script:
```
./scripts/run_python_exps.sh RESULTS_DIR REPS
```
Where `RESULTS_DIR` is the name of the directory where results will be saved, and `REPS` is the number of repetitions to perform. 

The results will be put in `RESULTS_DIR/python-data`. If the script finds output for rep `i` it will skip that rep to avoid data loss. Thus, if you run `./scripts/run_python_exps.sh fresh-baked 3` for the first time three reps will be executed. However, if you run `./scripts/run_python_exps.sh fresh-baked 3` *after* having run `./scripts/run_python_exps.sh fresh-baked 1`, only 2 reps will be executed, since the first rep results already exist. 

Each rep takes around 1-2 minutes to execute. In the paper we ran 10 reps; you can run a smaller number (we recommend at least 3-5) depending on your resource constraints

### Main Evaluation 

The main evaluation of this paper involves experiments with **4 fuzzing techniques** on **4 benchmark programs**. The experiments can be launched via `scripts/run_java_exps.sh`, whose usage is as follows:

```
./scripts/run_java_exps.sh RESULTS_DIR REPS
```

Where `RESULTS_DIR` is the name of the directory where results will be saved, and `REPS` is the number of repetitions to perform. The file `experiments.log` will be generated during execution of this script; you can examine it to monitor the progress of the data generation. 

Again, if the script finds output for rep `i` it will skip that rep to avoid data loss. Thus, if you run `./scripts/run_java_exps.sh fresh-baked 3` for the first time three reps will be executed. However, if you run `./scripts/run_java_exps.sh fresh-baked 3` *after* having run `./scripts/run_java_exps.sh fresh-baked 1`, only 2 reps will be executed, since the first rep results already exist. 

Running the above script produces `24 x REPS` sub-directories in `fresh-baked/java-data`, with the naming convention `$TECHNIQUE-$BENCH-$ID(-replay)`, where:
- `BENCH` is one of `ant`, `maven`, `closure`, `rhino`. 
- `TECHNIQUE` is one of `rl`, `rl-blackbox` (symlink to `rl`), `rl-greybox`, `quickcheck`, or `zest`.
- `replay` exists for `rl`, `rl-blackbox` (symlink to `rl-...-replay`), and `quickcheck`
- `ID` is a number between 0 and `REPS-1`, inclusive.


One rep takes around *14-15 hours* on our machine. The base runtime for each rep is (4 techniques) x (4 benchmarks) x 5 minutes = 80 minutes. The two blackbox techniques (RLCheck + QuickCheck) are run without instrumentation. While this allows us to fairly compare them to the greybox techniques (in terms of the speedup blackbox provides), this means we lack data for Figures 6-10. Thus, to collect data for Figures 6-10, we *replay* RLCheck (with the same random seed) and QuickCheck with instrumentation, stopping them after they generate the number of inputs generated in 5 minutes without instrumentation. For the closure benchmark, this replay can take nearly 3 hours. 

For the paper we ran 10 reps. Depending on your resource contraints, you can run a smaller number (at least 1; 3-5 will give results with less variance). 


## Plotting data from experiments


1. Generate plots for Figures 4-5:
```
python3 scripts/gen_fig4_fig5.py OUT_DIR
```
2. Generate plots for Figures 6-8:
```
python3 scripts/gen_fig6_fig7_fig8.py OUT_DIR
```
3. Generate plots for Figures 9-10:
```
python3 scripts/gen_fig9_fi

