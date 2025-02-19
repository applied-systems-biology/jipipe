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

public enum Cellpose3DenoiseTrainingNoiseType {
    poisson("poisson", "Poisson Noise Only (poisson)", "poisson=0.8, blur=0.0, downsample=0.0, beta=0.7, gblur=1.0, iso"),
    blurExpr("blur_expr", "Poisson + Mild Blur (blur_expr)", "poisson=0.8, blur=0.8, downsample=0.0, beta=0.1, gblur=0.5, iso"),
    blur("blur", "Poisson + Strong Blur (blur)", "poisson=0.8, blur=0.8, downsample=0.0, beta=0.1, gblur=10.0, iso, uni"),
    downsampleExpr("downsample_expr", "Poisson + Mild Downsample (downsample_expr)", "poisson=0.8, blur=0.8, downsample=0.8, beta=0.03, gblur=1.0, iso"),
    downsample("downsample", "Poisson + Strong Downsample (downsample)", "poisson=0.8, blur=0.8, downsample=0.8, beta=0.03, gblur=5.0, iso, uni"),
    all("all", "Poisson + Blur + Downsample (all)", "poisson=[0.8, 0.8, 0.8], blur=[0.0, 0.8, 0.8], downsample=[0.0, 0.0, 0.8], beta=[0.7, 0.1, 0.03], gblur=[0.0, 10.0, 5.0], iso, uni"),
    aniso("aniso", "Poisson + Anisotropic Blur (aniso)", "poisson=0.8, blur=0.8, downsample=0.8, beta=0.1, gblur=ds_max * 1.5"),
    Custom("", "Custom", "Input custom settings");

    private final String id;
    private final String label;
    private final String description;

    Cellpose3DenoiseTrainingNoiseType(String id, String label, String description) {
        this.id = id;
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return label;
    }

    public String getId() {
        return id;
    }
}
