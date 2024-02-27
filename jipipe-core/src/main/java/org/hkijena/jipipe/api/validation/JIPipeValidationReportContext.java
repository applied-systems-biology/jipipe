/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.validation;

import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public abstract class JIPipeValidationReportContext {

    private final JIPipeValidationReportContext parent;

    public JIPipeValidationReportContext() {
        if (getClass() != UnspecifiedValidationReportContext.class)
            this.parent = new UnspecifiedValidationReportContext();
        else
            this.parent = null;
    }

    public JIPipeValidationReportContext(JIPipeValidationReportContext parent) {
        if (getClass() != UnspecifiedValidationReportContext.class)
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
            if (currentContext instanceof NavigableJIPipeValidationReportContext) {
                contexts.add((NavigableJIPipeValidationReportContext) currentContext);
            }
            currentContext = currentContext.getParent();
        }
        return contexts;
    }

}
