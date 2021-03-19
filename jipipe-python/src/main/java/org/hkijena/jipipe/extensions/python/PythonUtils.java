package org.hkijena.jipipe.extensions.python;

import org.apache.commons.exec.*;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameters;
import org.hkijena.jipipe.extensions.parameters.generators.IntegerRange;
import org.hkijena.jipipe.extensions.parameters.primitives.DoubleList;
import org.hkijena.jipipe.extensions.parameters.primitives.IntegerList;
import org.hkijena.jipipe.extensions.parameters.primitives.StringList;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.utils.MacroUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import javax.crypto.Mac;
import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PythonUtils {

    private static Path PYTHON_ADAPTER_PATH;
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
     * @return the path
     */
    public static Path getPythonAdapterPath() {
        if(PYTHON_ADAPTER_PATH == null || !Files.isDirectory(PYTHON_ADAPTER_PATH)) {
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
                if(!Files.isDirectory(targetPath.getParent())) {
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
        if(PythonExtensionSettings.getInstance().isProvidePythonAdapter()) {
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

    public static void runPython(Path scriptFile, JIPipeProgressInfo progressInfo) {
        Path pythonExecutable = PythonExtensionSettings.getInstance().getPythonExecutable();
        CommandLine commandLine = new CommandLine(pythonExecutable.toFile());

        ExpressionParameters parameters = new ExpressionParameters();
        parameters.set("script_file", scriptFile.toString());
        parameters.set("python_executable", PythonExtensionSettings.getInstance().getPythonExecutable().toString());
        Object evaluationResult = PythonExtensionSettings.getInstance().getPythonArguments().evaluate(parameters);
        for(Object item : (Collection<?>)evaluationResult) {
            commandLine.addArgument(StringUtils.nullToEmpty(item));
        }
        progressInfo.log("Running Python: " + Arrays.stream(commandLine.toStrings()).map(s -> {
            if(s.contains(" ")) {
                return "\"" + MacroUtils.escapeString(s) + "\"";
            }
            else {
                return MacroUtils.escapeString(s);
            }
        }).collect(Collectors.joining(" ")));

        LogOutputStream progressInfoLog = new LogOutputStream() {
            @Override
            protected void processLine(String s, int i) {
               progressInfo.log(s);
            }
        };

        DefaultExecutor executor = new DefaultExecutor();
        executor.setWatchdog(new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT));
        executor.setStreamHandler(new PumpStreamHandler(progressInfoLog, progressInfoLog));

        try {
            executor.execute(commandLine);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



}
