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

import org.apache.commons.compress.utils.Sets;
import org.hkijena.jipipe.*;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.core.CoreExtension;
import org.hkijena.jipipe.extensions.forms.algorithms.IteratingFormProcessorAlgorithm;
import org.hkijena.jipipe.extensions.forms.algorithms.MergingFormProcessorAlgorithm;
import org.hkijena.jipipe.extensions.forms.algorithms.SimpleIteratingFormProcessorAlgorithm;
import org.hkijena.jipipe.extensions.forms.algorithms.generators.*;
import org.hkijena.jipipe.extensions.forms.datatypes.*;
import org.hkijena.jipipe.extensions.parameters.library.jipipe.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.extensions.strings.StringsExtension;
import org.hkijena.jipipe.extensions.tables.TablesExtension;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Extension that adds forms
 */
@Plugin(type = JIPipeJavaExtension.class)
public class FormsExtension extends JIPipePrepackagedDefaultJavaExtension {

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:forms",
            JIPipe.getJIPipeVersion(),
            "Forms");

    public FormsExtension() {
    }

    @Override
    public PluginCategoriesEnumParameter.List getCategories() {
        return new PluginCategoriesEnumParameter.List(PluginCategoriesEnumParameter.CATEGORY_INTERACTIVE, PluginCategoriesEnumParameter.CATEGORY_USER_INTERFACE);
    }

    @Override
    public Set<JIPipeDependency> getDependencies() {
        return Sets.newHashSet(CoreExtension.AS_DEPENDENCY, TablesExtension.AS_DEPENDENCY, StringsExtension.AS_DEPENDENCY);
    }

    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:forms";
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
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
        registerDatatype("form", FormData.class, UIUtils.getIconURLFromResources("data-types/form.png"));

        registerDatatype("group-header-form", GroupHeaderFormData.class, UIUtils.getIconURLFromResources("data-types/form.png"));
        registerDatatype("string-form", StringFormData.class, UIUtils.getIconURLFromResources("data-types/form.png"));
        registerDatatype("integer-form", IntegerFormData.class, UIUtils.getIconURLFromResources("data-types/form.png"));
        registerDatatype("number-form", DoubleFormData.class, UIUtils.getIconURLFromResources("data-types/form.png"));
        registerDatatype("boolean-form", BooleanFormData.class, UIUtils.getIconURLFromResources("data-types/form.png"));
        registerDatatype("enum-form", EnumFormData.class, UIUtils.getIconURLFromResources("data-types/form.png"));
        registerDatatype("path-form", PathFormData.class, UIUtils.getIconURLFromResources("data-types/form.png"));

        registerNodeType("group-header-form", GroupHeaderFormGeneratorAlgorithm.class, UIUtils.getIconURLFromResources("data-types/form.png"));
        registerNodeType("string-form", StringFormGeneratorAlgorithm.class, UIUtils.getIconURLFromResources("data-types/form.png"));
        registerNodeType("integer-form", IntegerFormGeneratorAlgorithm.class, UIUtils.getIconURLFromResources("data-types/form.png"));
        registerNodeType("number-form", DoubleFormGeneratorAlgorithm.class, UIUtils.getIconURLFromResources("data-types/form.png"));
        registerNodeType("boolean-form", BooleanFormGeneratorAlgorithm.class, UIUtils.getIconURLFromResources("data-types/form.png"));
        registerNodeType("enum-form", EnumFormGeneratorAlgorithm.class, UIUtils.getIconURLFromResources("data-types/form.png"));
        registerNodeType("path-form", PathFormGeneratorAlgorithm.class, UIUtils.getIconURLFromResources("data-types/form.png"));

        registerNodeType("form-processor-simple-iterating", SimpleIteratingFormProcessorAlgorithm.class, UIUtils.getIconURLFromResources("data-types/form.png"));
        registerNodeType("form-processor-merging", MergingFormProcessorAlgorithm.class, UIUtils.getIconURLFromResources("data-types/form.png"));
        registerNodeType("form-processor-iterating", IteratingFormProcessorAlgorithm.class, UIUtils.getIconURLFromResources("data-types/form.png"));
    }

}
