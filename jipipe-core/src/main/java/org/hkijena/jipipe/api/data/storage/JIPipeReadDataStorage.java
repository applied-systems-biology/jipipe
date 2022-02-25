package org.hkijena.jipipe.api.data.storage;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * {@link JIPipeDataStorage} that is used for reading files into a {@link org.hkijena.jipipe.api.data.JIPipeData} instance.
 */
public interface JIPipeReadDataStorage extends JIPipeDataStorage {
    /**
     * Returns a new storage that resolves to a path inside this storage
     * @param name the path name
     * @return the sub-storage
     */
    JIPipeReadDataStorage resolve(String name);

    /**
     * Returns a new storage that resolves to a path inside this storage
     * @param path the path
     * @return the sub-storage
     */
    JIPipeReadDataStorage resolve(Path path);

    /**
     * Returns true if the element with given name is a file
     * @param name the element name
     * @return if the element is a file
     */
    boolean isFile(String name);

    /**
     * Returns true if the element with given name is a file
     * @param path the element path. relative to the current storage.
     * @return if the element is a file
     */
    boolean isFile(Path path);

    /**
     * Returns true if the element with given name is a directory
     * @param name the element name
     * @return if the element is a file
     */
    default boolean isDirectory(String name) {
        return !isFile(name);
    }

    /**
     * Returns true if the element with given name is a directory
     * @param path the element path. relative to the current storage.
     * @return if the element is a file
     */
    default boolean isDirectory(Path path) {
        return !isFile(path);
    }

    /**
     * Returns true if the storage contains given element
     * @param name the element name
     * @return if the storage contains the element
     */
    boolean exists(String name);

    /**
     * Returns true if the storage contains given element
     * @param path the element path. relative to the current storage.
     * @return if the storage contains the element
     */
    boolean exists(Path path);

    /**
     * Lists the relative paths of all elements in this storage
     * @return list of elements. relative to the current storage.
     */
    Collection<Path> list();

    /**
     * Lists the names of the elements in this storage
     * @return the element names
     */
    default Collection<String> listNames() {
        return list().stream().map(p -> p.getFileName().toString()).collect(Collectors.toList());
    }

    /**
     * Lists all file elements that have given extension.
     * Extension matching is case-insensitive.
     * @param extensions extensions
     * @return files ending with given extensions
     */
    default List<Path> findFilesByExtension(String... extensions) {
        List<Path> result = new ArrayList<>();
        for (Path path : list()) {
            if(isFile(path)) {
                String fileName = path.toString().toLowerCase(Locale.ROOT);
                for (String extension : extensions) {
                    if(fileName.endsWith(extension.toLowerCase(Locale.ROOT))) {
                        result.add(path);
                        break;
                    }
                }
            }
        }
        return result;
    }

    /**
     * Opens the specified file element as stream.
     * Please do not forget to close the stream.
     * @param name the file name
     * @return the file stream
     */
    InputStream open(String name);

    /**
     * Opens the specified file element as stream.
     * Please do not forget to close the stream.
     * @param path the file path. relative to the current store.
     * @return the file stream
     */
    InputStream open(Path path);
}
