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

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.io.Resources;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.environments.ExternalEnvironment;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.library.filesystem.PathParameterSettings;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.utils.*;
import org.hkijena.jipipe.utils.scripting.MacroUtils;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import javax.swing.*;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
    @PathParameterSettings(key = FileChooserSettings.LastDirectoryKey.External, pathMode = PathType.DirectoriesOnly, ioMode = PathIOMode.Open)
    @JsonGetter("library-directory")
    public Path getLibraryDirectory() {
        return libraryDirectory;
    }

    @JIPipeParameter("library-directory")
    @JsonSetter("library-directory")
    public void setLibraryDirectory(Path libraryDirectory) {
        this.libraryDirectory = libraryDirectory;
    }

    @JIPipeDocumentation(name = "Provided by Python environment", description = "If enabled, the library will be ignored. It is assumed that all packages are provided by the Python environment (Conda/Virtualenv).")
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
    public void reportValidity(JIPipeIssueReport report) {

    }

    public Path getAbsoluteLibraryDirectory() {
        return PathUtils.relativeToImageJToAbsolute(libraryDirectory);
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
            return !Files.isDirectory(getAbsoluteLibraryDirectory());
        }
        return false;
    }

    /**
     * Returns true if the currently installed library is the newest version
     * Only auto-installed (resource-based) installations are tested
     * Returns true if needsInstall() returns true (as the newest lib will be installed anyways)
     *
     * @return if the newest version is installed
     */
    public boolean isNewestVersion() {
        if (needsInstall())
            return true;
        Map<String, String> installedVersions = getInstalledVersions();
        Map<String, String> packagedVersions = getPackagedVersions();

        for (Map.Entry<String, String> entry : packagedVersions.entrySet()) {
            String installedVersion = installedVersions.getOrDefault(entry.getKey(), "0");
            if (StringUtils.compareVersions(installedVersion, entry.getValue()) < 0)
                return false;
        }
        return true;
    }

    /**
     * Gets the installed versions of packages
     * Returns an empty map if the version file could not be found or the environment is not installed
     * Returns an empty map if the library is provided by the environment
     *
     * @return package to version map
     */
    public Map<String, String> getInstalledVersions() {
        if (needsInstall())
            return Collections.emptyMap();
        Path versionsFile = getAbsoluteLibraryDirectory().resolve("version.txt");
        if (Files.exists(versionsFile)) {
            try {
                Map<String, String> result = new HashMap<>();
                for (String line : Files.readAllLines(versionsFile)) {
                    String[] split = line.split("[><=]+");
                    result.put(split[0].trim(), split[1].trim());
                }
                return result;
            } catch (IOException e) {
                e.printStackTrace();
                return Collections.emptyMap();
            }
        } else {
            return Collections.emptyMap();
        }
    }

    /**
     * Gets the packaged versions of the installed libraries
     *
     * @return package to version map
     */
    public abstract Map<String, String> getPackagedVersions();

    /**
     * Installs the library into the target directory
     */
    public abstract void install(JIPipeProgressInfo progressInfo);

    /**
     * Gets the packaged versions from resources
     *
     * @param resourcePath   the resource path
     * @param resourceLoader the resource loader
     * @return packaged versions
     */
    protected Map<String, String> getPackagedVersionsFromResources(String resourcePath, Class<?> resourceLoader) {
        Map<String, String> result = new HashMap<>();
        URL url = resourceLoader.getResource(resourcePath);
        try {
            String text = Resources.toString(url, StandardCharsets.UTF_8);
            for (String line : text.split("\n")) {
                String[] split = line.split("[><=]+");
                result.put(split[0].trim(), split[1].trim());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    /**
     * Installs the library from resources into the target directory
     *
     * @param javaPackage    the java package (for the resource search)
     * @param globalFolder   the folder that contains the library
     * @param resourceLoader the resource loader
     */
    protected void installFromResources(String javaPackage, String globalFolder, Class<?> resourceLoader, JIPipeProgressInfo progressInfo) {
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage(javaPackage))
                .setScanners(new ResourcesScanner()));
        Set<String> allResources = reflections.getResources(Pattern.compile(".*"));
        allResources = allResources.stream().map(s -> {
            if (!s.startsWith("/"))
                return "/" + s;
            else
                return s;
        }).collect(Collectors.toSet());
        Set<String> toInstall = allResources.stream().filter(s -> s.startsWith(globalFolder)).collect(Collectors.toSet());
        for (String resource : toInstall) {
            Path targetPath = PathUtils.relativeToImageJToAbsolute(getLibraryDirectory().resolve(resource.substring(globalFolder.length() + 1)));
            progressInfo.log("Installing " + resource + " to " + targetPath);
            if (!Files.isDirectory(targetPath.getParent())) {
                try {
                    Files.createDirectories(targetPath.getParent());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            try {
                Files.copy(resourceLoader.getResourceAsStream(resource), targetPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
