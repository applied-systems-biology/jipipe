package org.hkijena.jipipe.api.validation.causes;

import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryCause;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class APIErrorValidationReportEntryCause extends JIPipeValidationReportEntryCause {

    public APIErrorValidationReportEntryCause() {
    }

    public APIErrorValidationReportEntryCause(JIPipeValidationReportEntryCause parent) {
        super(parent);
    }

    @Override
    public boolean canNavigate(JIPipeWorkbench workbench) {
        return false;
    }

    @Override
    public void navigate(JIPipeWorkbench workbench) {

    }

    @Override
    public String renderName() {
        return "Internal error (report to developers)";
    }

    @Override
    public Icon renderIcon() {
        return UIUtils.getIconFromResources("actions/bug.png");
    }
}
