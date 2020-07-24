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

package org.hkijena.jipipe.extensions.strings;

import org.hkijena.jipipe.api.JIPipeDocumentation;

@JIPipeDocumentation(name = "Json", description = "Text in JSON format")
public class JsonData extends StringData {
    public JsonData(String data) {
        super(data);
    }

    public JsonData(StringData other) {
        super(other);
    }

    @Override
    public String getOutputExtension() {
        return ".json";
    }

    @Override
    public String getMimeType() {
        return "application-json";
    }
}
