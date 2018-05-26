#!/bin/bash
mvn package
echo "Done compiling and packaging"
#cd ..
echo "Adding necessary configuration files and wordnet database..."
cp /home/sk1105/sumanta/resources/similarity.conf /home/sk1105/sumanta/resources/jawjaw.conf /home/sk1105/sumanta/resources/wnjpn.db .
jar uf target/porcupine-0.0.1-SNAPSHOT.jar similarity.conf jawjaw.conf wnjpn.db
rm similarity.conf jawjaw.conf wnjpn.db
echo "Done"
mkdir porcupine-results
echo "Installation complete"
#echo "Run Porcupine using: ./run.sh"
echo "Results will be saved at porcupine-results"
