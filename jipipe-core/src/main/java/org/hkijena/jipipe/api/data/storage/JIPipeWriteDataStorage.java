package org.hkijena.jipipe.api.data.storage;

public interface JIPipeWriteDataStorage extends JIPipeDataStorage {
    /**
     * Returns a new storage that resolves to a path inside this storage
     * @param name the path name
     * @return the sub-storage
     */
    JIPipeWriteDataStorage resolve(String name);
}
