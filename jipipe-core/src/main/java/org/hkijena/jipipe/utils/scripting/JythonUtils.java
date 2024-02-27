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

package org.hkijena.jipipe.utils.scripting;

import org.hkijena.jipipe.api.parameters.JIPipeCustomParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.extensions.parameters.library.pairs.IntegerAndIntegerPairParameter;
import org.hkijena.jipipe.extensions.parameters.library.pairs.StringAndStringPairParameter;
import org.python.core.PyCode;
import org.python.core.PyDictionary;
import org.python.util.PythonInterpreter;

import java.util.Map;

/**
 * Utilities for Jython
 */
public class JythonUtils {

    private JythonUtils() {

    }

    public static void passParametersToPython(PythonInterpreter pythonInterpreter, JIPipeCustomParameterCollection collection) {
        for (Map.Entry<String, JIPipeParameterAccess> entry : collection.getParameters().entrySet()) {
            if (entry.getValue().getFieldClass() == StringAndStringPairParameter.List.class) {
                PyDictionary dictionary = new PyDictionary();
                for (StringAndStringPairParameter pair : entry.getValue().get(StringAndStringPairParameter.List.class)) {
                    dictionary.put(pair.getKey(), pair.getValue());
                }
                pythonInterpreter.set(entry.getKey(), dictionary);
            } else if (entry.getValue().getFieldClass() == IntegerAndIntegerPairParameter.List.class) {
                PyDictionary dictionary = new PyDictionary();
                for (IntegerAndIntegerPairParameter pair : entry.getValue().get(IntegerAndIntegerPairParameter.List.class)) {
                    dictionary.put(pair.getKey(), pair.getValue());
                }
                pythonInterpreter.set(entry.getKey(), dictionary);
            } else {
                pythonInterpreter.set(entry.getKey(), entry.getValue().get(Object.class));
            }
        }
    }

    public static void checkScriptValidity(String code, JIPipeCustomParameterCollection scriptParameters, JIPipeValidationReportContext context, JIPipeValidationReport report) {
        try {
            PythonInterpreter pythonInterpreter = new PythonInterpreter();
            JythonUtils.passParametersToPython(pythonInterpreter, scriptParameters);
            PyCode compile = pythonInterpreter.compile(code);
            if (compile == null) {
                report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                        context,
                        "The script is invalid!",
                        "The script could not be compiled.",
                        "Please check if your Python script is correct.",
                        code));
            }
        } catch (Exception e) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    context,
                    "The script is invalid!",
                    "The script could not be compiled.",
                    "Please check if your Python script is correct.",
                    code + "\n\n" + e));
        }
    }

    public static void checkScriptParametersValidity(JIPipeCustomParameterCollection scriptParameters, JIPipeValidationReportContext context, JIPipeValidationReport report) {
        for (String key : scriptParameters.getParameters().keySet()) {
            if (!MacroUtils.isValidVariableName(key)) {
                report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                        context,
                        "Invalid name!",
                        "'" + key + "' is an invalid Python variable name!",
                        "Please ensure that script variables are compatible with the Python language."));
            }
        }
    }

}
