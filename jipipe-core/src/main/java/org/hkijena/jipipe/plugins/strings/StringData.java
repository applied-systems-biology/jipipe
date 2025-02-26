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
import org.apache.commons.lang3.CharSetUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeIconLabelThumbnailData;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeTextThumbnailData;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeThumbnailData;
import org.hkijena.jipipe.utils.PathUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A data type that contains a string
 */
@SetJIPipeDocumentation(name = "String", description = "A text")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains a single *.txt file that stores the current string.",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/string-data.schema.json")
public class StringData implements JIPipeData {

    private final String data;

    public StringData(String data) {
        this.data = data;
    }

    public StringData(StringData other) {
        this.data = other.data;
    }

    public static StringData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        Path file = PathUtils.findFileByExtensionIn(storage.getFileSystemPath(), ".txt");
        try {
            return new StringData(new String(Files.readAllBytes(file), Charsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void exportData(JIPipeWriteDataStorage storage, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        try (FileWriter writer = new FileWriter(storage.getFileSystemPath().resolve(name + getOutputExtension()).toFile())) {
            writer.write(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return new StringData(data);
    }

    @Override
    public JIPipeThumbnailData createThumbnail(int width, int height, JIPipeProgressInfo progressInfo) {
        return new JIPipeTextThumbnailData(data.length() + " characters");
    }

    /**
     * The extension that is generated by the output function
     *
     * @return the extension
     */
    public String getOutputExtension() {
        return ".txt";
    }

    /**
     * Returns the MIME type of the string
     *
     * @return the MIME type
     */
    public String getMimeType() {
        return "text-plain";
    }

    public String getData() {
        return data;
    }

    @Override
    public String toString() {
        if (data.contains("\n")) {
            return "String (" + CharSetUtils.count(data, "\n") + " lines)";
        } else {
            return data;
        }
    }
}
