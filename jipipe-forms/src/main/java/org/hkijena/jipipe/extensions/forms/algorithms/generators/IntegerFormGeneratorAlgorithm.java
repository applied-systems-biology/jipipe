package org.hkijena.jipipe.extensions.forms.algorithms.generators;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.DefineJIPipeNode;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.extensions.forms.algorithms.SimpleFormGeneratorAlgorithm;
import org.hkijena.jipipe.extensions.forms.datatypes.FormData;
import org.hkijena.jipipe.extensions.forms.datatypes.IntegerFormData;

@SetJIPipeDocumentation(name = "Integer input form", description = "Creates a integer input form. ")
@AddJIPipeInputSlot(value = FormData.class, slotName = "Existing")
@AddJIPipeOutputSlot(value = FormData.class, slotName = "Combined")
@DefineJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class IntegerFormGeneratorAlgorithm extends SimpleFormGeneratorAlgorithm {

    public IntegerFormGeneratorAlgorithm(JIPipeNodeInfo info) {
        super(info, new IntegerFormData());
    }

    public IntegerFormGeneratorAlgorithm(IntegerFormGeneratorAlgorithm other) {
        super(other);
    }

}
