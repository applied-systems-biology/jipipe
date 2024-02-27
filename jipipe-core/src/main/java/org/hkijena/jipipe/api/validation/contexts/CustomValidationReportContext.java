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

package org.hkijena.jipipe.api.validation.contexts;

import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
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
    public String renderName() {
        return name;
    }

    @Override
    public Icon renderIcon() {
        return icon;
    }
}
