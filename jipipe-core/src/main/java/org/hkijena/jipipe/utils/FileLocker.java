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

import org.hkijena.jipipe.api.JIPipeProgressInfo;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class FileLocker {
    private final JIPipeProgressInfo progressInfo;
    private final Path lockFilePath;
    private FileChannel fileChannel;
    private FileLock fileLock;

    public FileLocker(JIPipeProgressInfo progressInfo, Path lockFilePath) {
        this.progressInfo = progressInfo;
        this.lockFilePath = lockFilePath;
    }

    public boolean tryWriteLock() {
        try {
            Files.createDirectories(lockFilePath.getParent());
            progressInfo.log("Attempting to acquire WRITE lock " + lockFilePath);
            fileChannel = FileChannel.open(lockFilePath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            fileLock = fileChannel.tryLock();
            if (fileLock != null) {
                releaseLock();
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    public boolean tryReadLock() {
        try {

            // Create the file if it does not exist
            if (!Files.isRegularFile(lockFilePath)) {
                acquireWriteLock();
                releaseLock();
                return true;
            }

            progressInfo.log("Attempting to acquire READ lock " + lockFilePath);
            fileChannel = FileChannel.open(lockFilePath, StandardOpenOption.READ);
            fileLock = fileChannel.tryLock(0L, Long.MAX_VALUE, true);
            if (fileLock != null) {
                releaseLock();
                return true;
            }

        } catch (Exception e) {
            return false;
        }

        return false;
    }

    public boolean acquireWriteLock() {
        try {
            while (true) {
                Files.createDirectories(lockFilePath.getParent());
                progressInfo.log("Attempting to acquire WRITE lock " + lockFilePath);
                fileChannel = FileChannel.open(lockFilePath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                fileLock = fileChannel.tryLock();
                if (fileLock != null) {
                    return true;
                }

                progressInfo.log("... not successful, retrying in 1s");
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Acquires the write lock, giving up after the timeout. Unlike
     * {@link #tryWriteLock()} (a probe that releases immediately), the lock is
     * HELD on success until {@link #releaseLock()}.
     *
     * @param timeoutMillis maximum time to wait
     * @return true if the lock is held
     */
    public boolean acquireWriteLock(long timeoutMillis) {
        try {
            long deadline = System.currentTimeMillis() + Math.max(0, timeoutMillis);
            boolean firstAttempt = true;
            while (true) {
                Files.createDirectories(lockFilePath.getParent());
                progressInfo.log("Attempting to acquire WRITE lock " + lockFilePath + " (bounded, " + timeoutMillis + " ms)");
                fileChannel = FileChannel.open(lockFilePath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                try {
                    fileLock = fileChannel.tryLock();
                } catch (OverlappingFileLockException e) {
                    // The lock is held by another channel OF THIS JVM (FileLock is
                    // per-JVM, not per-process): treat it as "held" and retry until
                    // the deadline. Between processes the same contention surfaces
                    // as a plain null return instead.
                    fileLock = null;
                }
                if (fileLock != null) {
                    return true;
                }
                // Close the channel (it does not hold the lock, but avoids leaking it)
                try {
                    fileChannel.close();
                } catch (IOException ignored) {
                }
                fileChannel = null;
                fileLock = null;

                if (System.currentTimeMillis() >= deadline) {
                    progressInfo.log("... giving up after " + timeoutMillis + " ms (bounded)");
                    return false;
                }

                // Short first retry, then the standard 1 s interval
                long sleepMillis = firstAttempt ? 100 : 1000;
                firstAttempt = false;
                progressInfo.log("... not successful, retrying in " + sleepMillis + " ms");
                Thread.sleep(sleepMillis);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean acquireReadLock() {
        try {
            while (true) {
                progressInfo.log("Attempting to acquire READ lock " + lockFilePath);

                // Create the file if it does not exist
                if (!Files.isRegularFile(lockFilePath)) {
                    acquireWriteLock();
                    releaseLock();
                    return true;
                }

                fileChannel = FileChannel.open(lockFilePath, StandardOpenOption.READ);
                fileLock = fileChannel.tryLock(0L, Long.MAX_VALUE, true);
                if (fileLock != null) {
                    return true;
                }

                progressInfo.log("... not successful, retrying in 1s");
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            return false;
        }
    }

    public void releaseLock() {
        try {
            progressInfo.log("Releasing lock " + lockFilePath);
            if (fileLock != null && fileLock.isValid()) {
                fileLock.release(); // Release the lock
            }
            if (fileChannel != null && fileChannel.isOpen()) {
                fileChannel.close(); // Close the file channel
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
