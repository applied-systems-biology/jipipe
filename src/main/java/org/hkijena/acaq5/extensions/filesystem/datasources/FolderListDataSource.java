package org.hkijena.acaq5.extensions.filesystem.datasources;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.filesystem.dataypes.FolderData;
import org.hkijena.acaq5.extensions.parameters.collections.PathListParameter;
import org.hkijena.acaq5.extensions.parameters.editors.FilePathParameterSettings;
import org.hkijena.acaq5.ui.components.PathEditor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Provides an input folder
 */
@ACAQDocumentation(name = "Folder list")
@AlgorithmOutputSlot(value = FolderData.class, slotName = "Folder paths", autoCreate = true)
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.DataSource)
public class FolderListDataSource extends ACAQAlgorithm {

    private PathListParameter folderPaths = new PathListParameter();
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
    @FilePathParameterSettings(ioMode = PathEditor.IOMode.Open, pathMode = PathEditor.PathMode.DirectoriesOnly)
    public PathListParameter getFolderPaths() {
        return folderPaths;
    }

    /**
     * Sets the folder path
     *
     * @param folderPaths Folder paths
     */
    @ACAQParameter("folder-paths")
    public void setFolderPaths(PathListParameter folderPaths) {
        this.folderPaths = folderPaths;
        getEventBus().post(new ParameterChangedEvent(this, "folder-paths"));
    }

    /**
     * @return Folder paths as absolute paths
     */
    public PathListParameter getAbsoluteFolderPaths() {
        PathListParameter result = new PathListParameter();
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
                report.reportIsInvalid("Invalid folder path!",
                        "An input folder path does not exist!",
                        "Please provide a valid path.",
                        this);
            } else if (!Files.isDirectory(folderPath)) {
                report.reportIsInvalid("Invalid folder path!",
                        "Input folder '" + folderPath + "' does not exist!",
                        "Please provide a valid path.",
                        this);
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
