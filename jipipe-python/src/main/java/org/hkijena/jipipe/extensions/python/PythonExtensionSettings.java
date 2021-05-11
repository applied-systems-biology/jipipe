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
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.parameters.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameterVariable;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.parameters.external.PythonEnvironmentParameter;
import org.hkijena.jipipe.extensions.parameters.primitives.FilePathParameterSettings;
import org.hkijena.jipipe.ui.components.PathEditor;
import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class PythonExtensionSettings implements JIPipeParameterCollection {

    public static String ID = "org.hkijena.jipipe:python";

    private final EventBus eventBus = new EventBus();
    private PythonEnvironmentParameter pythonEnvironment = new PythonEnvironmentParameter();
    private boolean providePythonAdapter = true;

    public PythonExtensionSettings() {
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @JIPipeDocumentation(name = "Python environment", description = "The Python environment that is utilized by the Python nodes. " +
            "Click the 'Select' button to select an existing environment or install a new Python.")
    @JIPipeParameter("python-environment")
    public PythonEnvironmentParameter getPythonEnvironment() {
        return pythonEnvironment;
    }

    @JIPipeParameter("python-environment")
    public void setPythonEnvironment(PythonEnvironmentParameter pythonEnvironment) {
        this.pythonEnvironment = pythonEnvironment;
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

    public static PythonExtensionSettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, PythonExtensionSettings.class);
    }

    /**
     * Checks if the Python settings are valid or throws an exception
     */
    public static void checkPythonSettings() {
        if (!pythonSettingsAreValid()) {
            throw new UserFriendlyRuntimeException("The Python installation is invalid!\n" +
                    "Python=" + getInstance().getPythonEnvironment(),
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
     *
     * @param report the report
     */
    public static void checkPythonSettings(JIPipeValidityReport report) {
        if (!pythonSettingsAreValid()) {
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
            if (instance.pythonEnvironment.getExecutablePath() != null) {
                executable = instance.pythonEnvironment.getExecutablePath().toString();
            }
        }
        boolean invalid = false;
        if (StringUtils.isNullOrEmpty(executable) || !Files.exists(Paths.get(executable))) {
            invalid = true;
        }
        return !invalid;
    }
}
