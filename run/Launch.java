package run;

public class Launch {
    // Set where the class files should be put
    public static final String BUILD_DIRECTORY = "build";
    // Set which classes should be run
    // Order matters. The first will be run first, then the second, and so on
    public static final String[] CLASS_NAMES = {
        // Put class names here with full package name to run
        // Ex. to run this, put "run.Launch"
        // Then go to the "Run and Debug" menu in VSCode (left side of screen)
        // Select "Launch Client + Server" from the drop down in the menu
        "network.Server",
        "Utictactoe.java",
        "Utictactoe.java"
    };

    // Generated from CLASS_NAMES
    private static final String STRING_CLASS_NAMES = String.join(" ", CLASS_NAMES);

    public static void main(String[] args) throws Exception {
        String osName = System.getProperty("os.name");

        // Detect if this is Windows
        if (osName.toLowerCase().contains("windows")) {
            runWindows();
        }
        // Detect if this is Mac
        else if (osName.toLowerCase().contains("mac")) {
            runMac();
        }
        // Assume Linux if all else fails
        else {
            runLinux();
        }
    }

    public static void runWindows() throws Exception {
        // Init a process builder
        ProcessBuilder builder = new ProcessBuilder();

        // Gather all java files
        // waitFor() forces the thread to wait for all the files
        builder.command("cmd.exe /c dir /s /B *.java > .tmp/sources.txt".split(" "));
        builder.start().waitFor();

        // Compile the collected java files
        // waitFor() forces the compilation to finish before running
        builder.command(
            "javac",
            "-d", "./" + BUILD_DIRECTORY,
            "-cp", "./lib/*.jar",
            "@.tmp/sources.txt"
        );
        builder.start().waitFor();

        // Run the specified classes
        // waitFor() is not used because that would be problematic
        for (String className : CLASS_NAMES) {
            // Run the file
            // This one liner is comprised of 3 parts
            // String.format - replaces the "%s" with the current class to run
            // split - splits the string into an array so it can be passed to the builder
            // builder.command - update the builder with the new command
            builder.command(String.format("cmd.exe /k start /wait java -cp ./%s;lib/* %s", BUILD_DIRECTORY, className).split(" "));
            builder.start();

            // Sleep for a bit to allow it the launched class to fully load
            Thread.sleep(500);
        }
    }

    public static void runMac() throws Exception {
        // Init a process builder
        ProcessBuilder builder = new ProcessBuilder();
        builder.command("cat launchmac.md".split(" "));
        builder.start();
    }

    public static void runLinux() throws Exception {

        // Init a process builder
        ProcessBuilder builder = new ProcessBuilder();
        //builder.command("bash run/launchwsl.sh build".split(" "));

        // Generate the command in the form "bash run/launchwsl.sh" <OUPTUT DIR> <CLASS NAMES>
        builder.command(String.format("bash run/launchwsl.sh build %s", STRING_CLASS_NAMES).split(" "));
        builder.start();
    }
}
