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

package org.hkijena.jipipe.api.artifacts;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.utils.FileLocker;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Context that allows to track the state of the current artifact management run
 */
public class JIPipeArtifactOperationContext implements Closeable, AutoCloseable {

    private final JIPipeProgressInfo progressInfo;
    private LockState currentLockState = LockState.None;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final FileLocker fileLocker;

    public JIPipeArtifactOperationContext(JIPipeProgressInfo progressInfo) {
        this.progressInfo = progressInfo;
        this.fileLocker = JIPipe.getArtifacts().createFileLocker();
    }

    public boolean canRead() {
        ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        try {
            readLock.lock();
            return currentLockState == LockState.Read || currentLockState == LockState.ReadWrite;
        }
        finally {
            readLock.unlock();
        }
    }

    public boolean canWrite() {
        ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        try {
            readLock.lock();
            return currentLockState == LockState.ReadWrite;
        }
        finally {
            readLock.unlock();
        }
    }

    public void waitUntilRead() {
        if(canRead()) {
            return;
        }
        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            fileLocker.releaseLock();
            if(!fileLocker.acquireReadLock()) {
                throw new IllegalStateException("Could not acquire read lock on artifact repository!");
            }
            currentLockState = LockState.Read;
        }
        finally {
            writeLock.unlock();
        }
    }

    public void waitUntilWrite() {
        if(canWrite()) {
            return;
        }
        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            fileLocker.releaseLock();
            if(!fileLocker.acquireWriteLock()) {
                throw new IllegalStateException("Could not acquire read lock on artifact repository!");
            }
            currentLockState = LockState.ReadWrite;
        }
        finally {
            writeLock.unlock();
        }
    }

    public void release() {
        // Check if we don't need any release
        ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        try {
            readLock.lock();
            if(currentLockState == LockState.None) {
                return;
            }
        }
        finally {
            readLock.unlock();
        }

        // Release
        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            fileLocker.releaseLock();
            currentLockState = LockState.None;
        }
        finally {
            writeLock.unlock();
        }
    }

    public LockState getCurrentLockState() {
        return currentLockState;
    }

    public JIPipeProgressInfo getProgressInfo() {
        return progressInfo;
    }

    @Override
    public void close() throws IOException {
        release();
    }

    public enum LockState {
        None,
        Read,
        ReadWrite,
    }
}
