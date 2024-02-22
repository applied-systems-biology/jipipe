package org.hkijena.jipipe.extensions.forms.algorithms.generators;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.DefineJIPipeNode;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.extensions.forms.algorithms.SimpleFormGeneratorAlgorithm;
import org.hkijena.jipipe.extensions.forms.datatypes.EnumFormData;
import org.hkijena.jipipe.extensions.forms.datatypes.FormData;

@SetJIPipeDocumentation(name = "Selection input form", description = "Creates a selection input form (combo box). ")
@AddJIPipeInputSlot(value = FormData.class, slotName = "Existing")
@AddJIPipeOutputSlot(value = FormData.class, slotName = "Combined")
@DefineJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class EnumFormGeneratorAlgorithm extends SimpleFormGeneratorAlgorithm {

    public EnumFormGeneratorAlgorithm(JIPipeNodeInfo info) {
        super(info, new EnumFormData());
    }

    public EnumFormGeneratorAlgorithm(EnumFormGeneratorAlgorithm other) {
        super(other);
    }

}
