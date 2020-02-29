/*
 * Copyright by Ruman Gerst
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Insitute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * This code is licensed under BSD 2-Clause
 * See the LICENSE file provided with this code for the full license.
 */

package org.hkijena.acaq5.ui.plotbuilder;

import java.util.ArrayList;
import java.util.List;

public class ACAQPlotSeriesData {
    private String name;
    private List<Object> data = new ArrayList<>();

    public ACAQPlotSeriesData(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<Object> getData() {
        return data;
    }

    public int getSize() {
        return data.size();
    }
}
