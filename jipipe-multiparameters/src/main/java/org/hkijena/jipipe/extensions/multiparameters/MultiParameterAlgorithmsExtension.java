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

package org.hkijena.jipipe.extensions.multiparameters;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.multiparameters.nodes.*;
import org.hkijena.jipipe.extensions.parameters.library.jipipe.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

/**
 * Extension that provides capabilities to run multiple parameters
 */
@Plugin(type = JIPipeJavaExtension.class)
public class MultiParameterAlgorithmsExtension extends JIPipePrepackagedDefaultJavaExtension {

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:multi-parameters-algorithms",
            JIPipe.getJIPipeVersion(),
            "Multi parameter algorithms");

    public MultiParameterAlgorithmsExtension() {
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
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
        JIPipe.getNodes().getEventBus().register(this);

        // Register algorithms
        registerNodeType("parameters-define", DefineParametersAlgorithm.class);
        registerNodeType("parameters-define-table", DefineParametersTableAlgorithm.class);
        registerNodeType("parameters-define-table-expression", GenerateParametersFromExpressionAlgorithm.class);
        registerNodeType("parameters-from-node", ExtractParametersAlgorithm.class);

        registerNodeType("parameters-to-annotations", ParametersToAnnotationsAlgorithm.class, UIUtils.getIconURLFromResources("data-types/parameters.png"));
        registerNodeType("annotations-to-parameters", AnnotationsToParametersAlgorithm.class, UIUtils.getIconURLFromResources("data-types/parameters.png"));
        registerNodeType("parameters-merge", MergeParametersAlgorithm.class, UIUtils.getIconURLFromResources("actions/rabbitvcs-merge.png"));
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:multi-parameters-algorithms";
    }


}
