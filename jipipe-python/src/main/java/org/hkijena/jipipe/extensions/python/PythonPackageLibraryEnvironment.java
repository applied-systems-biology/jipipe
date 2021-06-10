/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.python;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.environments.ExternalEnvironment;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.utils.MacroUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * An environment-like type that points to a package
 */
public abstract class PythonPackageLibraryEnvironment extends ExternalEnvironment {

    private Path libraryDirectory = Paths.get("library");
    private boolean providedByEnvironment = false;

    public PythonPackageLibraryEnvironment() {

    }

    public PythonPackageLibraryEnvironment(PythonPackageLibraryEnvironment other) {
        super(other);
        this.libraryDirectory = other.libraryDirectory;
        this.providedByEnvironment = other.providedByEnvironment;
    }

    @JIPipeDocumentation(name = "Library directory", description = "The directory that contains the Python packages. Ignored if the Python packages are provided by the Python environment.")
    @JIPipeParameter("library-directory")
    public Path getLibraryDirectory() {
        return libraryDirectory;
    }

    @JIPipeParameter("library-directory")
    public void setLibraryDirectory(Path libraryDirectory) {
        this.libraryDirectory = libraryDirectory;
    }

    @JIPipeDocumentation(name = "Provided by Python environment", description = "If enabled, the library will be ignored. It is assumed that all packages are provided by the Python environment (Conda/Virtualenv).")
    @JIPipeParameter("provided-by-environment")
    public boolean isProvidedByEnvironment() {
        return providedByEnvironment;
    }

    @JIPipeParameter("provided-by-environment")
    public void setProvidedByEnvironment(boolean providedByEnvironment) {
        this.providedByEnvironment = providedByEnvironment;
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {

    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/plugins.png");
    }

    @Override
    public String getInfo() {
        if (providedByEnvironment) {
            return "Provided by Python environment";
        } else {
            return libraryDirectory.toString();
        }
    }

    /**
     * Installs the library if needed
     *
     * @param code the current Python code
     */
    public void generateCode(StringBuilder code, JIPipeProgressInfo progressInfo) {
        if (needsInstall()) {
            install(progressInfo);
        }
        if (!isProvidedByEnvironment()) {
            if (!code.toString().contains("import sys")) {
                code.append("import sys\n");
            }
            code.append("sys.path.append(\"").append(MacroUtils.escapeString(getLibraryDirectory().toAbsolutePath().toString())).append("\")\n");
        }
    }

    /**
     * Returns true if the library is not installed
     *
     * @return if the library is not installed
     */
    public boolean needsInstall() {
        if (!isProvidedByEnvironment()) {
            return !Files.isDirectory(libraryDirectory);
        }
        return false;
    }

    /**
     * Installs the library into the target directory
     */
    public abstract void install(JIPipeProgressInfo progressInfo);
}