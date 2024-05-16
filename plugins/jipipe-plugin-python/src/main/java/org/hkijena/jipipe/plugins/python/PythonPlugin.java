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

package org.hkijena.jipipe.plugins.python;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.plugins.python.adapter.JIPipePythonAdapterLibraryEnvironment;
import org.hkijena.jipipe.plugins.python.adapter.OptionalJIPipePythonAdapterLibraryEnvironment;
import org.hkijena.jipipe.plugins.python.adapter.JIPipePythonPluginAdapterApplicationSettings;
import org.hkijena.jipipe.plugins.python.algorithms.jython.IteratingJythonScriptAlgorithm;
import org.hkijena.jipipe.plugins.python.algorithms.jython.JythonScriptAlgorithm;
import org.hkijena.jipipe.plugins.python.algorithms.jython.MergingJythonScriptAlgorithm;
import org.hkijena.jipipe.plugins.python.algorithms.jython.SimpleIteratingJythonScriptAlgorithm;
import org.hkijena.jipipe.plugins.python.algorithms.python.IteratingPythonScriptAlgorithm;
import org.hkijena.jipipe.plugins.python.algorithms.python.MergingPythonScriptAlgorithm;
import org.hkijena.jipipe.plugins.python.algorithms.python.PythonScriptAlgorithm;
import org.hkijena.jipipe.plugins.python.installers.SelectCondaEnvPythonInstaller;
import org.hkijena.jipipe.plugins.python.installers.SelectSystemPythonInstaller;
import org.hkijena.jipipe.plugins.python.installers.SelectVirtualEnvPythonInstaller;
import org.hkijena.jipipe.utils.JIPipeResourceManager;
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

    public static final JIPipeResourceManager RESOURCES = new JIPipeResourceManager(PythonPlugin.class, "org/hkijena/jipipe/plugins/python");

    public PythonPlugin() {
        getMetadata().addCategories(PluginCategoriesEnumParameter.CATEGORY_SCRIPTING);
    }

    public static PythonEnvironment getEnvironment(JIPipeProject project, OptionalPythonEnvironment nodeEnvironment) {
        if(nodeEnvironment.isEnabled()) {
            return nodeEnvironment.getContent();
        }
        if(project != null && project.getSettingsSheet(PythonPluginProjectSettings.class).getProjectDefaultEnvironment().isEnabled()) {
            return project.getSettingsSheet(PythonPluginProjectSettings.class).getProjectDefaultEnvironment().getContent();
        }
        return PythonPluginApplicationSettings.getInstance().getDefaultPythonEnvironment();
    }

    public static JIPipePythonAdapterLibraryEnvironment getAdapterEnvironment(JIPipeProject project) {
        if(project != null && project.getSettingsSheet(PythonPluginProjectSettings.class).getProjectPythonAdapterLibraryEnvironment().isEnabled()) {
            return project.getSettingsSheet(PythonPluginProjectSettings.class).getProjectPythonAdapterLibraryEnvironment().getContent();
        }
        return JIPipePythonPluginAdapterApplicationSettings.getInstance().getDefaultPythonAdapterLibraryEnvironment();
    }

    @Override
    public StringList getDependencyProvides() {
        return new StringList();
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
        PythonPluginApplicationSettings settings = new PythonPluginApplicationSettings();
        JIPipePythonPluginAdapterApplicationSettings adapterExtensionSettings = new JIPipePythonPluginAdapterApplicationSettings();

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

        registerEnumParameterType("python-environment-type",
                PythonEnvironmentType.class,
                "Python environment type",
                "A Python environment type");
        registerApplicationSettingsSheet(settings);
        registerApplicationSettingsSheet(adapterExtensionSettings);
        registerProjectSettingsSheet(PythonPluginProjectSettings.class);

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

        registerNodeExamplesFromResources(RESOURCES, "examples");
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
