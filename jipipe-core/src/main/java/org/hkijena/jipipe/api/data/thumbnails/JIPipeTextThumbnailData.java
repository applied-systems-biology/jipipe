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

package org.hkijena.jipipe.api.data.thumbnails;

import com.google.common.base.Charsets;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.LabelAsJIPipeHidden;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.utils.PathUtils;

import javax.swing.*;
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SetJIPipeDocumentation(name = "Text thumbnail", description = "Text thumbnail data (used internally)")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains a single *.txt file that stores the current string.",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/string-data.schema.json")
@LabelAsJIPipeHidden
public class JIPipeTextThumbnailData implements JIPipeThumbnailData {

    private final String text;

    public JIPipeTextThumbnailData(String text) {
        this.text = text;
    }

    public JIPipeTextThumbnailData(JIPipeTextThumbnailData other) {
        this.text = other.text;
    }

    public static JIPipeTextThumbnailData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        Path file = PathUtils.findFileByExtensionIn(storage.getFileSystemPath(), ".txt");
        try {
            return new JIPipeTextThumbnailData(file != null ? new String(Files.readAllBytes(file), Charsets.UTF_8) : "N/A");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void exportData(JIPipeWriteDataStorage storage, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        try (FileWriter writer = new FileWriter(PathUtils.ensureExtension(storage.getFileSystemPath().resolve(name), ".txt").toFile())) {
            writer.write(text);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return new JIPipeTextThumbnailData(this);
    }

    @Override
    public void display(String displayName, JIPipeDesktopWorkbench desktopWorkbench, JIPipeDataSource source) {

    }

    public String getText() {
        return text;
    }

    @Override
    public Component renderToComponent(int width, int height) {
        return new JLabel(text);
    }

    @Override
    public boolean hasSize() {
        return false;
    }

    @Override
    public int getWidth() {
        return 0;
    }

    @Override
    public int getHeight() {
        return 0;
    }
}
