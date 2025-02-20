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

package org.hkijena.jipipe.plugins.cellpose.utils;

import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageSliceIndex;

import java.util.HashMap;
import java.util.Map;

public class CellposeImageInfo {
    private final int sourceRow;
    private final Map<ImageSliceIndex, String> sliceBaseNames;

    public CellposeImageInfo(int sourceRow) {
        this.sourceRow = sourceRow;
        this.sliceBaseNames = new HashMap<>();
    }

    public int getSourceRow() {
        return sourceRow;
    }

    public Map<ImageSliceIndex, String> getSliceBaseNames() {
        return sliceBaseNames;
    }
}
