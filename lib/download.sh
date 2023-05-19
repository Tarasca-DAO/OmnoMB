fileLicense=LICENSE-2.0.txt
fileLibJson=json-simple-1.1.1.jar

file=$fileLicense
if [ ! -f $file ]; then
 wget -c http://www.apache.org/licenses/$file
fi

file=$fileLibJson
if [ ! -f $file ]; then
 wget -c https://storage.googleapis.com/google-code-archive-downloads/v2/code.google.com/json-simple/$file
fi
