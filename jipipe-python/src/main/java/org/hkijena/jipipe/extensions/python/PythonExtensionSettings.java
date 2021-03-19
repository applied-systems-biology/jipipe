/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.python;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.parameters.JIPipeContextAction;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.parameters.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameterVariable;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.parameters.primitives.FilePathParameterSettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.PathEditor;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class PythonExtensionSettings implements JIPipeParameterCollection {

    public static String ID = "org.hkijena.jipipe:python";

    private final EventBus eventBus = new EventBus();

    private Path pythonExecutable;
    private DefaultExpressionParameter pythonArguments = new DefaultExpressionParameter("script_file");
    private boolean providePythonAdapter = true;

    public PythonExtensionSettings() {
    }

    public static PythonExtensionSettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, PythonExtensionSettings.class);
    }

    /**
     * Checks if the Python settings are valid or throws an exception
     */
    public static void checkPythonSettings() {
        if(!pythonSettingsAreValid()) {
            throw new UserFriendlyRuntimeException("The Python installation is invalid!\n" +
                    "Python=" + getInstance().getPythonExecutable(),
                    "Python is not configured!",
                    "Project > Application settings > Extensions > Python integration",
                    "This node requires an installation of Python. You have to point JIPipe to a Python installation.",
                    "Please install Python from https://www.python.org/, or from https://www.anaconda.com/ or https://docs.conda.io/en/latest/miniconda.html. " +
                            "If Python is installed, go to Project > Application settings > Extensions > Python integration and " +
                            "set the Python executable. virtualenv is supported (you can find the exe in the environment bin folder).");
        }
    }

    /**
     * Checks if the Python settings are valid or reports an invalid state
     * @param report the report
     */
    public static void checkPythonSettings(JIPipeValidityReport report) {
        if(!pythonSettingsAreValid()) {
            report.reportIsInvalid("Python is not configured!",
                    "Project > Application settings > Extensions > Python integration",
                    "This node requires an installation of Python. You have to point JIPipe to a Python installation.",
                    "Please install Python from https://www.python.org/, or from https://www.anaconda.com/ or https://docs.conda.io/en/latest/miniconda.html. " +
                            "If Python is installed, go to Project > Application settings > Extensions > Python integration and " +
                            "set the Python executable. virtualenv is supported (you can find the exe in the environment bin folder).");
        }
    }

    /**
     * Checks the Python settings
     *
     * @return if the settings are correct
     */
    public static boolean pythonSettingsAreValid() {
        String executable = null;
        if (JIPipe.getInstance() != null) {
            PythonExtensionSettings instance = getInstance();
            if (instance.pythonExecutable != null) {
                executable = instance.pythonExecutable.toString();
            }
        }
        boolean invalid = false;
        if (StringUtils.isNullOrEmpty(executable) || !Files.exists(Paths.get(executable))) {
            invalid = true;
        }
        return !invalid;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @JIPipeDocumentation(name = "Python executable", description = "Points at the Python executable." +
            "<ul>" +
            "<li>If you use standard Python, point it to python.exe</li>" +
            "<li>If you use Conda, point it to Scripts/conda.exe</li>" +
            "<li>If you use virtualenv, point it to venv/Scripts/python.exe</li>" +
            "</ul>")
    @JIPipeParameter("python-executable")
    @FilePathParameterSettings(pathMode = PathEditor.PathMode.FilesOnly, ioMode = PathEditor.IOMode.Open)
    public Path getPythonExecutable() {
        return pythonExecutable;
    }

    @JIPipeParameter("python-executable")
    public void setPythonExecutable(Path pythonExecutable) {
        this.pythonExecutable = pythonExecutable;
    }

    @JIPipeDocumentation(name = "Provide Python adapter", description = "If enabled, JIPipe will provide any Python node with the JIPipe Python Adapter modules. " +
            "You may want to disable this if you installed a JIPipe adapter into your Python library.")
    @JIPipeParameter("provide-python-adapter")
    public boolean isProvidePythonAdapter() {
        return providePythonAdapter;
    }

    @JIPipeParameter("provide-python-adapter")
    public void setProvidePythonAdapter(boolean providePythonAdapter) {
        this.providePythonAdapter = providePythonAdapter;
    }

//    @JIPipeDocumentation(name = "Use Conda", description = "Select a Conda environment.")
//    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/apps/python.png")
//    public void setToConda(JIPipeWorkbench parent) {
//        // Insert code here
//    }

    @JIPipeDocumentation(name = "Python arguments", description = "Expression that determines the Python arguments. Must return an array of strings." +
            "<ul>" +
            "<li>If you use standard Python, set it to <code>ARRAY(script_file)</code></li>" +
            "<li>If you use Conda, set it to <code>ARRAY(\"run\", \"-n\", \"base\", \"python\", script_file)</code>. " +
            "You can replace 'base' with any other Conda environment you currently have installed.</li>" +
            "<li>If you use virtualenv, set it to <code>ARRAY(script_file)</code></li>" +
            "</ul>")
    @JIPipeParameter("python-arguments")
    public DefaultExpressionParameter getPythonArguments() {
        return pythonArguments;
    }

    @JIPipeParameter("python-arguments")
    public void setPythonArguments(DefaultExpressionParameter pythonArguments) {
        this.pythonArguments = pythonArguments;
    }

    public static class PythonArgumentsVariableSource implements ExpressionParameterVariableSource {

        @Override
        public Set<ExpressionParameterVariable> getVariables(JIPipeParameterAccess parameterAccess) {
            Set<ExpressionParameterVariable> result = new HashSet<>();
            result.add(new ExpressionParameterVariable("Python executable", "The Python executable", "python_executable"));
            result.add(new ExpressionParameterVariable("Script file", "The Python script file to be executed", "script_file"));
            return result;
        }
    }
}
