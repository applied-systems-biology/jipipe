package org.hkijena.jipipe.utils;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.hkijena.jipipe.api.JIPipeProgressInfo;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;

public class TarGzExtractor {

    private static final int BUFFER_SIZE = 64 * 1024;

    /**
     * Extracts a .tar.gz to targetDir.
     * - Regular files are written normally
     * - Hard links are materialized as full file copies (never hard links)
     * - Symlinks are created with the archived link text (no path rewriting);
     *   if symlinks aren’t supported, we fallback to copying the target when safe/available
     */
    public static void decompressTarGZ(Path tarGzFile, Path targetDir, JIPipeProgressInfo progressInfo) throws IOException {
        List<Map.Entry<String, Path>> pendingSymlinks = new ArrayList<>(); // (linkText, linkPathOnDisk)
        List<Map.Entry<Path, Path>> pendingHardlinks = new ArrayList<>();  // (existingTargetPathOnDisk, whereToCreateCopy)

        Files.createDirectories(targetDir);

        try (FileInputStream fis = new FileInputStream(tarGzFile.toFile());
             GzipCompressorInputStream gzipIn = new GzipCompressorInputStream(fis);
             TarArchiveInputStream tarIn = new TarArchiveInputStream(gzipIn)) {

            TarArchiveEntry entry;
            byte[] buffer = new byte[BUFFER_SIZE];

            while ((entry = tarIn.getNextTarEntry()) != null) {
                String rawName = entry.getName();
                if (rawName == null || rawName.isEmpty()) {
                    log(progressInfo, "Skipping nameless entry");
                    continue;
                }

                // --- Sanitize path ---
                Path entryRel = Paths.get(rawName).normalize();
                if (entryRel.isAbsolute() || entryRel.startsWith("..")) {
                    log(progressInfo, "Skipping unsafe entry: " + rawName);
                    continue;
                }
                Path outPath = targetDir.resolve(entryRel).normalize();
                if (!outPath.startsWith(targetDir.normalize())) {
                    log(progressInfo, "Skipping traversal entry: " + rawName);
                    continue;
                }

                log(progressInfo, "Entry " + rawName + " -> " + outPath);

                if (entry.isDirectory()) {
                    Files.createDirectories(outPath);
                    continue;
                }

                if (entry.isSymbolicLink()) {
                    // Preserve archived link text AS-IS
                    String linkText = entry.getLinkName();
                    if (linkText == null) linkText = "";
                    pendingSymlinks.add(new AbstractMap.SimpleEntry<>(linkText, outPath));
                    continue;
                }

                if (entry.isLink()) {
                    // HARD LINK -> COPY
                    String linkName = entry.getLinkName();
                    if (linkName == null || linkName.isEmpty()) {
                        log(progressInfo, "Skipping hardlink with empty target for " + rawName);
                        continue;
                    }
                    Path linkNameRel = Paths.get(linkName).normalize();
                    if (linkNameRel.isAbsolute() || linkNameRel.startsWith("..")) {
                        log(progressInfo, "Skipping unsafe hardlink target: " + linkName);
                        continue;
                    }
                    Path existingTarget = targetDir.resolve(linkNameRel).normalize();
                    pendingHardlinks.add(new AbstractMap.SimpleEntry<>(existingTarget, outPath));
                    continue;
                }

                if (entry.isFile()) {
                    Files.createDirectories(outPath.getParent());
                    try (OutputStream os = new BufferedOutputStream(
                            Files.newOutputStream(outPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE),
                            BUFFER_SIZE)) {
                        int read;
                        while ((read = tarIn.read(buffer)) != -1) {
                            os.write(buffer, 0, read);
                        }
                    }
                    // Preserve perms / mtime (best-effort)
                    applyPermissionsFromMode(outPath, entry.getMode(), progressInfo);
                    applyMTime(outPath, entry, progressInfo);
                    continue;
                }

                log(progressInfo, "Unsupported entry type: " + rawName + " (skipped)");
            }
        }

        // --- Materialize "hard links" as copies ---
        for (Map.Entry<Path, Path> e : pendingHardlinks) {
            Path existing = e.getKey();
            Path copyPath = e.getValue();
            Files.createDirectories(copyPath.getParent());
            if (!Files.exists(existing)) {
                throw new IOException("Hard link target missing (for copy): " + existing);
            }
            log(progressInfo, "Copying hardlink " + copyPath + " <- " + existing);
            Files.copy(existing, copyPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            // Try to mirror POSIX perms even if attributes didn’t carry over
            mirrorPosixPerms(existing, copyPath, progressInfo);
            mirrorMTime(existing, copyPath, progressInfo);
        }

        // --- Create symlinks (fallback to copy if symlinks aren’t allowed and target resolvable inside targetDir) ---
        for (Map.Entry<String, Path> e : pendingSymlinks) {
            String linkText = e.getKey();
            Path linkPath = e.getValue();
            Files.createDirectories(linkPath.getParent());

            log(progressInfo, "Symlinking " + linkPath + " -> " + linkText);
            try {
                Files.deleteIfExists(linkPath);
                Files.createSymbolicLink(linkPath, Paths.get(linkText)); // keep text as stored (relative or absolute)
            } catch (UnsupportedOperationException | FileSystemException ex) {
                // Fallback: attempt safe copy if the link resolves to a file inside targetDir
                log(progressInfo, "Symlink unsupported here; attempting safe-copy fallback: " + ex.getMessage());
                Path resolved = resolveSymlinkTargetSafely(linkPath, linkText, targetDir);
                if (resolved != null && Files.isRegularFile(resolved)) {
                    log(progressInfo, "Copying in place of symlink: " + linkPath + " <- " + resolved);
                    Files.copy(resolved, linkPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                    mirrorPosixPerms(resolved, linkPath, progressInfo);
                    mirrorMTime(resolved, linkPath, progressInfo);
                } else {
                    log(progressInfo, "Could not safely resolve symlink target inside extraction root; leaving as missing: " + linkText);
                }
            }
        }
    }

    // ----- helpers -----

    private static void log(JIPipeProgressInfo progressInfo, String msg) {
        if (progressInfo != null) progressInfo.log(msg);
        else System.out.println(msg);
    }

    private static void applyPermissionsFromMode(Path path, int mode, JIPipeProgressInfo progressInfo) {
        try {
            Set<PosixFilePermission> perms = EnumSet.noneOf(PosixFilePermission.class);
            if ((mode & 0400) != 0) perms.add(PosixFilePermission.OWNER_READ);
            if ((mode & 0200) != 0) perms.add(PosixFilePermission.OWNER_WRITE);
            if ((mode & 0100) != 0) perms.add(PosixFilePermission.OWNER_EXECUTE);
            if ((mode & 0040) != 0) perms.add(PosixFilePermission.GROUP_READ);
            if ((mode & 0020) != 0) perms.add(PosixFilePermission.GROUP_WRITE);
            if ((mode & 0010) != 0) perms.add(PosixFilePermission.GROUP_EXECUTE);
            if ((mode & 0004) != 0) perms.add(PosixFilePermission.OTHERS_READ);
            if ((mode & 0002) != 0) perms.add(PosixFilePermission.OTHERS_WRITE);
            if ((mode & 0001) != 0) perms.add(PosixFilePermission.OTHERS_EXECUTE);
            if (!perms.isEmpty()) {
                Files.setPosixFilePermissions(path, perms);
            }
        } catch (UnsupportedOperationException ignored) {
            // Non-POSIX FS; ignore
        } catch (IOException io) {
            log(progressInfo, "Warning: failed to set POSIX permissions on " + path + " (" + io.getMessage() + ")");
        }
    }

    private static void applyMTime(Path path, TarArchiveEntry entry, JIPipeProgressInfo progressInfo) {
        try {
            if (entry.getModTime() != null) {
                Files.setLastModifiedTime(path, FileTime.fromMillis(entry.getModTime().getTime()));
            }
        } catch (IOException io) {
            log(progressInfo, "Warning: failed to set mtime on " + path + " (" + io.getMessage() + ")");
        }
    }

    private static void mirrorPosixPerms(Path src, Path dst, JIPipeProgressInfo progressInfo) {
        try {
            Set<PosixFilePermission> perms = Files.getPosixFilePermissions(src);
            Files.setPosixFilePermissions(dst, perms);
        } catch (UnsupportedOperationException ignored) {
        } catch (IOException io) {
            log(progressInfo, "Warning: failed to mirror POSIX perms from " + src + " to " + dst + " (" + io.getMessage() + ")");
        }
    }

    private static void mirrorMTime(Path src, Path dst, JIPipeProgressInfo progressInfo) {
        try {
            FileTime t = Files.getLastModifiedTime(src);
            Files.setLastModifiedTime(dst, t);
        } catch (IOException io) {
            log(progressInfo, "Warning: failed to mirror mtime from " + src + " to " + dst + " (" + io.getMessage() + ")");
        }
    }

    /**
     * Resolve the symlink’s link text against its parent, but only accept targets that end up
     * inside targetDir. Returns null if unsafe or non-resolvable.
     */
    private static Path resolveSymlinkTargetSafely(Path linkPath, String linkText, Path targetDir) {
        try {
            Path candidate = Paths.get(linkText);
            if (!candidate.isAbsolute()) {
                candidate = linkPath.getParent().resolve(candidate);
            }
            candidate = candidate.normalize();
            Path rootNorm = targetDir.normalize();
            if (!candidate.startsWith(rootNorm)) {
                return null; // outside extraction root -> unsafe
            }
            return candidate;
        } catch (Exception ignored) {
            return null;
        }
    }
}
