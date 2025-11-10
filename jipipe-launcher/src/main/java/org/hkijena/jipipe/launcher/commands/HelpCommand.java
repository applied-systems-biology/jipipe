package org.hkijena.jipipe.launcher.commands;

public class HelpCommand {
    public static void showHelp() {
        System.out.println("\n" +
                "\n" +
                "                                                                                                    \n" +
                "  -----        ------   ------                                                                      \n" +
                "  ------------ ------ ----  ----                                                                    \n" +
                "  -----    --- ---------     ---             ##  ##   ######      ##                                \n" +
                "           ---       ---     ---             ##  ##   ##    ####  ##                                \n" +
                "           ---  ---  ---  ------             ##  ##   ##      ##          ###         ##            \n" +
                "           --- ----- --- -----               ##  ##   ##      ##  ##  ####   ##    ###  ###         \n" +
                "           ---  ---  ---                     ##  ##   ##     ###  ##  ##      ##  ##      ##        \n" +
                "    -      ---  ---  ---                     ##  ##   ########    ##  ##       ## ##########        \n" +
                "   ---     ---  ---  ---                     ##  ##   ##          ##  ##      ##  ##                \n" +
                "   ---     ---  ---  ---              ##    ##   ##   ##          ##  ###    ###  ###    ###        \n" +
                "   ---     ---  --- ------              #####    ##   ##          ##  ## #####      ######          \n" +
                "    ---------   --- ------                                            ##                            \n" +
                "       ----      -- ------                                            ##                            \n" +
                "                                                                                                    \n" +
                "\n\n");
        System.out.println("JIPipe CLI https://www.jipipe.org/");
        System.out.println("Developed by Applied Systems Biology, HKI Jena, Germany");
        System.out.println();
        System.out.println("<no arguments>");
        System.out.println("    Runs the JIPipe GUI");
        System.out.println("gui");
        System.out.println("    Runs the JIPipe GUI");
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
        System.out.println("    --overwrite-user-paths <JSON file>                                             Read user path overrides from a JSON file (object with key to value pairing)");
        System.out.println("    --overwrite-user-directories <JSON file>                                       Read user path overrides from a JSON file (object with key to value pairing). Deprecated.");
        System.out.println("    --U<User path key> <User path value>                                           Overrides one user path key with the specified path");
        System.out.println("    --output-results <all/none/only-compartment-outputs>                           Determines which standard JIPipe outputs are written (default: all)");
        System.out.println();
        System.out.println("    Advanced settings:");
        System.out.println("    --verbose                                                                      Print all initialization logs (a lot of text)");
        System.out.println("    --profile-dir                                                                  Sets the directory for the JIPipe profile (location of settings, artifacts, etc.)");
        System.out.println("    --fast-init                                                                    Skips the validation steps to make the JIPipe initialization faster");
        System.out.println();
        System.out.println("render <options>");
        System.out.println("    Renders the pipeline and saves the output to a PNG file.");
        System.out.println("    --project <Project file>                                                       Sets the project file to render");
        System.out.println("    --compartment <Name/UUID>                                                      The name or UUID of the compartment");
        System.out.println("    --output <PNG file>                                                            Sets the output PNG file");
        System.out.println();
        System.out.println("install-artifacts <options>");
        System.out.println("    Downloads and deploys all compatible artifacts for the current system.");
        System.out.println("    Set JIPIPE_OVERRIDE_USER_ARTIFACTS_DIR to control where the artifacts are installed.");
        System.out.println("    --deploy <all|none|readonly>                                                   Determines if deployment scripts are run after installation. 'all' = Deploy all, 'readonly' = Skip for artifacts that don't support read-only deployment. Defaults to 'none'.");
        System.out.println("    --query <group>.<artifact>:<version>-<classifiers>                             If provided, set the query. Can be used multiple times to add multiple queries. Supports wildcards. Defaults to '*' (download all compatible)");
        System.out.println();
        System.out.println("Environment variables:");
        System.out.println("JIPIPE_OVERRIDE_USER_DIR_BASE                                                      Overrides the base directory where JIPipe looks for profiles (the directory itself will contain sub-directories for the JIPipe version)");
        System.out.println("JIPIPE_OVERRIDE_USER_ARTIFACTS_DIR                                                 Overrides the base directory where JIPipe looks for artifacts (if read-only, all utilized artifacts must be pre-installed!)");
        System.out.println("JIPIPE_OVERRIDE_SHARED_DIR                                                         Overrides the base directory where JIPipe puts miscellaneous files (e.g., CEF)");
        System.out.println("JIPIPE_OVERRIDE_SYSTEM_ARTIFACTS_DIR                                               Overrides the base directory where JIPipe looks for system-provided artifacts (read-only)");
    }
}
