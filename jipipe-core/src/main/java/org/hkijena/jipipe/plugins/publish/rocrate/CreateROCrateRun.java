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

package org.hkijena.jipipe.plugins.publish.rocrate;

import org.hkijena.jipipe.api.DefaultJIPipeRunnable;
import org.hkijena.jipipe.api.project.JIPipeProject;

import java.nio.file.Path;

public class CreateROCrateRun extends DefaultJIPipeRunnable {

    private final JIPipeProject project;
    private final Path projectFile;
    private final Path roCrateFile;

    public CreateROCrateRun(JIPipeProject project, Path projectFile, Path roCrateFile) {
        this.project = project;
        this.projectFile = projectFile;
        this.roCrateFile = roCrateFile;
    }

    @Override
    public String getTaskLabel() {
        return "Create RO-Crate";
    }

    @Override
    public void run() {
        getProgressInfo().log("Creating RO-Crate for project " + project.getWorkDirectory());
    }

    public JIPipeProject getProject() {
        return project;
    }

    public Path getProjectFile() {
        return projectFile;
    }

    public Path getRoCrateFile() {
        return roCrateFile;
    }
}
