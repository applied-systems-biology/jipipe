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

package org.hkijena.jipipe.api.progress;

/**
 * Log levels for progress log records, following the log4j-style numeric scale.
 * The numeric values are part of the NDJSON wire contract ({@code lv} field of {@code l} records) and must not change.
 */
public enum JIPipeLogLevel {

    DEBUG(10),
    INFO(20),
    WARN(30),
    ERROR(40);

    private final int value;

    JIPipeLogLevel(int value) {
        this.value = value;
    }

    /**
     * Returns the numeric level value (part of the NDJSON wire contract).
     *
     * @return the numeric level value
     */
    public int value() {
        return value;
    }
}
