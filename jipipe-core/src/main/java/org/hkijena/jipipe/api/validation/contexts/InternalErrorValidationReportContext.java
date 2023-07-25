package org.hkijena.jipipe.api.validation.contexts;

import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class InternalErrorValidationReportContext extends JIPipeValidationReportContext {

    public InternalErrorValidationReportContext() {
    }

    public InternalErrorValidationReportContext(JIPipeValidationReportContext parent) {
        super(parent);
    }

    @Override
    public String renderName() {
        return "Internal error";
    }

    @Override
    public Icon renderIcon() {
        return UIUtils.getIconFromResources("actions/bug.png");
    }
}
