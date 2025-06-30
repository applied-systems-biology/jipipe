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

package org.hkijena.jipipe.plugins.publish;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameterArchetype;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.plugins.python.*;
import org.hkijena.jipipe.plugins.python.adapter.JIPipePythonAdapterLibraryEnvironment;
import org.hkijena.jipipe.plugins.python.adapter.JIPipePythonPluginAdapterApplicationSettings;
import org.hkijena.jipipe.plugins.python.adapter.OptionalJIPipePythonAdapterLibraryEnvironment;
import org.hkijena.jipipe.plugins.python.installers.SelectCondaEnvPythonInstaller;
import org.hkijena.jipipe.plugins.python.installers.SelectSystemPythonInstaller;
import org.hkijena.jipipe.plugins.python.installers.SelectVirtualEnvPythonInstaller;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

/**
 * Python nodes
 */
@Plugin(type = JIPipeJavaPlugin.class)
public class PublishPlugin extends JIPipePrepackagedDefaultJavaPlugin {

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:publish",
            JIPipe.getJIPipeVersion(),
            "Publication");

    public PublishPlugin() {
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
        return "Publication";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Menu items for the 'Publish' menu");
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
        registerMenuExtension(ProjectReportMenuExtension.class);
    }

    @Override
    public List<ImageIcon> getSplashIcons() {
        return Arrays.asList(JIPipe.RESOURCES.getIcon32("actions/share-nodes.png"));
    }

    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:publish";
    }

}
