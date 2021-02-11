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

package org.hkijena.jipipe.extensions.plots.utils;

import org.hkijena.jipipe.extensions.parameters.primitives.EnumParameterSettings;

import java.awt.*;

@EnumParameterSettings(itemInfo = ColorMapEnumItemInfo.class)
public enum ColorMap {
    Accent(new Paint[]{new Color(0x7fc97f),
            new Color(0xbeaed4),
            new Color(0xfdc086),
            new Color(0xffff99),
            new Color(0x386cb0),
            new Color(0xf0027f),
            new Color(0xbf5b17),
            new Color(0x666666)}),
    Dark2(new Paint[]{new Color(0x1b9e77),
            new Color(0xd95f02),
            new Color(0x7570b3),
            new Color(0xe7298a),
            new Color(0x66a61e),
            new Color(0xe6ab02),
            new Color(0xa6761d),
            new Color(0x666666)}),
    Paired(new Paint[]{new Color(0xa6cee3),
            new Color(0x1f78b4),
            new Color(0xb2df8a),
            new Color(0x33a02c),
            new Color(0xfb9a99),
            new Color(0xe31a1c),
            new Color(0xfdbf6f),
            new Color(0xff7f00),
            new Color(0xcab2d6),
            new Color(0x6a3d9a),
            new Color(0xffff99),
            new Color(0xb15928)}),
    Pastel1(new Paint[]{new Color(0xfbb4ae),
            new Color(0xb3cde3),
            new Color(0xccebc5),
            new Color(0xdecbe4),
            new Color(0xfed9a6),
            new Color(0xffffcc),
            new Color(0xe5d8bd),
            new Color(0xfddaec),
            new Color(0xf2f2f2)
    }),
    Pastel2(new Paint[]{new Color(0xb3e2cd),
            new Color(0xfdcdac),
            new Color(0xcbd5e8),
            new Color(0xf4cae4),
            new Color(0xe6f5c9),
            new Color(0xfff2ae),
            new Color(0xf1e2cc),
            new Color(0xcccccc)}),
    Set1(new Paint[]{new Color(0xe41a1c),
            new Color(0x377eb8),
            new Color(0x4daf4a),
            new Color(0x984ea3),
            new Color(0xff7f00),
            new Color(0xffff33),
            new Color(0xa65628),
            new Color(0xf781bf),
            new Color(0x999999)}),
    Set2(new Paint[]{new Color(0x66c2a5),
            new Color(0xfc8d62),
            new Color(0x8da0cb),
            new Color(0xe78ac3),
            new Color(0xa6d854),
            new Color(0xffd92f),
            new Color(0xe5c494),
            new Color(0xb3b3b3)}),
    Set3(new Paint[]{new Color(0x8dd3c7),
            new Color(0xffffb3),
            new Color(0xbebada),
            new Color(0xfb8072),
            new Color(0x80b1d3),
            new Color(0xfdb462),
            new Color(0xb3de69),
            new Color(0xfccde5),
            new Color(0xd9d9d9),
            new Color(0xbc80bd),
            new Color(0xccebc5),
            new Color(0xffed6f)});

    private final Paint[] colors;

    ColorMap(Paint[] colors) {
        this.colors = colors;
    }

    public Paint[] getColors() {
        return colors;
    }
}
