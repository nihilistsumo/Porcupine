#!/bin/bash
#echo "Cloning Git Repository...."
#git clone https://github.com/nihilistsumo/Mongoose.git
#echo "Done"
#cd Mongoose
mvn package
echo "Done compiling and packaging"
#cd ..
echo "Adding necessary configuration files and wordnet database..."
cp /home/sk1105/sumanta/resources/similarity.conf /home/sk1105/sumanta/resources/jawjaw.conf /home/sk1105/sumanta/resources/wnjpn.db .
#cp -r /home/mong/prolog .
jar uf target/Mongoose-0.0.1-SNAPSHOT-jar-with-dependencies.jar similarity.conf jawjaw.conf wnjpn.db
rm similarity.conf jawjaw.conf wnjpn.db
echo "Done"
#cd Mongoose
mkdir porcupine-results
echo "Installation complete"
#echo "Run Porcupine using: ./run.sh"
echo "Results will be saved at porcupine-results"
#java -jar target/Mongoose-0.0.1-SNAPSHOT-jar-with-dependencies.jar
