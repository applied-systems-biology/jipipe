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

import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.validation.NavigableJIPipeValidationReportContext;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;

public class ProjectSettingsValidationReportContext extends ProjectValidationReportContext implements NavigableJIPipeValidationReportContext {

    public ProjectSettingsValidationReportContext(JIPipeProject project) {
        super(project);
    }

    @Override
    public boolean canNavigate(JIPipeWorkbench workbench) {
        return workbench.getProject() == getProject();
    }

    @Override
    public void navigate(JIPipeWorkbench workbench) {
        ((JIPipeDesktopProjectWorkbench) workbench).openProjectSettings("/General/Environments");
    }

}
