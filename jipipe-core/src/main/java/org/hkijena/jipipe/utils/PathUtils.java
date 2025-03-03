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

package org.hkijena.jipipe.utils;

import ij.IJ;
import ij.Prefs;
import org.apache.commons.lang3.SystemUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utilities for handling paths
 */
public class PathUtils {

    private static final String RANDOM_TMP_CHARACTERS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int RANDOM_TMP_LENGTH = 7;

    private PathUtils() {

    }

    /**
     * Makes a file/directory executable
     * Does nothing if the OS is Windows
     *
     * @param path the path
     */
    public static void makeUnixExecutable(Path path) {
        if (SystemUtils.IS_OS_WINDOWS)
            return;
        try {
            Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(path);
            permissions.add(PosixFilePermission.OWNER_EXECUTE);
            permissions.add(PosixFilePermission.GROUP_EXECUTE);
            permissions.add(PosixFilePermission.OTHERS_EXECUTE);
            Files.setPosixFilePermissions(path, permissions);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static Path resolveAndMakeSubDirectory(Path directory, String name) {
        return resolveAndMakeSubDirectory(directory, Paths.get(name));
    }

    public static Path resolveAndMakeSubDirectory(Path directory, Path name) {
        Path result = directory.resolve(name);
        if (!Files.exists(result)) {
            try {
                Files.createDirectories(result);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    public static void copyDirectory(Path sourcePath, Path targetPath, JIPipeProgressInfo progressInfo) {
        // Copy the directory with progress logging
        try {
            Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    // Create the corresponding destination directory
                    Path relativePath = sourcePath.relativize(dir);
                    Path destinationDir = targetPath.resolve(relativePath);
                    Files.createDirectories(destinationDir);

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    // Copy the file with attributes
                    Path relativePath = sourcePath.relativize(file);
                    Path destinationFile = targetPath.resolve(relativePath);

                    progressInfo.log(file + " --> " + destinationFile);
                    Files.copy(file, destinationFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {

                    progressInfo.log("ERROR: Unable to process " + file);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void copyDirectory(Path sourcePath, Path targetPath, Predicate<Path> directoryFilter, JIPipeProgressInfo progressInfo) {
        // Copy the directory with progress logging
        try {
            Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (directoryFilter.test(dir)) {

                        // Create the corresponding destination directory
                        Path relativePath = sourcePath.relativize(dir);
                        Path destinationDir = targetPath.resolve(relativePath);
                        Files.createDirectories(destinationDir);

                        return FileVisitResult.CONTINUE;
                    } else {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    // Copy the file with attributes
                    Path relativePath = sourcePath.relativize(file);
                    Path destinationFile = targetPath.resolve(relativePath);

                    progressInfo.log(file + " --> " + destinationFile);
                    Files.copy(file, destinationFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {

                    progressInfo.log("ERROR: Unable to process " + file);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void deleteDirectoryRecursively(Path path, JIPipeProgressInfo progressInfo) {
        FileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                progressInfo.log("Delete: " + file.toString());
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                progressInfo.log("Delete: " + file.toString());
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                }
                progressInfo.log("Delete: " + dir.toString());
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        };
        try {
            Files.walkFileTree(path, visitor);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Path> listDirectory(Path directory) throws IOException {
        List<Path> result = new ArrayList<>();
        try(Stream<Path> stream = Files.list(directory)) {
            stream.forEach(result::add);
        }
        return result;
    }

    /**
     * Computes the SHA1 of a file
     *
     * @param file the file
     * @return the SHA1
     * @throws IOException exception
     */
    public static String computeFileSHA1(File file) throws IOException {
        String sha1 = null;
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e1) {
            throw new IOException("Impossible to get SHA-1 digester", e1);
        }
        try (InputStream input = new FileInputStream(file);
             DigestInputStream digestStream = new DigestInputStream(input, digest)) {
            while (digestStream.read() != -1) {
                // read file stream without buffer
            }
            MessageDigest msgDigest = digestStream.getMessageDigest();
            sha1 = new HexBinaryAdapter().marshal(msgDigest.digest());
        }
        return sha1;
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
        try (Stream<Path> stream = Files.list(folder)) {
            return stream.filter(p -> Files.isRegularFile(p) && Arrays.stream(extensions).anyMatch(e -> p.toString().endsWith(e))).findFirst().orElse(null);
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
    public static Path findFileByExtensionRecursivelyIn(Path folder, String... extensions) {
        try (Stream<Path> stream = Files.walk(folder)) {
            return stream.filter(p -> Files.isRegularFile(p) && Arrays.stream(extensions).anyMatch(e -> p.toString().endsWith(e))).findFirst().orElse(null);
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
        try (Stream<Path> stream = Files.list(folder)) {
            return stream.filter(p -> Files.isRegularFile(p) && (extensions.length == 0 || Arrays.stream(extensions).anyMatch(e -> p.toString().endsWith(e)))).collect(Collectors.toList());
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    /**
     * Finds a file in the specified folder with given extension
     *
     * @param folder the path
     * @return null if no file was found
     */
    public static List<Path> listSubDirectories(Path folder) {
        try (Stream<Path> stream = Files.list(folder)) {
            return stream.filter(Files::isDirectory).collect(Collectors.toList());
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

    /**
     * Gets the legacy user directory.
     * This will be automatically moved to the correct user directory if needed
     *
     * @return the legacy user directory
     */
    public static Path getLegacyJIPipeUserDir() {
        Path result = PathUtils.getImageJDir().resolve("jipipe");
        try {
            Files.createDirectories(result);
        } catch (IOException e) {
            IJ.handleException(e);
        }
        return result;
    }

    /**
     * Returns the base directory that contains all profiles
     *
     * @return the base directory
     */
    public static Path getJIPipeUserDirBase() {
        if (System.getProperties().containsKey("JIPIPE_OVERRIDE_USER_DIR_BASE")) {
            return Paths.get(System.getProperties().getProperty("JIPIPE_OVERRIDE_USER_DIR_BASE"));
        }
        if (JIPipe.OVERRIDE_USER_DIR_BASE != null) {
            return JIPipe.OVERRIDE_USER_DIR_BASE;
        }
        if (SystemUtils.IS_OS_WINDOWS) {
            return Paths.get(System.getenv("APPDATA")).resolve("JIPipe")
                    .resolve("profiles");
        } else if (SystemUtils.IS_OS_LINUX) {
            if (System.getProperties().containsKey("XDG_DATA_HOME") && !StringUtils.isNullOrEmpty(System.getProperty("XDG_DATA_HOME"))) {
                return Paths.get(System.getProperty("XDG_DATA_HOME"))
                        .resolve("JIPipe")
                        .resolve("profiles");
            } else {
                return Paths.get(System.getProperty("user.home")).resolve(".local")
                        .resolve("share").resolve("JIPipe")
                        .resolve("profiles");
            }
        } else if (SystemUtils.IS_OS_MAC_OSX) {
            return Paths.get(System.getProperty("user.home")).resolve("Library").resolve("Application Support")
                    .resolve("JIPipe").resolve("profiles");
        } else {
            return getLegacyJIPipeUserDir().resolve("profiles");
        }
    }

    /**
     * Returns the JIPipe user directory
     *
     * @return the JIPipe user directory
     */
    public static Path getJIPipeUserDir() {
        Path result = getJIPipeUserDirBase().resolve(VersionUtils.getJIPipeVersion());
        try {
            Files.createDirectories(result);
        } catch (IOException e) {
            IJ.handleException(e);
        }
        return result;
    }

    /**
     * Gets the ImageJ directory
     *
     * @return the ImageJ directory
     */
    public static Path getImageJDir() {
        Path imageJDir = Paths.get(Prefs.getImageJDir());
        if (!imageJDir.isAbsolute())
            imageJDir = imageJDir.toAbsolutePath();
        if (!Files.isDirectory(imageJDir)) {
            try {
                Files.createDirectories(imageJDir);
            } catch (IOException e) {
                IJ.handleException(e);
            }
        }
        return imageJDir;
    }

    public static Path absoluteToJIPipeUserDirRelative(Path path) {
        if (!path.isAbsolute())
            return path;
        return getJIPipeUserDir().relativize(path);
    }

    /**
     * Converts relative paths to absolute paths, relative to the ImageJ directory
     * Absolute paths are left unchanged
     *
     * @param path the path
     * @return absolute paths
     */
    public static Path relativeJIPipeUserDirToAbsolute(Path path) {
        if (path.isAbsolute())
            return path;
        return getJIPipeUserDir().resolve(path);
    }

    /**
     * Ensures that the path exists and is empty
     *
     * @param parent the parent component for the dialog box
     * @param path   the path
     * @return if the operation was successful
     */
    public static boolean ensureEmptyFolder(Component parent, Path path) {
        if (Files.isRegularFile(path)) {
            return false;
        }
        if (Files.isDirectory(path)) {
            try {
                if (Files.list(path).findAny().isPresent()) {
                    int response = JOptionPane.showConfirmDialog(parent,
                            "The selected directory '" + path + "' is not empty! Remove contents and continue?",
                            "Empty directory expected",
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.ERROR_MESSAGE);
                    if (response == JOptionPane.YES_OPTION) {
                        deleteDirectoryRecursively(path, new JIPipeProgressInfo());
                        Files.createDirectories(path);
                        return true;
                    } else if (response == JOptionPane.NO_OPTION) {
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    return true;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }
    }

    /**
     * Ensures that the file name of the path has one of the provided extensions
     *
     * @param path                  the path
     * @param extension             the extension (should include the dot)
     * @param alternativeExtensions alternative extensions that are also valid (should include the dot)
     * @return path with a filename that has the provided extension
     */
    public static Path ensureExtension(Path path, String extension, String... alternativeExtensions) {
        if (path.getFileName().toString().endsWith(extension))
            return path;
        for (String ext : alternativeExtensions) {
            if (path.getFileName().toString().endsWith(ext))
                return path;
        }
        if (path.getParent() != null) {
            return path.getParent().resolve(path.getFileName() + extension);
        } else {
            return Paths.get(path + extension);
        }
    }

    /**
     * Ensures that the parent directories of the path exist.
     * If the parent directory is null, nothing will be done
     *
     * @param path the path
     */
    public static void ensureParentDirectoriesExist(Path path) {
        if (path.getParent() != null) {
            try {
                Files.createDirectories(path.getParent());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Returns the first path if it exists or the alternative path if it doesn't
     *
     * @param firstChoice the first choice. can be null.
     * @param alternative the alternative
     * @return firstChoice if it is not null and if it does exist, alternative instead
     */
    public static Path orElse(Path firstChoice, Path alternative) {
        if (firstChoice != null && Files.exists(firstChoice)) {
            return firstChoice;
        } else {
            return alternative;
        }
    }

    /**
     * Marks all files in that directory as Unix executable
     *
     * @param dir          the directory
     * @param progressInfo the progress
     */
    public static void makeAllUnixExecutable(Path dir, JIPipeProgressInfo progressInfo) {
        progressInfo.log("Postprocess: Marking all files in " + dir + " as executable");
        for (Path path : PathUtils.findFilesByExtensionIn(dir)) {
            if (Files.isRegularFile(path)) {
                progressInfo.log(" - chmod +x " + path);
                PathUtils.makeUnixExecutable(path);
            }
        }
    }

    /**
     * Create a unique subdirectory rooted at system-wide temporary directory
     *
     * @param prefix the prefix (can be null, but not recommended)
     * @return the temporary directory
     */
    public static Path createGlobalTempDirectory(String prefix) {
        Path rootDir = Paths.get(System.getProperty("java.io.tmpdir"));
        return createTempSubDirectory(rootDir, prefix);
    }

    /**
     * Create a unique subdirectory rooted at the root path
     *
     * @param root the root directory
     * @return the temporary directory
     */
    public static Path createTempSubDirectory(Path root) {
        return createTempSubDirectory(root, null);
    }

    /**
     * Create a unique subdirectory rooted at the root path
     *
     * @param root   the root directory
     * @param prefix the prefix (can be null)
     * @return the temporary directory
     */
    public static Path createTempSubDirectory(Path root, String prefix) {
        try {
            Files.createDirectories(root);
            while (true) {
                Path path = root.resolve(StringUtils.nullToEmpty(prefix) + StringUtils.generateRandomString(RANDOM_TMP_CHARACTERS, RANDOM_TMP_LENGTH));
                if (!Files.exists(path)) {
                    Files.createDirectories(path);
                    return path;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a path to a file that does not exist yet
     * The file will be located in the system-wide temporary path
     *
     * @param prefix the file name prefix (can be null)
     * @param suffix the file name suffix (can be null)
     * @return the path
     */
    public static Path createGlobalTempFilePath(String prefix, String suffix) {
        Path rootDir = Paths.get(System.getProperty("java.io.tmpdir"));
        return createSubTempFilePath(rootDir, prefix, suffix);
    }

    /**
     * Creates a path to a file that does not exist yet
     *
     * @param root   the root directory
     * @param prefix the file name prefix (can be null)
     * @param suffix the file name suffix (can be null)
     * @return the path
     */
    public static Path createSubTempFilePath(Path root, String prefix, String suffix) {
        try {
            Files.createDirectories(root);
            while (true) {
                Path path = root.resolve(StringUtils.nullToEmpty(prefix) +
                        StringUtils.generateRandomString(RANDOM_TMP_CHARACTERS, RANDOM_TMP_LENGTH) + StringUtils.nullToEmpty(suffix));
                if (!Files.exists(path)) {
                    return path;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Same as Files.createDirectories, but throws a {@link RuntimeException}
     *
     * @param path the path
     * @return the path
     */
    public static Path createDirectories(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return path;
    }

    /**
     * Checks if a file exists and is an executable
     *
     * @param path the file
     * @return if it is an executable
     */
    public static boolean isExecutable(Path path) {
        return Files.exists(path) && Files.isRegularFile(path) && Files.isExecutable(path);
    }

    public static void deleteIfExists(Path path, JIPipeProgressInfo progressInfo) {
        if(Files.isRegularFile(path)) {
            try {
                progressInfo.log("Deleting " + path);
                Files.delete(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else if(Files.isDirectory(path)) {
            progressInfo.log("Deleting " + path);
            deleteDirectoryRecursively(path, progressInfo);
        }
    }

    public static String getPathNameSafe(Path path) {
        return StringUtils.orElse(path.getFileName(), "Root");
    }
}
