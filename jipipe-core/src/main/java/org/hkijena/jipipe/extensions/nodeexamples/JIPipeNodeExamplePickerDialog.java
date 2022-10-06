package org.hkijena.jipipe.extensions.nodeexamples;

import org.hkijena.jipipe.api.nodes.JIPipeNodeExample;
import org.hkijena.jipipe.ui.components.pickers.PickerDialog;

import java.awt.*;

public class JIPipeNodeExamplePickerDialog extends PickerDialog<JIPipeNodeExample> {

    public JIPipeNodeExamplePickerDialog(Window parent) {
        super(parent);
        setCellRenderer(new JIPipeNodeExampleListCellRenderer());
    }

    @Override
    protected String getSearchString(JIPipeNodeExample item) {
        return item.getNodeTemplate().getName() + item.getNodeTemplate().getDescription().getBody();
    }
}
