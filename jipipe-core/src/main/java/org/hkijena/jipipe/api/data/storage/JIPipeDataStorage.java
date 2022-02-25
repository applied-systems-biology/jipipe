package org.hkijena.jipipe.api.data.storage;

import java.nio.file.Path;

/**
 * A storage for serialized {@link org.hkijena.jipipe.api.data.JIPipeData}
 */
public interface JIPipeDataStorage {
    /**
     * Returns a path on the file system where data can be read/written
     * @return the path
     */
    Path getFileSystemPath();
}
