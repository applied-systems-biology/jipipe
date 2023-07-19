package org.hkijena.jipipe.api.validation.contexts;

import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class JavaExtensionValidationReportContext extends JIPipeValidationReportContext {

    private final JIPipeDependency extension;

    public JavaExtensionValidationReportContext(JIPipeDependency extension) {
        this.extension = extension;
    }

    public JavaExtensionValidationReportContext(JIPipeValidationReportContext parent, JIPipeDependency extension) {
        super(parent);
        this.extension = extension;
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
        return String.format("Extension '%s' (%s)", extension.getMetadata().getName(), extension.getDependencyId());
    }

    @Override
    public Icon renderIcon() {
        return UIUtils.getIconFromResources("actions/plugins.png");
    }

    public JIPipeDependency getExtension() {
        return extension;
    }
}
