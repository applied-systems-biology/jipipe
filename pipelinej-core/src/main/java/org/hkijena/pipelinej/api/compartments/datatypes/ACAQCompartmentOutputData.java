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

package org.hkijena.pipelinej.api.compartments.datatypes;

import org.hkijena.pipelinej.api.ACAQDocumentation;
import org.hkijena.pipelinej.api.ACAQHidden;
import org.hkijena.pipelinej.api.data.ACAQData;
import org.hkijena.pipelinej.ui.ACAQWorkbench;

import java.nio.file.Path;

/**
 * Represents an {@link org.hkijena.pipelinej.api.compartments.algorithms.ACAQCompartmentOutput} in the compartment graph.
 * This is purely structural data.
 */
@ACAQDocumentation(name = "Output data", description = "Output of a compartment")
@ACAQHidden
public class ACAQCompartmentOutputData implements ACAQData {
    @Override
    public void saveTo(Path storageFilePath, String name, boolean forceName) {

    }

    @Override
    public ACAQData duplicate() {
        return new ACAQCompartmentOutputData();
    }

    @Override
    public void display(String displayName, ACAQWorkbench workbench) {

    }
}
