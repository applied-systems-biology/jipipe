package org.hkijena.jipipe.api.validation;

import org.hkijena.jipipe.api.validation.causes.JavaExtensionValidationReportEntryCause;
import org.hkijena.jipipe.api.validation.causes.UnspecifiedReportEntryCause;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

import javax.swing.*;

public abstract class JIPipeValidationReportEntryCause {

    private final JIPipeValidationReportEntryCause parent;

    public JIPipeValidationReportEntryCause() {
        if(getClass() != UnspecifiedReportEntryCause.class)
            this.parent = new UnspecifiedReportEntryCause();
        else
            this.parent = null;
    }

    public JIPipeValidationReportEntryCause(JIPipeValidationReportEntryCause parent) {
        if(getClass() != UnspecifiedReportEntryCause.class)
            this.parent = parent != null ? parent : new UnspecifiedReportEntryCause();
        else
            this.parent = null;
    }

    public JIPipeValidationReportEntryCause getParent() {
        return parent;
    }

    public abstract boolean canNavigate(JIPipeWorkbench workbench);

    public abstract void navigate(JIPipeWorkbench workbench);

    public abstract String renderName();

    public abstract Icon renderIcon();
}
