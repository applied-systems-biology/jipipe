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

package org.hkijena.jipipe.plugins.ilastik.parameters;

public enum IlastikProjectValidationMode {
    CrashOnError("Crash on error"),
    SkipOnError("Skip on error"),
    Ignore("Do not validate");

    private final String text;

    IlastikProjectValidationMode(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
