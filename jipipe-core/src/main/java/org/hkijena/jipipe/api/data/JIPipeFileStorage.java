package org.hkijena.jipipe.api.data;

import java.nio.file.Path;

/**
 * Encapsulates a file storage.
 * Currently only returns a file system path
 */
public interface JIPipeFileStorage {
    Path getFileSystemPath();
}
