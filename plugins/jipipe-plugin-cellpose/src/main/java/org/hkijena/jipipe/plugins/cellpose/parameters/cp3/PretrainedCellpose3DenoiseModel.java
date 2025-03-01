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

import org.hkijena.jipipe.plugins.cellpose.parameters.PretrainedCellposeModelEnumItemInfo;
import org.hkijena.jipipe.plugins.parameters.api.enums.EnumParameterSettings;

@EnumParameterSettings(itemInfo = PretrainedCellposeModelEnumItemInfo.class)
public enum PretrainedCellpose3DenoiseModel {
    denoise_cyto3("denoise_cyto3", "Cytoplasm3 (Denoise)", "cyto3"),
    deblur_cyto3("deblur_cyto3", "Cytoplasm3 (Deblur)", "cyto3"),
    upsample_cyto3("upsample_cyto3", "Cytoplasm3 (Upsample objects to 30px)", "cyto3"),
    oneclick_cyto3("oneclick_cyto3", "Cytoplasm3 (Denoise+Deblur+Upsample)", "cyto3"),

    denoise_cyto2("denoise_cyto2", "Cytoplasm2 (Denoise)", "cyto2"),
    denoise_per_cyto2("denoise_per_cyto2", "Cytoplasm2 (Denoise - Perceptual Loss*)", "cyto2"),
    denoise_seg_cyto2("denoise_seg_cyto2", "Cytoplasm2 (Denoise - Segmentation Loss*)", "cyto2"),
    denoise_rec_cyto2("denoise_rec_cyto2", "Cytoplasm2 (Denoise - Reconstruction Loss*)", "cyto2"),
    deblur_cyto2("deblur_cyto2", "Cytoplasm2 (Deblur)", "cyto2"),
    deblur_per_cyto2("deblur_per_cyto2", "Cytoplasm2 (Deblur - Perceptual Loss*)", "cyto2"),
    deblur_seg_cyto2("deblur_seg_cyto2", "Cytoplasm2 (Deblur - Segmentation Loss*)", "cyto2"),
    deblur_rec_cyto2("deblur_rec_cyto2", "Cytoplasm2 (Deblur - Reconstruction Loss*)", "cyto2"),
    upsample_cyto2("upsample_cyto2", "Cytoplasm2 (Upsample objects to 30px)", "cyto2"),
    upsample_per_cyto2("upsample_per_cyto2", "Cytoplasm2 (Upsample objects to 30px - Perceptual Loss*)", "cyto2"),
    upsample_seg_cyto2("upsample_seg_cyto2", "Cytoplasm2 (Upsample objects to 30px - Segmentation Loss*)", "cyto2"),
    upsample_rec_cyto2("upsample_rec_cyto2", "Cytoplasm2 (Upsample objects to 30px - Reconstruction Loss*)", "cyto2"),
    oneclick_cyto2("oneclick_cyto2", "Cytoplasm2 (Denoise+Deblur+Upsample)", "cyto2"),
    oneclick_per_cyto2("oneclick_per_cyto2", "Cytoplasm2 (Denoise+Deblur+Upsample) [broken]", "cyto2"),
    oneclick_seg_cyto2("oneclick_seg_cyto2", "Cytoplasm2 (Denoise+Deblur+Upsample) [broken]", "cyto2"),
    oneclick_rec_cyto2("oneclick_rec_cyto2", "Cytoplasm2 (Denoise+Deblur+Upsample) [broken]", "cyto2"),
    aniso_cyto2("aniso_cyto2", "Cytoplasm2 (Anisotropic*)", "cyto2"),

    denoise_nuclei("denoise_nuclei", "Nuclei (Denoise)", "nuclei"),
    denoise_per_nuclei("denoise_per_nuclei", "Nuclei (Denoise - Perceptual Loss*)", "nuclei"),
    denoise_seg_nuclei("denoise_seg_nuclei", "Nuclei (Denoise - Segmentation Loss*)", "nuclei"),
    denoise_rec_nuclei("denoise_rec_nuclei", "Nuclei (Denoise - Reconstruction Loss*)", "nuclei"),
    deblur_nuclei("deblur_nuclei", "Nuclei (Deblur)", "nuclei"),
    deblur_per_nuclei("deblur_per_nuclei", "Nuclei (Deblur - Perceptual Loss*)", "nuclei"),
    deblur_seg_nuclei("deblur_seg_nuclei", "Nuclei (Deblur - Segmentation Loss*)", "nuclei"),
    deblur_rec_nuclei("deblur_rec_nuclei", "Nuclei (Deblur - Reconstruction Loss*)", "nuclei"),
    upsample_nuclei("upsample_nuclei", "Nuclei (Upsample objects to 17px)", "nuclei"),
    upsample_per_nuclei("upsample_per_nuclei", "Nuclei (Upsample objects to 17px - Perceptual Loss*)", "nuclei"),
    upsample_seg_nuclei("upsample_seg_nuclei", "Nuclei (Upsample objects to 17px - Segmentation Loss*)", "nuclei"),
    upsample_rec_nuclei("upsample_rec_nuclei", "Nuclei (Upsample objects to 17px - Reconstruction Loss*)", "nuclei"),
    oneclick_nuclei("oneclick_nuclei", "Nuclei (Denoise+Deblur+Upsample)", "nuclei"),
    oneclick_per_nuclei("oneclick_per_nuclei", "Nuclei (Denoise+Deblur+Upsample) [broken]", "nuclei"),
    oneclick_seg_nuclei("oneclick_seg_nuclei", "Nuclei (Denoise+Deblur+Upsample) [broken]", "nuclei"),
    oneclick_rec_nuclei("oneclick_rec_nuclei", "Nuclei (Denoise+Deblur+Upsample) [broken]", "nuclei"),
    aniso_nuclei("aniso_nuclei", "Nuclei (Anisotropic)", "nuclei");

    private final String id;
    private final String name;
    private final String parentModel;

    PretrainedCellpose3DenoiseModel(String id, String name, String parentModel) {
        this.id = id;
        this.name = name;
        this.parentModel = parentModel;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }


    @Override
    public String toString() {
        return name;
    }

    public String getParentModel() {
        return parentModel;
    }
}
