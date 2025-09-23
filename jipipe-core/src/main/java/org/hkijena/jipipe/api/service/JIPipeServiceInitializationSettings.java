package org.hkijena.jipipe.api.service;

import java.nio.file.Path;

/**
 * Settings for JIPipe initialization
 */
public class JIPipeServiceInitializationSettings {
    private JIPipeServiceMode mode = JIPipeServiceMode.GUI;
    private boolean verbose = true;
    private Path overrideUserDirBase;
    private Path overrideArtifactsDir;
    private Path overrideSharedDir;

    public JIPipeServiceInitializationSettings() {
    }

    public JIPipeServiceInitializationSettings(JIPipeServiceInitializationSettings other) {
        this.mode = other.mode;
        this.verbose = other.verbose;
        this.overrideUserDirBase = other.overrideUserDirBase;
        this.overrideArtifactsDir = other.overrideArtifactsDir;
        this.overrideSharedDir = other.overrideSharedDir;
    }

    public Path getOverrideUserDirBase() {
        return overrideUserDirBase;
    }

    public void setOverrideUserDirBase(Path overrideUserDirBase) {
        this.overrideUserDirBase = overrideUserDirBase;
    }

    public Path getOverrideArtifactsDir() {
        return overrideArtifactsDir;
    }

    public void setOverrideArtifactsDir(Path overrideArtifactsDir) {
        this.overrideArtifactsDir = overrideArtifactsDir;
    }

    public Path getOverrideSharedDir() {
        return overrideSharedDir;
    }

    public void setOverrideSharedDir(Path overrideSharedDir) {
        this.overrideSharedDir = overrideSharedDir;
    }

    public JIPipeServiceMode getMode() {
        return mode;
    }

    public void setMode(JIPipeServiceMode mode) {
        this.mode = mode;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
}
