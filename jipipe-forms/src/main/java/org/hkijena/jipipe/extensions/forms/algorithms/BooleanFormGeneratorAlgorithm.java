package org.hkijena.jipipe.extensions.forms.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.extensions.forms.datatypes.BooleanFormData;
import org.hkijena.jipipe.extensions.forms.datatypes.FormData;

@JIPipeDocumentation(name = "Boolean input form", description = "Creates a boolean input form (a checkbox). " )
@JIPipeInputSlot(value = FormData.class, slotName = "Existing")
@JIPipeOutputSlot(value = FormData.class, slotName = "Combined")
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class BooleanFormGeneratorAlgorithm extends SimpleFormGeneratorAlgorithm {

    public BooleanFormGeneratorAlgorithm(JIPipeNodeInfo info) {
        super(info, new BooleanFormData());
    }

    public BooleanFormGeneratorAlgorithm(BooleanFormGeneratorAlgorithm other) {
        super(other);
    }

}
