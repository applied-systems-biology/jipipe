package org.hkijena.jipipe.extensions.forms.algorithms.generators;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.extensions.forms.algorithms.SimpleFormGeneratorAlgorithm;
import org.hkijena.jipipe.extensions.forms.datatypes.FormData;
import org.hkijena.jipipe.extensions.forms.datatypes.PathFormData;

@SetJIPipeDocumentation(name = "Path input form", description = "Creates a path input form. ")
@AddJIPipeInputSlot(value = FormData.class, slotName = "Existing")
@AddJIPipeOutputSlot(value = FormData.class, slotName = "Combined")
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class PathFormGeneratorAlgorithm extends SimpleFormGeneratorAlgorithm {

    public PathFormGeneratorAlgorithm(JIPipeNodeInfo info) {
        super(info, new PathFormData());
    }

    public PathFormGeneratorAlgorithm(PathFormGeneratorAlgorithm other) {
        super(other);
    }

}
