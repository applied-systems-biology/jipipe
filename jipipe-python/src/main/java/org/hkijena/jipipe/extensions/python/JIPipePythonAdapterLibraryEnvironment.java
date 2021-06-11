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

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.parameters.collections.ListParameter;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Environment that supplies the JIPipe Python adapter library
 */
public class JIPipePythonAdapterLibraryEnvironment extends PythonPackageLibraryEnvironment {

    public JIPipePythonAdapterLibraryEnvironment() {
        this.setName("Default");
        this.setLibraryDirectory(Paths.get("lib-jipipe-py"));
    }

    public JIPipePythonAdapterLibraryEnvironment(PythonPackageLibraryEnvironment other) {
        super(other);
    }

    @Override
    public void install(JIPipeProgressInfo progressInfo) {
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage("org.hkijena.jipipe"))
                .setScanners(new ResourcesScanner()));
        Set<String> allResources = reflections.getResources(Pattern.compile(".*"));
        allResources = allResources.stream().map(s -> {
            if (!s.startsWith("/"))
                return "/" + s;
            else
                return s;
        }).collect(Collectors.toSet());
        String globalFolder = "/org/hkijena/jipipe/extensions/python/lib";
        Set<String> toInstall = allResources.stream().filter(s -> s.startsWith(globalFolder)).collect(Collectors.toSet());
        for (String resource : toInstall) {
            Path targetPath = getLibraryDirectory().resolve(resource.substring(globalFolder.length() + 1));
            progressInfo.log("Installing " + resource + " to " + targetPath);
            if (!Files.isDirectory(targetPath.getParent())) {
                try {
                    Files.createDirectories(targetPath.getParent());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            try {
                Files.copy(PythonUtils.class.getResourceAsStream(resource), targetPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * A list of {@link JIPipePythonAdapterLibraryEnvironment}
     */
    public static class List extends ListParameter<JIPipePythonAdapterLibraryEnvironment> {
        public List() {
            super(JIPipePythonAdapterLibraryEnvironment.class);
        }

        public List(JIPipePythonAdapterLibraryEnvironment.List other) {
            super(JIPipePythonAdapterLibraryEnvironment.class);
            for (JIPipePythonAdapterLibraryEnvironment environment : other) {
                add(new JIPipePythonAdapterLibraryEnvironment(environment));
            }
        }
    }
}
