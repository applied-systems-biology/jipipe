package org.hkijena.jipipe.extensions.forms.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.extensions.forms.datatypes.FormData;
import org.hkijena.jipipe.extensions.forms.datatypes.GroupHeaderFormData;

@JIPipeDocumentation(name = "Group header form", description = "Creates a group header element to structure the form. " )
@JIPipeInputSlot(value = FormData.class, slotName = "Existing")
@JIPipeOutputSlot(value = FormData.class, slotName = "Combined")
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class GroupHeaderFormGeneratorAlgorithm extends SimpleFormGeneratorAlgorithm {

    public GroupHeaderFormGeneratorAlgorithm(JIPipeNodeInfo info) {
        super(info, new GroupHeaderFormData());
    }

    public GroupHeaderFormGeneratorAlgorithm(GroupHeaderFormGeneratorAlgorithm other) {
        super(other);
    }

}
