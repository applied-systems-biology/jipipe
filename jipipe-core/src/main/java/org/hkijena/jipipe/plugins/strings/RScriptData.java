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
import org.hkijena.jipipe.api.data.documentation.DefineJIPipeDataCrateEntity;
import org.hkijena.jipipe.api.data.documentation.JIPipeDataCrateEntityType;
import org.hkijena.jipipe.api.data.documentation.ConfigureJIPipeDataCrate;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.utils.PathUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SetJIPipeDocumentation(name = "R script", description = "An R script")
@ConfigureJIPipeDataCrate(
        entities = @DefineJIPipeDataCrateEntity(id = "glob:./*.R", type = JIPipeDataCrateEntityType.File, name = "R script", description = "The R script file", encodingFormat = EncodingFormats.R_SCRIPT)
)
public class RScriptData extends StringData {
    public RScriptData(String data) {
        super(data);
    }

    public RScriptData(StringData other) {
        super(other);
    }

    public static RScriptData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        Path file = PathUtils.findFileByExtensionIn(storage.getFileSystemPath(), ".R");
        try {
            return new RScriptData(new String(Files.readAllBytes(file), Charsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getOutputExtension() {
        return ".R";
    }

    @Override
    public String getMimeType() {
        return "text/x-r";
    }
}
