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

import com.google.common.base.Charsets;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.utils.PathUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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

    public static JsonData importFrom(Path path) {
        Path file = PathUtils.findFileByExtensionIn(path, ".json");
        try {
            return new JsonData(new String(Files.readAllBytes(file), Charsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
