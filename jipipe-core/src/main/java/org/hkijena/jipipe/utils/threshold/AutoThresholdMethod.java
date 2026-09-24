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

package org.hkijena.jipipe.utils.threshold;

import org.hkijena.jipipe.plugins.parameters.api.enums.EnumParameterSettings;

/**
 * Auto-thresholding methods, mirroring {@link ij.process.AutoThresholder.Method}.
 * The constant names are intentionally identical to ImageJ's, as enum parameters
 * are serialized via the constant name and existing projects must stay loadable.
 * The implementations operate on arbitrary bin counts (see {@link NBinsAutoThresholder}).
 */
@EnumParameterSettings(itemInfo = AutoThresholdMethodEnumItemInfo.class)
public enum AutoThresholdMethod {
    Default,
    Huang,
    Intermodes,
    IsoData,
    IJ_IsoData,
    Li,
    MaxEntropy,
    Mean,
    MinError,
    Minimum,
    Moments,
    Otsu,
    Percentile,
    RenyiEntropy,
    Shanbhag,
    Triangle,
    Yen
}
