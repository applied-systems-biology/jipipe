package org.hkijena.acaq5.extensions.filesystem.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQAnnotation;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.filesystem.dataypes.FolderData;
import org.hkijena.acaq5.extensions.filesystem.dataypes.PathData;
import org.hkijena.acaq5.utils.StringUtils;

import java.nio.file.Files;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Algorithm that generates annotations from folder names
 */
@ACAQDocumentation(name = "Path to annotation", description = "Creates an annotation for each path based on its name or its full path.")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Annotation, menuPath = "Generate")
@AlgorithmInputSlot(value = PathData.class, slotName = "Paths", autoCreate = true)
@AlgorithmOutputSlot(value = PathData.class, slotName = "Annotated paths", autoCreate = true, inheritedSlot = "Paths")
public class SimplePathAnnotationGenerator extends ACAQSimpleIteratingAlgorithm {

    private String generatedAnnotation = "Dataset";
    private boolean fullPath = false;
    private boolean removeExtensions = true;

    /**
     * Creates a new instance
     *
     * @param declaration The algorithm declaration
     */
    public SimplePathAnnotationGenerator(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public SimplePathAnnotationGenerator(SimplePathAnnotationGenerator other) {
        super(other);
        this.generatedAnnotation = other.generatedAnnotation;
        this.fullPath = other.fullPath;
        this.removeExtensions = other.removeExtensions;
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        if (!StringUtils.isNullOrEmpty(generatedAnnotation)) {
            FolderData inputData = dataInterface.getInputData(getFirstInputSlot(), FolderData.class);
            boolean removeThisExtension = removeExtensions && Files.isRegularFile(inputData.getPath());

            String annotationValue;
            if (removeThisExtension && fullPath) {
                String fileName = inputData.getPath().getFileName().toString();
                fileName = removeExtension(fileName);
                annotationValue = inputData.getPath().getParent().resolve(fileName).toString();
            } else {
                if (fullPath) {
                    annotationValue = inputData.getPath().toString();
                } else {
                    annotationValue = inputData.getPath().getFileName().toString();
                }
                if (removeThisExtension) {
                    annotationValue = removeExtension(annotationValue);
                }
            }

            dataInterface.addGlobalAnnotation(new ACAQAnnotation(generatedAnnotation, annotationValue));
            dataInterface.addOutputData(getFirstOutputSlot(), inputData);
        }
    }

    private String removeExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex <= 0)
            return fileName;
        else
            return fileName.substring(0, dotIndex);
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Generated annotation").checkNonEmpty(generatedAnnotation, this);
    }

    @ACAQDocumentation(name = "Generated annotation", description = "Select which annotation type is generated for each path")
    @ACAQParameter("generated-annotation")
    public String getGeneratedAnnotation() {
        return generatedAnnotation;
    }

    @ACAQParameter("generated-annotation")
    public void setGeneratedAnnotation(String generatedAnnotation) {
        this.generatedAnnotation = generatedAnnotation;
    }

    @ACAQDocumentation(name = "Annotate with full path", description = "If true, the full path is put into the annotation. Otherwise, only the file or folder name is stored.")
    @ACAQParameter("full-path")
    public boolean isFullPath() {
        return fullPath;
    }

    @ACAQParameter("full-path")
    public void setFullPath(boolean fullPath) {
        this.fullPath = fullPath;
    }

    @ACAQDocumentation(name = "Remove file extensions", description = "If a path is a file, remove its extension. The extension is the substring starting with the last dot. " +
            "Unix dot-files (that start with a dot) are ignored. Ignores files that have no extension.")
    @ACAQParameter("remove-extensions")
    public boolean isRemoveExtensions() {
        return removeExtensions;
    }

    @ACAQParameter("remove-extensions")
    public void setRemoveExtensions(boolean removeExtensions) {
        this.removeExtensions = removeExtensions;
    }
}
