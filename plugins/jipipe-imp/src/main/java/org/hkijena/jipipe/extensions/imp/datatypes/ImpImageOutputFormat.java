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

package org.hkijena.jipipe.extensions.imp.datatypes;

public enum ImpImageOutputFormat {
    PNG("PNG", ".png"),
    BMP("BMP", ".bmp"),
    JPG("JPG", ".jpg", ".jpeg"),
    GIF("GIF", ".gif");

    private final String nativeValue;
    private final String[] extensions;

    ImpImageOutputFormat(String nativeValue, String... extensions) {

        this.nativeValue = nativeValue;
        this.extensions = extensions;
    }

    public String getNativeValue() {
        return nativeValue;
    }

    public String[] getExtensions() {
        return extensions;
    }

    public String getExtension() {
        return extensions[0];
    }
}
