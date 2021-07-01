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
import org.hkijena.jipipe.api.notifications.JIPipeNotification;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationAction;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.parameters.primitives.HTMLText;
import org.hkijena.jipipe.extensions.parameters.primitives.StringList;
import org.hkijena.jipipe.extensions.python.algorithms.IteratingJythonScriptAlgorithm;
import org.hkijena.jipipe.extensions.python.algorithms.IteratingPythonScriptAlgorithm;
import org.hkijena.jipipe.extensions.python.algorithms.JythonScriptAlgorithm;
import org.hkijena.jipipe.extensions.python.algorithms.MergingJythonScriptAlgorithm;
import org.hkijena.jipipe.extensions.python.algorithms.MergingPythonScriptAlgorithm;
import org.hkijena.jipipe.extensions.python.algorithms.PythonScriptAlgorithm;
import org.hkijena.jipipe.extensions.python.algorithms.SimpleIteratingJythonScriptAlgorithm;
import org.hkijena.jipipe.extensions.python.installers.MinicondaEnvPythonInstaller;
import org.hkijena.jipipe.extensions.python.installers.SelectCondaEnvPythonInstaller;
import org.hkijena.jipipe.extensions.python.installers.SelectSystemPythonInstaller;
import org.hkijena.jipipe.extensions.python.installers.SelectVirtualEnvPythonInstaller;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.DocumentTabPane;
import org.hkijena.jipipe.ui.running.JIPipeRunExecuterUI;
import org.hkijena.jipipe.ui.settings.JIPipeApplicationSettingsUI;
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
    public void postprocess() {
        if(!PythonExtensionSettings.pythonSettingsAreValid()) {
            JIPipeNotification notification = new JIPipeNotification(getDependencyId() + ":python-not-configured");
            notification.setHeading("Python is not configured");
            notification.setDescription("To make use of Python within JIPipe, you need to either provide JIPipe with an " +
                    "existing Python installation or let JIPipe install a Python distribution for you.");
            notification.getActions().add(new JIPipeNotificationAction("Install Miniconda",
                    "Installs Miniconda 3",
                    UIUtils.getIconFromResources("actions/browser-download.png"),
                    PythonExtension::installConda));
            notification.getActions().add(new JIPipeNotificationAction("Select Conda",
                    "Selects an existing Conda installation",
                    UIUtils.getIconFromResources("actions/folder-open.png"),
                    PythonExtension::selectConda));
            notification.getActions().add(new JIPipeNotificationAction("Configure Python",
                    "Opens the applications settings page",
                    UIUtils.getIconFromResources("actions/configure.png"),
                    PythonExtension::openSettingsPage));
            JIPipeNotificationInbox.getInstance().push(notification);
        }
        if(!PythonExtensionSettings.getInstance().getPythonAdapterLibraryEnvironment().isNewestVersion()) {
            JIPipeNotification notification = new JIPipeNotification(getDependencyId() + ":old-python-adapter");
            notification.setHeading("Old library version");
            notification.setDescription("JIPipe has detected that the installed version of the JIPipe Python adapter library is outdated. " +
                    "Please click the button below to install the newest version.");
            notification.getActions().add(new JIPipeNotificationAction("Install newest version",
                    "Installs the newest version of the Python library",
                    UIUtils.getIconFromResources("actions/run-install.png"),
                    PythonExtension::installPythonAdapterLibrary));
            notification.getActions().add(new JIPipeNotificationAction("Configure",
                    "Opens the applications settings page",
                    UIUtils.getIconFromResources("actions/configure.png"),
                    PythonExtension::openSettingsPage));
            JIPipeNotificationInbox.getInstance().push(notification);
        }
    }

    private static void installPythonAdapterLibrary(JIPipeWorkbench workbench) {
        PythonExtensionSettings settings = PythonExtensionSettings.getInstance();
        JIPipeParameterTree tree = new JIPipeParameterTree(settings);
        JIPipeParameterAccess parameterAccess = tree.getParameters().get("python-adapter-library");
        JIPipePythonAdapterLibraryEnvironmentInstaller installer = new JIPipePythonAdapterLibraryEnvironmentInstaller(workbench, parameterAccess);
        JIPipeRunExecuterUI.runInDialog(workbench.getWindow(), installer);
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
        return "2021.7";
    }

    private static void installConda(JIPipeWorkbench workbench) {
        PythonExtensionSettings settings = PythonExtensionSettings.getInstance();
        JIPipeParameterTree tree = new JIPipeParameterTree(settings);
        JIPipeParameterAccess parameterAccess = tree.getParameters().get("python-environment");
        MinicondaEnvPythonInstaller installer = new MinicondaEnvPythonInstaller(workbench, parameterAccess);
        JIPipeRunExecuterUI.runInDialog(workbench.getWindow(), installer);
    }

    private static void selectConda(JIPipeWorkbench workbench) {
        PythonExtensionSettings settings = PythonExtensionSettings.getInstance();
        JIPipeParameterTree tree = new JIPipeParameterTree(settings);
        JIPipeParameterAccess parameterAccess = tree.getParameters().get("python-environment");
        SelectCondaEnvPythonInstaller installer = new SelectCondaEnvPythonInstaller(workbench, parameterAccess);
        JIPipeRunExecuterUI.runInDialog(workbench.getWindow(), installer);
    }

    private static void openSettingsPage(JIPipeWorkbench workbench) {
        DocumentTabPane.DocumentTab tab = workbench.getDocumentTabPane().selectSingletonTab(JIPipeProjectWorkbench.TAB_APPLICATION_SETTINGS);
        JIPipeApplicationSettingsUI applicationSettingsUI = (JIPipeApplicationSettingsUI) tab.getContent();
        applicationSettingsUI.selectNode("/Extensions/Python integration");
    }
}
