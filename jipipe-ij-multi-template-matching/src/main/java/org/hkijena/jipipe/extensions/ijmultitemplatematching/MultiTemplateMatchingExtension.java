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

package org.hkijena.jipipe.extensions.ijmultitemplatematching;

import org.apache.commons.compress.utils.Sets;
import org.hkijena.jipipe.*;
import org.hkijena.jipipe.api.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.core.CoreExtension;
import org.hkijena.jipipe.extensions.imagejdatatypes.ImageJDataTypesExtension;
import org.hkijena.jipipe.extensions.parameters.library.images.ImageParameter;
import org.hkijena.jipipe.extensions.parameters.library.jipipe.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.extensions.strings.StringsExtension;
import org.hkijena.jipipe.extensions.tables.TablesExtension;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Extension that adds ImageJ2 algorithms
 */
@Plugin(type = JIPipeJavaExtension.class)
public class MultiTemplateMatchingExtension extends JIPipePrepackagedDefaultJavaExtension {

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:ij-multi-template-matching",
            JIPipe.getJIPipeVersion(),
            "Multi-Template matching");

    public MultiTemplateMatchingExtension() {
    }

    @Override
    public Set<JIPipeDependency> getDependencies() {
        return Sets.newHashSet(CoreExtension.AS_DEPENDENCY, TablesExtension.AS_DEPENDENCY, StringsExtension.AS_DEPENDENCY, ImageJDataTypesExtension.AS_DEPENDENCY);
    }

    @Override
    public PluginCategoriesEnumParameter.List getCategories() {
        return new PluginCategoriesEnumParameter.List(PluginCategoriesEnumParameter.CATEGORY_FEATURE_EXTRACTION, PluginCategoriesEnumParameter.CATEGORY_OBJECT_DETECTION);
    }

    @Override
    public ImageParameter getThumbnail() {
        return new ImageParameter(ResourceUtils.getPluginResource("thumbnails/multi-template-matching.png"));
    }

    @Override
    public JIPipeAuthorMetadata.List getAcknowledgements() {
        return new JIPipeAuthorMetadata.List(new JIPipeAuthorMetadata("", "Laurent S. V.", "Thomas", new StringList("Acquifer is a division of Ditabis, Digital Biomedical Imaging Systems AG, Pforzheim, Germany",
                "Centre of Paediatrics and Adolescent Medicine, University Hospital Heidelberg, Heidelberg, Germany"), "", "", true, false),
                new JIPipeAuthorMetadata("", "Jochen", "Gehrig", new StringList("Acquifer is a division of Ditabis, Digital Biomedical Imaging Systems AG, Pforzheim, Germany"), "", "", true, false));
    }

    @Override
    public StringList getDependencyCitations() {
        StringList result = new StringList();
        result.add("Thomas, L.S.V., Gehrig, J. Multi-template matching: a versatile tool for object-localization in microscopy images.\n" +
                "BMC Bioinformatics 21, 44 (2020). https://doi.org/10.1186/s12859-020-3363-7");
        return result;
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:ij-multi-template-matching";
    }

    @Override
    public List<JIPipeImageJUpdateSiteDependency> getImageJUpdateSiteDependencies() {
        return Arrays.asList(new JIPipeImageJUpdateSiteDependency("IJ-OpenCV-plugins", "https://sites.imagej.net/IJ-OpenCV/"),
                new JIPipeImageJUpdateSiteDependency("Multi-Template-Matching", "https://sites.imagej.net/Multi-Template-Matching/"));
    }

    @Override
    public String getName() {
        return "Multi-Template matching";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Integrates the Multi-Template matching algorithm by L.Thomas and J.Gehrig into JIPipe.");
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
        registerEnumParameterType("ij-multi-template-matching:matching-method", TemplateMatchingMethod.class, "Template matching method", "Formula used to compute the probability map");
        registerNodeType("ij-multi-template-matching", MultiTemplateMatchingAlgorithm.class, UIUtils.getIconURLFromResources("actions/paint-pattern.png"));
    }


}
