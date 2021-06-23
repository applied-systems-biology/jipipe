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

package org.hkijena.jipipe.extensions.deeplearning;

import com.google.common.io.Resources;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.parameters.collections.ListParameter;
import org.hkijena.jipipe.extensions.python.PythonPackageLibraryEnvironment;
import org.hkijena.jipipe.utils.PathUtils;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Environment that supplies the JIPipe Python adapter library
 */
public class DeepLearningToolkitLibraryEnvironment extends PythonPackageLibraryEnvironment {

    public DeepLearningToolkitLibraryEnvironment() {
        this.setName("Default");
        this.setLibraryDirectory(Paths.get("jipipe").resolve("lib-dltoolkit"));
    }

    public DeepLearningToolkitLibraryEnvironment(PythonPackageLibraryEnvironment other) {
        super(other);
    }

    @Override
    public Map<String, String> getPackagedVersions() {
        return getPackagedVersionsFromResources("/org/hkijena/jipipe/extensions/deeplearning/lib/version.txt", DeepLearningExtension.class);
    }

    @Override
    public void install(JIPipeProgressInfo progressInfo) {
        installFromResources("org.hkijena.jipipe", "/org/hkijena/jipipe/extensions/deeplearning/lib", DeepLearningExtension.class, progressInfo);
    }

    /**
     * A list of {@link DeepLearningToolkitLibraryEnvironment}
     */
    public static class List extends ListParameter<DeepLearningToolkitLibraryEnvironment> {
        public List() {
            super(DeepLearningToolkitLibraryEnvironment.class);
        }

        public List(DeepLearningToolkitLibraryEnvironment.List other) {
            super(DeepLearningToolkitLibraryEnvironment.class);
            for (DeepLearningToolkitLibraryEnvironment environment : other) {
                add(new DeepLearningToolkitLibraryEnvironment(environment));
            }
        }
    }
}
