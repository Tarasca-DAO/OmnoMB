#!/bin/bash

fileLicense="LICENSE-2.0.txt"
fileLibJson="json-simple-1.1.1.jar"

download_file() {
    local url="$1"
    local file="$2"

    if [ ! -f "$file" ]; then
        wget -c "$url" -O "$file"
    fi
}

download_file "http://www.apache.org/licenses/$fileLicense" "$fileLicense"
download_file "https://storage.googleapis.com/google-code-archive-downloads/v2/code.google.com/json-simple/$fileLibJson" "$fileLibJson"
