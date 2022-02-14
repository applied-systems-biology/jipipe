package org.hkijena.jipipe.extensions.r;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.LogOutputStream;
import org.apache.commons.exec.PumpStreamHandler;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.parameters.library.pairs.StringQueryExpressionAndStringPairParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.DoubleList;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.IntegerList;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.extensions.parameters.library.primitives.ranges.IntegerRange;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.utils.ProcessUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.scripting.MacroUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class RUtils {
    public static Class<?>[] ALLOWED_PARAMETER_CLASSES = new Class[]{
            String.class,
            Byte.class,
            Short.class,
            Integer.class,
            Double.class,
            Float.class,
            Path.class,
            Boolean.class,
            IntegerList.class,
            StringList.class,
            DoubleList.class,
            IntegerRange.class
    };

    /**
     * Converts annotations into R code.
     *
     * @param code        the code
     * @param annotations the annotations
     */
    public static void annotationsToR(StringBuilder code, Collection<JIPipeTextAnnotation> annotations) {
        code.append("JIPipe.Annotations <- list()\n");
        for (JIPipeTextAnnotation annotation : annotations) {
            code.append(String.format("Annotations$\"%s\" <- \"%s\"\n", MacroUtils.escapeString(annotation.getName()),
                    MacroUtils.escapeString(annotation.getValue())));
        }
    }

    /**
     * Converts parameters into R code. Only parameters in ALLOWED_PARAMETER_CLASSES are converted.
     * All parameters are also available in a list JIPipe.Variables
     *
     * @param code                the code
     * @param parameterCollection the parameters
     */
    public static void parametersToR(StringBuilder code, JIPipeParameterCollection parameterCollection) {
        JIPipeParameterTree tree = new JIPipeParameterTree(parameterCollection);
        code.append("JIPipe.Variables <- list()\n");
        for (Map.Entry<String, JIPipeParameterAccess> entry : tree.getParameters().entrySet()) {
            String value = null;

            Object o = entry.getValue().get(Object.class);
            if (o instanceof String) {
                value = "\"" + MacroUtils.escapeString((String) o) + "\"";
            } else if (o instanceof Path) {
                value = "\"" + MacroUtils.escapeString("" + o) + "\"";
            } else if (o instanceof Number) {
                if (o instanceof Float) {
                    float num = (float) o;
                    if (Float.isNaN(num)) {
                        value = "NaN";
                    } else if (num == Float.POSITIVE_INFINITY) {
                        value = "Inf";
                    } else if (num == Float.NEGATIVE_INFINITY) {
                        value = "-Inf";
                    } else {
                        value = "" + num;
                    }
                } else if (o instanceof Double) {
                    double num = (double) o;
                    if (Double.isNaN(num)) {
                        value = "NaN";
                    } else if (num == Double.POSITIVE_INFINITY) {
                        value = "Inf";
                    } else if (num == Double.NEGATIVE_INFINITY) {
                        value = "-Inf";
                    } else {
                        value = "" + num;
                    }
                } else {
                    value = "" + o;
                }
            } else if (o instanceof Boolean) {
                value = (boolean) o ? "TRUE" : "FALSE";
            } else if (o instanceof IntegerList) {
                value = "c(" + ((IntegerList) o).stream().map(i -> i + "").collect(Collectors.joining(", ")) + ")";
            } else if (o instanceof DoubleList) {
                value = "c(" + ((DoubleList) o).stream().map(i -> i + "").collect(Collectors.joining(", ")) + ")";
            } else if (o instanceof IntegerRange) {
                value = "c(" + ((IntegerRange) o).getIntegers(0, 0).stream().map(i -> i + "").collect(Collectors.joining(", ")) + ")";
            } else if (o instanceof StringList) {
                value = "c(" + ((StringList) o).stream().map(s -> "\"" + MacroUtils.escapeString(s) + "\"").collect(Collectors.joining(", ")) + ")";
            }

            if (value != null) {
                if (MacroUtils.isValidVariableName(entry.getKey())) {
                    code.append(entry.getKey()).append(" <- ").append(value).append("\n");
                }
                code.append("JIPipe.Variables$\"").append(entry.getKey()).append("\" <- ").append(value).append("\n");
            }
        }
    }

    public static void inputSlotsToR(StringBuilder code, Map<String, Path> inputSlotPaths, List<JIPipeDataSlot> inputSlots) {

        Map<String, JIPipeDataSlot> inputSlotMap = new HashMap<>();
        for (JIPipeDataSlot outputSlot : inputSlots) {
            inputSlotMap.put(outputSlot.getName(), outputSlot);
        }

        code.append("JIPipe.InputSlotFolders <- list()\n");
        code.append("JIPipe.InputSlotRowCounts <- list()\n");
        code.append("JIPipe.InputSlotRowAnnotations <- list()\n");
        for (Map.Entry<String, Path> entry : inputSlotPaths.entrySet()) {
            JIPipeDataSlot slot = inputSlotMap.get(entry.getKey());
            String escapedKey = MacroUtils.escapeString(entry.getKey());
            code.append("JIPipe.InputSlotFolders$\"").append(escapedKey).append("\" <- \"")
                    .append(MacroUtils.escapeString(entry.getValue() + "")).append("\"\n");
            code.append("JIPipe.InputSlotRowCounts$\"").append(escapedKey).append("\" <- ").append(slot.getRowCount()).append("\n");
            code.append("JIPipe.InputSlotRowAnnotations$\"").append(escapedKey).append("\" <- list()\n");
            StringBuilder stringBuilder = new StringBuilder();
            for (int row = 0; row < slot.getRowCount(); row++) {
                stringBuilder.setLength(0);
                for (JIPipeTextAnnotation annotation : slot.getAnnotations(row)) {
                    if (stringBuilder.length() > 0)
                        stringBuilder.append(", ");
                    stringBuilder.append("\"")
                            .append(MacroUtils.escapeString(annotation.getName()))
                            .append("\" = \"")
                            .append(MacroUtils.escapeString(annotation.getValue()))
                            .append("\"");
                }
                code.append("JIPipe.InputSlotRowAnnotations$\"").append(escapedKey).append("\"[[")
                        .append(row).append(1).append("]] <- list(").append(stringBuilder).append(")\n");
            }
        }

        // The getter function
        code.append("JIPipe.GetInputFolder <- function(slot, row=0) { " +
                "return(file.path(JIPipe.InputSlotFolders[[slot]], row)) " +
                "}\n");
    }

    public static void outputSlotsToR(StringBuilder code, List<JIPipeDataSlot> outputSlots, Map<String, Path> outputSlotPaths) {

        Map<String, JIPipeDataSlot> outputSlotMap = new HashMap<>();
        for (JIPipeDataSlot outputSlot : outputSlots) {
            outputSlotMap.put(outputSlot.getName(), outputSlot);
        }

        code.append("JIPipe.OutputSlotFolders <- list()\n");
        code.append("JIPipe.OutputSlots.Table <- list()\n");
        for (Map.Entry<String, Path> entry : outputSlotPaths.entrySet()) {
            code.append("JIPipe.OutputSlotFolders$\"").append(MacroUtils.escapeString(entry.getKey())).append("\" <- \"")
                    .append(MacroUtils.escapeString(entry.getValue() + "")).append("\"\n");
            code.append("JIPipe.OutputSlots.Table$\"").append(MacroUtils.escapeString(entry.getKey())).append("\" <- ")
                    .append("list(rows=list(), ").append("slot=\"").append(MacroUtils.escapeString(entry.getKey()))
                    .append("\", ").append("\"data-type\"=\"")
                    .append(MacroUtils.escapeString(JIPipeDataInfo.getInstance(outputSlotMap.get(entry.getKey()).getAcceptedDataType()).getId())).append("\")\n");
        }

        // The getter function
        code.append("JIPipe.AddOutputFolder <- function(slot, annotations=list()) { " +
                "count <- length(JIPipe.OutputSlots.Table[[slot]][[\"rows\"]]);" +
                "result <- file.path(JIPipe.OutputSlotFolders[[slot]], count); " +
                "dir.create(result); " +
                "data.type <- JIPipe.OutputSlots.Table[[slot]][[\"data-type\"]]; " +
                ".GlobalEnv$JIPipe.OutputSlots.Table[[slot]][[\"rows\"]][[count + 1]] <- list(index=count, " +
                "annotations=annotations, " +
                "\"true-data-type\"=data.type); " +
                "return(result);" +
                "}\n");
    }

    public static void installInputLoaderCode(StringBuilder code) {
        code.append("JIPipe.GetInputAsDataFrame <- function(slot, row=0) { " +
                "folder <- JIPipe.GetInputFolder(slot, row=row);" +
                "file.name <- list.files(folder, pattern=glob2rx(\"*.csv\"))[1];" +
                "return(read.csv(file.path(folder, file.name))); " +
                "}\n");
    }

    public static void installOutputGeneratorCode(StringBuilder code) {
        code.append("JIPipe.AddOutputDataFrame <- function(data, slot, annotations=list()) { " +
                "folder <- JIPipe.AddOutputFolder(slot, annotations=annotations);" +
                "write.csv(data, file=file.path(folder, \"data.csv\"));" +
                "}\n");
        code.append("JIPipe.AddOutputPNGImagePath <- function(data, slot, annotations=list()) { " +
                "folder <- JIPipe.AddOutputFolder(slot, annotations=annotations);" +
                "return(file.path(folder, \"data.png\"));" +
                "}\n");
        code.append("JIPipe.AddOutputTIFFImagePath <- function(data, slot, annotations=list()) { " +
                "folder <- JIPipe.AddOutputFolder(slot, annotations=annotations);" +
                "return(file.path(folder, \"data.tif\"));" +
                "}\n");
    }

    public static void installPostprocessorCode(StringBuilder code) {
        code.append("if(!require(rjson)) { install.packages(\"rjson\"); library(rjson); }\n");
        code.append("for(slot in names(JIPipe.OutputSlots.Table)) { " +
                "folder <- JIPipe.OutputSlotFolders[[slot]]; " +
                "data <- JIPipe.OutputSlots.Table[[slot]];" +
                "writeLines(toJSON(data, indent=4), con=file.path(folder, \"data-table.json\"));" +
                "}\n");
    }

    public static void runR(String script, REnvironment environment, JIPipeProgressInfo progressInfo) {
        Path codeFilePath = RuntimeSettings.generateTempFile("R", ".R");
        try {
            Files.write(codeFilePath, script.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        runR(codeFilePath, environment, progressInfo);
    }

    public static void runR(Path scriptFile, REnvironment environment, JIPipeProgressInfo progressInfo) {
        Path rExecutable = environment.getRScriptExecutablePath();
        CommandLine commandLine = new CommandLine(rExecutable.toFile());

        Map<String, String> environmentVariables = new HashMap<>();
        ExpressionVariables existingEnvironmentVariables = new ExpressionVariables();
        for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
            existingEnvironmentVariables.put(entry.getKey(), entry.getValue());
            environmentVariables.put(entry.getKey(), entry.getValue());
        }
        for (StringQueryExpressionAndStringPairParameter environmentVariable : environment.getEnvironmentVariables()) {
            String value = StringUtils.nullToEmpty(environmentVariable.getKey().evaluate(existingEnvironmentVariables));
            environmentVariables.put(environmentVariable.getValue(), value);
        }
        for (Map.Entry<String, String> entry : environmentVariables.entrySet()) {
            progressInfo.log("Setting environment variable " + entry.getKey() + "=" + entry.getValue());
        }

        ExpressionVariables parameters = new ExpressionVariables();
        parameters.set("script_file", scriptFile.toString());
        parameters.set("r_executable", rExecutable.toString());
        Object evaluationResult = environment.getArguments().evaluate(parameters);
        for (Object item : (Collection<?>) evaluationResult) {
            commandLine.addArgument(StringUtils.nullToEmpty(item));
        }
        progressInfo.log("Running R: " + Arrays.stream(commandLine.toStrings()).map(s -> {
            if (s.contains(" ")) {
                return "\"" + MacroUtils.escapeString(s) + "\"";
            } else {
                return MacroUtils.escapeString(s);
            }
        }).collect(Collectors.joining(" ")));

        LogOutputStream progressInfoLog = new LogOutputStream() {
            @Override
            protected void processLine(String s, int i) {
                progressInfo.log(s);
            }
        };

        ProcessUtils.ExtendedExecutor executor = new ProcessUtils.ExtendedExecutor(ExecuteWatchdog.INFINITE_TIMEOUT, progressInfo);
        executor.setStreamHandler(new PumpStreamHandler(progressInfoLog, progressInfoLog));

        try {
            executor.execute(commandLine, environmentVariables);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
