package org.hkijena.jipipe.extensions.forms.algorithms.generators;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.DefineJIPipeNode;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.extensions.forms.algorithms.SimpleFormGeneratorAlgorithm;
import org.hkijena.jipipe.extensions.forms.datatypes.FormData;
import org.hkijena.jipipe.extensions.forms.datatypes.GroupHeaderFormData;

@SetJIPipeDocumentation(name = "Group header form", description = "Creates a group header element to structure the form. ")
@AddJIPipeInputSlot(value = FormData.class, slotName = "Existing")
@AddJIPipeOutputSlot(value = FormData.class, slotName = "Combined")
@DefineJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class GroupHeaderFormGeneratorAlgorithm extends SimpleFormGeneratorAlgorithm {

    public GroupHeaderFormGeneratorAlgorithm(JIPipeNodeInfo info) {
        super(info, new GroupHeaderFormData());
    }

    public GroupHeaderFormGeneratorAlgorithm(GroupHeaderFormGeneratorAlgorithm other) {
        super(other);
    }

}
