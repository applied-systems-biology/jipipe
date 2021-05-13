package org.hkijena.jipipe.extensions.python;

import org.apache.commons.exec.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.WordUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameters;
import org.hkijena.jipipe.extensions.environments.PythonEnvironment;
import org.hkijena.jipipe.extensions.parameters.generators.IntegerRange;
import org.hkijena.jipipe.extensions.parameters.pairs.StringQueryExpressionAndStringPairParameter;
import org.hkijena.jipipe.extensions.parameters.primitives.DoubleList;
import org.hkijena.jipipe.extensions.parameters.primitives.IntegerList;
import org.hkijena.jipipe.extensions.parameters.primitives.StringList;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.utils.JIPipeRunCancellationExecuteWatchdog;
import org.hkijena.jipipe.utils.MacroUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PythonUtils {

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
    private static Path PYTHON_ADAPTER_PATH;

    private PythonUtils() {

    }

    /**
     * Converts annotations into Python code.
     *
     * @param code        the code
     * @param annotations the annotations
     */
    public static void annotationsToPython(StringBuilder code, Collection<JIPipeAnnotation> annotations) {
        code.append("jipipe_annotations = {}\n");
        for (JIPipeAnnotation annotation : annotations) {
            code.append("jipipe_annotations[\"").append(MacroUtils.escapeString(annotation.getName())).append("\"] = ")
                    .append("\"").append(annotation.getValue()).append("\"\n");
        }
    }

    /**
     * Converts parameters into Python code. Only parameters in ALLOWED_PARAMETER_CLASSES are converted.
     * All parameters are also available in a list JIPipe.Variables
     *
     * @param code                the code
     * @param parameterCollection the parameters
     */
    public static void parametersToPython(StringBuilder code, JIPipeParameterCollection parameterCollection) {
        JIPipeParameterTree tree = new JIPipeParameterTree(parameterCollection);
        code.append("jipipe_variables = {}\n");
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
                        value = "float(\"nan\")";
                    } else if (num == Float.POSITIVE_INFINITY) {
                        value = "float(\"inf\")";
                    } else if (num == Float.NEGATIVE_INFINITY) {
                        value = "float(\"-inf\")";
                    } else {
                        value = "" + num;
                    }
                } else if (o instanceof Double) {
                    double num = (double) o;
                    if (Double.isNaN(num)) {
                        value = "float(\"nan\")";
                    } else if (num == Double.POSITIVE_INFINITY) {
                        value = "float(\"inf\")";
                    } else if (num == Double.NEGATIVE_INFINITY) {
                        value = "float(\"-inf\")";
                    } else {
                        value = "" + num;
                    }
                } else {
                    value = "" + o;
                }
            } else if (o instanceof Boolean) {
                value = (boolean) o ? "True" : "False";
            } else if (o instanceof IntegerList) {
                value = "[" + ((IntegerList) o).stream().map(i -> i + "").collect(Collectors.joining(", ")) + "]";
            } else if (o instanceof DoubleList) {
                value = "[" + ((DoubleList) o).stream().map(i -> i + "").collect(Collectors.joining(", ")) + "]";
            } else if (o instanceof IntegerRange) {
                value = "[" + ((IntegerRange) o).getIntegers().stream().map(i -> i + "").collect(Collectors.joining(", ")) + "]";
            } else if (o instanceof StringList) {
                value = "[" + ((StringList) o).stream().map(s -> "\"" + MacroUtils.escapeString(s) + "\"").collect(Collectors.joining(", ")) + "]";
            }

            if (value != null) {
                if (MacroUtils.isValidVariableName(entry.getKey())) {
                    code.append(entry.getKey()).append(" = ").append(value).append("\n");
                }
                code.append("jipipe_variables[\"").append(entry.getKey()).append("\"] = ").append(value).append("\n");
            }
        }
    }

    /**
     * Gets the current path of the Python adapter. Extracts the adapter if needed.
     *
     * @return the path
     */
    public static Path getPythonAdapterPath() {
        if (PYTHON_ADAPTER_PATH == null || !Files.isDirectory(PYTHON_ADAPTER_PATH)) {
            Path tempDir = RuntimeSettings.generateTempDirectory("python-adapter");
            Reflections reflections = new Reflections(new ConfigurationBuilder()
                    .setUrls(ClasspathHelper.forPackage("org.hkijena.jipipe"))
                    .setScanners(new ResourcesScanner()));
            Set<String> allResources = reflections.getResources(Pattern.compile(".*"));
            allResources = allResources.stream().map(s -> {
                if (!s.startsWith("/"))
                    return "/" + s;
                else
                    return s;
            }).collect(Collectors.toSet());
            String globalFolder = "/org/hkijena/jipipe/extensions/python/adapter";
            Set<String> toInstall = allResources.stream().filter(s -> s.startsWith(globalFolder)).collect(Collectors.toSet());
            for (String resource : toInstall) {
                Path targetPath = tempDir.resolve(resource.substring(globalFolder.length() + 1));
                if (!Files.isDirectory(targetPath.getParent())) {
                    try {
                        Files.createDirectories(targetPath.getParent());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                try {
                    Files.copy(PythonUtils.class.getResourceAsStream(resource), targetPath);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            PYTHON_ADAPTER_PATH = tempDir;
        }
        return PYTHON_ADAPTER_PATH;
    }

    public static void installAdapterCodeIfNeeded(StringBuilder pythonCode) {
        if (PythonExtensionSettings.getInstance().isProvidePythonAdapter()) {
            Path adapterPath = getPythonAdapterPath();
            pythonCode.append("import sys\n");
            pythonCode.append("sys.path.append(\"").append(MacroUtils.escapeString(adapterPath.toAbsolutePath().toString())).append("\")\n");
        }
        pythonCode.append("import jipipe.data_slot\n");
    }

    public static void inputSlotsToPython(StringBuilder code, Map<String, Path> inputSlotPaths) {
        code.append("jipipe_inputs = {}\n");
        for (Map.Entry<String, Path> entry : inputSlotPaths.entrySet()) {
            code.append("jipipe_inputs[\"").append(MacroUtils.escapeString(entry.getKey()))
                    .append("\"] = jipipe.data_slot.import_from_folder(\"")
                    .append(MacroUtils.escapeString(entry.getValue().toString())).append("\")\n");
        }
    }

    public static void outputSlotsToPython(StringBuilder code, List<JIPipeDataSlot> outputSlots, Map<String, Path> outputSlotPaths) {
        Map<String, JIPipeDataSlot> outputSlotMap = new HashMap<>();
        for (JIPipeDataSlot outputSlot : outputSlots) {
            outputSlotMap.put(outputSlot.getName(), outputSlot);
        }
        code.append("jipipe_outputs = {}\n");
        for (Map.Entry<String, Path> entry : outputSlotPaths.entrySet()) {
            code.append("jipipe_outputs[\"").append(MacroUtils.escapeString(entry.getKey()))
                    .append("\"] = jipipe.data_slot.DataSlot(storage_path=\"")
                    .append(MacroUtils.escapeString(entry.getValue().toString()))
                    .append("\", data_type=\"")
                    .append(MacroUtils.escapeString(JIPipeDataInfo.getInstance(outputSlotMap.get(entry.getKey()).getAcceptedDataType()).getId()))
                    .append("\", name=\"").append(MacroUtils.escapeString(entry.getKey())).append("\"").append(")\n");
        }
    }

    public static void addPostprocessorCode(StringBuilder code, List<JIPipeDataSlot> outputSlots) {
        for (JIPipeDataSlot outputSlot : outputSlots) {
            code.append("jipipe_outputs[\"").append(MacroUtils.escapeString(outputSlot.getName())).append("\"].save()\n");
        }
    }

    public static Map<String, Path> installInputSlots(StringBuilder code, JIPipeDataBatch dataBatch, JIPipeGraphNode node, List<JIPipeDataSlot> effectiveInputSlots, JIPipeProgressInfo progressInfo) {
        Map<String, Path> inputSlotPaths = new HashMap<>();
        for (JIPipeDataSlot slot : effectiveInputSlots) {
            Path tempPath = RuntimeSettings.generateTempDirectory("py-input");
            progressInfo.log("Input slot '" + slot.getName() + "' is stored in " + tempPath);
            JIPipeDataSlot dummy = dataBatch.toDummySlot(slot.getInfo(), node, slot);
            dummy.save(tempPath, null, progressInfo);
            inputSlotPaths.put(slot.getName(), tempPath);
        }
        PythonUtils.inputSlotsToPython(code, inputSlotPaths);
        return inputSlotPaths;
    }

    public static Map<String, Path> installInputSlots(StringBuilder code, List<JIPipeDataSlot> effectiveInputSlots, JIPipeProgressInfo progressInfo) {
        Map<String, Path> inputSlotPaths = new HashMap<>();
        for (JIPipeDataSlot slot : effectiveInputSlots) {
            Path tempPath = RuntimeSettings.generateTempDirectory("py-input");
            progressInfo.log("Input slot '" + slot.getName() + "' is stored in " + tempPath);
            slot.save(tempPath, null, progressInfo);
            inputSlotPaths.put(slot.getName(), tempPath);
        }
        PythonUtils.inputSlotsToPython(code, inputSlotPaths);
        return inputSlotPaths;
    }

    public static Map<String, Path> installInputSlots(StringBuilder code, JIPipeMergingDataBatch dataBatch, JIPipeGraphNode node, List<JIPipeDataSlot> effectiveInputSlots, JIPipeProgressInfo progressInfo) {
        Map<String, Path> inputSlotPaths = new HashMap<>();
        for (JIPipeDataSlot slot : effectiveInputSlots) {
            Path tempPath = RuntimeSettings.generateTempDirectory("py-input");
            progressInfo.log("Input slot '" + slot.getName() + "' is stored in " + tempPath);
            JIPipeDataSlot dummy = dataBatch.toDummySlot(slot.getInfo(), node, slot);
            dummy.save(tempPath, null, progressInfo);
            inputSlotPaths.put(slot.getName(), tempPath);
        }
        PythonUtils.inputSlotsToPython(code, inputSlotPaths);
        return inputSlotPaths;
    }

    public static void runPython(String code, PythonEnvironment environment, JIPipeProgressInfo progressInfo) {
        progressInfo.log(code);
        Path codeFilePath = RuntimeSettings.generateTempFile("py", ".py");
        try {
            Files.write(codeFilePath, code.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        runPython(codeFilePath, environment, progressInfo);
    }

    public static void extractOutputs(JIPipeDataBatch dataBatch, Map<String, Path> outputSlotPaths, List<JIPipeDataSlot> outputSlots, JIPipeAnnotationMergeStrategy annotationMergeStrategy, JIPipeProgressInfo progressInfo) {
        for (JIPipeDataSlot outputSlot : outputSlots) {
            Path storagePath = outputSlotPaths.get(outputSlot.getName());
            JIPipeExportedDataTable table = JIPipeExportedDataTable.loadFromJson(outputSlotPaths.get(outputSlot.getName()).resolve("data-table.json"));
            for (int row = 0; row < table.getRowCount(); row++) {
                JIPipeDataInfo dataInfo = table.getDataTypeOf(row);
                Path rowStoragePath = table.getRowStoragePath(storagePath, row);
                JIPipeData data = JIPipe.importData(rowStoragePath, dataInfo.getDataClass());
                dataBatch.addOutputData(outputSlot, data, table.getRowList().get(row).getAnnotations(), annotationMergeStrategy, progressInfo);
            }
        }
    }

    public static void extractOutputs(JIPipeMergingDataBatch dataBatch, Map<String, Path> outputSlotPaths, List<JIPipeDataSlot> outputSlots, JIPipeAnnotationMergeStrategy annotationMergeStrategy, JIPipeProgressInfo progressInfo) {
        for (JIPipeDataSlot outputSlot : outputSlots) {
            Path storagePath = outputSlotPaths.get(outputSlot.getName());
            JIPipeExportedDataTable table = JIPipeExportedDataTable.loadFromJson(outputSlotPaths.get(outputSlot.getName()).resolve("data-table.json"));
            for (int row = 0; row < table.getRowCount(); row++) {
                JIPipeDataInfo dataInfo = table.getDataTypeOf(row);
                Path rowStoragePath = table.getRowStoragePath(storagePath, row);
                JIPipeData data = JIPipe.importData(rowStoragePath, dataInfo.getDataClass());
                dataBatch.addOutputData(outputSlot, data, table.getRowList().get(row).getAnnotations(), annotationMergeStrategy, progressInfo);
            }
        }
    }

    public static void extractOutputs(Map<String, Path> outputSlotPaths, List<JIPipeDataSlot> outputSlots, JIPipeProgressInfo progressInfo) {
        for (JIPipeDataSlot outputSlot : outputSlots) {
            Path storagePath = outputSlotPaths.get(outputSlot.getName());
            JIPipeExportedDataTable table = JIPipeExportedDataTable.loadFromJson(outputSlotPaths.get(outputSlot.getName()).resolve("data-table.json"));
            for (int row = 0; row < table.getRowCount(); row++) {
                JIPipeDataInfo dataInfo = table.getDataTypeOf(row);
                Path rowStoragePath = table.getRowStoragePath(storagePath, row);
                JIPipeData data = JIPipe.importData(rowStoragePath, dataInfo.getDataClass());
                outputSlot.addData(data, table.getRowList().get(row).getAnnotations(), JIPipeAnnotationMergeStrategy.OverwriteExisting, progressInfo);
            }
        }
    }

    public static void setupLogger(CommandLine commandLine, DefaultExecutor executor, JIPipeProgressInfo progressInfo) {
        progressInfo.log("Running " + Arrays.stream(commandLine.toStrings()).map(s -> {
            if (s.contains(" ")) {
                return "\"" + MacroUtils.escapeString(s) + "\"";
            } else {
                return MacroUtils.escapeString(s);
            }
        }).collect(Collectors.joining(" ")));

        LogOutputStream progressInfoLog = new LogOutputStream() {
            @Override
            protected void processLine(String s, int i) {
                for (String s1 : s.split("\\r")) {
                    progressInfo.log(WordUtils.wrap(s1, 120));
                }
            }
        };
        executor.setStreamHandler(new PumpStreamHandler(progressInfoLog, progressInfoLog));
    }

    public static void runPython(Path scriptFile, PythonEnvironment environment, JIPipeProgressInfo progressInfo) {
        Path pythonExecutable = environment.getExecutablePath();
        CommandLine commandLine = new CommandLine(pythonExecutable.toFile());

        Map<String, String> environmentVariables = new HashMap<>();
        ExpressionParameters existingEnvironmentVariables = new ExpressionParameters();
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

        ExpressionParameters parameters = new ExpressionParameters();
        parameters.set("script_file", scriptFile.toString());
        parameters.set("python_executable", environment.getExecutablePath().toString());
        Object evaluationResult = environment.getArguments().evaluate(parameters);
        for (Object item : (Collection<?>) evaluationResult) {
            commandLine.addArgument(StringUtils.nullToEmpty(item));
        }

        DefaultExecutor executor = new DefaultExecutor();
        executor.setProcessDestroyer(new ShutdownHookProcessDestroyer());
        executor.setWatchdog(new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT));
        setupLogger(commandLine, executor, progressInfo);

        try {
            executor.execute(commandLine, environmentVariables);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Escapes a string to be used within Python code
     * Will not add quotes around the string
     *
     * @param value unescaped string
     * @return escaped string
     */
    public static String escapeString(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * Converts the object into valid Python code
     * @param object the object
     * @return Python code
     */
    public static String objectToPython(Object object) {
        if(object instanceof String) {
            return "\"" + escapeString(object.toString()) +"\"";
        }
        else if(object instanceof Boolean) {
            return (((Boolean)object) ? "True" : "False");
        }
        else if(object == null) {
            return "None";
        }
        else if(object instanceof Collection) {
            return listToPythonArray((Collection<Object>) object, true);
        }
        else if(object instanceof Map) {
            return mapToPythonDict((Map<String, Object>) object, true);
        }
        else {
            return "" + object;
        }
    }
    
    /**
     * Converts a collection into a Python array
     * @param items the items
     * @param withSurroundingBrackets if enabled, surrounding brackets are added
     * @return Python code
     */
    public static String listToPythonArray(Collection<Object> items, boolean withSurroundingBrackets) {
        return (withSurroundingBrackets ? "[" : "") + items.stream().map(PythonUtils::objectToPython).collect(Collectors.joining(", "))
                + (withSurroundingBrackets ? "]" : "");
    }

    /**
     * Converts a dictionary into a Python dict
     * @param parameters the parameters
     * @param withSurroundingBraces if enabled, dict braces are added
     * @return Python code
     */
    public static String mapToPythonDict(Map<String, Object> parameters, boolean withSurroundingBraces) {
        return (withSurroundingBraces ? "{" : "") + parameters.entrySet().stream().map(entry ->
                entry.getKey() + "=" + objectToPython(entry.getValue())).collect(Collectors.joining(", ")) + (withSurroundingBraces ? "}" : "");
    }

    public static void cleanup(Map<String, Path> inputSlotPaths, Map<String, Path> outputSlotPaths, JIPipeProgressInfo progressInfo) {
        progressInfo.log("Cleaning up ...");
        for (Map.Entry<String, Path> entry : inputSlotPaths.entrySet()) {
            try {
                FileUtils.deleteDirectory(entry.getValue().toFile());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        for (Map.Entry<String, Path> entry : outputSlotPaths.entrySet()) {
            try {
                FileUtils.deleteDirectory(entry.getValue().toFile());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static Map<String, Path> installOutputSlots(StringBuilder code, List<JIPipeDataSlot> outputSlots, JIPipeProgressInfo progressInfo) {
        Map<String, Path> outputSlotPaths = new HashMap<>();
        for (JIPipeDataSlot slot : outputSlots) {
            Path tempPath = RuntimeSettings.generateTempDirectory("py-output");
            progressInfo.log("Output slot '" + slot.getName() + "' is stored in " + tempPath);
            outputSlotPaths.put(slot.getName(), tempPath);
        }
        PythonUtils.outputSlotsToPython(code, outputSlots, outputSlotPaths);
        return  outputSlotPaths;
    }

    public static RawPythonCode rawPythonCode(String code) {
        return new RawPythonCode(code);
    }

    public static class RawPythonCode {
        private final String code;

        public RawPythonCode(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        @Override
        public String toString() {
           return code;
        }
    }
}
