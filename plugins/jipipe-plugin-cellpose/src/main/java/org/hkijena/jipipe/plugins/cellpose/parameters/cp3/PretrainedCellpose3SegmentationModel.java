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

package org.hkijena.jipipe.plugins.cellpose.parameters.cp3;

import org.hkijena.jipipe.api.AddJIPipeDocumentationDescription;
import org.hkijena.jipipe.plugins.cellpose.parameters.PretrainedCellposeModelEnumItemInfo;
import org.hkijena.jipipe.plugins.parameters.api.enums.EnumParameterSettings;

@AddJIPipeDocumentationDescription(description = "See https://cellpose.readthedocs.io/en/latest/models.html for a description of all models")
@EnumParameterSettings(itemInfo = PretrainedCellposeModelEnumItemInfo.class)
public enum PretrainedCellpose3SegmentationModel {
    cyto3("cyto3", "Cytoplasm3"),
    nuclei("nuclei", "Nuclei"),
    cyto2_cp3("cyto2_cp3", "Cytoplasm2 (Cellpose 3)"),
    tissuenet_cp3("tissuenet_cp3", "TissueNet (Cellpose 3)"),
    livecell_cp3("livecell_cp3", "LiveCell (Cellpose 3)"),
    yeast_PhC_cp3("yeast_PhC_cp3", "Yeast Phase Contrast (Cellpose 3)"),
    yeast_BF_cp3("yeast_BF_cp3", "Yeast Brightfield (Cellpose 3)"),
    bact_phase_cp3("bact_phase_cp3", "Bacteria Phase (Cellpose 3)"),
    bact_fluor_cp3("bact_fluor_cp3", "Bacteria Fluorescence (Cellpose 3)"),
    deepbacs_cp3("deepbacs_cp3", "DeepBacs (Cellpose 3)"),
    cyto2("cyto2", "Cytoplasm2"),
    cyto("cyto", "Cytoplasm"),
    CPx("CPx", "CPx"),
    transformer_cp3("transformer_cp3", "Transformer (Cellpose 3)"),
    neurips_cellpose_default("neurips_cellpose_default", "NeurIPS Cellpose Default"),
    neurips_cellpose_transformer("neurips_cellpose_transformer", "NeurIPS Cellpose Transformer"),
    neurips_grayscale_cyto2("neurips_grayscale_cyto2", "NeurIPS Grayscale Cytoplasm2"),
    CP("CP", "CP"),
    TN1("TN1", "TissueNet1"),
    TN2("TN2", "TissueNet2"),
    TN3("TN3", "TissueNet3"),
    LC1("LC1", "LiveCell1"),
    LC2("LC2", "LiveCell2"),
    LC3("LC3", "LiveCell3"),
    LC4("LC4", "LiveCell4"),
    None(null, "None (only training)");

    private final String id;
    private final String name;

    PretrainedCellpose3SegmentationModel(String id, String name) {
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

