package org.hkijena.jipipe.extensions.forms.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.extensions.forms.datatypes.FormData;
import org.hkijena.jipipe.extensions.forms.datatypes.PathFormData;

@JIPipeDocumentation(name = "Path input form", description = "Creates a path input form. " )
@JIPipeInputSlot(value = FormData.class, slotName = "Existing")
@JIPipeOutputSlot(value = FormData.class, slotName = "Combined")
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class PathFormGeneratorAlgorithm extends SimpleFormGeneratorAlgorithm {

    public PathFormGeneratorAlgorithm(JIPipeNodeInfo info) {
        super(info, new PathFormData());
    }

    public PathFormGeneratorAlgorithm(PathFormGeneratorAlgorithm other) {
        super(other);
    }

}
