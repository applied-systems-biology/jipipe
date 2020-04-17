package org.hkijena.acaq5.extensions.standardparametereditors.ui;

import org.hkijena.acaq5.ui.components.FileSelection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Attached to the getter or setter of {@link java.nio.file.Path} parameter to setup the GUI
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface FilePathParameterSettings {
    /**
     * @return If the path is an input or an output
     */
    FileSelection.IOMode ioMode();

    /**
     * @return If the path should be a file, directory or anything
     */
    FileSelection.PathMode pathMode();
}
