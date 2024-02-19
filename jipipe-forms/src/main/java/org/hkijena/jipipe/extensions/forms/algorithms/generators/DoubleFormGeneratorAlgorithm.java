package org.hkijena.jipipe.extensions.forms.algorithms.generators;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.DefineJIPipeNode;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.extensions.forms.algorithms.SimpleFormGeneratorAlgorithm;
import org.hkijena.jipipe.extensions.forms.datatypes.DoubleFormData;
import org.hkijena.jipipe.extensions.forms.datatypes.FormData;

@SetJIPipeDocumentation(name = "Number input form", description = "Creates a real number input form. ")
@AddJIPipeInputSlot(value = FormData.class, slotName = "Existing")
@AddJIPipeOutputSlot(value = FormData.class, slotName = "Combined")
@DefineJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class DoubleFormGeneratorAlgorithm extends SimpleFormGeneratorAlgorithm {

    public DoubleFormGeneratorAlgorithm(JIPipeNodeInfo info) {
        super(info, new DoubleFormData());
    }

    public DoubleFormGeneratorAlgorithm(DoubleFormGeneratorAlgorithm other) {
        super(other);
    }

}
