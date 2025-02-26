package org.hkijena.jipipe.cli;

public class JIPipeCLIHelp {
    public static void showHelp() {
        System.out.println("\n" +
                "                                         \n" +
                "     ,--.,--.,------. ,--.               \n" +
                "     |  ||  ||  .--. '`--' ,---.  ,---.  \n" +
                ",--. |  ||  ||  '--' |,--.| .-. || .-. : \n" +
                "|  '-'  /|  ||  | --' |  || '-' '\\   --. \n" +
                " `-----' `--'`--'     `--'|  |-'  `----' \n" +
                "                          `--'           \n\n");
        System.out.println("JIPipe CLI https://www.jipipe.org/");
        System.out.println("Developed by Applied Systems Biology, HKI Jena, Germany");
        System.out.println();
        System.out.println("run <options>");
        System.out.println("    Runs a project file and writes outputs to the specified directory.");
        System.out.println("    --project <Project file>                                                       Sets the project file to run");
        System.out.println();
        System.out.println("    Optional parameters:");
        System.out.println("    --output-folder <Output folder>                                                Sets the output directory (if not set, will use a temporary directory)");
        System.out.println("    --num-threads <N=1,2,...>                                                      Sets the maximum number of threads for parallelization");
        System.out.println("    --overwrite-parameters <JSON file>                                             Overrides parameters (global and node) from a JSON file (key to value pairing)");
        System.out.println("    --P<Node ID>/<Parameter ID> <Parameter Value (JSON)>                           Overrides one parameter from the specified JSON data");
        System.out.println("    --P/<Global Parameter ID> <Parameter Value (JSON)>                             Overrides a global parameter from the specified JSON data");
        System.out.println("    --overwrite-user-directories <JSON file>                                       Read user directory overrides from a JSON file (object with key to value pairing)");
        System.out.println("    --U<User directory key> <User directory value>                                 Overrides one user directory key with the specified directory");
        System.out.println("    --output-results <all/none/only-compartment-outputs>                           Determines which standard JIPipe outputs are written (default: all)");
        System.out.println();
        System.out.println("render <options>");
        System.out.println("    Renders the pipeline and saves the output to a PNG file.");
        System.out.println("    --project <Project file>                                                       Sets the project file to render");
        System.out.println("    --compartment <Name/UUID>                                                      The name or UUID of the compartment");
        System.out.println("    --output <PNG file>                                                            Sets the output PNG file");
        System.out.println();
        System.out.println("Advanced settings:");
        System.out.println("--verbose                                                                          Print all initialization logs (a lot of text)");
        System.out.println("--profile-dir                                                                      Sets the directory for the JIPipe profile (location of settings, artifacts, etc.)");
        System.out.println("--fast-init                                                                        Skips the validation steps to make the JIPipe initialization faster");
        System.out.println();
        System.out.println("To run this tool, execute following command:");
        System.out.println("<ImageJ executable> --debug --pass-classpath --full-classpath --main-class org.hkijena.jipipe.cli.JIPipeCLIMain");
        System.out.println();
        System.out.println("Environment variables:");
        System.out.println("JIPIPE_OVERRIDE_USER_DIR_BASE                                                      Overrides the base directory where JIPipe looks for profiles (the directory itself will contain sub-directories for the JIPipe version)");
    }
}
