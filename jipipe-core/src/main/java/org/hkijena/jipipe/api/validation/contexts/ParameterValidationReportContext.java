package org.hkijena.jipipe.api.validation.contexts;

import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class ParameterValidationReportContext extends JIPipeValidationReportContext {

    private final JIPipeParameterCollection parameterCollection;

    private final String name;
    private final String key;

    public ParameterValidationReportContext(JIPipeParameterCollection parameterCollection, String name, String key) {
        this.parameterCollection = parameterCollection;
        this.name = name;
        this.key = key;
    }

    public ParameterValidationReportContext(JIPipeValidationReportContext parent, JIPipeParameterCollection parameterCollection, String name, String key) {
        super(parent);
        this.parameterCollection = parameterCollection;
        this.name = name;
        this.key = key;
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
