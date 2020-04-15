package org.hkijena.acaq5.extensions.filesystem.api.datasources;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.PathCollection;
import org.hkijena.acaq5.extensions.filesystem.api.dataypes.FolderData;
import org.hkijena.acaq5.extensions.standardparametereditors.ui.parametereditors.FilePathParameterSettings;
import org.hkijena.acaq5.ui.components.FileSelection;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Provides an input folder
 */
@ACAQDocumentation(name = "Folder list")
@AlgorithmOutputSlot(value = FolderData.class, slotName = "Folder paths", autoCreate = true)
@AlgorithmMetadata(category = ACAQAlgorithmCategory.DataSource)
public class FolderListDataSource extends ACAQAlgorithm {

    private PathCollection folderPaths = new PathCollection();
    private Path currentWorkingDirectory;

    /**
     * Creates a new instance
     *
     * @param declaration The algorithm declaration
     */
    public FolderListDataSource(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public FolderListDataSource(FolderListDataSource other) {
        super(other);
        this.folderPaths.addAll(other.folderPaths);
        this.currentWorkingDirectory = other.currentWorkingDirectory;
    }

    @Override
    public void run(ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        for (Path folderPath : folderPaths) {
            getFirstOutputSlot().addData(new FolderData(folderPath));
        }
    }

    /**
     * @return Gets the folder paths
     */
    @ACAQParameter("folder-paths")
    @ACAQDocumentation(name = "Folder paths")
    @FilePathParameterSettings(ioMode = FileSelection.IOMode.Open, pathMode = FileSelection.PathMode.DirectoriesOnly)
    public PathCollection getFolderPaths() {
        return folderPaths;
    }

    /**
     * Sets the folder path
     *
     * @param folderPaths Folder paths
     */
    @ACAQParameter("folder-paths")
    public void setFolderPaths(PathCollection folderPaths) {
        this.folderPaths = folderPaths;
        getEventBus().post(new ParameterChangedEvent(this, "folder-paths"));
    }

    /**
     * @return Folder paths as absolute paths
     */
    public PathCollection getAbsoluteFolderPaths() {
        PathCollection result = new PathCollection();
        for (Path folderPath : folderPaths) {
            if (folderPath == null)
                result.add(null);
            else if (currentWorkingDirectory != null)
                result.add(currentWorkingDirectory.resolve(folderPath));
            else
                result.add(folderPath);
        }
        return result;
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        for (Path folderPath : getAbsoluteFolderPaths()) {
            if (folderPath == null) {
                report.reportIsInvalid("An input folder path does not exist! Please provide a valid path.");
            } else if (!Files.isDirectory(folderPath)) {
                report.reportIsInvalid("Input folder '" + folderPath + "' does not exist! Please provide a valid path.");
            }
        }
    }

    @Override
    public void setWorkDirectory(Path workDirectory) {
        super.setWorkDirectory(workDirectory);

        boolean modified = false;
        for (int i = 0; i < folderPaths.size(); ++i) {
            Path folderPath = folderPaths.get(i);
            if (folderPath != null) {
                // Make absolute
                if (!folderPath.isAbsolute()) {
                    if (currentWorkingDirectory != null) {
                        folderPath = currentWorkingDirectory.resolve(folderPath);
                        modified = true;
                    } else if (workDirectory != null) {
                        folderPath = workDirectory.resolve(folderPath);
                        modified = true;
                    }
                }
                // Make relative if already absolute and workDirectory != null
                if (folderPath.isAbsolute()) {
                    if (workDirectory != null && folderPath.startsWith(workDirectory)) {
                        folderPath = workDirectory.relativize(folderPath);
                        modified = true;
                    }
                }

                if (modified)
                    this.folderPaths.set(i, folderPath);
            }
        }
        currentWorkingDirectory = workDirectory;
        if (modified) {
            getEventBus().post(new ParameterChangedEvent(this, "folder-paths"));
        }
    }
}
