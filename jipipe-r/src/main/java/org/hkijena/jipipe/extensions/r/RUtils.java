package org.hkijena.jipipe.extensions.r;

import com.github.rcaller.rstuff.RCode;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.parameters.generators.IntegerRange;
import org.hkijena.jipipe.extensions.parameters.primitives.DoubleList;
import org.hkijena.jipipe.extensions.parameters.primitives.IntegerList;
import org.hkijena.jipipe.extensions.parameters.primitives.StringList;
import org.hkijena.jipipe.utils.MacroUtils;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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
    public static void annotationsToR(RCode code, Collection<JIPipeAnnotation> annotations) {
        code.addRCode("JIPipe.Annotations <- list()");
        for (JIPipeAnnotation annotation : annotations) {
            code.addRCode(String.format("Annotations$\"%s\" <- \"%s\"", MacroUtils.escapeString(annotation.getName()),
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
    public static void parametersToR(RCode code, JIPipeParameterCollection parameterCollection) {
        JIPipeParameterTree tree = new JIPipeParameterTree(parameterCollection);
        code.addRCode("JIPipe.Variables <- list()");
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
                value = "c(" + ((IntegerRange) o).getIntegers().stream().map(i -> i + "").collect(Collectors.joining(", ")) + ")";
            } else if (o instanceof StringList) {
                value = "c(" + ((StringList) o).stream().map(s -> "\"" + MacroUtils.escapeString(s) + "\"").collect(Collectors.joining(", ")) + ")";
            }

            if (value != null) {
                if (MacroUtils.isValidVariableName(entry.getKey())) {
                    code.addRCode(entry.getKey() + " <- " + value);
                }
                code.addRCode("JIPipe.Variables$\"" + entry.getKey() + "\" <- " + value);
            }
        }
    }

    public static void inputSlotsToR(RCode code, Map<String, Path> inputSlotPaths, Function<String, Integer> slotRowCount) {
        code.addRCode("JIPipe.InputSlotFolders <- list()");
        code.addRCode("JIPipe.InputSlotRowCounts <- list()");
        for (Map.Entry<String, Path> entry : inputSlotPaths.entrySet()) {
            code.addRCode("JIPipe.InputSlotFolders$\"" + MacroUtils.escapeString(entry.getKey()) + "\" <- \""
                    + MacroUtils.escapeString(entry.getValue() + "") + "\"");
            code.addRCode("JIPipe.InputSlotRowCounts$\"" + MacroUtils.escapeString(entry.getKey()) + "\" <- "
                    + slotRowCount.apply(entry.getKey()));
        }

        // The getter function
        code.addRCode("JIPipe.GetInputFolder <- function(slot, row=0) { " +
                "return(file.path(JIPipe.InputSlotFolders[[slot]], row)) " +
                "}");
    }

    public static void outputSlotsToR(RCode code, List<JIPipeDataSlot> outputSlots, Map<String, Path> outputSlotPaths) {

        Map<String, JIPipeDataSlot> outputSlotMap = new HashMap<>();
        for (JIPipeDataSlot outputSlot : outputSlots) {
            outputSlotMap.put(outputSlot.getName(), outputSlot);
        }

        code.addRCode("JIPipe.OutputSlotFolders <- list()");
        code.addRCode("JIPipe.OutputSlots.Table <- list()");
        for (Map.Entry<String, Path> entry : outputSlotPaths.entrySet()) {
            code.addRCode("JIPipe.OutputSlotFolders$\"" + MacroUtils.escapeString(entry.getKey()) + "\" <- \""
                    + MacroUtils.escapeString(entry.getValue() + "") + "\"");
            code.addRCode("JIPipe.OutputSlots.Table$\"" + MacroUtils.escapeString(entry.getKey()) + "\" <- " +
                    "list(rows=list(), " +
                    "slot=\"" + MacroUtils.escapeString(entry.getKey()) + "\", " +
                    "\"data-type\"=\"" + MacroUtils.escapeString(JIPipeDataInfo.getInstance(outputSlotMap.get(entry.getKey()).getAcceptedDataType()).getId()) + "\")");
        }

        // The getter function
        code.addRCode("JIPipe.AddOutputFolder <- function(slot, annotations=list()) { " +
                "count <- length(JIPipe.OutputSlots.Table[[slot]][[\"rows\"]]);" +
                "result <- file.path(JIPipe.OutputSlotFolders[[slot]], count); " +
                "dir.create(result); " +
                "data.type <- JIPipe.OutputSlots.Table[[slot]][[\"data-type\"]]; " +
                ".GlobalEnv$JIPipe.OutputSlots.Table[[slot]][[\"rows\"]][[count + 1]] <- list(index=count, " +
                "annotations=annotations, " +
                "\"true-data-type\"=data.type); " +
                "return(result);" +
                "}");
    }

    public static void installInputLoaderCode(RCode code) {
        code.addRCode("JIPipe.GetInputAsDataFrame <- function(slot, row=0) { " +
                "folder <- JIPipe.GetInputFolder(slot, row=row);" +
                "file.name <- list.files(folder, pattern=glob2rx(\"*.csv\"))[1];" +
                "return(read.csv(file.path(folder, file.name))); " +
                "}");
    }

    public static void installOutputGeneratorCode(RCode code) {
        code.addRCode("JIPipe.AddOutputDataFrame <- function(data, slot, annotations=list()) { " +
                "folder <- JIPipe.AddOutputFolder(slot, annotations=annotations);" +
                "write.csv(data, file=file.path(folder, \"data.csv\"));" +
                "}");
        code.addRCode("JIPipe.AddOutputPNGImagePath <- function(data, slot, annotations=list()) { " +
                "folder <- JIPipe.AddOutputFolder(slot, annotations=annotations);" +
                "return(file.path(folder, \"data.png\"));" +
                "}");
        code.addRCode("JIPipe.AddOutputTIFFImagePath <- function(data, slot, annotations=list()) { " +
                "folder <- JIPipe.AddOutputFolder(slot, annotations=annotations);" +
                "return(file.path(folder, \"data.tif\"));" +
                "}");
    }

    public static void installPostprocessorCode(RCode code) {
        code.addRCode("if(!require(rjson)) { install.packages(\"rjson\"); library(rjson); }");
        code.addRCode("for(slot in names(JIPipe.OutputSlots.Table)) { " +
                "folder <- JIPipe.OutputSlotFolders[[slot]]; " +
                "data <- JIPipe.OutputSlots.Table[[slot]];" +
                "writeLines(toJSON(data, indent=4), con=file.path(folder, \"data-table.json\"));" +
                "}");
    }
}
