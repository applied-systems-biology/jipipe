package org.hkijena.jipipe.api.data.storage;

import java.nio.file.Path;

/**
 * A storage for serialized {@link org.hkijena.jipipe.api.data.JIPipeData}
 */
public interface JIPipeDataStorage {
    /**
     * Returns a path on the file system where data can be read/written
     * This path is always ensured to exist.
     * @return the path
     */
    Path getFileSystemPath();

    /**
     * The current path relative to the root storage
     * @return the path relative to the root storage
     */
    Path getInternalPath();
}
