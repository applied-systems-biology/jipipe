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

package org.hkijena.jipipe.api.data;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeThumbnailData;
import org.hkijena.jipipe.utils.data.Store;

import java.io.Closeable;
import java.io.IOException;
import java.util.WeakHashMap;
import java.util.concurrent.locks.StampedLock;

/**
 * Manages data and users
 */
public class JIPipeDataItemStore implements AutoCloseable, Closeable, Store<JIPipeData> {

    private final StampedLock stampedLock = new StampedLock();
    private final Class<? extends JIPipeData> dataClass;
    private final String stringRepresentation;
    private final WeakHashMap<Object, Boolean> users = new WeakHashMap<>();
    private JIPipeData data;
    private JIPipeThumbnailData thumbnail;
    private boolean closed = false;

    /**
     * Create virtual data from data
     *
     * @param data data
     */
    public JIPipeDataItemStore(JIPipeData data) {
        this.dataClass = data.getClass();
        this.data = data;
        this.stringRepresentation = data.toString();
    }

    /**
     * Fully duplicates the virtual data
     *
     * @param progressInfo the progress info
     * @return the copy
     */
    public JIPipeDataItemStore duplicate(JIPipeProgressInfo progressInfo) {
        if (closed) {
            throw new IllegalStateException("The data object is already destroyed (use-after-free)");
        }
        JIPipeData data = getData(progressInfo).duplicate(progressInfo);
        JIPipeDataItemStore copy = new JIPipeDataItemStore(data);
        copy.thumbnail = thumbnail;
        return copy;
    }

    /**
     * Returns true if this object is closed and thus should not be used anymore
     *
     * @return if the object is closed
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Gets the currently stored data. Applies conversion if needed.
     *
     * @param klass        the output type
     * @param progressInfo the progress info
     * @param <T>          the output type
     * @return the output data
     */
    public <T extends JIPipeData> T getData(Class<T> klass, JIPipeProgressInfo progressInfo) {
        long stamp = stampedLock.readLock();
        try {
            JIPipeData data = getData_(progressInfo);
            data = JIPipe.getDataTypes().convert(data, klass, progressInfo);
            return (T) data;
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    /**
     * Gets the currently stored thumbnail
     *
     * @return the thumbnail or null
     */
    public JIPipeThumbnailData getThumbnail() {
        long stamp = stampedLock.readLock();
        try {
            if (closed) {
                throw new IllegalStateException("The data object is already destroyed (use-after-free)");
            }
            return thumbnail;
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    /**
     * Sets the thumbnail
     *
     * @param thumbnail the thumbnail or null
     */
    public void setThumbnail(JIPipeThumbnailData thumbnail) {
        long stamp = stampedLock.writeLock();
        try {
            if (closed) {
                throw new IllegalStateException("The data object is already destroyed (use-after-free)");
            }
            this.thumbnail = thumbnail;
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    /**
     * Gets the currently stored data
     *
     * @param progressInfo the progress info
     * @return the data
     */
    protected JIPipeData getData_(JIPipeProgressInfo progressInfo) {
        if (closed) {
            throw new IllegalStateException("The data object is already destroyed (use-after-free)");
        }
        return data;
    }

    /**
     * Gets the currently stored data
     *
     * @param progressInfo the progress info
     * @return the data
     */
    public JIPipeData getData(JIPipeProgressInfo progressInfo) {
        long stamp = stampedLock.readLock();
        try {
            return getData_(progressInfo);
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    public String getStringRepresentation() {
        if (data != null) {
            return "" + data;
        }
        return stringRepresentation;
    }

    public Class<? extends JIPipeData> getDataClass() {
        return dataClass;
    }

    /**
     * Marks the provided object as user of this data
     *
     * @param obj the object
     */
    public void addUser(Object obj) {
        long stamp = stampedLock.writeLock();
        try {
            users.put(obj, true);
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    /**
     * Un-marks the provided objects as user of this data
     *
     * @param obj the object
     */
    public void removeUser(Object obj) {
        long stamp = stampedLock.writeLock();
        try {
            users.remove(obj);
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    /**
     * Returns true if the data has no users and thus can be closed
     *
     * @return if the data has no users
     */
    public boolean canClose() {
        long stamp = stampedLock.readLock();
        try {
            return users.isEmpty();
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    @Override
    public void close() throws IOException {
        long stamp = stampedLock.writeLock();
        try {
            if (closed)
                return;
            closed = true;
            if (data != null) {
                data.close();
            }
            data = null;
            users.clear();
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    @Override
    public JIPipeData get() {
        if (closed) {
            throw new IllegalStateException("The data object is already destroyed (use-after-free)");
        }
        return data;
    }

    @Override
    public boolean isPresent() {
        return !isClosed();
    }
}
