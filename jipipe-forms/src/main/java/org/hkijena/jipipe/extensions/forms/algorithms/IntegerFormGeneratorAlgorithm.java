package org.hkijena.jipipe.extensions.forms.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.extensions.forms.datatypes.FormData;
import org.hkijena.jipipe.extensions.forms.datatypes.IntegerFormData;

@JIPipeDocumentation(name = "Integer input form", description = "Creates a integer input form. " + FormGeneratorAlgorithm.DOCUMENTATION_DESCRIPTION)
@JIPipeInputSlot(value = FormData.class, slotName = "Existing")
@JIPipeOutputSlot(value = FormData.class, slotName = "Combined")
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class IntegerFormGeneratorAlgorithm extends SimpleFormGeneratorAlgorithm {

    public IntegerFormGeneratorAlgorithm(JIPipeNodeInfo info) {
        super(info, new IntegerFormData());
    }

    public IntegerFormGeneratorAlgorithm(IntegerFormGeneratorAlgorithm other) {
        super(other);
    }

}
