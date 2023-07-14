package org.hkijena.jipipe.api.validation.causes;

import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryCause;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class CustomReportEntryCause extends JIPipeValidationReportEntryCause {

    private final String name;

    private final Icon icon;

    public CustomReportEntryCause(String name, Icon icon) {
        this.name = name;
        this.icon = icon;
    }

    public CustomReportEntryCause(JIPipeValidationReportEntryCause parent, String name, Icon icon) {
        super(parent);
        this.name = name;
        this.icon = icon;
    }

    public CustomReportEntryCause(String name) {
        this(name, UIUtils.getIconFromResources("actions/dialog-warning.png"));
    }

    public CustomReportEntryCause(JIPipeValidationReportEntryCause parent, String name) {
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
