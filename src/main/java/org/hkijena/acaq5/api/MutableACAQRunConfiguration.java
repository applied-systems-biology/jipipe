package org.hkijena.acaq5.api;

import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extension.ui.parametereditors.FilePathParameterSettings;
import org.hkijena.acaq5.ui.components.FileSelection;

import java.nio.file.Path;

public class MutableACAQRunConfiguration implements ACAQRunConfiguration {
    private Path outputPath;
    private boolean flushingEnabled = true;
    private String endAlgorithmId;

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
    public String getEndAlgorithmId() {
        return endAlgorithmId;
    }

    public void setEndAlgorithmId(String endAlgorithmId) {
        this.endAlgorithmId = endAlgorithmId;
    }
}
