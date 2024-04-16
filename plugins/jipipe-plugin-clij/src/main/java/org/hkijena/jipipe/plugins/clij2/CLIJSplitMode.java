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

package org.hkijena.jipipe.plugins.clij2;

public enum CLIJSplitMode {
    None,
    To2D,
    To3D;


    @Override
    public String toString() {
        switch (this) {
            case To2D:
                return "Into 2D slices (per Z/C/T)";
            case To3D:
                return "Into 3D cubes (per C/T)";
            default:
                return "None (might not always work)";
        }
    }
}
