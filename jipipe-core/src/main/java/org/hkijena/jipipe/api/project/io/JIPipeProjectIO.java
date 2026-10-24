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

package org.hkijena.jipipe.api.project.io;

import java.nio.file.Path;
import java.util.List;

/**
 * A JIPipe project input/output format.
 * Formats identify themselves by a stable string id and the file
 * extensions they handle.
 */
public interface JIPipeProjectIO {

    /**
     * Gets the id of this format.
     * The id is a stable string literal — it is persisted in saved
     * projects and must never be derived from code.
     *
     * @return the format id
     */
    String getId();

    /**
     * Gets the file extensions (without the leading dot) that this format handles.
     *
     * @return the file extensions
     */
    List<String> getFileExtensions();

    /**
     * Checks whether this format can handle the given file.
     *
     * @param path the file to check
     * @return true if this format can handle the file
     */
    boolean supports(Path path);
}
