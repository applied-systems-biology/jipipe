package org.hkijena.acaq5.api;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extension.ui.parametereditors.FilePathParameterSettings;
import org.hkijena.acaq5.ui.components.FileSelection;

import java.nio.file.Path;
import java.util.Set;

public class ACAQMutableRunConfiguration implements ACAQRunConfiguration {
    private Path outputPath;
    private boolean flushingEnabled = true;
    private String endAlgorithmId;
    private boolean onlyRunningEndAlgorithm;
    private boolean flushingKeepsDataEnabled = false;

    @ACAQParameter("output-path")
    public void setOutputPath(Path outputPath) {
        this.outputPath = outputPath;
    }

    @Override
    @ACAQParameter("output-path")
    @ACAQDocumentation(name = "Output folder")
    @FilePathParameterSettings(ioMode = FileSelection.IOMode.Open, pathMode = FileSelection.PathMode.DirectoriesOnly)
    public Path getOutputPath() {
        return outputPath;
    }

    @Override
    public boolean isFlushingEnabled() {
        return flushingEnabled;
    }

    public void setFlushingEnabled(boolean flushingEnabled) {
        this.flushingEnabled = flushingEnabled;
    }

    @Override
    public boolean isOnlyRunningEndAlgorithm() {
        return onlyRunningEndAlgorithm;
    }

    public void setOnlyRunningEndAlgorithm(boolean onlyRunningEndAlgorithm) {
        this.onlyRunningEndAlgorithm = onlyRunningEndAlgorithm;
    }

    @Override
    public boolean isFlushingKeepsDataEnabled() {
        return flushingKeepsDataEnabled;
    }

    public void setFlushingKeepsDataEnabled(boolean flushingKeepsDataEnabled) {
        this.flushingKeepsDataEnabled = flushingKeepsDataEnabled;
    }

    @Override
    public String getEndAlgorithmId() {
        return endAlgorithmId;
    }

    public void setEndAlgorithmId(String endAlgorithmId) {
        this.endAlgorithmId = endAlgorithmId;
    }
}
