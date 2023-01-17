package org.hkijena.jipipe.extensions.forms.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentationDescription;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.extensions.forms.datatypes.FormData;

/**
 * A node base class that adds a new form element into the list of existing forms
 */
@JIPipeDocumentationDescription(description = "A new form element is added to the list of existing form elements and stored into the combined list. " +
        "The input slot requires no incoming edge.")
public abstract class FormGeneratorAlgorithm extends JIPipeAlgorithm {

    public FormGeneratorAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Existing", "Existing list of forms", FormData.class, true)
                .addOutputSlot("Combined", "Existing list of forms plus the form defined by this node", FormData.class, null)
                .seal()
                .build());
    }

    public FormGeneratorAlgorithm(FormGeneratorAlgorithm other) {
        super(other);
    }

    @Override
    public void run(JIPipeProgressInfo progressInfo) {
        getFirstOutputSlot().addDataFromSlot(getFirstInputSlot(), progressInfo);
        if (!isPassThrough()) {
            run(getFirstOutputSlot(), progressInfo);
        }
    }

    /**
     * This method should add new {@link FormData} elements into the combined slot.
     *
     * @param combined     the output slot
     * @param progressInfo the progress info
     */
    public abstract void run(JIPipeDataSlot combined, JIPipeProgressInfo progressInfo);

    @Override
    protected boolean canPassThrough() {
        return true;
    }
}
