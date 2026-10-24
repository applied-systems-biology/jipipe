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

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.project.JIPipeProject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.file.Path;

/**
 * Writes a {@link JIPipeProject} to a file or stream.
 */
public interface JIPipeProjectWriter extends JIPipeProjectIO {

    /**
     * Writes a project to a file.
     *
     * @param project      the project to write
     * @param target       the target file
     * @param progressInfo the progress info
     * @throws IOException triggered by the JSON writer
     */
    void write(JIPipeProject project, Path target, JIPipeProgressInfo progressInfo) throws IOException;

    /**
     * Writes a project to a writer.
     *
     * @param project the project to write
     * @param writer  the target writer
     * @throws IOException triggered by the JSON writer
     */
    void write(JIPipeProject project, Writer writer) throws IOException;

    /**
     * Writes a project to an output stream.
     *
     * @param project      the project to write
     * @param outputStream the target output stream
     * @throws IOException triggered by the JSON writer
     */
    void write(JIPipeProject project, OutputStream outputStream) throws IOException;
}
