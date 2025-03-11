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

package org.hkijena.jipipe.plugins.dataenvironment;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.artifacts.JIPipeLocalArtifact;
import org.hkijena.jipipe.api.environments.JIPipeArtifactEnvironment;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.plugins.parameters.api.collections.ListParameter;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * An artifact environment that does no have any function workload, but only contains data
 * Points towards a directory
 */
public class JIPipeDataDirectoryEnvironment extends JIPipeArtifactEnvironment {

    private Path directory = Paths.get("");

    public JIPipeDataDirectoryEnvironment() {
    }

    public JIPipeDataDirectoryEnvironment(JIPipeDataDirectoryEnvironment other) {
        super(other);
        this.directory = other.directory;
    }

    @SetJIPipeDocumentation(name = "Directory", description = "The directory that contains the data")
    @JIPipeParameter("directory")
    @JsonGetter("directory")
    public Path getDirectory() {
        return directory;
    }

    @JIPipeParameter("directory")
    @JsonSetter("directory")
    public void setDirectory(Path directory) {
        this.directory = directory;
    }

    @Override
    public void applyConfigurationFromArtifact(JIPipeLocalArtifact artifact, JIPipeProgressInfo progressInfo) {
        setDirectory(artifact.getLocalPath());
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/vcs-update-cvs-cervisia.png");
    }

    @Override
    public String getInfo() {
        if (isLoadFromArtifact()) {
            return StringUtils.orElse(getArtifactQuery().getQuery(), "<Not set>");
        } else {
            return StringUtils.orElse(getDirectory(), "<Not set>");
        }
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        if (!isLoadFromArtifact()) {
            if (isDirectoryValid()) {
                report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error, reportContext,
                        "Directory does not exist",
                        "You need to provide an directory",
                        "Provide an directory"));
            }
        }
    }

    private boolean isDirectoryValid() {
        return directory == null || StringUtils.isNullOrEmpty(directory) || !Files.isDirectory(directory);
    }

    @Override
    public String toString() {
        return "JIPipeDataDirectoryArtifactEnvironment{" +
                "directory=" + directory +
                '}';
    }

    public static class List extends ListParameter<JIPipeDataDirectoryEnvironment> {
        public List() {
            super(JIPipeDataDirectoryEnvironment.class);
        }

        public List(List other) {
            super(JIPipeDataDirectoryEnvironment.class);
            for (JIPipeDataDirectoryEnvironment environment : other) {
                add(new JIPipeDataDirectoryEnvironment(environment));
            }
        }
    }
}
