/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.data;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.utils.data.Store;

import java.io.Closeable;
import java.io.IOException;
import java.util.WeakHashMap;

/**
 * Manages virtual data
 * This class can store data in memory or in a temporary path.
 * Please note that the temporary data is not automatically cleaned up by finalize()! Please call close() to remove any files.
 */
public class JIPipeDataItemStore implements AutoCloseable, Closeable, Store<JIPipeData> {
    private final Class<? extends JIPipeData> dataClass;
    private JIPipeData data;
    private final String stringRepresentation;
    private final WeakHashMap<Object, Boolean> users = new WeakHashMap<>();
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
        if (closed)
            throw new IllegalStateException("The data object is already destroyed (use-after-free)");
        JIPipeData data = getData(progressInfo).duplicate(progressInfo);
        return new JIPipeDataItemStore(data);
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
    public synchronized <T extends JIPipeData> T getData(Class<T> klass, JIPipeProgressInfo progressInfo) {
        JIPipeData data = getData(progressInfo);
        data = JIPipe.getDataTypes().convert(data, klass, progressInfo);
        return (T) data;
    }

    /**
     * Gets the currently stored data
     *
     * @param progressInfo the progress info
     * @return the data
     */
    public synchronized JIPipeData getData(JIPipeProgressInfo progressInfo) {
        if (closed) {
            throw new IllegalStateException("The data object is already destroyed (use-after-free)");
        }
        return data;
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
    public synchronized void addUser(Object obj) {
        users.put(obj, true);
    }

    /**
     * Un-marks the provided objects as user of this data
     *
     * @param obj the object
     */
    public synchronized void removeUser(Object obj) {
        users.remove(obj);
    }

    /**
     * Returns true if the data has no users and thus can be closed
     *
     * @return if the data has no users
     */
    public synchronized boolean canClose() {
        return users.isEmpty();
    }

    @Override
    public synchronized void close() throws IOException {
        if (closed)
            return;
        closed = true;
        if (data != null) {
            data.close();
        }
        data = null;
        users.clear();
    }

    @Override
    public JIPipeData get() {
        return data;
    }

    @Override
    public boolean isPresent() {
        return !isClosed();
    }
}
