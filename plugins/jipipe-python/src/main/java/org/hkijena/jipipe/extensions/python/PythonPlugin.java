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

package org.hkijena.jipipe.extensions.python;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.notifications.JIPipeNotification;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationAction;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.running.JIPipeDesktopRunExecuterUI;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.extensions.parameters.library.images.ImageParameter;
import org.hkijena.jipipe.extensions.parameters.library.jipipe.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.extensions.python.adapter.*;
import org.hkijena.jipipe.extensions.python.algorithms.*;
import org.hkijena.jipipe.extensions.python.installers.*;
import org.hkijena.jipipe.utils.JIPipeResourceManager;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

/**
 * Python nodes
 */
@Plugin(type = JIPipeJavaPlugin.class)
public class PythonPlugin extends JIPipePrepackagedDefaultJavaPlugin {

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:python",
            JIPipe.getJIPipeVersion(),
            "Python integration");

    public static final JIPipeResourceManager RESOURCES = new JIPipeResourceManager(PythonPlugin.class, "org/hkijena/jipipe/extensions/python");

    public PythonPlugin() {
        getMetadata().addCategories(PluginCategoriesEnumParameter.CATEGORY_SCRIPTING);
        getMetadata().setThumbnail(new ImageParameter(ResourceUtils.getPluginResource("thumbnails/python.png")));
    }

    private static void easyInstallPython(JIPipeWorkbench workbench) {
        PythonExtensionSettings settings = PythonExtensionSettings.getInstance();
        JIPipeParameterTree tree = new JIPipeParameterTree(settings);
        JIPipeParameterAccess parameterAccess = tree.getParameters().get("python-environment");
        PythonEasyInstaller installer = new PythonEasyInstaller((JIPipeDesktopWorkbench) workbench, parameterAccess);
        JIPipeDesktopRunExecuterUI.runInDialog((JIPipeDesktopWorkbench) workbench, ((JIPipeDesktopWorkbench) workbench).getWindow(), installer);
    }

    private static void easyInstallPythonAdapter(JIPipeWorkbench workbench) {
        PythonAdapterExtensionSettings settings = PythonAdapterExtensionSettings.getInstance();
        JIPipeParameterTree tree = new JIPipeParameterTree(settings);
        JIPipeParameterAccess parameterAccess = tree.getParameters().get("python-adapter-library");
        JIPipePythonAdapterLibraryEnvironmentInstaller installer = new JIPipePythonAdapterLibraryEnvironmentInstaller((JIPipeDesktopWorkbench) workbench, parameterAccess);
        JIPipeDesktopRunExecuterUI.runInDialog((JIPipeDesktopWorkbench) workbench, ((JIPipeDesktopWorkbench) workbench).getWindow(), installer);
    }

    private static void openSettingsPage(JIPipeWorkbench workbench) {
        if (workbench instanceof JIPipeDesktopProjectWorkbench) {
            ((JIPipeDesktopProjectWorkbench) workbench).openApplicationSettings("/Extensions/Python integration");
        }
    }

    private static void openAdapterSettingsPage(JIPipeWorkbench workbench) {
        if (workbench instanceof JIPipeDesktopProjectWorkbench) {
            ((JIPipeDesktopProjectWorkbench) workbench).openApplicationSettings("/Extensions/Python integration (adapter)");
        }
    }

    public static void createMissingPythonNotificationIfNeeded(JIPipeNotificationInbox inbox) {
        if (!PythonExtensionSettings.pythonSettingsAreValid(new UnspecifiedValidationReportContext())) {
            JIPipeNotification notification = new JIPipeNotification(AS_DEPENDENCY.getDependencyId() + ":python-not-configured");
            notification.setHeading("Python is not configured");
            notification.setDescription("To make use of Python within JIPipe, you need to either provide JIPipe with an " +
                    "existing Python installation or let JIPipe install a Python distribution for you. " +
                    "Click 'Open settings' to let JIPipe setup a Python distribution automatically. " +
                    "Alternatively, click 'Configure' to visit the settings page with more options, including the selection of an existing Python environment.\n\n" +
                    "For more information, please visit https://www.jipipe.org/installation/third-party/python/");
            notification.getActions().add(new JIPipeNotificationAction("Install Python",
                    "Installs a pre-packaged Python distribution",
                    UIUtils.getIconInvertedFromResources("actions/download.png"),
                    JIPipeNotificationAction.Style.Success,
                    PythonPlugin::easyInstallPython));
            notification.getActions().add(new JIPipeNotificationAction("Open settings",
                    "Opens the applications settings page",
                    UIUtils.getIconFromResources("actions/configure.png"),
                    PythonPlugin::openSettingsPage));
            inbox.push(notification);
        }
    }

    public static void createMissingLibJIPipePythonNotificationIfNeeded(JIPipeNotificationInbox inbox) {
        if (!PythonAdapterExtensionSettings.pythonSettingsAreValid()) {
            JIPipeNotification notification = new JIPipeNotification(AS_DEPENDENCY.getDependencyId() + ":missing-lib-jipipe-python");
            notification.setHeading("Missing Python adapter library");
            notification.setDescription("To make use of Python within JIPipe, you need to install the JIPipe Python adapter. " +
                    "Click 'Install Python adapter' to install the adapter from an online source. " +
                    "Alternatively, click 'Open settings' to visit the settings page with more options.");
            notification.getActions().add(new JIPipeNotificationAction("Install Python adapter",
                    "Installs a pre-packaged Python distribution",
                    UIUtils.getIconInvertedFromResources("actions/download.png"),
                    JIPipeNotificationAction.Style.Success,
                    PythonPlugin::easyInstallPythonAdapter));
            notification.getActions().add(new JIPipeNotificationAction("Open settings",
                    "Opens the applications settings page",
                    UIUtils.getIconFromResources("actions/configure.png"),
                    PythonPlugin::openAdapterSettingsPage));
            inbox.push(notification);
        }
    }

    public static void createOldLibJIPipePythonNotification(JIPipeNotificationInbox inbox, String currentVersion, String updateVersion) {
        JIPipeNotification notification = new JIPipeNotification(AS_DEPENDENCY.getDependencyId() + ":old-lib-jipipe-python");
        notification.setHeading("Old Python adapter library");
        notification.setDescription("The currently installed version of the JIPipe Python update library is " + currentVersion + ". A new version (v" + updateVersion + ") is available. " +
                "Click 'Update Python adapter' to install the adapter from an online source. " +
                "Alternatively, click 'Open settings' to visit the settings page with more options.");
        notification.getActions().add(new JIPipeNotificationAction("Update Python adapter",
                "Installs a pre-packaged Python distribution",
                UIUtils.getIconInvertedFromResources("actions/download.png"),
                JIPipeNotificationAction.Style.Success,
                PythonPlugin::easyInstallPythonAdapter));
        notification.getActions().add(new JIPipeNotificationAction("Open settings",
                "Opens the applications settings page",
                UIUtils.getIconFromResources("actions/configure.png"),
                PythonPlugin::openAdapterSettingsPage));
        inbox.push(notification);
    }

    @Override
    public boolean isBeta() {
        return true;
    }

    @Override
    public String getName() {
        return "Python integration";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Provides algorithms and data types that allow Python scripting");
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
        PythonExtensionSettings settings = new PythonExtensionSettings();
        PythonAdapterExtensionSettings adapterExtensionSettings = new PythonAdapterExtensionSettings();

        registerEnvironment(PythonEnvironment.class,
                PythonEnvironment.List.class,
                settings,
                PythonEnvironment.ENVIRONMENT_ID,
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
                JIPipePythonAdapterLibraryEnvironment.ENVIRONMENT_ID,
                "JIPipe Python adapter library",
                "Additional library for Python",
                UIUtils.getIconFromResources("actions/plugins.png"));
        registerParameterType("optional-" + JIPipePythonAdapterLibraryEnvironment.ENVIRONMENT_ID,
                OptionalJIPipePythonAdapterLibraryEnvironment.class,
                null,
                null,
                "Optional JIPipe Python adapter library",
                "An optional JIPipe Python adapter library",
                null);
        registerEnvironmentInstaller(JIPipePythonAdapterLibraryEnvironment.class,
                JIPipePythonAdapterLibraryEnvironmentInstaller.class,
                UIUtils.getIconFromResources("actions/download.png"));

        registerEnumParameterType("python-environment-type",
                PythonEnvironmentType.class,
                "Python environment type",
                "A Python environment type");
        registerSettingsSheet(PythonExtensionSettings.ID,
                "Python integration",
                "Connect existing Python installations to JIPipe or automatically install a new Python environment if none is available",
                UIUtils.getIconFromResources("apps/python.png"),
                "Extensions",
                UIUtils.getIconFromResources("actions/plugins.png"),
                settings);
        registerSettingsSheet(PythonAdapterExtensionSettings.ID,
                "Python integration (adapter)",
                "Settings for the Python adapter library that is utilized by JIPipe",
                UIUtils.getIconFromResources("apps/python.png"),
                "Extensions",
                UIUtils.getIconFromResources("actions/plugins.png"),
                adapterExtensionSettings);

        registerEnvironmentInstaller(PythonEnvironment.class, MinicondaEnvPythonInstaller.class, UIUtils.getIconFromResources("actions/download.png"));
        registerEnvironmentInstaller(PythonEnvironment.class, PortableEnvPythonInstaller.class, UIUtils.getIconFromResources("actions/download.png"));
        registerEnvironmentInstaller(PythonEnvironment.class, SelectCondaEnvPythonInstaller.class, UIUtils.getIconFromResources("actions/project-open.png"));
        registerEnvironmentInstaller(PythonEnvironment.class, SelectSystemPythonInstaller.class, UIUtils.getIconFromResources("actions/project-open.png"));
        registerEnvironmentInstaller(PythonEnvironment.class, SelectVirtualEnvPythonInstaller.class, UIUtils.getIconFromResources("actions/project-open.png"));
        registerEnvironmentInstaller(PythonEnvironment.class, PythonEasyInstaller.class, UIUtils.getIconFromResources("emblems/emblem-rabbitvcs-normal.png"));

        registerNodeType("python-script", JythonScriptAlgorithm.class, UIUtils.getIconURLFromResources("apps/python.png"));
        registerNodeType("python-script-iterating-simple", SimpleIteratingJythonScriptAlgorithm.class, UIUtils.getIconURLFromResources("apps/python.png"));
        registerNodeType("python-script-iterating", IteratingJythonScriptAlgorithm.class, UIUtils.getIconURLFromResources("apps/python.png"));
        registerNodeType("python-script-merging", MergingJythonScriptAlgorithm.class, UIUtils.getIconURLFromResources("apps/python.png"));
        registerNodeType("cpython-script", PythonScriptAlgorithm.class, UIUtils.getIconURLFromResources("apps/python.png"));
        registerNodeType("cpython-script-iterating", IteratingPythonScriptAlgorithm.class, UIUtils.getIconURLFromResources("apps/python.png"));
        registerNodeType("cpython-script-merging", MergingPythonScriptAlgorithm.class, UIUtils.getIconURLFromResources("apps/python.png"));

        registerNodeExamplesFromResources(RESOURCES, "examples");
    }

    @Override
    public void postprocess(JIPipeProgressInfo progressInfo) {
        createMissingPythonNotificationIfNeeded(JIPipeNotificationInbox.getInstance());
        createMissingLibJIPipePythonNotificationIfNeeded(JIPipeNotificationInbox.getInstance());
        if (PythonAdapterExtensionSettings.getInstance().isCheckForUpdates()) {
            JIPipeRunnableQueue.getInstance().enqueue(new JIPipePythonAdapterUpdateChecker());
        }
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

}
