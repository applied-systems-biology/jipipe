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
import org.hkijena.jipipe.api.data.documentation.JIPipeDataCrate;
import org.hkijena.jipipe.api.data.documentation.JIPipeDataCrateEntity;
import org.hkijena.jipipe.api.data.documentation.JIPipeDataCrateEntityType;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.utils.PathUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SetJIPipeDocumentation(name = "URI", description = "Uniform Resource Identifier string")
@JIPipeDataCrate(
        entities = @JIPipeDataCrateEntity(id = "regex:\\./.*\\.(uri|url|txt)",
                type = JIPipeDataCrateEntityType.File,
                name = "Text file",
                description = "A text file that contains the URI",
                encodingFormat = EncodingFormats.TXT)
)
public class URIData extends StringData {
    public URIData(String data) {
        super(data);
    }

    public URIData(StringData other) {
        super(other);
    }

    public static URIData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        Path file = PathUtils.findFileByExtensionIn(storage.getFileSystemPath(), ".uri", ".url", ".txt");
        try {
            return new URIData(new String(Files.readAllBytes(file), Charsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getOutputExtension() {
        return ".uri";
    }
}
