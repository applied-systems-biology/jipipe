package org.hkijena.jipipe.extensions.r;

import com.github.rcaller.rstuff.RCode;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.parameters.generators.IntegerRange;
import org.hkijena.jipipe.extensions.parameters.primitives.DoubleList;
import org.hkijena.jipipe.extensions.parameters.primitives.IntegerList;
import org.hkijena.jipipe.extensions.parameters.primitives.StringList;
import org.hkijena.jipipe.utils.MacroUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Path;
import java.util.Map;
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
     * Converts parameters into R code. Only parameters in ALLOWED_PARAMETER_CLASSES are converted.
     * All parameters are also available in a list ParameterVariables
     * @param code the code
     * @param parameterCollection the parameters
     */
    public static void parametersToVariables(RCode code, JIPipeParameterCollection parameterCollection, boolean createParameterVariablesList, boolean addToParameterVariablesList) {
        JIPipeParameterTree tree = new JIPipeParameterTree(parameterCollection);
        if(createParameterVariablesList) {
            code.addRCode("ParameterVariables <- list()");
        }
        for (Map.Entry<String, JIPipeParameterAccess> entry : tree.getParameters().entrySet()) {
            String value = null;

            Object o = entry.getValue().get(Object.class);
            if(o instanceof String) {
                value = "\"" + MacroUtils.escapeString((String) o) + "\"";
            }
            else  if(o instanceof Path) {
                value = "\"" + MacroUtils.escapeString("" + o) + "\"";
            }
            else if(o instanceof Number) {
                if(o instanceof Float) {
                    float num = (float) o;
                    if(Float.isNaN(num)) {
                        value = "NaN";
                    }
                    else if(num == Float.POSITIVE_INFINITY) {
                        value = "Inf";
                    }
                    else if(num == Float.NEGATIVE_INFINITY) {
                        value = "-Inf";
                    }
                    else {
                        value = "" + num;
                    }
                }
                else if(o instanceof Double) {
                    double num = (double) o;
                    if(Double.isNaN(num)) {
                        value = "NaN";
                    }
                    else if(num == Double.POSITIVE_INFINITY) {
                        value = "Inf";
                    }
                    else if(num == Double.NEGATIVE_INFINITY) {
                        value = "-Inf";
                    }
                    else {
                        value = "" + num;
                    }
                }
                else {
                    value = "" + o;
                }
            }
            else if(o instanceof Boolean) {
                value = (boolean)o ? "TRUE" : "FALSE";
            }
            else if(o instanceof IntegerList) {
                value = "c(" + ((IntegerList) o).stream().map(i -> i + "").collect(Collectors.joining(", ")) + ")";
            }
            else if(o instanceof DoubleList) {
                value = "c(" + ((DoubleList) o).stream().map(i -> i + "").collect(Collectors.joining(", ")) + ")";
            }
            else if(o instanceof IntegerRange) {
                value = "c(" + ((IntegerRange) o).getIntegers().stream().map(i -> i + "").collect(Collectors.joining(", ")) + ")";
            }
            else if(o instanceof StringList) {
                value = "c(" + ((StringList) o).stream().map(s -> "\"" + MacroUtils.escapeString(s) + "\"").collect(Collectors.joining(", ")) + ")";
            }

            if(value != null) {
                if(MacroUtils.isValidVariableName(entry.getKey())) {
                    code.addRCode(entry.getKey() + " <- " + value);
                }
                if(addToParameterVariablesList) {
                    code.addRCode("ParameterVariables$\"" + entry.getKey() + "\" <- " + value);
                }
            }
        }
    }
}
