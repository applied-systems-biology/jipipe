package org.hkijena.acaq5.api.data;

import org.hkijena.acaq5.api.ACAQDocumentation;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Base class for any ACAQ data wrapper class
 * Any custom data should have following constructors:
 * 1. A constructor that takes the wrapped data type
 * 2. A constructor that takes a path to a folder to load the data from
 */
public abstract class ACAQData {

    /**
     * Constructs data from a folder that contains files
     * @param storageFilePath
     */
    public ACAQData(Path storageFilePath) throws IOException {
    }

    public ACAQData() {

    }

    /**
     * Saves the data to a folder
     * @param storageFilePath A folder that already exists
     * @param name A name reference that can be used to generate filename(s)
     */
    public abstract void saveTo(Path storageFilePath, String name);

    /**
     * Returns the name of a data type
     * @param klass
     * @return
     */
    public static String getNameOf(Class<? extends ACAQData> klass) {
        ACAQDocumentation[] annotations = klass.getAnnotationsByType(ACAQDocumentation.class);
        if(annotations.length > 0) {
            return annotations[0].name();
        }
        else {
            return klass.getSimpleName();
        }
    }
}
