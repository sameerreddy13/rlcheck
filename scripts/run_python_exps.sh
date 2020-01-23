#!/bin/bash

if [ "$#" -ne 2 ]; then
        echo "Usage: $0 out_dir num_reps"
        exit 1
fi

OUT_DIR=$1/python-data
NUM_REPS=$2
mkdir -p $OUT_DIR

SCRIPT_DIRNAME="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
PYTHON_FILE=$SCRIPT_DIRNAME/../rlcheck/bst_example/bst_fuzz.py


echo "Script start at $(date)"
for REP in $(seq 0 $((NUM_REPS - 1 ))); do
	echo "========= Starting rep: $REP =========="
	RES_FILE=$OUT_DIR/results-$REP.txt
	if [ -f $RES_FILE ]; then
		echo "Result file $RES_FILE already exists. skipping this rep. Remove results file to force re-run."
	else
		python3 $PYTHON_FILE > $RES_FILE
		echo "Done rep $REP; results in $RES_FILE"
	fi
done
echo "Script end at $(date)"
