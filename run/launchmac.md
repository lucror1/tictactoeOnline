# Compiling and Running on Mac OS

For the life of me, I cannot figure out how to automate the compilation and launching of multiple classes.
Instructions to run this manually are provided below.

## Compilation

1. In the terminal, navigate to the parent directory of "run". If you run `ls`, the output should include the "run" directory.
2. Run the command `find . -name '*.java' > .tmp/sources.txt`. This will find all of the java files in the directory tree and put their location in ".tmp/sources.txt". WARNING: THIS WILL BREAK IF ANY FOLDER OR FILE HAS A SPACE IN ITS NAME.
3. Run the command `javac -d ./build -cp ./lib/*.jar @.tmp/sources.txt`. This will compile all the files and output them to the "build" directory. Alternatively, "build" can be replaced with whatever build destination you want.

## Running

1. For each of the classes that you want to run, open up a new terminal window.
2. Navigate to the project directory (the directory with the "run" folder in it).
3. Run `java -cp ./build:lib/* [CLASSNAME]`, where "\[CLASSNAME\] should be replaced with the name of the class you want to run.
    - For example, to run the class "Server" in the package "telnet", the command would be `java -cp ./build:lib/* telnet.Server`. 
    - If you changed the location of the build when compiling, you should replace "build" with whatever directory you outputed the compiled files to.