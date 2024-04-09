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

package org.hkijena.jipipe.plugins.opencv;

import com.google.common.collect.Sets;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.imagejdatatypes.ImageJDataTypesPlugin;
import org.hkijena.jipipe.plugins.imp.nodes.*;
import org.hkijena.jipipe.plugins.parameters.library.images.ImageParameter;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.utils.JIPipeResourceManager;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

import java.util.Set;

/**
 * Python nodes
 */
@Plugin(type = JIPipeJavaPlugin.class)
public class OpenCvPlugin extends JIPipePrepackagedDefaultJavaPlugin {

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:opencv",
            JIPipe.getJIPipeVersion(),
            "OpenCV Integration");

    public static final JIPipeResourceManager RESOURCES = new JIPipeResourceManager(OpenCvPlugin.class, "org/hkijena/jipipe/plugins/opencv");

    public OpenCvPlugin() {
        getMetadata().addCategories(PluginCategoriesEnumParameter.CATEGORY_IMPORT_EXPORT,
                PluginCategoriesEnumParameter.CATEGORY_VISUALIZATION);
        getMetadata().setThumbnail(new ImageParameter(RESOURCES.getResourceURL("thumbnail.png")));
    }


    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public StringList getDependencyProvides() {
        return new StringList();
    }

    @Override
    public String getName() {
        return "OpenCV Integration";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Integrates OpenCV algorithms");
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:opencv";
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
    }

    @Override
    public Set<JIPipeDependency> getAllDependencies() {
        return Sets.newHashSet(ImageJDataTypesPlugin.AS_DEPENDENCY);
    }
}
