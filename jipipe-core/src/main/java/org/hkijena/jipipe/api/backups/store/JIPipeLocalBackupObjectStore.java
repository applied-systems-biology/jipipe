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

package org.hkijena.jipipe.api.backups.store;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.utils.FileLocker;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Local file-based implementation of {@link JIPipeBackupObjectStore}.
 * <p>
 * Layout (under the backup root):
 * <pre>
 * store/
 *   format.json     — {"format-version": 1}
 *   lock            — cross-process write lock file
 *   objects/
 *     hh/           — directory per first two hex characters of the hash
 *       &lt;hash&gt;      — the immutable object file
 * snapshots/        — snapshot manifests (referenced by GC)
 * files-cache/      — (reserved for later tasks)
 * </pre>
 * Objects are immutable once written; identical content always resolves to
 * the same object file, which is what dedup and cross-snapshot sharing rest on.
 * <p>
 * Writes: the content is streamed (hashing with a {@link DigestInputStream})
 * into a temp file {@code .tmp-<uuid>} inside the objects tree, then moved
 * with an atomic rename into {@code objects/<hh>/<hash>}. The temp file is on
 * the same file system as the final object, which is what makes the atomic
 * move possible; if the target already exists (dedup), the temp file is
 * simply deleted.
 * <p>
 * Locking: {@code put} and {@link #garbageCollect(JIPipeProgressInfo)} are
 * caller-holds-lock (the methods do NOT acquire the lock themselves — FileLock
 * re-entrancy is platform-dependent, so re-acquiring is avoided entirely).
 * {@code get}/{@code has}/{@code sizeOf}/{@code totalObjectBytes} are lock-free.
 */
public class JIPipeLocalBackupObjectStore implements JIPipeBackupObjectStore {
    private static final Logger log = LoggerFactory.getLogger(JIPipeLocalBackupObjectStore.class);
    private static final int FORMAT_VERSION = 1;
    private static final Pattern VALID_HASH = Pattern.compile("[0-9a-f]{64}");
    private static final HexFormat HEX = HexFormat.of();

    private final JIPipeProgressInfo progressInfo;
    private final Path backupRoot;
    private final Path storeDirectory;
    private final Path snapshotsDirectory;
    private final Path objectsDirectory;
    private final Path formatFile;
    private final Path lockFile;
    private final FileLocker fileLocker;

    /**
     * Creates a store for the given backup root. The directory structure is
     * created lazily on first use.
     *
     * @param backupRoot the backup root containing {@code store/}, {@code snapshots/} and {@code files-cache/}
     */
    public JIPipeLocalBackupObjectStore(Path backupRoot) {
        this.backupRoot = backupRoot;
        this.progressInfo = new JIPipeProgressInfo();
        this.storeDirectory = backupRoot.resolve("store");
        this.snapshotsDirectory = backupRoot.resolve("snapshots");
        this.objectsDirectory = storeDirectory.resolve("objects");
        this.formatFile = storeDirectory.resolve("format.json");
        this.lockFile = storeDirectory.resolve("lock");
        this.fileLocker = new FileLocker(progressInfo, lockFile);
    }

    @Override
    public boolean acquireWriteLock(long timeoutMillis) {
        return fileLocker.acquireWriteLock(timeoutMillis);
    }

    @Override
    public void releaseWriteLock() {
        fileLocker.releaseLock();
    }

    @Override
    public String put(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            return put(in, Files.size(file));
        }
    }

    @Override
    public String put(InputStream content, long size) throws IOException {
        checkFormat();
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 is not available", e);
        }
        // Temp file inside the objects tree (same file system as the final
        // object, required for the atomic move), named .tmp-<uuid>.
        Path tempFile = objectsDirectory.resolve(".tmp-" + UUID.randomUUID());
        try {
            Files.createDirectories(objectsDirectory);
            try (InputStream digestIn = new DigestInputStream(content, digest)) {
                Files.copy(digestIn, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }
            String hash = HEX.formatHex(digest.digest());

            Path target = objectsDirectory.resolve(hash.substring(0, 2)).resolve(hash);
            if (Files.exists(target)) {
                // Dedup: identical content is already stored — keep the existing object
            } else {
                Files.createDirectories(target.getParent());
                try {
                    Files.move(tempFile, target, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    // Same file system, so this is only a theoretical fallback
                    Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            progressInfo.log("Stored backup object " + hash + " (" + sizeOf(hash) + " bytes, advisory " + size + ")");
            return hash;
        } finally {
            // No .tmp- leftovers on success or failure
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException e) {
                log.warn("Could not delete temp file {}", tempFile, e);
            }
        }
    }

    @Override
    public boolean has(String hash) {
        if (!isValidHash(hash))
            return false;
        return Files.isRegularFile(objectFile(hash));
    }

    @Override
    public long sizeOf(String hash) {
        if (!isValidHash(hash))
            return -1;
        try {
            Path f = objectFile(hash);
            return Files.isRegularFile(f) ? Files.size(f) : -1;
        } catch (IOException e) {
            return -1;
        }
    }

    @Override
    public InputStream get(String hash) throws IOException {
        if (!isValidHash(hash))
            throw new IOException("Invalid backup object hash: " + hash);
        Path f = objectFile(hash);
        if (!Files.isRegularFile(f))
            throw new IOException("No backup object for hash " + hash);
        // The expected length is snapshotted at open time; the stream then
        // fails with a truncation error if fewer bytes arrive.
        return new TruncationCheckingInputStream(Files.newInputStream(f), hash, Files.size(f));
    }

    @Override
    public long totalObjectBytes() throws IOException {
        if (!Files.isDirectory(objectsDirectory))
            return 0;
        try (Stream<Path> s = Files.walk(objectsDirectory)) {
            return s.filter(Files::isRegularFile)
                    .filter(p -> isValidHash(p.getFileName().toString()))
                    .mapToLong(JIPipeLocalBackupObjectStore::safeSize)
                    .sum();
        }
    }

    @Override
    public void garbageCollect(JIPipeProgressInfo gcProgress) throws IOException {
        // Caller-holds-lock (see interface Javadoc). Do NOT re-acquire here.
        Set<String> referenced = collectReferencedHashes();
        gcProgress.log("Backup GC: " + referenced.size() + " referenced objects across snapshot manifests");

        long deletedObjects = 0;
        long deletedTemps = 0;
        if (Files.isDirectory(objectsDirectory)) {
            try (Stream<Path> entries = Files.list(objectsDirectory)) {
                for (Path entry : entries.toList()) {
                    String name = entry.getFileName().toString();
                    if (Files.isRegularFile(entry)) {
                        if (name.startsWith(".tmp-")) {
                            // Stray temp file from an interrupted put
                            Files.deleteIfExists(entry);
                            deletedTemps++;
                        }
                        continue;
                    }
                    if (!Files.isDirectory(entry))
                        continue;
                    boolean empty = true;
                    try (Stream<Path> files = Files.list(entry)) {
                        for (Path file : files.toList()) {
                            if (gcProgress.isCancelled())
                                return;
                            String fileName = file.getFileName().toString();
                            if (fileName.startsWith(".tmp-")) {
                                Files.deleteIfExists(file);
                                deletedTemps++;
                                continue;
                            }
                            if (Files.isRegularFile(file) && !referenced.contains(fileName)) {
                                Files.deleteIfExists(file);
                                deletedObjects++;
                                continue;
                            }
                            empty = false;
                        }
                    }
                    if (empty) {
                        Files.deleteIfExists(entry);
                    }
                }
            }
        }
        gcProgress.log("Backup GC: deleted " + deletedObjects + " unreferenced objects, " + deletedTemps + " stray temp files");
    }

    @Override
    public Path getStoreDirectory() {
        return storeDirectory;
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private Path objectFile(String hash) {
        return objectsDirectory.resolve(hash.substring(0, 2)).resolve(hash);
    }

    private static boolean isValidHash(String hash) {
        return hash != null && VALID_HASH.matcher(hash).matches();
    }

    private static long safeSize(Path p) {
        try {
            return Files.size(p);
        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * Ensures the store format is supported: creates {@code format.json} on
     * first use, and refuses to operate if a newer format version is found.
     */
    private void checkFormat() throws IOException {
        Files.createDirectories(storeDirectory);
        if (Files.exists(formatFile)) {
            JsonNode node;
            try {
                node = JsonUtils.getObjectMapper().readTree(formatFile.toFile());
            } catch (IOException | UncheckedIOException e) {
                throw new IOException("Corrupt store format file " + formatFile, e);
            }
            JsonNode version = node.get("format-version");
            if (version != null && version.isNumber() && version.asInt() > FORMAT_VERSION) {
                throw new IOException("Unsupported store format version " + version.asInt());
            }
        } else {
            JsonUtils.saveToFile(Map.of("format-version", FORMAT_VERSION), formatFile);
            progressInfo.log("Created backup store format file " + formatFile + " (version " + FORMAT_VERSION + ")");
        }
    }

    /**
     * Walks every {@code snapshots/*.json} manifest and collects all hashes
     * referenced in {@code entries[].hash}. Until Task 2 introduces the typed
     * {@code JIPipeBackupSnapshot} model, manifests are parsed with a raw
     * JsonNode walk; unreadable manifests are skipped with a warning rather
     * than failing the whole sweep.
     */
    private Set<String> collectReferencedHashes() throws IOException {
        Set<String> hashes = new LinkedHashSet<>();
        if (!Files.isDirectory(snapshotsDirectory))
            return hashes;
        try (Stream<Path> s = Files.list(snapshotsDirectory)) {
            for (Path manifest : s.filter(p -> p.getFileName().toString().endsWith(".json")).toList()) {
                JsonNode node;
                try {
                    node = JsonUtils.getObjectMapper().readTree(manifest.toFile());
                } catch (IOException | UncheckedIOException e) {
                    log.warn("Skipping unreadable snapshot manifest {}", manifest, e);
                    continue;
                }
                JsonNode entries = node.get("entries");
                if (entries != null && entries.isArray()) {
                    for (JsonNode entry : entries) {
                        JsonNode h = entry.get("hash");
                        if (h != null && h.isTextual() && VALID_HASH.matcher(h.asText()).matches()) {
                            hashes.add(h.asText());
                        }
                    }
                }
            }
        }
        return hashes;
    }

    /**
     * Counts bytes read and throws {@code IOException("Object <hash> is truncated")}
     * when fewer bytes arrive than were present at the time the stream was
     * opened. Callers that need full integrity re-hash the content while
     * reading (restore does, Task 4).
     */
    private static final class TruncationCheckingInputStream extends InputStream {
        private final InputStream in;
        private final String hash;
        private final long expectedLength;
        private long bytesRead;

        private TruncationCheckingInputStream(InputStream in, String hash, long expectedLength) {
            this.in = in;
            this.hash = hash;
            this.expectedLength = expectedLength;
        }

        private void count(long n) throws IOException {
            bytesRead += n;
            if (bytesRead > expectedLength) {
                close();
                throw new IOException("Object " + hash + " is truncated");
            }
        }

        @Override
        public int read() throws IOException {
            int b = in.read();
            if (b >= 0) {
                count(1);
            } else if (bytesRead < expectedLength) {
                close();
                throw new IOException("Object " + hash + " is truncated");
            }
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int n = in.read(b, off, len);
            if (n > 0) {
                count(n);
            } else if (n < 0 && bytesRead < expectedLength) {
                close();
                throw new IOException("Object " + hash + " is truncated");
            }
            return n;
        }

        @Override
        public long skip(long n) throws IOException {
            long s = in.skip(n);
            if (s > 0) {
                count(s);
            } else if (s < 0 && bytesRead < expectedLength) {
                close();
                throw new IOException("Object " + hash + " is truncated");
            }
            return s;
        }

        @Override
        public int available() throws IOException {
            return in.available();
        }

        @Override
        public void close() throws IOException {
            in.close();
        }
    }
}
