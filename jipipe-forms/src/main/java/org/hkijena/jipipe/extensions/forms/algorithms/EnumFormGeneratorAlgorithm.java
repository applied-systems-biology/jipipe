package org.hkijena.jipipe.extensions.forms.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.extensions.forms.datatypes.EnumFormData;
import org.hkijena.jipipe.extensions.forms.datatypes.FormData;

@JIPipeDocumentation(name = "Selection input form", description = "Creates a selection input form (combo box). ")
@JIPipeInputSlot(value = FormData.class, slotName = "Existing")
@JIPipeOutputSlot(value = FormData.class, slotName = "Combined")
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class EnumFormGeneratorAlgorithm extends SimpleFormGeneratorAlgorithm {

    public EnumFormGeneratorAlgorithm(JIPipeNodeInfo info) {
        super(info, new EnumFormData());
    }

    public EnumFormGeneratorAlgorithm(EnumFormGeneratorAlgorithm other) {
        super(other);
    }

}
