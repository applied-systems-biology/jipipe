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

package org.hkijena.jipipe.extensions.omnipose;

import org.hkijena.jipipe.api.AddJIPipeDocumentationDescription;

@AddJIPipeDocumentationDescription(description = "See https://github.com/kevinjohncutler/omnipose for a description of all models")
public enum OmniposeModel {
    BactOmni("bact_omni"),
    Bact("bact"),
    Cyto2Omni("cyto2_omni"),
    Cytoplasm("cyto"),
    Cytoplasm2("cyto2"),
    Nucleus("nuclei"),
    Custom("");

    private final String id;

    OmniposeModel(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
