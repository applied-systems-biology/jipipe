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

package org.hkijena.jipipe.api.compartments.datatypes;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeHidden;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

import java.nio.file.Path;

/**
 * Represents an {@link org.hkijena.jipipe.api.compartments.algorithms.JIPipeCompartmentOutput} in the compartment graph.
 * This is purely structural data.
 */
@JIPipeDocumentation(name = "Output data", description = "Output of a compartment")
@JIPipeHidden
public class JIPipeCompartmentOutputData implements JIPipeData {
    @Override
    public void saveTo(Path storageFilePath, String name, boolean forceName) {

    }

    public static JIPipeCompartmentOutputData importFrom(Path path) {
        return new JIPipeCompartmentOutputData();
    }

    @Override
    public JIPipeData duplicate() {
        return new JIPipeCompartmentOutputData();
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {

    }
}
