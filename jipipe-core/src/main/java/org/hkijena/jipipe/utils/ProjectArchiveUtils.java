package org.hkijena.jipipe.utils;

import org.apache.commons.lang3.SystemUtils;
import org.hkijena.jipipe.api.JIPipeProgressInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

public final class ProjectArchiveUtils {
    private ProjectArchiveUtils() {
    }

    /**
     * Compute a shortest relative path mapping for a set of absolute, normalized Paths.
     * - If all inputs share a single filesystem root, maps are relative to their longest common ancestor.
     * - If inputs span multiple roots (e.g., multiple Windows drives), each group is relative to its own
     * longest common ancestor and gets a stable prefix (e.g., "drive_c/", "unc_server_share/").
     * The returned map values are GUARANTEED to be relative (no root).
     */
    public static Map<Path, Path> shortestRelativeMapping(Set<Path> absoluteNormalizedInputs) {
        Objects.requireNonNull(absoluteNormalizedInputs, "inputs");
        if (absoluteNormalizedInputs.isEmpty()) {
            return Map.of();
        }

        // Normalize defensively
        Set<Path> inputs = absoluteNormalizedInputs.stream()
                .map(p -> p.toAbsolutePath().normalize())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // Group by filesystem root key (Windows: drive/UNC; POSIX: single group)
        Map<String, List<Path>> byRoot = new LinkedHashMap<>();
        for (Path p : inputs) {
            if (p.getRoot() == null) {
                throw new IllegalArgumentException("Path must be absolute: " + p);
            }
            String key = rootKey(p);
            byRoot.computeIfAbsent(key, k -> new ArrayList<>()).add(p);
        }

        boolean multipleRoots = byRoot.size() > 1;
        Map<Path, Path> result = new LinkedHashMap<>();

        for (Map.Entry<String, List<Path>> e : byRoot.entrySet()) {
            String key = e.getKey();
            List<Path> group = e.getValue();

            Path base = longestCommonAncestor(group);
            Path prefix = multipleRoots ? groupLabelForArchive(key) : Paths.get(""); // always RELATIVE

            for (Path p : group) {
                Path rel = relativeFromBase(base, p); // <-- always relative
                Path archiveRel = prefix.getNameCount() == 0 ? rel : prefix.resolve(rel);
                // Double-check (paranoia): ensure relative
                if (archiveRel.isAbsolute()) {
                    archiveRel = stripRoot(archiveRel);
                }
                result.put(p, archiveRel);
            }
        }
        return result;
    }

    // ---- core: robust relative computation without Path.relativize ----

    /**
     * Returns a path p expressed relative to base. Result is guaranteed relative (no root).
     */
    private static Path relativeFromBase(Path base, Path p) {
        // Same root is required; handled by grouping.
        int baseNames = base.getNameCount();
        int pNames = p.getNameCount();

        // If base == p, return last name (avoid ".")
        if (base.equals(p)) {
            Path name = p.getFileName();
            return (name != null) ? name : Paths.get("root");
        }

        // Count equal leading name elements
        int i = 0;
        while (i < baseNames && i < pNames && base.getName(i).equals(p.getName(i))) {
            i++;
        }

        // Number of ".." needed to go up from base to the divergence point
        int ups = baseNames - i;
        Path rel = Paths.get("");
        for (int u = 0; u < ups; u++) {
            rel = rel.resolve("..");
        }

        // Then append the remaining names from p
        for (int j = i; j < pNames; j++) {
            rel = rel.resolve(p.getName(j));
        }

        // If base is root "/" (baseNames==0), the loop above yields only the names of p (relative)
        // If p is a direct child of base, ups==0 and we just take tail of p (relative)
        return rel.normalize();
    }

    private static Path stripRoot(Path absolute) {
        // Remove root, keep only name elements
        Path rel = Paths.get("");
        for (int i = 0; i < absolute.getNameCount(); i++) {
            rel = rel.resolve(absolute.getName(i));
        }
        return rel;
    }

    // ---- helpers (unchanged) ----

    private static String rootKey(Path p) {
        if (SystemUtils.IS_OS_WINDOWS) {
            String root = String.valueOf(p.getRoot()); // e.g., "C:\", "\\server\share\"
            String normalized = root.replace('\\', '/').toLowerCase(Locale.ROOT);

            if (normalized.matches("^[a-z]:/+$")) {
                char drive = Character.toLowerCase(normalized.charAt(0));
                return "drive_" + drive;
            }
            if (normalized.startsWith("//")) {
                String rest = normalized.substring(2);
                String[] parts = rest.split("/+");
                if (parts.length >= 2) {
                    return "unc_" + sanitize(parts[0]) + "_" + sanitize(parts[1]);
                }
                return "unc_unknown";
            }
            return "win_root_" + normalized.replaceAll("[^a-z0-9]+", "_");
        } else {
            return "posix_root";
        }
    }

    private static Path groupLabelForArchive(String rootKey) {
        return Paths.get(rootKey); // relative prefix
    }

    private static String sanitize(String s) {
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
    }

