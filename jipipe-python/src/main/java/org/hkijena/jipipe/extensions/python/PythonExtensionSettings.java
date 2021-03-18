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
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.parameters.primitives.FilePathParameterSettings;
import org.hkijena.jipipe.ui.components.PathEditor;
import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PythonExtensionSettings implements JIPipeParameterCollection {

    public static String ID = "org.hkijena.jipipe:python";

    private final EventBus eventBus = new EventBus();

    private Path pythonExecutable;

    public PythonExtensionSettings() {
    }

    public static PythonExtensionSettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, PythonExtensionSettings.class);
    }

    /**
     * Checks the R settings
     *
     * @return if the settings are correct
     */
    public static boolean checkPythonSettings() {
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

    @JIPipeDocumentation(name = "Python executable", description = "Points at the Python executable. Must point to python.exe (Windows) or equivalent on other systems.")
    @JIPipeParameter("python-executable")
    @FilePathParameterSettings(pathMode = PathEditor.PathMode.FilesOnly, ioMode = PathEditor.IOMode.Open)
    public Path getPythonExecutable() {
        return pythonExecutable;
    }

    @JIPipeParameter("python-executable")
    public void setPythonExecutable(Path pythonExecutable) {
        this.pythonExecutable = pythonExecutable;
    }
}
