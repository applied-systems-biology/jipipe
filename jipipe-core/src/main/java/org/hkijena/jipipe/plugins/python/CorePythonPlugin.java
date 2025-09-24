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
import org.hkijena.jipipe.api.environments.JIPipeEnvironmentArchetype;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.plugins.python.adapter.JIPipePythonAdapterLibraryEnvironment;
import org.hkijena.jipipe.plugins.python.adapter.OptionalJIPipePythonAdapterLibraryEnvironment;
import org.hkijena.jipipe.plugins.python.installers.SelectCondaEnvPythonInstaller;
import org.hkijena.jipipe.plugins.python.installers.SelectSystemPythonInstaller;
import org.hkijena.jipipe.plugins.python.installers.SelectVirtualEnvPythonInstaller;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

/**
 * Python nodes
 */
@Plugin(type = JIPipeJavaPlugin.class)
public class CorePythonPlugin extends JIPipePrepackagedDefaultJavaPlugin {

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:python-core",
            JIPipe.getJIPipeVersion(),
            "Python integration");

    public CorePythonPlugin() {
        getMetadata().addCategories(PluginCategoriesEnumParameter.CATEGORY_SCRIPTING);
    }

    @Override
    public boolean isCorePlugin() {
        return true;
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
        return "Python integration (core)";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("The core plugin for Python-based integrations");
    }

    @Override
    public void register(JIPipeService service, Context context, JIPipeProgressInfo progressInfo) {
        registerArtifactEnvironment(PythonEnvironment.ENVIRONMENT_ID,
                "org.python.*",
                JIPipeEnvironmentArchetype.Managed, PythonEnvironment.class,
                OptionalPythonEnvironment.class,
                PythonEnvironment.List.class,
                "Python",
                "A Python environment",
                JIPipe.RESOURCES.getIcon16("apps/python.png"));

        // JIPipe Python adapter
        registerArtifactEnvironment(JIPipePythonAdapterLibraryEnvironment.ENVIRONMENT_ID,
                "org.hkijena.jipipe-python-adapter:*",
                JIPipeEnvironmentArchetype.Managed, JIPipePythonAdapterLibraryEnvironment.class,
                OptionalJIPipePythonAdapterLibraryEnvironment.class,
                JIPipePythonAdapterLibraryEnvironment.List.class,
                "JIPipe Python adapter library",
                "Additional library for Python",
                JIPipe.RESOURCES.getIcon16("actions/plugins.png"));

        registerEnumParameterType("python-environment-type",
                PythonEnvironmentType.class,
                "Python environment type",
                "A Python environment type");

        registerEnvironmentInstaller(PythonEnvironment.class, SelectCondaEnvPythonInstaller.class, JIPipe.RESOURCES.getIcon16("actions/project-open.png"));
        registerEnvironmentInstaller(PythonEnvironment.class, SelectSystemPythonInstaller.class, JIPipe.RESOURCES.getIcon16("actions/project-open.png"));
        registerEnvironmentInstaller(PythonEnvironment.class, SelectVirtualEnvPythonInstaller.class, JIPipe.RESOURCES.getIcon16("actions/project-open.png"));
    }

    @Override
    public List<ImageIcon> getSplashIcons() {
        return Collections.singletonList(JIPipe.RESOURCES.getIcon32("apps/python.png"));
    }

    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:python-core";
    }

}
