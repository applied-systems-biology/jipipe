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

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.parameters.collections.ListParameter;
import org.hkijena.jipipe.extensions.python.PythonPackageLibraryEnvironment;

import java.nio.file.Paths;
import java.util.Map;

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
