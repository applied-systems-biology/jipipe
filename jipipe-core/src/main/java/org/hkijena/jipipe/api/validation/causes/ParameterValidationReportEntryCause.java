package org.hkijena.jipipe.api.validation.causes;

import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryCause;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class ParameterValidationReportEntryCause extends JIPipeValidationReportEntryCause {

    private final JIPipeParameterCollection parameterCollection;

    private final String name;
    private final String key;

    public ParameterValidationReportEntryCause(JIPipeParameterCollection parameterCollection, String name, String key) {
        this.parameterCollection = parameterCollection;
        this.name = name;
        this.key = key;
    }

    public ParameterValidationReportEntryCause(JIPipeValidationReportEntryCause parent, JIPipeParameterCollection parameterCollection, String name, String key) {
        super(parent);
        this.parameterCollection = parameterCollection;
        this.name = name;
        this.key = key;
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
        return String.format("%s [%s]", name, key);
    }

    @Override
    public Icon renderIcon() {
        return UIUtils.getIconFromResources("actions/configure_toolbars.png");
    }
}
