package org.hkijena.jipipe.api.validation;

import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

import javax.swing.*;

public abstract class JIPipeValidationReportContext {

    private final JIPipeValidationReportContext parent;

    public JIPipeValidationReportContext() {
        if(getClass() != UnspecifiedValidationReportContext.class)
            this.parent = new UnspecifiedValidationReportContext();
        else
            this.parent = null;
    }

    public JIPipeValidationReportContext(JIPipeValidationReportContext parent) {
        if(getClass() != UnspecifiedValidationReportContext.class)
            this.parent = parent != null ? parent : new UnspecifiedValidationReportContext();
        else
            this.parent = null;
    }

    public JIPipeValidationReportContext getParent() {
        return parent;
    }

    public abstract boolean canNavigate(JIPipeWorkbench workbench);

    public abstract void navigate(JIPipeWorkbench workbench);

    public abstract String renderName();

    public abstract Icon renderIcon();
}
