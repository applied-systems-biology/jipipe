package org.hkijena.jipipe.api.validation.contexts;

import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class UnspecifiedValidationReportContext extends JIPipeValidationReportContext {

    public UnspecifiedValidationReportContext() {
    }

    @Override
    public String renderName() {
        return "Unspecified";
    }

    @Override
    public Icon renderIcon() {
        return UIUtils.getIconFromResources("actions/dialog-warning.png");
    }
}
