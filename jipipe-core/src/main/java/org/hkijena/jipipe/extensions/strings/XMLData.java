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

@JIPipeDocumentation(name = "XML", description = "Text in extended markup language (XML)")
public class XMLData extends StringData {
    public XMLData(String data) {
        super(data);
    }

    public XMLData(StringData other) {
        super(other);
    }

    @Override
    public String getOutputExtension() {
        return ".xml";
    }

    @Override
    public String getMimeType() {
        return "text-xml";
    }

    public static XMLData importFrom(Path path) {
        Path file = PathUtils.findFileByExtensionIn(path, ".xml");
        try {
            return new XMLData(new String(Files.readAllBytes(file), Charsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
