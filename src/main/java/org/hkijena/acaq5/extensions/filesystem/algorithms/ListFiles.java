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

package org.hkijena.acaq5.extensions.filesystem.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQDataInterface;
import org.hkijena.acaq5.api.algorithm.ACAQSimpleIteratingAlgorithm;
import org.hkijena.acaq5.api.algorithm.AlgorithmInputSlot;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.filesystem.dataypes.FileData;
import org.hkijena.acaq5.extensions.filesystem.dataypes.FolderData;
import org.hkijena.acaq5.extensions.parameters.predicates.PathPredicate;
import org.hkijena.acaq5.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.acaq5.utils.ResourceUtils;
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
 * Algorithm that lists files in each folder
 */
@ACAQDocumentation(name = "List files", description = "Lists all files in the input folder")
@ACAQOrganization(menuPath = "List", algorithmCategory = ACAQAlgorithmCategory.FileSystem)

// Algorithm flow
@AlgorithmInputSlot(value = FolderData.class, slotName = "Folders", autoCreate = true)
@AlgorithmOutputSlot(value = FileData.class, slotName = "Files", autoCreate = true)

// Traits
public class ListFiles extends ACAQSimpleIteratingAlgorithm {

    private PathPredicate.List filters = new PathPredicate.List();
    private boolean filterOnlyFileNames = true;
    private String subFolder;
    private boolean recursive = false;
    private boolean recursiveFollowsLinks = true;

    /**
     * Creates new instance
     *
     * @param declaration The declaration
     */
    public ListFiles(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public ListFiles(ListFiles other) {
        super(other);
        this.filterOnlyFileNames = other.filterOnlyFileNames;
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
        if (!StringUtils.isNullOrEmpty(subFolder)) {
            inputPath = inputPath.resolve(subFolder);
        }
        try {
            Stream<Path> stream;
            if (recursive) {
                FileVisitOption[] options;
                if (recursiveFollowsLinks)
                    options = new FileVisitOption[]{FileVisitOption.FOLLOW_LINKS};
                else
                    options = new FileVisitOption[0];
                stream = Files.walk(inputPath, options).filter(Files::isRegularFile);
            } else
                stream = Files.list(inputPath).filter(Files::isRegularFile);
            for (Path file : stream.collect(Collectors.toList())) {
                Path testedFile;
                if (filterOnlyFileNames)
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

    @ACAQDocumentation(name = "Filter only file names", description = "If enabled, the filter is only applied for the file name. If disabled, the filter is " +
            "applied for the absolute path. For non-existing paths it cannot bne guaranteed that the absolute path is tested.")
    @ACAQParameter("only-filenames")
    public boolean isFilterOnlyFileNames() {
        return filterOnlyFileNames;
    }

    @ACAQParameter("only-filenames")
    public void setFilterOnlyFileNames(boolean filterOnlyFileNames) {
        this.filterOnlyFileNames = filterOnlyFileNames;
    }

    @ACAQDocumentation(name = "Subfolder", description = "Optional. If non-empty, all files are extracted from the provided sub-folder. " +
            "The sub-folder navigation is applied before recursive search (if 'Recursive' is enabled).")
    @ACAQParameter("subfolder")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/folder.png")
    public String getSubFolder() {
        return subFolder;
    }

    @ACAQParameter("subfolder")
    public void setSubFolder(String subFolder) {
        this.subFolder = subFolder;
    }

    @ACAQDocumentation(name = "Recursive", description = "If enabled, the search is recursive.")
    @ACAQParameter("recursive")
    public boolean isRecursive() {
        return recursive;
    }

    @ACAQParameter("recursive")
    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }

    @ACAQDocumentation(name = "Recursive search follows links", description = "If enabled, a recursive search follows symbolic links. " +
            "(Only Windows) Please note that Windows does not create symbolic links by default.")
    @ACAQParameter("recursive-follows-links")
    public boolean isRecursiveFollowsLinks() {
        return recursiveFollowsLinks;
    }

    @ACAQParameter("recursive-follows-links")
    public void setRecursiveFollowsLinks(boolean recursiveFollowsLinks) {
        this.recursiveFollowsLinks = recursiveFollowsLinks;
    }
}
