package org.hkijena.acaq5.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PathUtils {
    private PathUtils() {

    }

    /**
     * Finds a file in the specified folder with given extension
     * @param folder
     * @param extension Should contain the dot
     * @return null if no file was found
     */
    public static Path findFileByExtensionIn(Path folder, String extension) {
        try {
            return Files.list(folder).filter(p -> Files.isRegularFile(p) && p.toString().endsWith(extension)).findFirst().orElse(null);
        } catch (IOException e) {
            return null;
        }
    }
}
