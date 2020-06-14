package org.hkijena.acaq5.extensions.filesystem.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.filesystem.dataypes.FileData;
import org.hkijena.acaq5.extensions.filesystem.dataypes.FolderData;
import org.hkijena.acaq5.extensions.parameters.predicates.PathPredicate;
import org.hkijena.acaq5.utils.StringUtils;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Algorithms that lists the sub folders for each input folder
 */
@ACAQDocumentation(name = "List subfolders", description = "Lists all subfolders")
@ACAQOrganization(menuPath = "List", algorithmCategory = ACAQAlgorithmCategory.FileSystem)

// Algorithm flow
@AlgorithmInputSlot(value = FolderData.class, slotName = "Folders", autoCreate = true)
@AlgorithmOutputSlot(value = FolderData.class, slotName = "Subfolders", autoCreate = true)

// Traits
public class ListSubfolders extends ACAQSimpleIteratingAlgorithm {

    private PathPredicate.List filters = new PathPredicate.List();
    private boolean filterOnlyFolderNames = true;
    private String subFolder;
    private boolean recursive = false;
    private boolean recursiveFollowsLinks = true;

    /**
     * Creates a new instance
     *
     * @param declaration The algorithm declaration
     */
    public ListSubfolders(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public ListSubfolders(ListSubfolders other) {
        super(other);
        this.filterOnlyFolderNames = other.filterOnlyFolderNames;
        this.filters.clear();
        this.subFolder = other.subFolder;
        this.recursive = other.recursive;
        this.recursiveFollowsLinks = other.recursiveFollowsLinks;
        for (PathPredicate filter : other.filters) {
            this.filters.add(new PathPredicate(filter));
        }
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        FolderData inputFolder = dataInterface.getInputData(getFirstInputSlot(), FolderData.class);
        Path inputPath = inputFolder.getPath();
        if(!StringUtils.isNullOrEmpty(subFolder)) {
            inputPath = inputPath.resolve(subFolder);
        }
        try {
            Stream<Path> stream;
            if(recursive) {
                FileVisitOption[] options;
                if(recursiveFollowsLinks)
                    options = new FileVisitOption[] { FileVisitOption.FOLLOW_LINKS };
                else
                    options = new FileVisitOption[0];
                stream = Files.walk(inputPath, options).filter(Files::isDirectory);
            }
            else
                stream = Files.list(inputPath).filter(Files::isDirectory);
            for (Path file : stream.collect(Collectors.toList())) {
                Path testedFile;
                if (filterOnlyFolderNames)
                    testedFile = file.getFileName();
                else
                    testedFile = file;
                if (filters.isEmpty() || filters.stream().anyMatch(f -> f.test(testedFile))) {
                    dataInterface.addOutputData(getFirstOutputSlot(), new FileData(file));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Filters").report(filters);
    }

    @ACAQDocumentation(name = "Filters", description = "You can optionally filter the result folders. " +
            "The filters are connected via a logical OR operation. An empty list disables filtering")
    @ACAQParameter("filters")
    public PathPredicate.List getFilters() {
        return filters;
    }

    @ACAQParameter("filters")
    public void setFilters(PathPredicate.List filters) {
        this.filters = filters;
    }

    @ACAQDocumentation(name = "Filter only folder names", description = "If enabled, the filter is only applied for the folder name. If disabled, the filter is " +
            "applied for the absolute path. For non-existing paths it cannot bne guaranteed that the absolute path is tested.")
    @ACAQParameter("only-filenames")
    public boolean isFilterOnlyFolderNames() {
        return filterOnlyFolderNames;
    }

    @ACAQParameter("only-filenames")
    public void setFilterOnlyFolderNames(boolean filterOnlyFolderNames) {
        this.filterOnlyFolderNames = filterOnlyFolderNames;
    }
}
