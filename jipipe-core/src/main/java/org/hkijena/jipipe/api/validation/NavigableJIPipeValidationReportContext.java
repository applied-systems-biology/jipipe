package org.hkijena.jipipe.api.validation;

import org.hkijena.jipipe.ui.JIPipeWorkbench;

/**
 * A {@link JIPipeValidationReportContext} that can navigate to an UI element
 */
public abstract class NavigableJIPipeValidationReportContext extends JIPipeValidationReportContext {

    public NavigableJIPipeValidationReportContext() {
    }

    public NavigableJIPipeValidationReportContext(JIPipeValidationReportContext parent) {
        super(parent);
    }

    public abstract boolean canNavigate(JIPipeWorkbench workbench);

    public abstract void navigate(JIPipeWorkbench workbench);
}
