# RLCheck

This project contains the source code for RLCheck, a method for guiding generators with reinforcement learning.

Developed by Sameer Reddy (sameerreddy13) and Caroline Lemieux (carolemieux) on top of Rohan Padhye's [JQF](https://www.github.com/rohanpadhye/jqf).

## Directory structure

* `jqf`: the main RLCheck implementation built on top of JQF
* `bst_example`: a python implementation of RLCheck for a BST example
* `scripts` : contains scripts for running experiments and plotting results

## Build + run RLCheck

To build the main RLCheck go to the jqf folder and run
```mvn package```


You should then be able to run the following command:
```$JQF_DIR/bin/jqf-rl -c [CLASSPATH] TEST_CLASS TEST_METHOD RL_GENERATOR CONFIG_FILE [OUTPUT_DIR]```

Where $JQF_DIR is the location of the `jqf` subdirectory of RLCheck. For example, to run 
```$JQF_DIR/bin/jqf-rl -c $($JQF_DIR/scripts/examples_classpath.sh) edu.berkeley.cs.jqf.examples.maven.ModelReaderTest testWithInputStream edu.berkeley.cs.jqf.examples.xml.XmlRLGenerator $JQF_DIR/configFiles/mavenConfig.json [OUTPUT_DIR]```

**Note**: these commands run the *instrumented* version of RLCheck. While this results in a nice status screen, it also can cause substantial slowdowns on some benchmark. Add the `-n` flag to run an uninstrumented session (no status on increases in coverage, but faster execution), e.g.: `$JQF_DIR/bin/jqf-rl -n -c [CLASSPATH] ...`

## RLCheck Changes to JQF

The implementation of RLCheck on top of JQF in this repo is a prototype. Most of the code, including the base class for the generators as well as the learners, can be found in the `jqf/fuzz/src/main/java/edu/berkeley/cs/jqf/fuzz/rl` directory.

In this prototype implementation, RLCheck generators are not proper JQF generators. This is because (at the time of writing) JQF's Guidance interface only supported providing guidance at the bytestream level. RLCheck, on the other hand, provides guidance directly at the SourceOfRandomness. As such, specialized generators need to be built. The implementation on top of JQF in this repo provides two, notably:
```jqf/examples/src/main/java/edu/berkeley/cs/jqf/examples/xml/XmlRLGenerator.java```
and
```jqf/examples/src/main/java/edu/berkeley/cs/jqf/examples/js/JavaScriptRLGenerator.java```

## Further reading

* Our ICSE 2020 paper on RLCheck, see [author's version](https://www.carolemieux.com/rlcheck_preprint.pdf). 
* `REPLICATION.md` contains instructions to replicate the experiments from the RLCheck paper, including how to load a docker container containing a pre-built RLCheck

