package org.hkijena.jipipe.extensions.forms.algorithms.generators;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.extensions.forms.algorithms.SimpleFormGeneratorAlgorithm;
import org.hkijena.jipipe.extensions.forms.datatypes.DoubleFormData;
import org.hkijena.jipipe.extensions.forms.datatypes.FormData;

@JIPipeDocumentation(name = "Number input form", description = "Creates a real number input form. ")
@JIPipeInputSlot(value = FormData.class, slotName = "Existing")
@JIPipeOutputSlot(value = FormData.class, slotName = "Combined")
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class DoubleFormGeneratorAlgorithm extends SimpleFormGeneratorAlgorithm {

    public DoubleFormGeneratorAlgorithm(JIPipeNodeInfo info) {
        super(info, new DoubleFormData());
    }

    public DoubleFormGeneratorAlgorithm(DoubleFormGeneratorAlgorithm other) {
        super(other);
    }

}