    private static Path longestCommonAncestor(List<Path> paths) {
        if (paths.isEmpty()) {
            throw new IllegalArgumentException("paths is empty");
        }
        Path candidate = paths.getFirst();
        if (!isDirectoryLike(candidate)) {
            candidate = candidate.getParent();
        }

        while (candidate != null) {
            final Path base = candidate;
            boolean ok = paths.stream().allMatch(p -> isPrefix(base, p));
            if (ok) {
                return base;
            }
            candidate = candidate.getParent();
        }
        return paths.getFirst().getRoot();
    }

    private static boolean isDirectoryLike(Path p) {
        return p.getNameCount() > 0 || p.getParent() != null;
    }

    private static boolean isPrefix(Path base, Path p) {
        if (base.equals(p)) {
            return true;
        }
        if (base.getNameCount() == 0) {
            return Objects.equals(base.getRoot(), p.getRoot());
        }
        if (!Objects.equals(base.getRoot(), p.getRoot())) {
            return false;
        }
        if (p.getNameCount() < base.getNameCount()) {
            return false;
        }
        for (int i = 0; i < base.getNameCount(); i++) {
            if (!base.getName(i).equals(p.getName(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Copies the given mapping into {@code targetBase}.
     *
     * @param mapping      Map of absolute {@code source} -> {@code relativeTarget} (MUST be relative).
     * @param targetBase   Destination directory (created if missing).
     * @param overwrite    If true, existing files are replaced; otherwise they are kept (skipped).
     * @param progressInfo Progress/cancel reporting. Uses {@code log(String)} and checks {@code isCancelled().get()}.
     */
    public static void materializeMapping(Map<Path, Path> mapping,
                                          Path targetBase,
                                          boolean overwrite,
                                          JIPipeProgressInfo progressInfo) throws IOException {
        Objects.requireNonNull(mapping);
        Objects.requireNonNull(targetBase);
        Objects.requireNonNull(progressInfo);

        // Ensure target base exists
        Files.createDirectories(targetBase);

        // Split into dirs/files, normalize, and compute final destinations
        List<Map.Entry<Path, Path>> dirs = new ArrayList<>();
        List<Map.Entry<Path, Path>> files = new ArrayList<>();

        for (Map.Entry<Path, Path> e : mapping.entrySet()) {
            checkCancelled(progressInfo);

            Path src = requireAbsolute(e.getKey());
            Path rel = requireRelative(stripRootIfAccidental(e.getValue()).normalize());
            Path dst = normalizeUnderBase(targetBase, rel);

            // We check directory status while FOLLOWING links (for intent: follow symlinks)
            boolean isDir = Files.isDirectory(src, LinkOption.NOFOLLOW_LINKS)
                    || (Files.isSymbolicLink(src) && isSymlinkToDirectory(src));

            if (isDir) {
                dirs.add(Map.entry(src, dst));
            } else {
                files.add(Map.entry(src, dst));
            }
        }

        // Deterministic order is nice for logs
        Comparator<Map.Entry<Path, Path>> byDst = Comparator.comparing(e -> e.getValue().toString());
        dirs.sort(byDst);
        files.sort(byDst);

        // Track directories we've fully copied to skip nested duplicates
        final List<Path> coveredDirs = new ArrayList<>();

        // 1) Copy directories (recursive, FOLLOW_LINKS)
        for (Map.Entry<Path, Path> e : dirs) {
            checkCancelled(progressInfo);

            Path src = e.getKey();
            Path dst = e.getValue();

            if (isUnderAny(dst, coveredDirs)) {
                progressInfo.log("Skipping " + src + " -> " + dst + " (already covered)");
                continue;
            }

            progressInfo.log("Copying directory: " + src + " -> " + dst);

            // Avoid cycles when following links: track visited file-keys
            Set<Object> visitedKeys = new HashSet<>();

            Files.walkFileTree(src,
                    EnumSet.of(FileVisitOption.FOLLOW_LINKS),
                    Integer.MAX_VALUE,
                    new CopyTreeVisitor(src, dst, overwrite, progressInfo, visitedKeys));

            coveredDirs.add(dst);
        }

        // 2) Copy files (skip if their parent was already copied)
        for (Map.Entry<Path, Path> e : files) {
            checkCancelled(progressInfo);

            Path src = e.getKey();
            Path dst = e.getValue();

            if (isUnderAny(dst, coveredDirs)) {
                progressInfo.log("Skipping " + src + " -> " + dst + " (already covered)");
                continue;
            }

            Files.createDirectories(dst.getParent());
            if (Files.exists(dst) && !overwrite) {
                progressInfo.log("Skip existing file (keep): " + dst);
                continue;
            }

            progressInfo.log("Copying file: " + src + " -> " + dst);
            copyFileData(src, dst, overwrite);
        }
    }

    // ----- helpers -----

    private static void checkCancelled(JIPipeProgressInfo progressInfo) throws IOException {
        if (progressInfo.isCancelled()) {
            throw new IOException("Operation cancelled by user");
        }
    }

    private static Path requireAbsolute(Path p) {
        if (p == null || !p.isAbsolute()) {
            throw new IllegalArgumentException("Source path must be absolute: " + p);
        }
        return p.normalize();
    }

    private static Path requireRelative(Path p) {
        if (p == null || p.isAbsolute()) {
            throw new IllegalArgumentException("Target path must be relative: " + p);
        }
        return p;
    }

    /**
     * If someone accidentally fed an absolute target, strip its root to make it relative.
     */
    private static Path stripRootIfAccidental(Path p) {
        if (p == null || !p.isAbsolute()) {
            return p;
        }
        Path rel = Path.of("");
        for (int i = 0; i < p.getNameCount(); i++) {
            rel = rel.resolve(p.getName(i));
        }
        return rel;
    }

    /**
     * Resolve and normalize; ensure final path stays under base to avoid traversal.
     */
    private static Path normalizeUnderBase(Path base, Path relative) {
        Path dst = base.resolve(relative).normalize();
        if (!dst.startsWith(base.normalize())) {
            throw new IllegalArgumentException("Target escapes base directory: " + dst);
        }
        return dst;
    }

    private static boolean isUnderAny(Path candidate, List<Path> parents) {
        for (Path parent : parents) {
            if (candidate.startsWith(parent)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the symlink points to a directory (best-effort).
     */
    private static boolean isSymlinkToDirectory(Path link) {
        try {
            Path target = Files.readSymbolicLink(link);
            Path resolved = link.getParent() == null ? target : link.getParent().resolve(target);
            return Files.isDirectory(resolved);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Copies file DATA from src to dst, following symlinks (i.e., writes the target bytes, not a link).
     * Overwrites if requested.
     */
    private static void copyFileData(Path src, Path dst, boolean overwrite) throws IOException {
        Files.createDirectories(dst.getParent());
        if (overwrite) {
            // If it's a link or file, delete first so we always produce a regular file
            try {
                Files.deleteIfExists(dst);
            } catch (IOException ignored) {
            }
        }
        // Stream copy (InputStream follows symlinks by default)
        try (InputStream in = Files.newInputStream(src);
             OutputStream out = Files.newOutputStream(dst, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            in.transferTo(out);
        }
        // Best-effort: carry over last-modified time from the dereferenced source
        try {
            Files.setLastModifiedTime(dst, Files.getLastModifiedTime(src));
        } catch (IOException ignored) {
        }
    }

    // ----- FileVisitor used for directory copies (FOLLOWS LINKS, but guards cycles) -----

    private static final class CopyTreeVisitor implements FileVisitor<Path> {
        private final Path srcRoot;
        private final Path dstRoot;
        private final boolean overwrite;
        private final JIPipeProgressInfo progressInfo;
        private final Set<Object> visitedKeys;

        CopyTreeVisitor(Path srcRoot,
                        Path dstRoot,
                        boolean overwrite,
                        JIPipeProgressInfo progressInfo,
                        Set<Object> visitedKeys) {
            this.srcRoot = srcRoot;
            this.dstRoot = dstRoot;
            this.overwrite = overwrite;
            this.progressInfo = progressInfo;
            this.visitedKeys = visitedKeys;
        }

        private Path map(Path src) {
            Path rel = srcRoot.relativize(src); // relative within this subtree
            return dstRoot.resolve(rel);
        }

        private void recordVisited(Path p) throws IOException {
            checkCancelled(progressInfo);
            BasicFileAttributes attrs = Files.readAttributes(p, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            Object key = attrs.fileKey();
            if (key != null && !visitedKeys.add(key)) {
                // Cycle detected (hard link or symlink loop); skip by throwing and catching as continue
                throw new FileSystemLoopException("Cycle detected at: " + p);
            }
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            checkCancelled(progressInfo);
            try {
                recordVisited(dir);
            } catch (FileSystemLoopException e) {
                return FileVisitResult.SKIP_SUBTREE;
            }

            Path dst = map(dir);
            Files.createDirectories(dst);
            progressInfo.log("Creating directory: " + dst);

            // Best-effort preserve mtime
            try {
                Files.setLastModifiedTime(dst, Files.getLastModifiedTime(dir));
            } catch (IOException ignored) {
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            checkCancelled(progressInfo);
            try {
                recordVisited(file);
            } catch (FileSystemLoopException e) {
                return FileVisitResult.CONTINUE;
            }

            Path dst = map(file);
            if (Files.exists(dst) && !overwrite) {
                progressInfo.log("Skip existing file (keep): " + dst);
                return FileVisitResult.CONTINUE;
            }
            progressInfo.log("Copying file: " + file + " -> " + dst);
            copyFileData(file, dst, overwrite); // copies data (follows links)
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            // Propagate the error; caller can catch and log
            throw exc;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            if (exc != null) {
                throw exc;
            }
            checkCancelled(progressInfo);
            // Best-effort: restore mtime after contents copied
            Path dst = map(dir);
            try {
                Files.setLastModifiedTime(dst, Files.getLastModifiedTime(dir));
            } catch (IOException ignored) {
            }
            return FileVisitResult.CONTINUE;
        }
    }
}
