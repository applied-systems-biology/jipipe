package org.hkijena.jipipe.extensions.forms.datatypes;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.utils.UIUtils;

import java.awt.*;

@JIPipeDocumentation(name = "Group header form", description = "Generates a group header element that allows to structure forms.")
public class GroupHeaderFormData extends ParameterFormData {

    public GroupHeaderFormData() {
    }

    public GroupHeaderFormData(GroupHeaderFormData other) {
        super(other);
    }

    public static GroupHeaderFormData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return FormData.importData(storage, GroupHeaderFormData.class, progressInfo);
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext context, JIPipeValidationReport report) {

    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return new GroupHeaderFormData(this);
    }

    @Override
    public Component getEditor(JIPipeWorkbench workbench) {
        FormPanel.GroupHeaderPanel panel = new FormPanel.GroupHeaderPanel(getName(), UIUtils.getIconFromResources("actions/configure.png"), 8);
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
