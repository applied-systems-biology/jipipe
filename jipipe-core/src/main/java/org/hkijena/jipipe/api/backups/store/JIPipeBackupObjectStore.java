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

import org.hkijena.jipipe.api.JIPipeProgressInfo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * A content-addressed object store for backup snapshots: every object is
 * addressed by the SHA-256 hash of its content, stored as a loose file.
 * Objects are IMMUTABLE once written — the same content always maps to the
 * same object, which is what makes cross-snapshot deduplication possible.
 * <p>
 * Concurrency: all mutating operations require the caller to hold the
 * write lock ({@link #acquireWriteLock(long)} / {@link #releaseWriteLock()}).
 * Reads ({@link #get(String)}, {@link #has(String)}, {@link #sizeOf(String)},
 * {@link #totalObjectBytes()}) are lock-free because objects are immutable.
 */
public interface JIPipeBackupObjectStore {
    /**
     * Acquires the bounded cross-process write lock.
     * The lock is HELD on success until {@link #releaseWriteLock()}.
     *
     * @param timeoutMillis maximum time to wait
     * @return true if the lock is now held
     */
    boolean acquireWriteLock(long timeoutMillis);

    /**
     * Releases the write lock. Only valid if this store instance acquired it.
     */
    void releaseWriteLock();

    /**
     * Stores a file's bytes; returns the SHA-256 hex hash of the content.
     * Existing hashes are NOT re-written (dedup): if the object already
     * exists, the existing file is kept and the hash is returned.
     * <p>
     * The write lock MUST be held by the caller (this method does not
     * acquire it itself).
     *
     * @param file the file whose bytes to store
     * @return the lowercase SHA-256 hex hash
     * @throws IOException if the file cannot be read or the object cannot be written
     */
    String put(Path file) throws IOException;

    /**
     * Stores streamed bytes of known size; returns the SHA-256 hex hash.
     * The stream is fully consumed and closed by this method.
     * <p>
     * The write lock MUST be held by the caller (this method does not
     * acquire it itself).
     *
     * @param content the bytes to store
     * @param size    advisory size of the content (used for progress logging)
     * @return the lowercase SHA-256 hex hash
     * @throws IOException if the content cannot be read or the object cannot be written
     */
    String put(InputStream content, long size) throws IOException;

    /**
     * Lock-free: objects are immutable once written.
     *
     * @param hash the SHA-256 hex hash
     * @return true if the object exists in this store
     */
    boolean has(String hash);

    /**
     * Lock-free: objects are immutable once written.
     *
     * @param hash the SHA-256 hex hash
     * @return the size of the object in bytes, or -1 if it does not exist
     */
    long sizeOf(String hash);

    /**
     * Lock-free: objects are immutable once written.
     * Opens the object for reading. The stream verifies that exactly
     * {@link #sizeOf(String)} bytes can be read: if fewer bytes arrive
     * (the object file was truncated or corrupted after the stream was
     * opened), reading to the end throws
     * {@code IOException("Object <hash> is truncated")}. Callers that need
     * full integrity re-hash the content while reading.
     *
     * @param hash the SHA-256 hex hash
     * @return an input stream over the object bytes (the caller closes it)
     * @throws IOException if the object does not exist
     */
    InputStream get(String hash) throws IOException;

    /**
     * Lock-free: objects are immutable once written.
     *
     * @return the sum of all object file sizes in bytes
     * @throws IOException if the store cannot be read
     */
    long totalObjectBytes() throws IOException;

    /**
     * Mark-and-sweep garbage collection: deletes object files that are not
     * referenced by any snapshot manifest in {@code <backupRoot>/snapshots/},
     * plus stray temp files left behind by interrupted puts.
     * <p>
     * The write lock MUST be held by the caller (this method does not
     * acquire it itself).
     *
     * @param progressInfo progress logging / cancellation
     * @throws IOException if the store cannot be read or written
     */
    void garbageCollect(JIPipeProgressInfo progressInfo) throws IOException;

    /**
     * @return the store's own directory ({@code <backupRoot>/store})
     */
    Path getStoreDirectory();
}
