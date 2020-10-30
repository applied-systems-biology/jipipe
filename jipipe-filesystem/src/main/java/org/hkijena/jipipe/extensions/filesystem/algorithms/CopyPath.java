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

package org.hkijena.jipipe.extensions.filesystem.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.FileSystemNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Applies subfolder navigation to each input folder
 */
@JIPipeDocumentation(name = "Copy path", description = "Copies a path to the destination path. " +
        "The behaviour depends on whether the source and destinations are files, folders, or do not exist. \n" +
        "[File] to [File]: The source is copied to the target file.\n" +
        "[File] to [Folder]: The source is copied to [Destination]/[Source file name]\n" +
        "[File] to [Not existing]: The destination is assumed to be a folder, which will be automatically created. The [File] to [Folder] rule is applied." +
        "[Folder] to [Folder]: By default, the contents of the source folder are copied into the destination folder. You can enable a parameter that instead " +
        "copies the contents into [Destination]/[Source folder name]\n" +
        "[Folder] to [Not existing]: The destination is automatically created. The [Folder] to [Folder] rule is applied.")
@JIPipeOrganization(nodeTypeCategory = FileSystemNodeTypeCategory.class)


@JIPipeInputSlot(value = PathData.class, slotName = "Source", autoCreate = true)
@JIPipeInputSlot(value = PathData.class, slotName = "Destination", autoCreate = true)
@JIPipeOutputSlot(value = PathData.class, slotName = "Copied path", autoCreate = true)

// Traits
public class CopyPath extends JIPipeIteratingAlgorithm {

    private boolean skipInvalid = false;
    private boolean skipExisting = false;
    private boolean appendDirectoryNameToTarget = false;


    /**
     * @param info Algorithm info
     */
    public CopyPath(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public CopyPath(CopyPath other) {
        super(other);
        this.skipInvalid = other.skipInvalid;
        this.skipExisting = other.skipExisting;
        this.appendDirectoryNameToTarget = other.appendDirectoryNameToTarget;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        Path input = dataBatch.getInputData("Source", PathData.class).getPath();
        Path destination = dataBatch.getInputData("Destination", PathData.class).getPath();

        try {
            if (Files.isDirectory(input)) {
                // Input is a directory
                if (Files.isDirectory(destination)) {
                    if (appendDirectoryNameToTarget) {
                        destination = destination.resolve(input.getFileName());
                        Files.createDirectories(destination);
                    }
                } else if (Files.isRegularFile(destination)) {
                    throw new IOException("Destination is a file!");
                } else {
                    Files.createDirectories(destination);
                    if (appendDirectoryNameToTarget) {
                        destination = destination.resolve(input.getFileName());
                        Files.createDirectories(destination);
                    }
                }
                for (Path source : Files.walk(input, FileVisitOption.FOLLOW_LINKS).collect(Collectors.toList())) {
                    if (Files.isDirectory(source))
                        continue;
                    Path targetPath = destination.resolve(input.relativize(source));
                    if (!Files.exists(targetPath.getParent())) {
                        Files.createDirectories(targetPath.getParent());
                    }
                    if (Files.exists(targetPath)) {
                        Files.delete(targetPath);
                    }
                    algorithmProgress.accept(subProgress.resolve(String.format("Copying '%s' to '%s'", source, targetPath)));
                    Files.copy(source, targetPath);
                }

            } else if (Files.exists(input)) {
                // Input is a file
                if (Files.isDirectory(destination)) {
                    destination = destination.resolve(input.getFileName());
                } else if (Files.isRegularFile(destination)) {
                    Files.delete(destination);
                } else {
                    Files.createDirectories(destination);
                    destination = destination.resolve(input.getFileName());
                }
                algorithmProgress.accept(subProgress.resolve(String.format("Copying '%s' to '%s'", input, destination)));
                Files.copy(input, destination);
            } else {
                if (!skipInvalid)
                    throw new UserFriendlyRuntimeException(new FileNotFoundException(input.toString()),
                            "Cannot find source path!",
                            getName(),
                            "The path '" + input + "' does not exist.",
                            "Please check if the path is correct.");
            }
        } catch (IOException e) {
            if (!skipInvalid)
                throw new UserFriendlyRuntimeException(e,
                        "Error while copying.",
                        getName(),
                        "Please refer to the technical details.",
                        "Please check if the paths is correct.");
        }

        dataBatch.addOutputData(getFirstOutputSlot(), new PathData(destination));
    }

    @JIPipeDocumentation(name = "Skip invalid paths", description = "If enabled, invalid copy instructions are skipped.")
    @JIPipeParameter("skip-invalid")
    public boolean isSkipInvalid() {
        return skipInvalid;
    }

    @JIPipeParameter("skip-invalid")
    public void setSkipInvalid(boolean skipInvalid) {
        this.skipInvalid = skipInvalid;
    }

    @JIPipeDocumentation(name = "Skip existing targets", description = "If enabled, no operations are executed if the target file/folder already exists.")
    @JIPipeParameter("skip-existing")
    public boolean isSkipExisting() {
        return skipExisting;
    }

    @JIPipeParameter("skip-existing")
    public void setSkipExisting(boolean skipExisting) {
        this.skipExisting = skipExisting;
    }

    @JIPipeDocumentation(name = "Append source directory name to destination", description = "If the source is a directory and this option is enabled, " +
            "its contents will be copied into [target]/[source directory name]. Otherwise they will be copied into [target].")
    @JIPipeParameter("append-directory-name-to-target")
    public boolean isAppendDirectoryNameToTarget() {
        return appendDirectoryNameToTarget;
    }

    @JIPipeParameter("append-directory-name-to-target")
    public void setAppendDirectoryNameToTarget(boolean appendDirectoryNameToTarget) {
        this.appendDirectoryNameToTarget = appendDirectoryNameToTarget;
    }
}
