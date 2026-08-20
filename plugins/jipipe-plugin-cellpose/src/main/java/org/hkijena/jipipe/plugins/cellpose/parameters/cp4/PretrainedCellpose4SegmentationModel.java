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

package org.hkijena.jipipe.plugins.cellpose.parameters.cp4;

import org.hkijena.jipipe.api.AddJIPipeDocumentationDescription;
import org.hkijena.jipipe.plugins.cellpose.parameters.PretrainedCellposeModelEnumItemInfo;
import org.hkijena.jipipe.plugins.parameters.api.enums.EnumParameterSettings;

@AddJIPipeDocumentationDescription(description = "See https://cellpose.readthedocs.io/en/latest/models.html for a description of all models")
@EnumParameterSettings(itemInfo = PretrainedCellposeModelEnumItemInfo.class)
public enum PretrainedCellpose4SegmentationModel {
    cpsam_v2("cpsam_v2", "Cellpose SAM v2"),
    cpdino("cpdino", "Cellpose DINO (ViT-L)"),
    cpdino_vitb("cpdino-vitb", "Cellpose DINO (ViT-B)"),
    cpsam("cpsam", "Cellpose SAM"),
    None(null, "None (only training)");

    private final String id;
    private final String name;

    PretrainedCellpose4SegmentationModel(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        if (id != null) {
            return name + " [" + id + "]";
        } else {
            return "No model";
        }
    }
}

