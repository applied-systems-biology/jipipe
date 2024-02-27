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

import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
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
