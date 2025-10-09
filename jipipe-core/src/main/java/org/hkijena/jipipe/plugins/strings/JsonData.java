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

package org.hkijena.jipipe.plugins.strings;

import com.google.common.base.Charsets;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.documentation.EncodingFormats;
import org.hkijena.jipipe.api.data.documentation.Entity;
import org.hkijena.jipipe.api.data.documentation.EntityType;
import org.hkijena.jipipe.api.data.documentation.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.utils.PathUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SetJIPipeDocumentation(name = "Json", description = "Text in JSON format")
@JIPipeDataStorageDocumentation(
        @Entity(id = "glob://./*.json", type = EntityType.File, name = "JSON file", description = "The JSON file", encodingFormat = EncodingFormats.JSON)
)
public class JsonData extends StringData {
    public JsonData(String data) {
        super(data);
    }

    public JsonData(StringData other) {
        super(other);
    }

    public static JsonData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        Path file = PathUtils.findFileByExtensionIn(storage.getFileSystemPath(), ".json");
        try {
            return new JsonData(new String(Files.readAllBytes(file), Charsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
