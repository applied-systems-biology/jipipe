package org.hkijena.jipipe.extensions.forms.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.extensions.forms.datatypes.DoubleFormData;
import org.hkijena.jipipe.extensions.forms.datatypes.FormData;

@JIPipeDocumentation(name = "Number input form", description = "Creates a real number input form. " + FormGeneratorAlgorithm.DOCUMENTATION_DESCRIPTION)
@JIPipeInputSlot(value = FormData.class, slotName = "Existing")
@JIPipeOutputSlot(value = FormData.class, slotName = "Combined")
@JIPipeOrganization(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class DoubleFormGeneratorAlgorithm extends SimpleFormGeneratorAlgorithm {

    public DoubleFormGeneratorAlgorithm(JIPipeNodeInfo info) {
        super(info, new DoubleFormData());
    }

    public DoubleFormGeneratorAlgorithm(DoubleFormGeneratorAlgorithm other) {
        super(other);
    }

}
