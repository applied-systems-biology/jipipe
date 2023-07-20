package org.hkijena.jipipe.api.validation;

import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

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

    public abstract String renderName();

    public abstract Icon renderIcon();

    public List<JIPipeValidationReportContext> traverse() {
        List<JIPipeValidationReportContext> contexts = new ArrayList<>();
        JIPipeValidationReportContext currentContext = this;
        while (currentContext != null) {
            contexts.add(currentContext);
            currentContext = currentContext.getParent();
        }
        return contexts;
    }

    public List<NavigableJIPipeValidationReportContext> traverseNavigable() {
        List<NavigableJIPipeValidationReportContext> contexts = new ArrayList<>();
        JIPipeValidationReportContext currentContext = this;
        while (currentContext != null) {
            if(currentContext instanceof NavigableJIPipeValidationReportContext) {
                contexts.add((NavigableJIPipeValidationReportContext) currentContext);
            }
            currentContext = currentContext.getParent();
        }
        return contexts;
    }

}
