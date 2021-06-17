/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.utils;

import org.apache.commons.lang3.SystemUtils;
import org.hkijena.jipipe.api.JIPipeProgressInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utilities for handling paths
 */
public class PathUtils {
    private PathUtils() {

    }

    public static void copyOrLink(Path source, Path target, JIPipeProgressInfo progressInfo) {
        if (SystemUtils.IS_OS_WINDOWS) {
            // Copy file
            progressInfo.log("Copy " + source + " to " + target);
            try {
                Files.copy(source, target);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            // Create symlink
            progressInfo.log("Link " + source + " to " + target);
            try {
                Files.createSymbolicLink(target, source);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Finds a file in the specified folder with given extension
     *
     * @param folder     the path
     * @param extensions Should contain the dot
     * @return null if no file was found
     */
    public static Path findFileByExtensionIn(Path folder, String... extensions) {
        try {
            return Files.list(folder).filter(p -> Files.isRegularFile(p) && Arrays.stream(extensions).anyMatch(e -> p.toString().endsWith(e))).findFirst().orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Finds a file in the specified folder with given extension
     *
     * @param folder     the path
     * @param extensions Should contain the dot
     * @return null if no file was found
     */
    public static List<Path> findFilesByExtensionIn(Path folder, String... extensions) {
        try {
            return Files.list(folder).filter(p -> Files.isRegularFile(p) && (extensions.length == 0 || Arrays.stream(extensions).anyMatch(e -> p.toString().endsWith(e)))).collect(Collectors.toList());
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    /**
     * Converts UNIX paths to Windows and Windows paths to UNIX
     *
     * @param paths the paths. This list must be modifiable
     */
    public static void normalizeList(List<Path> paths) {
        for (int i = 0; i < paths.size(); i++) {
            try {
                if (SystemUtils.IS_OS_WINDOWS) {
                    paths.set(i, Paths.get(StringUtils.nullToEmpty(paths.get(i)).replace('/', '\\')));
                } else {
                    paths.set(i, Paths.get(StringUtils.nullToEmpty(paths.get(i)).replace('\\', '/')));
                }
            } catch (Exception e) {
                paths.set(i, Paths.get(""));
            }
        }
    }

    public static Path normalize(Path path) {
        try {
            if (SystemUtils.IS_OS_WINDOWS) {
                return Paths.get(StringUtils.nullToEmpty(path).replace('/', '\\'));
            } else {
                return Paths.get(StringUtils.nullToEmpty(path).replace('\\', '/'));
            }
        } catch (Exception e) {
            return Paths.get("");
        }
    }

    /**
     * Returns the first path that exists
     *
     * @param paths paths
     * @return first path that exists or null
     */
    public static Path findAnyOf(Path... paths) {
        for (Path path : paths) {
            if (Files.exists(path)) {
                return path;
            }
        }
        return null;
    }
}
