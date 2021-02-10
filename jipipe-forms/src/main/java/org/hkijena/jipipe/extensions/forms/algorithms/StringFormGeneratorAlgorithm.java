package org.hkijena.jipipe.extensions.forms.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.forms.datatypes.FormData;
import org.hkijena.jipipe.extensions.forms.datatypes.StringFormData;

@JIPipeDocumentation(name = "Text input form", description = "Creates a text input form. " + FormGeneratorAlgorithm.DOCUMENTATION_DESCRIPTION)
@JIPipeInputSlot(value = FormData.class, slotName = "Existing")
@JIPipeOutputSlot(value = FormData.class, slotName = "Combined")
@JIPipeOrganization(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class StringFormGeneratorAlgorithm extends FormGeneratorAlgorithm {

    private StringFormData formData = new StringFormData();

    public StringFormGeneratorAlgorithm(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(formData);
    }

    public StringFormGeneratorAlgorithm(StringFormGeneratorAlgorithm other) {
        super(other);
        this.formData = new StringFormData(other.formData);
        registerSubParameter(formData);
    }

    @JIPipeDocumentation(name = "Form element", description = "Use following settings to setup the generated form element.")
    @JIPipeParameter("form-data")
    public StringFormData getFormData() {
        return formData;
    }

    @Override
    public void run(JIPipeDataSlot combined, JIPipeProgressInfo progressInfo) {
        combined.addData(formData.duplicate(), progressInfo);
    }
}
