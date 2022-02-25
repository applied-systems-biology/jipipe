package org.hkijena.jipipe.api.data.storage;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

/**
 * {@link JIPipeDataStorage} that is used for writing a {@link org.hkijena.jipipe.api.data.JIPipeData}.
 */
public interface JIPipeWriteDataStorage extends JIPipeDataStorage {
    /**
     * Returns a new storage that resolves to a path inside this storage
     * @param name the path name
     * @return the sub-storage
     */
    JIPipeWriteDataStorage resolve(String name);

    /**
     * Returns a new storage that resolves to a path inside this storage
     * @param path the path
     * @return the sub-storage
     */
    JIPipeWriteDataStorage resolve(Path path);

    /**
     * Creates an output stream that writes the specified file element.
     * Please do not forget to close the stream.
     * @param name the file name
     * @return the file stream
     */
    OutputStream write(String name);

    /**
     * Creates an output stream that writes the specified file element.
     * Please do not forget to close the stream.
     * @param path the file path. relative to the current store.
     * @return the file stream
     */
    OutputStream write(Path path);
}
