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

import org.hkijena.jipipe.JIPipeImageJUpdateSiteDependency;
import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.parameters.primitives.HTMLText;
import org.hkijena.jipipe.extensions.parameters.primitives.StringList;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.plugin.Plugin;

import java.util.Arrays;
import java.util.List;

/**
 * Extension that adds ImageJ2 algorithms
 */
@Plugin(type = JIPipeJavaExtension.class)
public class MultiTemplateMatchingExtension extends JIPipePrepackagedDefaultJavaExtension {

    @Override
    public StringList getDependencyCitations() {
        StringList result = new StringList();
        result.add("Thomas, L.S.V., Gehrig, J. Multi-template matching: a versatile tool for object-localization in microscopy images.\n" +
                "BMC Bioinformatics 21, 44 (2020). https://doi.org/10.1186/s12859-020-3363-7");
        return result;
    }

    @Override
    public String getDependencyId() {
        return "ij-multi-template-matching";
    }

    @Override
    public String getDependencyVersion() {
        return "1.52.2";
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
    public void register() {
        registerEnumParameterType("ij-multi-template-matching:matching-method", TemplateMatchingMethod.class, "Template matching method", "Formula used to compute the probability map");
        registerNodeType("ij-multi-template-matching", MultiTemplateMatchingAlgorithm.class, UIUtils.getIconURLFromResources("actions/paint-pattern.png"));
    }


}
