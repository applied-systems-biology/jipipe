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

package org.hkijena.jipipe.api.data.storage;

import org.hkijena.jipipe.api.JIPipeProgressInfo;

import java.io.Closeable;
import java.nio.file.Path;

/**
 * A storage for serialized {@link org.hkijena.jipipe.api.data.JIPipeData}
 */
public interface JIPipeDataStorage extends AutoCloseable, Closeable {

    /**
     * Returns the {@link JIPipeProgressInfo} instance where information is logged
     *
     * @return the {@link JIPipeProgressInfo} instance
     */
    JIPipeProgressInfo getProgressInfo();

    /**
     * Returns true if the file system path is initialized.
     * For example, for certain storage modes, the storage backend might require to create such a storage, which slows
     * down the file operations.
     *
     * @return if the file system path is initialized
     */
    boolean isFileSystemPathInitialized();

    /**
     * Returns a path on the file system where data can be read/written
     * This path is always ensured to exist.
     * If possible, please use stream-based methods, as the backend might require additional operations to create a file system.
     *
     * @return the path
     */
    Path getFileSystemPath();

    /**
     * The current path relative to the root storage
     *
     * @return the path relative to the root storage
     */
    Path getInternalPath();

    /**
     * Converts a path relative to this storage into a path that is relative to the root storage.
     *
     * @param path the relative path. must be relative to this storage.
     * @return the path relative to the root storage
     */
    default Path relativeToAbsolute(Path path) {
        return getInternalPath().resolve(path);
    }
}
