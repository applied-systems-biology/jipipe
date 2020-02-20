package org.hkijena.acaq5.api.data;

import org.hkijena.acaq5.api.ACAQDocumentation;

import java.nio.file.Path;

public interface ACAQData {

    /**
     * Saves the data to a folder
     * @param storageFilePath A folder that already exists
     * @param name A name reference that can be used to generate filename(s)
     */
    void saveTo(Path storageFilePath, String name);

    /**
     * Returns the name of a data type
     * @param klass
     * @return
     */
    static String getNameOf(Class<? extends ACAQData> klass) {
        ACAQDocumentation[] annotations = klass.getAnnotationsByType(ACAQDocumentation.class);
        if(annotations.length > 0) {
            return annotations[0].name();
        }
        else {
            return klass.getSimpleName();
        }
    }
}
