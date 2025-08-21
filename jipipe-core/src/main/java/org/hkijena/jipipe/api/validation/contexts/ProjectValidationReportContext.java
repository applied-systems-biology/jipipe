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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;

import javax.swing.*;

public class ProjectValidationReportContext extends JIPipeValidationReportContext {

    private final JIPipeProject project;

    public ProjectValidationReportContext(JIPipeProject project) {
        this.project = project;
    }

    @Override
    public String renderName() {
        return "Project";
    }

    @Override
    public Icon renderIcon() {
        return JIPipe.RESOURCES.getIcon16("actions/dialog-warning.png");
    }

    public JIPipeProject getProject() {
        return project;
    }
}
