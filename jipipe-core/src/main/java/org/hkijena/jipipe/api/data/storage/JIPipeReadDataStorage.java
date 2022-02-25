package org.hkijena.jipipe.api.data.storage;

/**
 * {@link JIPipeDataStorage} that is used for reading files into a {@link org.hkijena.jipipe.api.data.JIPipeData} instance
 */
public interface JIPipeReadDataStorage extends JIPipeDataStorage {
    /**
     * Returns a new storage that resolves to a path inside this storage
     * @param name the path name
     * @return the sub-storage
     */
    JIPipeReadDataStorage resolve(String name);
}
