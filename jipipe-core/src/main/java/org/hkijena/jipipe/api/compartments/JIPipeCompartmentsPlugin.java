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

package org.hkijena.jipipe.api.compartments;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.compartments.algorithms.CompartmentNodeTypeCategory;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeCompartmentOutput;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.compartments.datatypes.JIPipeCompartmentOutputData;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

/**
 * Extension that provides compartment management functionality
 */
@Plugin(type = JIPipeJavaPlugin.class)
public class JIPipeCompartmentsPlugin extends JIPipePrepackagedDefaultJavaPlugin {

    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public String getName() {
        return "Compartment management";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Data types required for graph compartment management");
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:compartments";
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
        registerNodeTypeCategory(new CompartmentNodeTypeCategory());
        registerNodeType("jipipe:compartment-output", JIPipeCompartmentOutput.class, UIUtils.getIconURLFromResources("actions/graph-compartment.png"));
        registerNodeType("jipipe:project-compartment", JIPipeProjectCompartment.class, UIUtils.getIconURLFromResources("actions/graph-compartment.png"));

        registerDatatype("jipipe:compartment-output", JIPipeCompartmentOutputData.class,
                ResourceUtils.getPluginResource("icons/data-types/graph-compartment.png"));
    }

    @Override
    public boolean isCoreExtension() {
        return true;
    }
}
