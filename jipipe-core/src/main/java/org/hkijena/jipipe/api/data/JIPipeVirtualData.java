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
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.extensions.settings.VirtualDataSettings;

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
     * @param data data
     */
    public JIPipeVirtualData(JIPipeData data) {
        this.dataClass = data.getClass();
        this.data = data;
        this.stringRepresentation = data.toString();
    }

    /**
     * Creates a custom virtual data. You have to ensure that everything inside here is correct!
     * @param dataClass the data class
     * @param virtualStoragePath path for virtual storage (to create a new one use VirtualDataSettings.generateTempDirectory). Must be a valid row storage folder for the data type!
     * @param stringRepresentation string representation of the data
     */
    public JIPipeVirtualData(Class<? extends JIPipeData> dataClass, Path virtualStoragePath, String stringRepresentation) {
        this.dataClass = dataClass;
        this.virtualStoragePath = new PathContainer(virtualStoragePath);
        this.stringRepresentation = stringRepresentation;
    }

    /**
     * Copies the virtual data. This is a shallow copy
     * @param other the original
     */
    public JIPipeVirtualData(JIPipeVirtualData other) {
        this.dataClass = other.dataClass;
        this.data = other.data;
        this.dataReference = other.dataReference;
        this.virtualStoragePath = other.virtualStoragePath;
    }

    public synchronized boolean isVirtual() {
        return data == null;
    }

    public synchronized void makeVirtual(JIPipeProgressInfo progressInfo) {
        if(!isVirtual()) {
            if(JIPipe.getInstance() != null && !VirtualDataSettings.getInstance().isVirtualMode())
                return;
            if(virtualStoragePath.getPath() == null) {
                virtualStoragePath.setPath(VirtualDataSettings.generateTempDirectory("virtual"));
            }
            progressInfo.log("Saving data of type " + JIPipeDataInfo.getInstance(dataClass).getName() + " to virtual path " + virtualStoragePath.getPath());
            data.saveTo(virtualStoragePath.getPath(), "virtual", false, progressInfo);
            dataReference = new WeakReference<>(data);
            data = null;
            System.gc();
        }
    }

    public synchronized void makeNonVirtual(JIPipeProgressInfo progressInfo) {
        if(isVirtual()) {
            if(virtualStoragePath.getPath() == null) {
                throw new UnsupportedOperationException("Tried to load virtual data, but no path is set. This should not be possible.");
            }
            progressInfo.log("Loading data of type " + JIPipeDataInfo.getInstance(dataClass).getName() + " from virtual path " + virtualStoragePath.getPath());
            data = JIPipe.importData(virtualStoragePath.getPath(), dataClass);
            dataReference = null;
        }
    }

    public JIPipeData getData(JIPipeProgressInfo progressInfo) {
        if(dataReference != null) {
            JIPipeData existing = dataReference.get();
            if(existing != null)
                return existing;
        }
        makeNonVirtual(progressInfo);
        return data;
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
