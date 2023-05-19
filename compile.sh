rootdir=classes
cd lib
sh download.sh
cd ../
mkdir $rootdir
find -name "*.java" > sources.txt
javac -d $rootdir -classpath lib/json-simple-1.1.1.jar @sources.txt
rm sources.txt
jar cfm omno.jar manifest.txt -C "$rootdir" .
rm $rootdir -R