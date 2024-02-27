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

package org.hkijena.jipipe.extensions.parameters.library.filesystem;

import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.PathType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Attached to the getter or setter of {@link java.nio.file.Path} parameter to setup the GUI
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PathParameterSettings {
    /**
     * @return If the path is an input or an output
     */
    PathIOMode ioMode();

    /**
     * @return If the path should be a file, directory or anything
     */
    PathType pathMode();

    /**
     * @return File extensions that should be preferred. Only the extension (without period)
     */
    String[] extensions() default {};

    /**
     * @return The key for the starting location
     */
    FileChooserSettings.LastDirectoryKey key() default FileChooserSettings.LastDirectoryKey.Parameters;
}
