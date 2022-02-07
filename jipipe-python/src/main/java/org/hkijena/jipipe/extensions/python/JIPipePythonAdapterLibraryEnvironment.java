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
import org.hkijena.jipipe.extensions.parameters.api.collections.ListParameter;

import java.nio.file.Paths;
import java.util.Map;

/**
 * Environment that supplies the JIPipe Python adapter library
 */
public class JIPipePythonAdapterLibraryEnvironment extends PythonPackageLibraryEnvironment {

    public JIPipePythonAdapterLibraryEnvironment() {
        this.setName("Default");
        this.setLibraryDirectory(Paths.get("jipipe").resolve("lib-jipipe-py"));
    }

    public JIPipePythonAdapterLibraryEnvironment(PythonPackageLibraryEnvironment other) {
        super(other);
    }

    @Override
    public void install(JIPipeProgressInfo progressInfo) {
        installFromResources("org.hkijena.jipipe", "/org/hkijena/jipipe/extensions/python/lib", PythonExtension.class, progressInfo);
    }

    @Override
    public Map<String, String> getPackagedVersions() {
        return getPackagedVersionsFromResources("/org/hkijena/jipipe/extensions/python/lib/version.txt", PythonExtension.class);
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
