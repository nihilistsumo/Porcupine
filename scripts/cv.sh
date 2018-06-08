#!/bin/bash

#trecdir=/home/sumanta/Documents/Mongoose-data/trec-data/benchmarkY1-train
trecdir=/home/sk1105/sumanta/cs980assign/benchmarkY1/benchmarkY1-train
#jardir=/home/sumanta/git/Mongoose/target
jardir=/home/sk1105/sumanta/porcupine-cv-new/Porcupine/target
#rlib=~/Softwares
rlib=/home/sk1105/sumanta
jarname=porcupine-0.0.1-SNAPSHOT-jar-with-dependencies.jar

# arguments: 1. directory path to runfiles, 2. output directory path, 3. parasim qrels directory path, 4. ret no in combined run file

mkdir $2/runs-set1
mkdir $2/runs-set2

echo "Splitting data..."

java -jar $jardir/$jarname split $1 $trecdir/titles $2/runs-set1 $2/runs-set2

# Set 1 as train, Set 2 as test

echo "Set 1 as train, Set 2 as test"
echo "Creating fet file"
java -jar $jardir/$jarname cmb $2/runs-set1 $2/parasim-paratext-tr1tst2-fet $3
echo "Fet file created"
java -jar $rlib/RankLib-2.1-patched.jar -train $2/parasim-paratext-tr1tst2-fet -ranker 4 -metric2t MAP -save $2/parasim-paratext-tr1tst2-model
echo "Combining..."
java -jar $jardir/$jarname cmbrun $2/runs-set2 $2/parasim-paratext-tr1tst2-model $2/parasim-paratext-tr1tst2-comb-run 200
java -jar $jardir/$jarname cmbrun $2/runs-set1 $2/parasim-paratext-tr1tst2-model $2/parasim-paratext-tr1tst1-comb-run 200
echo "Combined"

# Set 2 as train, Set 1 as test

echo "Set 2 as train, Set 1 as test"
echo "Creating fet file"
java -jar $jardir/$jarname cmb $2/runs-set2 $2/parasim-paratext-tr2tst1-fet $3
echo "Fet file created"
java -jar $rlib/RankLib-2.1-patched.jar -train $2/parasim-paratext-tr2tst1-fet -ranker 4 -metric2t MAP -save $2/parasim-paratext-tr2tst1-model
echo "Combining..."
java -jar $jardir/$jarname cmbrun $2/runs-set1 $2/parasim-paratext-tr2tst1-model $2/parasim-paratext-tr2tst1-comb-run 200
java -jar $jardir/$jarname cmbrun $2/runs-set2 $2/parasim-paratext-tr2tst1-model $2/parasim-paratext-tr2tst2-comb-run 200
echo "Combined"

# Concat two combined run files
cat $2/parasim-paratext-tr1tst2-comb-run $2/parasim-paratext-tr2tst1-comb-run >> $2/parasim-paratext-comb-test-run
cat $2/parasim-paratext-tr1tst1-comb-run $2/parasim-paratext-tr2tst2-comb-run >> $2/parasim-paratext-comb-train-run
echo "Complete! Final run file stored at $2/parasim-paratext-comb-train/test-run"