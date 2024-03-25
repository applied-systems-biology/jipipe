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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.transform;

public enum WrapMode {
    None,
    Wrap,
    Replicate,
    Mirror;

    public int wrap(int value, int min, int max) {
        int width = Math.abs(max - min);
        switch (this) {
            case None:
                return value;
            case Wrap:
                value -= min;
                while (value < 0) {
                    value += width;
                }
                return min + (value % width);
            case Replicate:
                return Math.max(min, Math.min(max, value));
            case Mirror:
                if (value < min)
                    return 2 * min - value;
                else if (value > max)
                    return 2 * max - value;
                else
                    return value;
            default:
                throw new UnsupportedOperationException();
        }
    }
}
