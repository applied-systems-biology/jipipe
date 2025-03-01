package org.hkijena.jipipe.desktop.commons.components.filechoosernext;

import org.apache.commons.lang3.SystemUtils;

import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Provides common directories for a custom file chooser.
 * Excludes "Documents" due to localization concerns.
 * Compatible with Java 8.
 */
public class CommonDirectoriesProvider {

    /**
     * Gets the static directories that should always be present.
     * These are system-dependent but do not change during runtime.
     */
    public List<CommonDirectory> getStaticDirectories() {
        List<CommonDirectory> staticDirs = new ArrayList<>();

        Path home = Paths.get(System.getProperty("user.home"));
        staticDirs.add(new CommonDirectory(home, CommonDirectory.DirectoryType.HOME));

        if (SystemUtils.IS_OS_WINDOWS) {
            staticDirs.add(new CommonDirectory(Paths.get("C:\\"), CommonDirectory.DirectoryType.DRIVE));
        } else if (SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_LINUX) {
            staticDirs.add(new CommonDirectory(Paths.get("/"), CommonDirectory.DirectoryType.ROOT));
        }

        return staticDirs;
    }

    /**
     * Gets the refreshable directories (like drives or mounted volumes).
     * These can change during runtime.
     */
    public List<CommonDirectory> getRefreshableDirectories() {
        if (SystemUtils.IS_OS_WINDOWS) {
            return getWindowsDrives();
        } else {
            return getUnixMountPoints();
        }
    }

    /**
     * Detects all drives on Windows.
     */
    private List<CommonDirectory> getWindowsDrives() {
        List<CommonDirectory> drives = new ArrayList<>();
        for (Path root : FileSystems.getDefault().getRootDirectories()) {
            drives.add(new CommonDirectory(root, CommonDirectory.DirectoryType.DRIVE));
        }
        return drives;
    }

    /**
     * Detects all mount points on Unix-like systems (Linux/macOS).
     * This uses /media, /mnt, and possibly /Volumes (on macOS).
     */
    private List<CommonDirectory> getUnixMountPoints() {
        List<CommonDirectory> mounts = new ArrayList<>();

        String userName = System.getProperty("user.name");

        if(userName != null) {
            mounts.addAll(scanMountDirectory(Paths.get("/media/" + userName)));
        }

        mounts.addAll(scanMountDirectory(Paths.get("/mnt")));

        if (SystemUtils.IS_OS_MAC) {
            mounts.addAll(scanMountDirectory(Paths.get("/Volumes")));
        }

        return mounts;
    }

    /**
     * Scans a directory (like /media or /mnt) for non-empty subdirectories.
     */
    private List<CommonDirectory> scanMountDirectory(Path parentDir) {
        if (!Files.isDirectory(parentDir)) {
            return Collections.emptyList();
        }

        List<CommonDirectory> result = new ArrayList<>();

        DirectoryStream<Path> stream = null;
        try {
            stream = Files.newDirectoryStream(parentDir);
            for (Path entry : stream) {
                if (Files.isDirectory(entry) && containsAtLeastOneFile(entry)) {
                    result.add(new CommonDirectory(entry, CommonDirectory.DirectoryType.DRIVE));
                }
            }
        } catch (IOException e) {
            // Skip unreadable directories
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ignored) {}
            }
        }

        return result;
    }

    /**
     * Checks if the given directory contains at least one file (or directory).
     */
    private boolean containsAtLeastOneFile(Path directory) {
        DirectoryStream<Path> stream = null;
        try {
            stream = Files.newDirectoryStream(directory);
            return stream.iterator().hasNext();
        } catch (IOException e) {
            return false;
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ignored) {}
            }
        }
    }
}

