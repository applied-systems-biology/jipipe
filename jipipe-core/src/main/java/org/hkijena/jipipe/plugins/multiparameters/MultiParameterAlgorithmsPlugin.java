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

package org.hkijena.jipipe.plugins.multiparameters;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.multiparameters.nodes.*;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

/**
 * Extension that provides capabilities to run multiple parameters
 */
@Plugin(type = JIPipeJavaPlugin.class)
public class MultiParameterAlgorithmsPlugin extends JIPipePrepackagedDefaultJavaPlugin {

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:multi-parameters-algorithms",
            JIPipe.getJIPipeVersion(),
            "Multi parameter algorithms");

    public MultiParameterAlgorithmsPlugin() {
        getMetadata().addCategories(PluginCategoriesEnumParameter.CATEGORY_SCRIPTING);
    }

    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public String getName() {
        return "Multi parameter algorithms";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Extension that provides capabilities to run multiple parameters");
    }

    @Override
    public StringList getDependencyProvides() {
        return new StringList();
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {

        // Register algorithms
        registerNodeType("parameters-define", DefineParametersAlgorithm.class);
        registerNodeType("parameters-define-table", DefineParametersTableAlgorithm.class);
        registerNodeType("parameters-define-table-expression", GenerateParametersFromExpressionAlgorithm.class);
        registerNodeType("parameters-from-node", ExtractParametersAlgorithm.class);

        registerNodeType("parameters-to-annotations", ParametersToAnnotationsAlgorithm.class, UIUtils.getIconURLFromResources("data-types/parameters.png"));
        registerNodeType("annotations-to-parameters", AnnotationsToParametersAlgorithm.class, UIUtils.getIconURLFromResources("data-types/parameters.png"));
        registerNodeType("table-to-parameters", DefineParametersFromTableAlgorithm.class, UIUtils.getIconURLFromResources("data-types/parameters.png"));
        registerNodeType("parameters-merge", MergeParametersAlgorithm.class, UIUtils.getIconURLFromResources("actions/rabbitvcs-merge.png"));
        registerNodeType("parameters-multiply", MultiplyParametersAlgorithm.class, UIUtils.getIconURLFromResources("actions/asterisk.png"));
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:multi-parameters-algorithms";
    }

    @Override
    public boolean isCorePlugin() {
        return true;
    }
}
