package org.hkijena.acaq5.extensions.filesystem.api.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.filesystem.api.dataypes.FolderData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Applies subfolder navigation to each input folder
 */
@ACAQDocumentation(name = "Subfolders", description = "Goes to the specified subfolder")
@ACAQOrganization(menuPath = "Navigate", algorithmCategory = ACAQAlgorithmCategory.FileSystem)

// Algorithm flow
@AlgorithmInputSlot(value = FolderData.class, slotName = "Folders", autoCreate = true)
@AlgorithmOutputSlot(value = FolderData.class, slotName = "Subfolders", autoCreate = true)

// Traits
public class NavigateSubFolder extends ACAQIteratingAlgorithm {

    private String subFolder;

    /**
     * @param declaration Algorithm declaration
     */
    public NavigateSubFolder(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public NavigateSubFolder(NavigateSubFolder other) {
        super(other);
        this.subFolder = other.subFolder;
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        FolderData inputFolder = dataInterface.getInputData("Folders", FolderData.class);
        dataInterface.addOutputData("Subfolders", new FolderData(inputFolder.getPath().resolve(subFolder)));
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        if (subFolder == null || subFolder.isEmpty())
            report.forCategory("Subfolder name").reportIsInvalid("Invalid subfolder name!",
                    "The subfolder name is empty!",
                    "Please enter a name.",
                    this);
    }

    /**
     * @return The subfolder
     */
    @ACAQParameter("subfolder")
    @ACAQDocumentation(name = "Subfolder name")
    public String getSubFolder() {
        return subFolder;
    }

    /**
     * Sets the subfolder
     *
     * @param subFolder the subfolder
     */
    @ACAQParameter("subfolder")
    public void setSubFolder(String subFolder) {
        this.subFolder = subFolder;
    }
}
