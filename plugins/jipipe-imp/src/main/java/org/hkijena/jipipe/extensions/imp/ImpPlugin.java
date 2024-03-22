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

package org.hkijena.jipipe.extensions.imp;

import com.google.common.collect.Sets;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.extensions.imagejdatatypes.ImageJDataTypesPlugin;
import org.hkijena.jipipe.extensions.imp.datatypes.ImageJImageToImpImageDataTypeConverter;
import org.hkijena.jipipe.extensions.imp.datatypes.ImpImageData;
import org.hkijena.jipipe.extensions.imp.datatypes.ImpImageOutputFormat;
import org.hkijena.jipipe.extensions.imp.datatypes.ImpImageToImageJImageDataTypeConverter;
import org.hkijena.jipipe.extensions.imp.nodes.*;
import org.hkijena.jipipe.extensions.parameters.library.images.ImageParameter;
import org.hkijena.jipipe.extensions.parameters.library.jipipe.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.utils.JIPipeResourceManager;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

import java.util.Set;

/**
 * Python nodes
 */
@Plugin(type = JIPipeJavaPlugin.class)
public class ImpPlugin extends JIPipePrepackagedDefaultJavaPlugin {

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:imp",
            JIPipe.getJIPipeVersion(),
            "Image Manipulation Pipeline");

    public static final JIPipeResourceManager RESOURCES = new JIPipeResourceManager(ImpPlugin.class, "org/hkijena/jipipe/extensions/imp");

    public ImpPlugin() {
        getMetadata().addCategories(PluginCategoriesEnumParameter.CATEGORY_IMPORT_EXPORT,
                PluginCategoriesEnumParameter.CATEGORY_VISUALIZATION);
        getMetadata().setThumbnail(new ImageParameter(RESOURCES.getResourceURL("thumbnail.png")));
    }


    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public String getName() {
        return "Image Manipulation Pipeline";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Provides nodes that implement general non-scientific image manipulation techniques");
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:imp";
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
        registerDatatype("imp-image", ImpImageData.class, RESOURCES.getIconURLFromResources("imp-image.png"));
        registerDatatypeConversion(new ImageJImageToImpImageDataTypeConverter());
        registerDatatypeConversion(new ImpImageToImageJImageDataTypeConverter());

        registerNodeType("import-imp-image", ImportImpImageAlgorithm.class);
        registerNodeType("export-imp-image", ExportImpImageAlgorithm.class, UIUtils.getIconURLFromResources("actions/document-export.png"));
        registerNodeType("split-imp-image-alpha", SplitImpAlphaChannelAlgorithm.class, UIUtils.getIconURLFromResources("actions/split.png"));
        registerNodeType("set-imp-image-alpha", SetImpAlphaChannelAlgorithm.class, UIUtils.getIconURLFromResources("actions/adjusthsl.png"));
        registerNodeType("convert-imp-to-imagej", ConvertImpImageToImagePlusAlgorithm.class, UIUtils.getIconURLFromResources("actions/cm_refresh.png"));

        registerEnumParameterType("imp-image-output-format", ImpImageOutputFormat.class, "IMP image output format", "Output format supported by IMP");
    }

    @Override
    public Set<JIPipeDependency> getAllDependencies() {
        return Sets.newHashSet(ImageJDataTypesPlugin.AS_DEPENDENCY);
    }
}
