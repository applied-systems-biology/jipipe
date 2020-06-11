package org.hkijena.acaq5.api;

import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.parameters.primitives.FilePathParameterSettings;
import org.hkijena.acaq5.ui.components.PathEditor;

import java.nio.file.Path;

/**
 * A mutable implementation of {@link ACAQRunConfiguration}
 */
public class ACAQMutableRunConfiguration implements ACAQRunConfiguration {
    private EventBus eventBus = new EventBus();
    private Path outputPath;
    private boolean flushingEnabled = true;
    private String endAlgorithmId;
    private boolean onlyRunningEndAlgorithm;
    private boolean flushingKeepsDataEnabled = false;

    @Override
    @ACAQParameter("output-path")
    @ACAQDocumentation(name = "Output folder")
    @FilePathParameterSettings(ioMode = PathEditor.IOMode.Save, pathMode = PathEditor.PathMode.DirectoriesOnly)
    public Path getOutputPath() {
        return outputPath;
    }

    /**
     * Sets the output path
     *
     * @param outputPath The output path
     */
    @ACAQParameter("output-path")
    public void setOutputPath(Path outputPath) {
        this.outputPath = outputPath;
        getEventBus().post(new ParameterChangedEvent(this, "output-path"));
    }

    @Override
    public boolean isFlushingEnabled() {
        return flushingEnabled;
    }

    /**
     * Enables/Disables if data is written
     *
     * @param flushingEnabled Enables/Disables writing data
     */
    public void setFlushingEnabled(boolean flushingEnabled) {
        this.flushingEnabled = flushingEnabled;
    }

    @Override
    public boolean isOnlyRunningEndAlgorithm() {
        return onlyRunningEndAlgorithm;
    }

    /**
     * Enables/Disables if only the last algorithm is run.
     * Used for {@link org.hkijena.acaq5.api.testbench.ACAQTestbench}
     *
     * @param onlyRunningEndAlgorithm Enables/Disables if only the last algorithm is run.
     */
    public void setOnlyRunningEndAlgorithm(boolean onlyRunningEndAlgorithm) {
        this.onlyRunningEndAlgorithm = onlyRunningEndAlgorithm;
    }

    @Override
    public boolean isFlushingKeepsDataEnabled() {
        return flushingKeepsDataEnabled;
    }

    /**
     * Enables/Disables if data is kept after writing.
     *
     * @param flushingKeepsDataEnabled Enables/Disables if data is kept after writing.
     */
    public void setFlushingKeepsDataEnabled(boolean flushingKeepsDataEnabled) {
        this.flushingKeepsDataEnabled = flushingKeepsDataEnabled;
    }

    @Override
    public String getEndAlgorithmId() {
        return endAlgorithmId;
    }

    /**
     * Sets the end algorithm ID. Effective with isOnlyRunningEndAlgorithm()
     *
     * @param endAlgorithmId The algorithm ID
     */
    public void setEndAlgorithmId(String endAlgorithmId) {
        this.endAlgorithmId = endAlgorithmId;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }
}
