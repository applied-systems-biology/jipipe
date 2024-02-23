package org.hkijena.jipipe.extensions.forms.algorithms.generators;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.extensions.forms.algorithms.SimpleFormGeneratorAlgorithm;
import org.hkijena.jipipe.extensions.forms.datatypes.BooleanFormData;
import org.hkijena.jipipe.extensions.forms.datatypes.FormData;

@SetJIPipeDocumentation(name = "Boolean input form", description = "Creates a boolean input form (a checkbox). ")
@AddJIPipeInputSlot(value = FormData.class, slotName = "Existing")
@AddJIPipeOutputSlot(value = FormData.class, slotName = "Combined")
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class BooleanFormGeneratorAlgorithm extends SimpleFormGeneratorAlgorithm {

    public BooleanFormGeneratorAlgorithm(JIPipeNodeInfo info) {
        super(info, new BooleanFormData());
    }

    public BooleanFormGeneratorAlgorithm(BooleanFormGeneratorAlgorithm other) {
        super(other);
    }

}
