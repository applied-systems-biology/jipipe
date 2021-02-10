package org.hkijena.jipipe.extensions.forms.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.extensions.forms.datatypes.FormData;

@JIPipeDocumentation(name = "Form processor (simple iterating)", description = "An algorithm that iterates through each row " +
        "of its 'Data' slot and shows a user interface during the runtime that allows users to modify annotations via form elements. " +
        "These forms are provided via the 'Forms' slot, where all contained form elements are shown in the user interface.")
@JIPipeOrganization(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Forms")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Data", autoCreate = true)
@JIPipeInputSlot(value = FormData.class, slotName = "Forms", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Data", autoCreate = true)
public class SimpleIteratingFormProcessorAlgorithm extends JIPipeAlgorithm {
    public SimpleIteratingFormProcessorAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SimpleIteratingFormProcessorAlgorithm(SimpleIteratingFormProcessorAlgorithm other) {
        super(other);
    }
}
