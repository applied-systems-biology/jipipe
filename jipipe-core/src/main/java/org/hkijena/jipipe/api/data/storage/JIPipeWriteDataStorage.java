package org.hkijena.jipipe.api.data.storage;

import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * {@link JIPipeDataStorage} that is used for writing a {@link org.hkijena.jipipe.api.data.JIPipeData}.
 */
public interface JIPipeWriteDataStorage extends JIPipeDataStorage {
    /**
     * Returns a new storage that resolves to a path inside this storage
     * @param name the path name
     * @return the sub-storage
     */
    default JIPipeWriteDataStorage resolve(String name) {
        return resolve(Paths.get(name));
    }

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
    default OutputStream write(String name) {
        return write(Paths.get(name));
    }

    /**
     * Creates an output stream that writes the specified file element.
     * Please do not forget to close the stream.
     * @param path the file path. relative to the current store.
     * @return the file stream
     */
    OutputStream write(Path path);

    /**
     * Writes an object as JSON
     * @param path the relative path
     * @param obj the object
     */
    default void writeJSON(Path path, Object obj) {
        try(OutputStream stream = write(path)) {
            JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(stream, obj);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Writes an object as JSON
     * @param path the relative path
     * @param text the text
     */
    default void writeText(Path path, String text) {
        try(OutputStream stream = write(path)) {
            stream.write(text.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Writes a byte array
     * @param path the relative path
     * @param arr the byte array
     */
    default void writeBytes(Path path, byte[] arr) {
        try(OutputStream stream = write(path)) {
            stream.write(arr);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
