/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.forms.datatypes;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.utils.UIUtils;

import java.awt.*;

@SetJIPipeDocumentation(name = "Group header form", description = "Generates a group header element that allows to structure forms.")
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
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {

    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return new GroupHeaderFormData(this);
    }

    @Override
    public Component getEditor(JIPipeDesktopWorkbench workbench) {
        JIPipeDesktopFormPanel.GroupHeaderPanel panel = new JIPipeDesktopFormPanel.GroupHeaderPanel(getName(), UIUtils.getIconFromResources("actions/configure.png"), 8);
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
    public void loadData(JIPipeMultiIterationStep iterationStep) {

    }

    @Override
    public void writeData(JIPipeMultiIterationStep iterationStep) {

    }


}
