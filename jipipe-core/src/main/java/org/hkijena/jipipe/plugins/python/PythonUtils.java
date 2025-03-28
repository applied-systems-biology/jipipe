/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.python;

import org.apache.commons.exec.*;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.text.WordUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.data.serialization.JIPipeDataTableInfo;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemWriteDataStorage;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.parameters.library.pairs.StringQueryExpressionAndStringPairParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.DoubleList;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.IntegerList;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.plugins.parameters.library.primitives.ranges.IntegerRange;
import org.hkijena.jipipe.plugins.python.adapter.JIPipePythonAdapterLibraryEnvironment;
import org.hkijena.jipipe.plugins.settings.JIPipeRuntimeApplicationSettings;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.ProcessUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.scripting.MacroUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
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

    private PythonUtils() {

    }

    /**
     * Converts annotations into Python code.
     *
     * @param code        the code
     * @param annotations the annotations
     */
    public static void annotationsToPython(StringBuilder code, Collection<JIPipeTextAnnotation> annotations) {
        code.append("jipipe_annotations = {}\n");
        for (JIPipeTextAnnotation annotation : annotations) {
            code.append("jipipe_annotations[\"").append(MacroUtils.escapeString(annotation.getName())).append("\"] = ")
                    .append("\"").append(MacroUtils.escapeString(StringUtils.nullToEmpty(annotation.getValue()))).append("\"\n");
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
                value = "[" + ((IntegerRange) o).getIntegers(0, 0, new JIPipeExpressionVariablesMap()).stream().map(i -> i + "").collect(Collectors.joining(", ")) + "]";
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

    public static void installAdapterCodeIfNeeded(JIPipePythonAdapterLibraryEnvironment environment, StringBuilder pythonCode) {
        environment.generateCode(pythonCode, new JIPipeProgressInfo());
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

    public static void outputSlotsToPython(StringBuilder code, List<JIPipeOutputDataSlot> outputSlots, Map<String, Path> outputSlotPaths) {
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

    public static void addPostprocessorCode(StringBuilder code, List<JIPipeOutputDataSlot> outputSlots) {
        for (JIPipeDataSlot outputSlot : outputSlots) {
            code.append("jipipe_outputs[\"").append(MacroUtils.escapeString(outputSlot.getName())).append("\"].save()\n");
        }
    }

    public static Map<String, Path> installInputSlots(StringBuilder code, JIPipeSingleIterationStep iterationStep, JIPipeGraphNode node, List<JIPipeInputDataSlot> effectiveInputSlots, Path workDirectory, JIPipeProgressInfo progressInfo) {
        Map<String, Path> inputSlotPaths = new HashMap<>();
        for (JIPipeDataSlot slot : effectiveInputSlots) {
            Path tempPath = workDirectory.resolve("inputs").resolve(slot.getName());
            try {
                Files.createDirectories(tempPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            progressInfo.log("Input slot '" + slot.getName() + "' is stored in " + tempPath);
            JIPipeDataSlot dummy = iterationStep.toDummySlot(slot.getInfo(), node, slot, progressInfo);
            dummy.exportData(new JIPipeFileSystemWriteDataStorage(progressInfo, tempPath), progressInfo);
            inputSlotPaths.put(slot.getName(), tempPath);
        }
        PythonUtils.inputSlotsToPython(code, inputSlotPaths);
        return inputSlotPaths;
    }

    public static Map<String, Path> installInputSlots(StringBuilder code, List<JIPipeInputDataSlot> effectiveInputSlots, Path workDirectory, JIPipeProgressInfo progressInfo) {
        Map<String, Path> inputSlotPaths = new HashMap<>();
        for (JIPipeDataSlot slot : effectiveInputSlots) {
            Path tempPath = workDirectory.resolve("inputs").resolve(slot.getName());
            try {
                Files.createDirectories(tempPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            progressInfo.log("Input slot '" + slot.getName() + "' is stored in " + tempPath);
            slot.exportData(new JIPipeFileSystemWriteDataStorage(progressInfo, tempPath), progressInfo);
            inputSlotPaths.put(slot.getName(), tempPath);
        }
        PythonUtils.inputSlotsToPython(code, inputSlotPaths);
        return inputSlotPaths;
    }

    public static Map<String, Path> installInputSlots(StringBuilder code, JIPipeMultiIterationStep iterationStep, JIPipeGraphNode node, List<JIPipeInputDataSlot> effectiveInputSlots, Path workDirectory, JIPipeProgressInfo progressInfo) {
        Map<String, Path> inputSlotPaths = new HashMap<>();
        for (JIPipeDataSlot slot : effectiveInputSlots) {
            Path tempPath = workDirectory.resolve("inputs").resolve(slot.getName());
            try {
                Files.createDirectories(tempPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            progressInfo.log("Input slot '" + slot.getName() + "' is stored in " + tempPath);
            JIPipeDataSlot dummy = iterationStep.toDummySlot(slot.getInfo(), node, slot, progressInfo);
            dummy.exportData(new JIPipeFileSystemWriteDataStorage(progressInfo, tempPath), progressInfo);
            inputSlotPaths.put(slot.getName(), tempPath);
        }
        PythonUtils.inputSlotsToPython(code, inputSlotPaths);
        return inputSlotPaths;
    }

    public static void runPython(String code, PythonEnvironment environment, List<Path> libraryPaths, boolean suppressLogs, JIPipeProgressInfo progressInfo) {
        progressInfo.log(code);
        Path codeFilePath = JIPipeRuntimeApplicationSettings.getTemporaryFile("py", ".py");
        try {
            Files.write(codeFilePath, code.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        runPython(codeFilePath, environment, libraryPaths, suppressLogs, false, progressInfo);
    }

    public static void extractOutputs(JIPipeSingleIterationStep iterationStep, Map<String, Path> outputSlotPaths, List<JIPipeOutputDataSlot> outputSlots, JIPipeTextAnnotationMergeMode annotationMergeStrategy, JIPipeProgressInfo progressInfo) {
        for (JIPipeOutputDataSlot outputSlot : outputSlots) {
            Path storagePath = outputSlotPaths.get(outputSlot.getName());
            JIPipeDataTableInfo table = JIPipeDataTableInfo.loadFromJson(outputSlotPaths.get(outputSlot.getName()).resolve("data-table.json"));
            for (int row = 0; row < table.getRowCount(); row++) {
                JIPipeDataInfo dataInfo = table.getDataTypeOf(row);
                Path rowStoragePath = table.getRowStoragePath(storagePath, row);
                JIPipeData data = JIPipe.importData(new JIPipeFileSystemReadDataStorage(progressInfo, rowStoragePath), dataInfo.getDataClass(), progressInfo);
                iterationStep.addOutputData(outputSlot, data, table.getRowList().get(row).getTextAnnotations(), annotationMergeStrategy, progressInfo);
            }
        }
    }

    public static void extractOutputs(JIPipeMultiIterationStep iterationStep, Map<String, Path> outputSlotPaths, List<JIPipeOutputDataSlot> outputSlots, JIPipeTextAnnotationMergeMode annotationMergeStrategy, JIPipeProgressInfo progressInfo) {
        for (JIPipeOutputDataSlot outputSlot : outputSlots) {
            Path storagePath = outputSlotPaths.get(outputSlot.getName());
            JIPipeDataTableInfo table = JIPipeDataTableInfo.loadFromJson(outputSlotPaths.get(outputSlot.getName()).resolve("data-table.json"));
            for (int row = 0; row < table.getRowCount(); row++) {
                JIPipeDataInfo dataInfo = table.getDataTypeOf(row);
                Path rowStoragePath = table.getRowStoragePath(storagePath, row);
                JIPipeData data = JIPipe.importData(new JIPipeFileSystemReadDataStorage(progressInfo, rowStoragePath), dataInfo.getDataClass(), progressInfo);
                iterationStep.addOutputData(outputSlot, data, table.getRowList().get(row).getTextAnnotations(), annotationMergeStrategy, progressInfo);
            }
        }
    }

    public static void extractOutputs(Map<String, Path> outputSlotPaths, List<JIPipeOutputDataSlot> outputSlots, JIPipeProgressInfo progressInfo) {
        for (JIPipeOutputDataSlot outputSlot : outputSlots) {
            Path storagePath = outputSlotPaths.get(outputSlot.getName());
            JIPipeDataTableInfo table = JIPipeDataTableInfo.loadFromJson(outputSlotPaths.get(outputSlot.getName()).resolve("data-table.json"));
            for (int row = 0; row < table.getRowCount(); row++) {
                JIPipeDataInfo dataInfo = table.getDataTypeOf(row);
                Path rowStoragePath = table.getRowStoragePath(storagePath, row);
                JIPipeData data = JIPipe.importData(new JIPipeFileSystemReadDataStorage(progressInfo, rowStoragePath), dataInfo.getDataClass(), progressInfo);
                outputSlot.addData(data, table.getRowList().get(row).getTextAnnotations(), JIPipeTextAnnotationMergeMode.OverwriteExisting, progressInfo);
            }
        }
    }

    public static void setupLogger(CommandLine commandLine, DefaultExecutor executor, boolean suppressLogs, JIPipeProgressInfo progressInfo) {
        progressInfo.log("Running " + Arrays.stream(commandLine.toStrings()).map(s -> {
            if (s.contains(" ")) {
                return "\"" + MacroUtils.escapeString(s) + "\"";
            } else {
                return MacroUtils.escapeString(s);
            }
        }).collect(Collectors.joining(" ")));

        if (suppressLogs) {
            progressInfo.log("LOGS ARE SUPPRESSED. NOT STATUS UPDATES FROM PYTHON ARE RECEIVED.");
        } else {
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
    }

    /**
     * Runs a Python script file
     *
     * @param scriptFile   the script file
     * @param environment  the environment
     * @param libraryPaths additional library paths
     * @param suppressLogs if logging should be suppressed
     * @param detached     if the script should be run detached
     * @param progressInfo the progress info
     */
    public static void runPython(Path scriptFile, PythonEnvironment environment, List<Path> libraryPaths, boolean suppressLogs, boolean detached, JIPipeProgressInfo progressInfo) {
        Path pythonExecutable = PathUtils.relativeJIPipeUserDirToAbsolute(environment.getExecutablePath());
        CommandLine commandLine = new CommandLine(pythonExecutable.toFile());

        Map<String, String> environmentVariables = new HashMap<>();
        JIPipeExpressionVariablesMap existingEnvironmentVariables = new JIPipeExpressionVariablesMap();
        for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
            existingEnvironmentVariables.put(entry.getKey(), entry.getValue());
            environmentVariables.put(entry.getKey(), entry.getValue());
        }
        for (StringQueryExpressionAndStringPairParameter environmentVariable : environment.getEnvironmentVariables()) {
            String value = StringUtils.nullToEmpty(environmentVariable.getKey().evaluate(existingEnvironmentVariables));
            environmentVariables.put(environmentVariable.getValue(), value);
        }
        installLibraryPaths(environmentVariables, libraryPaths);
        for (Map.Entry<String, String> entry : environmentVariables.entrySet()) {
            progressInfo.log("Setting environment variable " + entry.getKey() + "=" + entry.getValue());
        }

        JIPipeExpressionVariablesMap parameters = new JIPipeExpressionVariablesMap();
        parameters.set("script_file", scriptFile.toString());
        parameters.set("python_executable", environment.getExecutablePath().toString());
        Object evaluationResult = environment.getArguments().evaluate(parameters);
        for (Object item : (Collection<?>) evaluationResult) {
            commandLine.addArgument(StringUtils.nullToEmpty(item), ProcessUtils.shouldHandleQuoting());
        }

        ProcessUtils.ExtendedExecutor executor = new ProcessUtils.ExtendedExecutor(ExecuteWatchdog.INFINITE_TIMEOUT, progressInfo);
        setupLogger(commandLine, executor, suppressLogs, progressInfo);

        try {
            if (detached) {
                executor.launch(commandLine, environmentVariables, scriptFile.getParent().toFile());
            } else {
                executor.execute(commandLine, environmentVariables);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets the PYTHONPATH variable, so it contains the provided libraries
     *
     * @param environmentVariables the env variables
     * @param libraryPaths         library paths to add
     */
    private static void installLibraryPaths(Map<String, String> environmentVariables, List<Path> libraryPaths) {
        if (libraryPaths.isEmpty())
            return;
        String delimiter = SystemUtils.IS_OS_WINDOWS ? ";" : ":";
        String existing = environmentVariables.getOrDefault("PYTHONPATH", null);
        if (StringUtils.isNullOrEmpty(existing)) {
            existing = libraryPaths.stream().map(Objects::toString).collect(Collectors.joining(delimiter));
        } else {
            existing = libraryPaths.stream().map(Objects::toString).collect(Collectors.joining(delimiter)) + delimiter + existing;
        }
        environmentVariables.put("PYTHONPATH", existing);
    }

    /**
     * Runs Python with a set of arguments
     *
     * @param arguments                      the arguments
     * @param environment                    the environment
     * @param libraryPaths                   additional library paths
     * @param additionalEnvironmentVariables additional environment variables
     * @param suppressLogs                   if logs should be suppressed
     * @param detached                       run the process detached
     * @param progressInfo                   the progress info
     */
    public static void runPython(String[] arguments, PythonEnvironment environment, List<Path> libraryPaths, Map<String, String> additionalEnvironmentVariables, boolean suppressLogs, boolean detached, JIPipeProgressInfo progressInfo) {
        Path pythonExecutable = PathUtils.relativeJIPipeUserDirToAbsolute(environment.getExecutablePath());
        CommandLine commandLine = new CommandLine(pythonExecutable.toFile());

        Map<String, String> environmentVariables = new HashMap<>();
        JIPipeExpressionVariablesMap existingEnvironmentVariables = new JIPipeExpressionVariablesMap();
        for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
            existingEnvironmentVariables.put(entry.getKey(), entry.getValue());
            environmentVariables.put(entry.getKey(), entry.getValue());
        }
        for (StringQueryExpressionAndStringPairParameter environmentVariable : environment.getEnvironmentVariables()) {
            String value = StringUtils.nullToEmpty(environmentVariable.getKey().evaluate(existingEnvironmentVariables));
            environmentVariables.put(environmentVariable.getValue(), value);
        }
        installLibraryPaths(environmentVariables, libraryPaths);
        environmentVariables.putAll(additionalEnvironmentVariables);


        for (Map.Entry<String, String> entry : environmentVariables.entrySet()) {
            progressInfo.log("Setting environment variable " + entry.getKey() + "=" + entry.getValue());
        }

        JIPipeExpressionVariablesMap parameters = new JIPipeExpressionVariablesMap();
        parameters.set("script_file", "");
        parameters.set("python_executable", environment.getExecutablePath().toString());
        Object evaluationResult = environment.getArguments().evaluate(parameters);
        for (Object item : (Collection<?>) evaluationResult) {
            String arg = StringUtils.nullToEmpty(item);
            if (!StringUtils.isNullOrEmpty(arg))
                commandLine.addArgument(arg, ProcessUtils.shouldHandleQuoting());
        }
        for (String argument : arguments) {
            commandLine.addArgument(argument, ProcessUtils.shouldHandleQuoting());
        }

        ProcessUtils.ExtendedExecutor executor = new ProcessUtils.ExtendedExecutor(ExecuteWatchdog.INFINITE_TIMEOUT, progressInfo);
        setupLogger(commandLine, executor, suppressLogs, progressInfo);

        try {
            if (detached) {
                executor.launch(commandLine, environmentVariables, executor.getWorkingDirectory());
            } else {
                executor.execute(commandLine, environmentVariables);
            }
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
     *
     * @param object the object
     * @return Python code
     */
    public static String objectToPython(Object object) {
        if (object instanceof String) {
            return "\"" + escapeString(object.toString()) + "\"";
        }
        if (object instanceof Path) {
            return "\"" + escapeString(object.toString()) + "\"";
        } else if (object instanceof Boolean) {
            return (((Boolean) object) ? "True" : "False");
        } else if (object == null) {
            return "None";
        } else if (object instanceof Collection) {
            return listToPythonArray((Collection<Object>) object, true);
        } else if (object instanceof Map) {
            return mapToPythonDict((Map<String, Object>) object);
        } else {
            return "" + object;
        }
    }

    /**
     * Converts a collection into a Python array
     *
     * @param items                   the items
     * @param withSurroundingBrackets if enabled, surrounding brackets are added
     * @return Python code
     */
    public static String listToPythonArray(Collection<Object> items, boolean withSurroundingBrackets) {
        return (withSurroundingBrackets ? "[" : "") + items.stream().map(PythonUtils::objectToPython).collect(Collectors.joining(", "))
                + (withSurroundingBrackets ? "]" : "");
    }

    /**
     * Converts a dictionary into a set of Python function arguments
     *
     * @param parameters the parameters
     * @return Python code
     */
    public static String mapToPythonDict(Map<String, Object> parameters) {
        return "dict(" + mapToPythonArguments(parameters) + ")";
    }

    /**
     * Converts a dictionary into a set of Python function arguments
     *
     * @param parameters the parameters
     * @return Python code
     */
    public static String mapToPythonArguments(Map<String, Object> parameters) {
        return parameters.entrySet().stream().map(entry ->
                entry.getKey() + "=" + objectToPython(entry.getValue())).collect(Collectors.joining(", "));
    }

    public static void cleanup(Map<String, Path> inputSlotPaths, Map<String, Path> outputSlotPaths, JIPipeProgressInfo progressInfo) {
        progressInfo.log("Cleaning up ...");
        for (Map.Entry<String, Path> entry : inputSlotPaths.entrySet()) {
            try {
                PathUtils.deleteDirectoryRecursively(entry.getValue(),
                        progressInfo.resolve("Cleanup"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for (Map.Entry<String, Path> entry : outputSlotPaths.entrySet()) {
            try {
                PathUtils.deleteDirectoryRecursively(entry.getValue(),
                        progressInfo.resolve("Cleanup"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static Map<String, Path> installOutputSlots(StringBuilder code, List<JIPipeOutputDataSlot> outputSlots, Path workDirectory, JIPipeProgressInfo progressInfo) {
        Map<String, Path> outputSlotPaths = new HashMap<>();
        for (JIPipeDataSlot slot : outputSlots) {
            Path tempPath = workDirectory.resolve("outputs").resolve(slot.getName());
            try {
                Files.createDirectories(tempPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            progressInfo.log("Output slot '" + slot.getName() + "' is stored in " + tempPath);
            outputSlotPaths.put(slot.getName(), tempPath);
        }
        PythonUtils.outputSlotsToPython(code, outputSlots, outputSlotPaths);
        return outputSlotPaths;
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
