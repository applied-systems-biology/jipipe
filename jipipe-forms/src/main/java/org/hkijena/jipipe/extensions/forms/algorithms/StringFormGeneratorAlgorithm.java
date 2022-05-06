package org.hkijena.jipipe.extensions.forms.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.extensions.forms.datatypes.FormData;
import org.hkijena.jipipe.extensions.forms.datatypes.StringFormData;

@JIPipeDocumentation(name = "Text input form", description = "Creates a text input form. " )
@JIPipeInputSlot(value = FormData.class, slotName = "Existing")
@JIPipeOutputSlot(value = FormData.class, slotName = "Combined")
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class StringFormGeneratorAlgorithm extends SimpleFormGeneratorAlgorithm {

    public StringFormGeneratorAlgorithm(JIPipeNodeInfo info) {
        super(info, new StringFormData());
    }

    public StringFormGeneratorAlgorithm(StringFormGeneratorAlgorithm other) {
        super(other);
    }

}
