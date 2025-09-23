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
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

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
    public void register(JIPipeService service, Context context, JIPipeProgressInfo progressInfo) {
        registerMenuExtension(PublishProjectReportMenuExtension.class);
        registerMenuExtension(PublishLegacyArchiveMenuExtension.class);
        registerMenuExtension(PublishROCrateMenuExtension.class);
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
