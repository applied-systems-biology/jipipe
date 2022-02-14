package org.hkijena.jipipe.extensions.forms.datatypes;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.utils.UIUtils;

import java.awt.*;
import java.nio.file.Path;

@JIPipeDocumentation(name = "Group header form", description = "Generates a group header element that allows to structure forms.")
public class GroupHeaderFormData extends ParameterFormData {

    public GroupHeaderFormData() {
    }

    public GroupHeaderFormData(GroupHeaderFormData other) {
        super(other);
    }

    public static GroupHeaderFormData importFrom(Path rowStorage) {
        return FormData.importFrom(rowStorage, GroupHeaderFormData.class);
    }

    @Override
    public void reportValidity(JIPipeIssueReport report) {

    }

    @Override
    public JIPipeData duplicate() {
        return new GroupHeaderFormData(this);
    }

    @Override
    public Component getEditor(JIPipeWorkbench workbench) {
        FormPanel.GroupHeaderPanel panel = new FormPanel.GroupHeaderPanel(getName(), UIUtils.getIconFromResources("actions/configure.png"));
        panel.setDescription(getDescription().getHtml());
        return panel;
    }

    @Override
    public boolean isShowName() {
        return false;
    }

    @Override
    public void setShowName(boolean showName) {
    }

    @Override
    public void loadData(JIPipeMergingDataBatch dataBatch) {

    }

    @Override
    public void writeData(JIPipeMergingDataBatch dataBatch) {

    }


}
