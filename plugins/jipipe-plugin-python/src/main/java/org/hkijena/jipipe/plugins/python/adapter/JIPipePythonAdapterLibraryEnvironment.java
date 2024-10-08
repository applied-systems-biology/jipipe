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

package org.hkijena.jipipe.plugins.python.adapter;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.artifacts.JIPipeLocalArtifact;
import org.hkijena.jipipe.plugins.parameters.api.collections.ListParameter;
import org.hkijena.jipipe.plugins.python.PythonPackageLibraryEnvironment;

import java.nio.file.Paths;

/**
 * Environment that supplies the JIPipe Python adapter library
 */
public class JIPipePythonAdapterLibraryEnvironment extends PythonPackageLibraryEnvironment {

    public static final String ENVIRONMENT_ID = "jipipe-python-adapter-library";

    public JIPipePythonAdapterLibraryEnvironment() {
        this.setName("Default");
        this.setLibraryDirectory(Paths.get("jipipe").resolve("lib-jipipe-python"));
    }

    public JIPipePythonAdapterLibraryEnvironment(PythonPackageLibraryEnvironment other) {
        super(other);
    }

    @Override
    public void applyConfigurationFromArtifact(JIPipeLocalArtifact artifact, JIPipeProgressInfo progressInfo) {
        setProvidedByEnvironment(false);
        setLibraryDirectory(artifact.getLocalPath().resolve("jipipe-python-main"));
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
