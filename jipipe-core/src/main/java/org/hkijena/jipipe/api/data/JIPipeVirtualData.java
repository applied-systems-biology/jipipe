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
import org.hkijena.jipipe.extensions.settings.VirtualDataSettings;
import org.hkijena.jipipe.utils.PathUtils;

import java.lang.ref.WeakReference;
import java.nio.file.Path;

/**
 * Manages virtual data
 */
public class JIPipeVirtualData {
    private final Class<? extends JIPipeData> dataClass;
    private JIPipeData data;
    private WeakReference<JIPipeData> dataReference;
    private PathContainer virtualStoragePath = new PathContainer();
    private String stringRepresentation;

    /**
     * Create virtual data from data
     *
     * @param data data
     */
    public JIPipeVirtualData(JIPipeData data) {
        this.dataClass = data.getClass();
        this.data = data;
        this.stringRepresentation = data.toString();
    }

    /**
     * Fully duplicates the virtual data
     * @param progressInfo the progress info
     * @return the copy
     */
    public JIPipeVirtualData duplicate(JIPipeProgressInfo progressInfo) {
        JIPipeData data = getData(progressInfo).duplicate(progressInfo);
        JIPipeVirtualData virtualData = new JIPipeVirtualData(data);
        if(isVirtual()) {
            virtualData.makeVirtual(progressInfo, true);
        }
        return virtualData;
    }

    /**
     * Creates a custom virtual data. You have to ensure that everything inside here is correct!
     *
     * @param dataClass            the data class
     * @param virtualStoragePath   path for virtual storage (to create a new one use VirtualDataSettings.generateTempDirectory). Must be a valid row storage folder for the data type!
     * @param stringRepresentation string representation of the data
     */
    public JIPipeVirtualData(Class<? extends JIPipeData> dataClass, Path virtualStoragePath, String stringRepresentation) {
        this.dataClass = dataClass;
        this.virtualStoragePath = new PathContainer(virtualStoragePath);
        this.stringRepresentation = stringRepresentation;
    }

    public synchronized boolean isVirtual() {
        return data == null;
    }

    @Override
    protected void finalize() throws Throwable {

        if (virtualStoragePath != null && virtualStoragePath.getPath() != null) {
            try {
                PathUtils.deleteDirectoryRecursively(virtualStoragePath.getPath(), new JIPipeProgressInfo());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        data = null;
        dataReference = null;
        if (virtualStoragePath != null) {
            virtualStoragePath.setPath(null);
            virtualStoragePath = null;
        }

        super.finalize();
    }

    /**
     * Makes the data virtual if not already
     *
     * @param progressInfo progress
     * @param discard      if existing data should be saved or discarded. Discard has no effect if data was not saved, yet.
     */
    public synchronized void makeVirtual(JIPipeProgressInfo progressInfo, boolean discard) {
        if (!isVirtual()) {
            if (JIPipe.getInstance() != null && !VirtualDataSettings.getInstance().isVirtualMode())
                return;
            boolean canDiscard = virtualStoragePath.getPath() != null;
            if (virtualStoragePath.getPath() == null) {
                virtualStoragePath.setPath(VirtualDataSettings.generateTempDirectory("virtual"));
            }
            if (canDiscard && discard) {
                progressInfo.log("Unloading data of type " + JIPipeDataInfo.getInstance(dataClass).getName());
            } else {
                progressInfo.log("Saving data of type " + JIPipeDataInfo.getInstance(dataClass).getName() + " to virtual path " + virtualStoragePath.getPath());
                data.saveTo(virtualStoragePath.getPath(), "virtual", false, progressInfo);
            }
            dataReference = new WeakReference<>(data);
            data = null;
//            System.gc();
        }
    }

    public synchronized void makeNonVirtual(JIPipeProgressInfo progressInfo, boolean removeVirtualDataStorage) {
        if (isVirtual()) {
            if (virtualStoragePath.getPath() == null) {
                throw new UnsupportedOperationException("Tried to load virtual data, but no path is set. This should not be possible.");
            }
            progressInfo.log("Loading data of type " + JIPipeDataInfo.getInstance(dataClass).getName() + " from virtual path " + virtualStoragePath.getPath());
            data = JIPipe.importData(virtualStoragePath.getPath(), dataClass, progressInfo);
            dataReference = null;
            stringRepresentation = "" + data;

            if (removeVirtualDataStorage && virtualStoragePath != null && virtualStoragePath.getPath() != null) {
                try {
                    PathUtils.deleteDirectoryRecursively(virtualStoragePath.getPath(), new JIPipeProgressInfo());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                virtualStoragePath.setPath(null);
            }
        }
    }

    /**
     * Gets the currently stored data. Applies conversion if needed.
     * @param klass the output type
     * @param progressInfo the progress info
     * @param <T> the output type
     * @return the output data
     */
    public synchronized <T extends JIPipeData> T getData(Class<T> klass, JIPipeProgressInfo progressInfo) {
        JIPipeData data = getData(progressInfo);
        data = JIPipe.getDataTypes().convert(data, klass);
        return (T)data;
    }

    /**
     * Gets the currently stored data
     * @param progressInfo the progress info
     * @return the data
     */
    public synchronized JIPipeData getData(JIPipeProgressInfo progressInfo) {
        boolean shouldBeVirtual = isVirtual();
        if (dataReference != null) {
            JIPipeData existing = dataReference.get();
            if (existing != null)
                return existing;
        }
        makeNonVirtual(progressInfo, false);
        JIPipeData cachedData = data;
        if (shouldBeVirtual && !isVirtual()) {
            makeVirtual(progressInfo, true);
        }
        return cachedData;
    }

    public String getStringRepresentation() {
        return stringRepresentation;
    }

    public Class<? extends JIPipeData> getDataClass() {
        return dataClass;
    }

    private static class PathContainer {
        private Path path;

        public PathContainer() {
        }

        public PathContainer(Path path) {
            this.path = path;
        }

        public Path getPath() {
            return path;
        }

        public void setPath(Path path) {
            this.path = path;
        }
    }
}
