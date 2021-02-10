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

package org.hkijena.jipipe.extensions.forms;

import org.hkijena.jipipe.JIPipeImageJUpdateSiteDependency;
import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.forms.algorithms.SimpleIteratingFormProcessorAlgorithm;
import org.hkijena.jipipe.extensions.forms.algorithms.StringFormGeneratorAlgorithm;
import org.hkijena.jipipe.extensions.forms.datatypes.FormData;
import org.hkijena.jipipe.extensions.forms.datatypes.StringFormData;
import org.hkijena.jipipe.extensions.parameters.primitives.HTMLText;
import org.hkijena.jipipe.extensions.parameters.primitives.StringList;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.plugin.Plugin;

import java.util.Collections;
import java.util.List;

/**
 * Extension that adds forms
 */
@Plugin(type = JIPipeJavaExtension.class)
public class FormsExtension extends JIPipePrepackagedDefaultJavaExtension {

    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public String getDependencyId() {
        return "forms";
    }

    @Override
    public String getDependencyVersion() {
        return "2021.2";
    }

    @Override
    public List<JIPipeImageJUpdateSiteDependency> getImageJUpdateSiteDependencies() {
        return Collections.emptyList();
    }

    @Override
    public String getName() {
        return "Forms";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Provides core functionality to include runtime user interactions into pipelines.");
    }

    @Override
    public void register() {
        registerDatatype("form", FormData.class, UIUtils.getIconURLFromResources("data-types/form.png"), null, null);
        registerDatatype("string-form", StringFormData.class, UIUtils.getIconURLFromResources("data-types/form.png"), null, null);

        registerNodeType("string-form", StringFormGeneratorAlgorithm.class, UIUtils.getIconURLFromResources("data-types/form.png"));

        registerNodeType("form-processor-simple-iterating", SimpleIteratingFormProcessorAlgorithm.class, UIUtils.getIconURLFromResources("data-types/form.png"));
    }

}
