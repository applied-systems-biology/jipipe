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
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Reads a {@link JIPipeProject} from a file.
 */
public interface JIPipeProjectReader extends JIPipeProjectIO {

    /**
     * Reads a project from the given file.
     *
     * @param path          the project file to read
     * @param context       the validation context
     * @param report        issue report
     * @param notifications notifications for the user
     * @param progressInfo  the progress info
     * @return the loaded project
     * @throws IOException triggered by the JSON parser
     */
    JIPipeProject read(Path path, JIPipeValidationReportContext context, JIPipeValidationReport report, JIPipeNotificationInbox notifications, JIPipeProgressInfo progressInfo) throws IOException;
}
