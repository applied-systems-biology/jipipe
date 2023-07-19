package org.hkijena.jipipe.api.validation.contexts;

import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class CustomValidationReportContext extends JIPipeValidationReportContext {

    private final String name;

    private final Icon icon;

    public CustomValidationReportContext(String name, Icon icon) {
        this.name = name;
        this.icon = icon;
    }

    public CustomValidationReportContext(JIPipeValidationReportContext parent, String name, Icon icon) {
        super(parent);
        this.name = name;
        this.icon = icon;
    }

    public CustomValidationReportContext(String name) {
        this(name, UIUtils.getIconFromResources("actions/dialog-warning.png"));
    }

    public CustomValidationReportContext(JIPipeValidationReportContext parent, String name) {
        this(parent, name, UIUtils.getIconFromResources("actions/dialog-warning.png"));
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
        return name;
    }

    @Override
    public Icon renderIcon() {
        return icon;
    }
}
