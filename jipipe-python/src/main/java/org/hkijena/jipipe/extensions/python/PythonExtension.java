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

import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.parameters.primitives.HTMLText;
import org.hkijena.jipipe.extensions.parameters.primitives.StringList;
import org.hkijena.jipipe.extensions.python.algorithms.*;
import org.hkijena.jipipe.extensions.python.installers.MinicondaEnvPythonInstaller;
import org.hkijena.jipipe.extensions.python.installers.SelectCondaEnvPythonInstaller;
import org.hkijena.jipipe.extensions.python.installers.SelectSystemPythonInstaller;
import org.hkijena.jipipe.extensions.python.installers.SelectVirtualEnvPythonInstaller;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

/**
 * Python nodes
 */
@Plugin(type = JIPipeJavaExtension.class)
public class PythonExtension extends JIPipePrepackagedDefaultJavaExtension {
    @Override
    public String getName() {
        return "Python integration";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Provides algorithms and data types that allow Python scripting");
    }

    @Override
    public void register() {
        PythonExtensionSettings settings = new PythonExtensionSettings();

        registerEnvironment(PythonEnvironment.class,
                PythonEnvironment.List.class,
                settings,
                "python",
                "Python environment",
                "A Python environment",
                UIUtils.getIconFromResources("apps/python.png"));
        registerParameterType("optional-python-environment",
                OptionalPythonEnvironment.class,
                null,
                null,
                "Optional Python environment",
                "An optional Python environment",
                null);

        // JIPipe Python adapter
        registerEnvironment(JIPipePythonAdapterLibraryEnvironment.class,
                JIPipePythonAdapterLibraryEnvironment.List.class,
                settings,
                "jipipe-python-adapter-library",
                "JIPipe Python adapter library",
                "Additional library for Python",
                UIUtils.getIconFromResources("actions/plugins.png"));
        registerEnvironmentInstaller(JIPipePythonAdapterLibraryEnvironment.class,
                JIPipePythonAdapterLibraryEnvironmentInstaller.class,
                UIUtils.getIconFromResources("actions/browser-download.png"));

        registerEnumParameterType("python-environment-type",
                PythonEnvironmentType.class,
                "Python environment type",
                "A Python environment type");
        registerSettingsSheet(PythonExtensionSettings.ID,
                "Python integration",
                UIUtils.getIconFromResources("apps/python.png"),
                "Extensions",
                UIUtils.getIconFromResources("actions/plugins.png"),
                settings);
        registerEnvironmentInstaller(PythonEnvironment.class, MinicondaEnvPythonInstaller.class, UIUtils.getIconFromResources("actions/browser-download.png"));
        registerEnvironmentInstaller(PythonEnvironment.class, SelectCondaEnvPythonInstaller.class, UIUtils.getIconFromResources("actions/project-open.png"));
        registerEnvironmentInstaller(PythonEnvironment.class, SelectSystemPythonInstaller.class, UIUtils.getIconFromResources("actions/project-open.png"));
        registerEnvironmentInstaller(PythonEnvironment.class, SelectVirtualEnvPythonInstaller.class, UIUtils.getIconFromResources("actions/project-open.png"));

        registerNodeType("python-script", JythonScriptAlgorithm.class, UIUtils.getIconURLFromResources("apps/python.png"));
        registerNodeType("python-script-iterating-simple", SimpleIteratingJythonScriptAlgorithm.class, UIUtils.getIconURLFromResources("apps/python.png"));
        registerNodeType("python-script-iterating", IteratingJythonScriptAlgorithm.class, UIUtils.getIconURLFromResources("apps/python.png"));
        registerNodeType("python-script-merging", MergingJythonScriptAlgorithm.class, UIUtils.getIconURLFromResources("apps/python.png"));
        registerNodeType("cpython-script", PythonScriptAlgorithm.class, UIUtils.getIconURLFromResources("apps/python.png"));
        registerNodeType("cpython-script-iterating", IteratingPythonScriptAlgorithm.class, UIUtils.getIconURLFromResources("apps/python.png"));
        registerNodeType("cpython-script-merging", MergingPythonScriptAlgorithm.class, UIUtils.getIconURLFromResources("apps/python.png"));
    }

    @Override
    public List<ImageIcon> getSplashIcons() {
        return Arrays.asList(UIUtils.getIcon32FromResources("apps/python.png"));
    }

    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:python";
    }

    @Override
    public String getDependencyVersion() {
        return "2021.6";
    }
}
