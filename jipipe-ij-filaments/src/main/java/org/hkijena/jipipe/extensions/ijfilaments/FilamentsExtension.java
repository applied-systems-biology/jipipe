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

package org.hkijena.jipipe.extensions.ijfilaments;

import org.apache.commons.compress.utils.Sets;
import org.hkijena.jipipe.*;
import org.hkijena.jipipe.api.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.core.CoreExtension;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.FilamentsData;
import org.hkijena.jipipe.extensions.ijfilaments.nodes.ImportFilamentsFromJsonAlgorithm;
import org.hkijena.jipipe.extensions.ijfilaments.nodes.SkeletonToFilamentsFijiAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ImageJAlgorithmsExtension;
import org.hkijena.jipipe.extensions.imagejdatatypes.ImageJDataTypesExtension;
import org.hkijena.jipipe.extensions.parameters.library.images.ImageParameter;
import org.hkijena.jipipe.extensions.parameters.library.jipipe.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.extensions.strings.StringsExtension;
import org.hkijena.jipipe.extensions.tables.TablesExtension;
import org.hkijena.jipipe.utils.JIPipeResourceManager;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Extension that adds filaments supports
 */
@Plugin(type = JIPipeJavaExtension.class)
public class FilamentsExtension extends JIPipePrepackagedDefaultJavaExtension {

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:ij-filaments",
            JIPipe.getJIPipeVersion(),
            "Filaments");

    public static final JIPipeResourceManager RESOURCES = new JIPipeResourceManager(FilamentsExtension.class, "org/hkijena/jipipe/extensions/ijfilaments");

    public FilamentsExtension() {
    }

    @Override
    public Set<JIPipeDependency> getDependencies() {
        return Sets.newHashSet(CoreExtension.AS_DEPENDENCY, TablesExtension.AS_DEPENDENCY, StringsExtension.AS_DEPENDENCY, ImageJDataTypesExtension.AS_DEPENDENCY, ImageJAlgorithmsExtension.AS_DEPENDENCY);
    }

    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public PluginCategoriesEnumParameter.List getCategories() {
        return new PluginCategoriesEnumParameter.List(PluginCategoriesEnumParameter.CATEGORY_FEATURE_EXTRACTION, PluginCategoriesEnumParameter.CATEGORY_OBJECT_DETECTION);
    }

    @Override
    public ImageParameter getThumbnail() {
        return new ImageParameter(RESOURCES.getResourceURL("thumbnail.png"));
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:ij-filaments";
    }

    @Override
    public String getName() {
        return "Filaments";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Introduces support for the processing of filaments.");
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
        registerDatatype("filaments", FilamentsData.class, RESOURCES.getIcon16URLFromResources("data-type-filaments.png"));
        registerNodeType("filaments-from-json", ImportFilamentsFromJsonAlgorithm.class);
        registerNodeType("filaments-skeleton-to-filaments", SkeletonToFilamentsFijiAlgorithm.class, UIUtils.getIconURLFromResources("actions/path-mode-spiro.png"));
    }


}
