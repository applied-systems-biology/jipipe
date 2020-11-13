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
import org.apache.commons.lang.CharSetUtils;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.texteditor.JIPipeTextEditor;
import org.hkijena.jipipe.utils.PathUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A data type that contains a string
 */
@JIPipeDocumentation(name = "String", description = "A text")
public class StringData implements JIPipeData {

    private final String data;

    public StringData(String data) {
        this.data = data;
    }

    public StringData(StringData other) {
        this.data = other.data;
    }

    public static StringData importFrom(Path path) {
        Path file = PathUtils.findFileByExtensionIn(path, ".txt");
        try {
            return new StringData(new String(Files.readAllBytes(file), Charsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void saveTo(Path storageFilePath, String name, boolean forceName) {
        try (FileWriter writer = new FileWriter(storageFilePath.resolve(name + getOutputExtension()).toFile())) {
            writer.write(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public JIPipeData duplicate() {
        return new StringData(data);
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {
        JIPipeTextEditor editor = JIPipeTextEditor.openInNewTab(workbench, displayName);
        editor.setMimeType(getMimeType());
        editor.setText(data);
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
