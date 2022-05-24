# Building and running the game

## Automatically building and running (VSCode on Windows only)

1. Open the project in VSCode
2. Select "Run and Debug" from the left menu
3. Select "Launch Client + Server" from the dropdown menu
4. Run the project. This will open up 1 server and 2 clients on port 8080

## Manually buliding (Windows)

1. Open up a command prompt and navigate to the "tictactoeOnline" directory
2. Run `mkdir build` and `mkdir .tmp` to create the necessary build folders
3. Run `copy stylesheet.css build` to copy the stylesheet to the build folder
4. Run `dir /s /B *.java > .tmp/sources.txt` to collect all the files to compile
    * There cannot be any spaces in the directory path for this to work
5. Run `javac -d ./build -cp ./lib/* @.tmp/sources.txt` to compile all the files

## Manually running (Windows)

1. After compiling, the server can be executed by running `java -cp ./build;./lib/* network.Server <optional port>`
2. The client can be executed by running `java -cp ./build;./lib/* Utictactoe <optional host> <optional port>`  
Note: each program will need to be in its own terminal window.

## Manually buliding (MacOS, Linux)

1. Open up a terminal and navigate to the "tictactoOnline" directory
2. Run `mkdir build` and `mkdir .tmp` to create the necessary build folders
3. Run `cp stylesheet.css build` to copy the stylesheet to the build folder
4. Run `find . -name '*.java' > .tmp/sources.txt` to collect all the files to compile
    * There cannot be any spaces in the directory path for this to work
5. Run `javac -d ./build -cp ./lib/* @.tmp/sources.txt` to compile all the files

## Manually running (MacOS, Linux)

1. After compiling, the server can be executed by running `java -cp ./build:./lib/* network.Server <optional port>`
2. The client can be executed by running `java -cp ./build:./lib/* Utictactoe <optional host> <optional port>`