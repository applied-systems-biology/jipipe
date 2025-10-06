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

package org.hkijena.jipipe.plugins.python;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.environments.JIPipeArtifactEnvironment;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportSettings;
import org.hkijena.jipipe.plugins.parameters.library.filesystem.PathParameterSettings;
import org.hkijena.jipipe.plugins.settings.application.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.PathType;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.scripting.ScriptUtils;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * An environment-like type that points to a package
 */
public abstract class PythonPackageLibraryEnvironment extends JIPipeArtifactEnvironment {

    private Path libraryDirectory = Paths.get("library");
    private boolean providedByEnvironment = false;

    public PythonPackageLibraryEnvironment() {

    }

    public PythonPackageLibraryEnvironment(PythonPackageLibraryEnvironment other) {
        super(other);
        this.libraryDirectory = other.libraryDirectory;
        this.providedByEnvironment = other.providedByEnvironment;
    }

    @SetJIPipeDocumentation(name = "Library directory", description = "The directory that contains the Python packages. Ignored if the Python packages are provided by the Python environment.")
    @JIPipeParameter("library-directory")
    @PathParameterSettings(key = JIPipeFileChooserApplicationSettings.LastDirectoryKey.External, pathMode = PathType.DirectoriesOnly, ioMode = PathIOMode.Open)
    @JsonGetter("library-directory")
    public Path getLibraryDirectory() {
        return libraryDirectory;
    }

    @JIPipeParameter("library-directory")
    @JsonSetter("library-directory")
    public void setLibraryDirectory(Path libraryDirectory) {
        this.libraryDirectory = libraryDirectory;
    }

    @SetJIPipeDocumentation(name = "Provided by Python environment", description = "If enabled, the library will be ignored. It is assumed that all packages are provided by the Python environment (Conda/Virtualenv).")
    @JIPipeParameter("provided-by-environment")
    @JsonGetter("provided-by-environment")
    public boolean isProvidedByEnvironment() {
        return providedByEnvironment;
    }

    @JIPipeParameter("provided-by-environment")
    @JsonSetter("provided-by-environment")
    public void setProvidedByEnvironment(boolean providedByEnvironment) {
        this.providedByEnvironment = providedByEnvironment;
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
        if ("library-directory".equals(access.getKey())) {
            return !isLoadFromArtifact();
        }
        return super.isParameterUIVisible(tree, access);
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReportSettings reportSettings, JIPipeValidationReport report, JIPipeProgressInfo progressInfo) {
        if (!isProvidedByEnvironment()) {
            super.reportValidity(reportContext, reportSettings, report, progressInfo);
            if (!isLoadFromArtifact()) {
                if (!Files.isDirectory(getAbsoluteLibraryDirectory())) {
                    reportContext.error().title("Missing Python adapter library!").explanation("The Python integration requires an adapter library. It was not found at " + getAbsoluteLibraryDirectory()).solution("Install the Python adapter library by navigating to Project > Application settings > Extensions > Python integration (adapter) or configure the adapter to be provided by the Python environment if applicable.").report(report);
                }
            }
        }
    }

    public Path getAbsoluteLibraryDirectory() {
        return PathUtils.relativeJIPipeUserDirToAbsolute(libraryDirectory);
    }

    @Override
    public Icon getIcon() {
        if (isLoadFromArtifact()) {
            return JIPipe.RESOURCES.getIcon16("actions/run-install.png");
        } else {
            return JIPipe.RESOURCES.getIcon16("actions/plugins.png");
        }
    }

    @Override
    public String getInfo() {
        if (isLoadFromArtifact()) {
            return StringUtils.orElse(getArtifactQuery().getQuery(), "<Not set>");
        } else if (isProvidedByEnvironment()) {
            return "<Internal>";
        } else {
            return StringUtils.orElse(getLibraryDirectory(), "<Not set>");
        }
    }

    /**
     * Installs the library if needed
     *
     * @param code the current Python code
     */
    public void generateCode(StringBuilder code, JIPipeProgressInfo progressInfo) {
        if (!isProvidedByEnvironment()) {
            if (!code.toString().contains("import sys")) {
                code.append("import sys\n");
            }
            code.append("sys.path.append(\"").append(ScriptUtils.escapeString(PathUtils.relativeJIPipeUserDirToAbsolute(getLibraryDirectory()).toString())).append("\")\n");
        }
    }

}
