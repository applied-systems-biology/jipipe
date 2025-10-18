package org.hkijena.jipipe.api.nodes.database;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.StampedLock;

public abstract class DefaultJIPipeNodeDatabaseEntry implements JIPipeNodeDatabaseEntry {
    private final Map<Class<?>, Object> attachments = new HashMap<>();
    private final StampedLock attachmentsLock = new StampedLock();

    @Override
    public <T> T getAttachment(Class<T> attachmentClass) {
        final long stamp = attachmentsLock.readLock();
        try {
            return (T) attachments.get(attachmentClass);
        }
        finally {
            attachmentsLock.unlock(stamp);
        }
    }

    @Override
    public <T> void attach(T attachment) {
        final long stamp = attachmentsLock.writeLock();
        try {
            attachments.put(attachment.getClass(), attachment);
        }
        finally {
            attachmentsLock.unlock(stamp);
        }
    }
}
