package org.hkijena.jipipe.extensions.forms.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.forms.datatypes.FormData;

import java.util.ArrayList;
import java.util.List;

public abstract class SimpleFormGeneratorAlgorithm extends FormGeneratorAlgorithm {

    private FormData formData;

    /**
     * Constructor for a new {@link SimpleFormGeneratorAlgorithm}.
     *
     * @param info     the node info
     * @param formData the initial formData object
     */
    public SimpleFormGeneratorAlgorithm(JIPipeNodeInfo info, FormData formData) {
        super(info);
        this.formData = formData;
        registerSubParameter(formData);
    }

    public SimpleFormGeneratorAlgorithm(SimpleFormGeneratorAlgorithm other) {
        super(other);
        this.formData = (FormData) other.formData.duplicate(new JIPipeProgressInfo());
        registerSubParameter(formData);
    }

    @JIPipeDocumentation(name = "Form element", description = "Use following settings to setup the generated form element.")
    @JIPipeParameter("form-data")
    public FormData getFormData() {
        return formData;
    }

    @Override
    public void run(JIPipeDataSlot combined, JIPipeProgressInfo progressInfo) {
        List<JIPipeTextAnnotation> annotationList = new ArrayList<>();
        if (formData.getTabSettings().getTabAnnotation().isEnabled()) {
            annotationList.add(formData.getTabSettings().getTabAnnotation().createAnnotation(formData.getTabSettings().getTab()));
        }
        combined.addData(formData.duplicate(progressInfo), annotationList, JIPipeTextAnnotationMergeMode.OverwriteExisting, progressInfo);
    }
}
