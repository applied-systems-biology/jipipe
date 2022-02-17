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

package org.hkijena.jipipe.extensions.utils.datatypes;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FolderData;
import org.hkijena.jipipe.utils.StringUtils;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.stream.Collectors;

@JIPipeDocumentation(name = "JIPipe output", description = "Output of a JIPipe run")
public class JIPipeOutputData extends FolderData {
    /**
     * Initializes file data from a file
     *
     * @param path File path
     */
    public JIPipeOutputData(Path path) {
        super(path);
    }

    public JIPipeOutputData(String path) {
        super(path);
    }

    public static JIPipeOutputData importFrom(Path folder) {
        return new JIPipeOutputData(FolderData.importFrom(folder, new JIPipeProgressInfo()).getPath());
    }

    @Override
    public void saveTo(Path storageFilePath, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        // Copy if we copy it to a different folder
        if (getPath() != null && !storageFilePath.equals(toPath()) && Files.isDirectory(toPath()) && Files.exists(toPath().resolve("project.jip"))) {
            Path outputPath = storageFilePath;
            if (forceName)
                outputPath = outputPath.resolve(StringUtils.makeFilesystemCompatible(name));
            try {
                if (!Files.isDirectory(outputPath))
                    Files.createDirectories(outputPath);
                // Copy the project file
                Files.copy(toPath().resolve("project.jip"), outputPath.resolve("project.jip"), StandardCopyOption.REPLACE_EXISTING);

                // Copy the results
                Path finalOutputPath = outputPath;
                List<Path> compartmentOutputFolders;
                try {
                    compartmentOutputFolders = Files.list(toPath()).filter(Files::isDirectory).collect(Collectors.toList());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                for (Path compartmentOutputFolder : compartmentOutputFolders) {
                    Files.walkFileTree(compartmentOutputFolder, new FileVisitor<Path>() {
                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                            Path internalPath = compartmentOutputFolder.relativize(dir);
                            Path absolutePath = finalOutputPath.resolve(internalPath);
                            if (!Files.isDirectory(absolutePath))
                                Files.createDirectories(absolutePath);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            Path internalPath = compartmentOutputFolder.relativize(file);
                            Path absolutePath = finalOutputPath.resolve(internalPath);
                            progressInfo.log("Copying " + internalPath);
                            Files.copy(file, absolutePath);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                            return FileVisitResult.CONTINUE;
                        }
                    });
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            this.setPath(outputPath);
        }
        super.saveTo(storageFilePath, name, forceName, progressInfo);
    }
}
