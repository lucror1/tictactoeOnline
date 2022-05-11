#!/bin/bash

# Usage is bash launch.sh $OUTPUTDIR $FILESTORUN

# Collect all java files
find . -name '*.java' > .tmp/sources.txt

# Compile the collected files
javac -d ./$1 -cp ./lib/*.jar @.tmp/sources.txt

# Run each class specified
for class in ${@:2}
do
    # Launch the class
    cmd.exe /c start bash -c "cd $PWD; java -cp ./$1:lib/* $class"

    # Sleep a bit to let it load
    sleep 0.5
done