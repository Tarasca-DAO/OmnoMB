#!/bin/bash

rootdir="classes"

# Descargar las dependencias
cd lib || exit
sh downloadv2.sh
cd ..

# Crear el directorio de clases
mkdir "$rootdir"

# Compilar los archivos fuente
find . -name "*.java" > sources.txt
javac -d "$rootdir" -classpath "lib/json-simple-1.1.1.jar" @"sources.txt"
rm sources.txt

# Crear el archivo JAR
jar cfm omno.jar manifest.txt -C "$rootdir" .
rm -r "$rootdir"
