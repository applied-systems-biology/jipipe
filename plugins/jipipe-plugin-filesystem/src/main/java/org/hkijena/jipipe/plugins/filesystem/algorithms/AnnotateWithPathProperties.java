/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.filesystem.algorithms;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.filesystem.dataypes.PathData;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

@SetJIPipeDocumentation(name = "Annotate with path properties", description = "Annotates files or directories with their properties")
@ConfigureJIPipeNode(menuPath = "For paths", nodeTypeCategory = AnnotationsNodeTypeCategory.class)
@AddJIPipeInputSlot(value = PathData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = PathData.class, slotName = "Output", create = true)
public class AnnotateWithPathProperties extends JIPipeSimpleIteratingAlgorithm {

    private OptionalTextAnnotationNameParameter fileNameAnnotation = new OptionalTextAnnotationNameParameter("File name", true);
    private OptionalTextAnnotationNameParameter parentPathAnnotation = new OptionalTextAnnotationNameParameter("Parent", true);
    private OptionalTextAnnotationNameParameter sizeAnnotation = new OptionalTextAnnotationNameParameter("Size", true);
    private OptionalTextAnnotationNameParameter typeAnnotation = new OptionalTextAnnotationNameParameter("Type", true);
    private OptionalTextAnnotationNameParameter lastModifiedTime = new OptionalTextAnnotationNameParameter("Last modified", true);


    public AnnotateWithPathProperties(JIPipeNodeInfo info) {
        super(info);
    }

    public AnnotateWithPathProperties(AnnotateWithPathProperties other) {
        super(other);
        this.fileNameAnnotation = new OptionalTextAnnotationNameParameter(other.fileNameAnnotation);
        this.parentPathAnnotation = new OptionalTextAnnotationNameParameter(other.parentPathAnnotation);
        this.sizeAnnotation = new OptionalTextAnnotationNameParameter(other.sizeAnnotation);
        this.typeAnnotation = new OptionalTextAnnotationNameParameter(other.typeAnnotation);
        this.lastModifiedTime = new OptionalTextAnnotationNameParameter(other.lastModifiedTime);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        List<JIPipeTextAnnotation> annotations = new ArrayList<>();
        PathData pathData = iterationStep.getInputData(getFirstInputSlot(), PathData.class, progressInfo);

        try {
            Path path = pathData.toPath();

            fileNameAnnotation.addAnnotationIfEnabled(annotations, path.getFileName().toString());
            parentPathAnnotation.addAnnotationIfEnabled(annotations, path.getParent().toString());
            if (typeAnnotation.isEnabled()) {
                String value = "Unknown";
                try {
                    if (Files.isRegularFile(path)) {
                        value = "File";
                    } else if (Files.isDirectory(path)) {
                        value = "Directory";
                    } else if (Files.isSymbolicLink(path)) {
                        value = "Link";
                    }
                } catch (Exception e) {
                }
                typeAnnotation.addAnnotationIfEnabled(annotations, value);
            }
            if (lastModifiedTime.isEnabled()) {
                String value = "Unknown";
                try {
                    value = Files.getLastModifiedTime(path).toString();
                } catch (IOException e) {
                }
                lastModifiedTime.addAnnotationIfEnabled(annotations, value);
            }
            if (sizeAnnotation.isEnabled()) {
                long[] size = new long[1];
                if (Files.isRegularFile(path)) {
                    size[0] = Files.size(path);
                } else {
                    Files.walkFileTree(path, new FileVisitor<Path>() {
                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            if (Files.isRegularFile(file))
                                size[0] += Files.size(file);
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
                sizeAnnotation.addAnnotationIfEnabled(annotations, "" + size[0]);
            }
        } catch (Exception e) {
            fileNameAnnotation.addAnnotationIfEnabled(annotations, "Error");
            parentPathAnnotation.addAnnotationIfEnabled(annotations, "Error");
            sizeAnnotation.addAnnotationIfEnabled(annotations, "Error");
            typeAnnotation.addAnnotationIfEnabled(annotations, "Error");
            lastModifiedTime.addAnnotationIfEnabled(annotations, "Error");
        }

        iterationStep.addOutputData(getFirstOutputSlot(), pathData, annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Annotate with file name", description = "If enabled, a file/directory name annotation is created")
    @JIPipeParameter("file-name-annotation")
    public OptionalTextAnnotationNameParameter getFileNameAnnotation() {
        return fileNameAnnotation;
    }

    @JIPipeParameter("file-name-annotation")
    public void setFileNameAnnotation(OptionalTextAnnotationNameParameter fileNameAnnotation) {
        this.fileNameAnnotation = fileNameAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with parent path", description = "If enabled, a parent path annotation is created")
    @JIPipeParameter("parent-path-annotation")
    public OptionalTextAnnotationNameParameter getParentPathAnnotation() {
        return parentPathAnnotation;
    }

    @JIPipeParameter("parent-path-annotation")
    public void setParentPathAnnotation(OptionalTextAnnotationNameParameter parentPathAnnotation) {
        this.parentPathAnnotation = parentPathAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with size", description = "If enabled, the path is annotated with its size in bytes. For directories, the size of the whole directory is returned.")
    @JIPipeParameter("size-annotation")
    public OptionalTextAnnotationNameParameter getSizeAnnotation() {
        return sizeAnnotation;
    }

    @JIPipeParameter("size-annotation")
    public void setSizeAnnotation(OptionalTextAnnotationNameParameter sizeAnnotation) {
        this.sizeAnnotation = sizeAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with type", description = "If enabled, the path type is annotated. <code>File</code> for files and <code>Directory</code> for directories.")
    @JIPipeParameter("type-annotation")
    public OptionalTextAnnotationNameParameter getTypeAnnotation() {
        return typeAnnotation;
    }

    @JIPipeParameter("type-annotation")
    public void setTypeAnnotation(OptionalTextAnnotationNameParameter typeAnnotation) {
        this.typeAnnotation = typeAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with last modification time", description = "If enabled, the path is annotated with its last modification time. The format is ISO 8601: <code>YYYY-MM-DDThh:mm:ss[.s+]Z</code>")
    @JIPipeParameter("last-modified-time-annotation")
    public OptionalTextAnnotationNameParameter getLastModifiedTime() {
        return lastModifiedTime;
    }

    @JIPipeParameter("last-modified-time-annotation")
    public void setLastModifiedTime(OptionalTextAnnotationNameParameter lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }
}
