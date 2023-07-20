package org.hkijena.jipipe.api.validation.contexts;

import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class APIErrorValidationReportContext extends JIPipeValidationReportContext {

    public APIErrorValidationReportContext() {
    }

    public APIErrorValidationReportContext(JIPipeValidationReportContext parent) {
        super(parent);
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
