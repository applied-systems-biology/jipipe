package org.hkijena.jipipe.extensions.filesystem.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.utils.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

@JIPipeDocumentation(name = "Annotate with path properties", description = "Annotates files or directories with their properties")
@JIPipeNode(menuPath = "For paths", nodeTypeCategory = AnnotationsNodeTypeCategory.class)
@JIPipeInputSlot(value = PathData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = PathData.class, slotName = "Output", autoCreate = true)
public class AnnotateWithPathProperties extends JIPipeSimpleIteratingAlgorithm {

    private OptionalAnnotationNameParameter fileNameAnnotation = new OptionalAnnotationNameParameter("File name", true);
    private OptionalAnnotationNameParameter parentPathAnnotation = new OptionalAnnotationNameParameter("Parent", true);
    private OptionalAnnotationNameParameter sizeAnnotation = new OptionalAnnotationNameParameter("Size", true);
    private OptionalAnnotationNameParameter typeAnnotation = new OptionalAnnotationNameParameter("Type", true);
    private OptionalAnnotationNameParameter lastModifiedTime = new OptionalAnnotationNameParameter("Last modified", true);


    public AnnotateWithPathProperties(JIPipeNodeInfo info) {
        super(info);
    }

    public AnnotateWithPathProperties(AnnotateWithPathProperties other) {
        super(other);
        this.fileNameAnnotation = new OptionalAnnotationNameParameter(other.fileNameAnnotation);
        this.parentPathAnnotation = new OptionalAnnotationNameParameter(other.parentPathAnnotation);
        this.sizeAnnotation = new OptionalAnnotationNameParameter(other.sizeAnnotation);
        this.typeAnnotation = new OptionalAnnotationNameParameter(other.typeAnnotation);
        this.lastModifiedTime = new OptionalAnnotationNameParameter(other.lastModifiedTime);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        List<JIPipeTextAnnotation> annotations = new ArrayList<>();
        PathData pathData = dataBatch.getInputData(getFirstInputSlot(), PathData.class, progressInfo);

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

        dataBatch.addOutputData(getFirstOutputSlot(), pathData, annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
    }

    @JIPipeDocumentation(name = "Annotate with file name", description = "If enabled, a file/directory name annotation is created")
    @JIPipeParameter("file-name-annotation")
    public OptionalAnnotationNameParameter getFileNameAnnotation() {
        return fileNameAnnotation;
    }

    @JIPipeParameter("file-name-annotation")
    public void setFileNameAnnotation(OptionalAnnotationNameParameter fileNameAnnotation) {
        this.fileNameAnnotation = fileNameAnnotation;
    }

    @JIPipeDocumentation(name = "Annotate with parent path", description = "If enabled, a parent path annotation is created")
    @JIPipeParameter("parent-path-annotation")
    public OptionalAnnotationNameParameter getParentPathAnnotation() {
        return parentPathAnnotation;
    }

    @JIPipeParameter("parent-path-annotation")
    public void setParentPathAnnotation(OptionalAnnotationNameParameter parentPathAnnotation) {
        this.parentPathAnnotation = parentPathAnnotation;
    }

    @JIPipeDocumentation(name = "Annotate with size", description = "If enabled, the path is annotated with its size in bytes. For directories, the size of the whole directory is returned.")
    @JIPipeParameter("size-annotation")
    public OptionalAnnotationNameParameter getSizeAnnotation() {
        return sizeAnnotation;
    }

    @JIPipeParameter("size-annotation")
    public void setSizeAnnotation(OptionalAnnotationNameParameter sizeAnnotation) {
        this.sizeAnnotation = sizeAnnotation;
    }

    @JIPipeDocumentation(name = "Annotate with type", description = "If enabled, the path type is annotated. <code>File</code> for files and <code>Directory</code> for directories.")
    @JIPipeParameter("type-annotation")
    public OptionalAnnotationNameParameter getTypeAnnotation() {
        return typeAnnotation;
    }

    @JIPipeParameter("type-annotation")
    public void setTypeAnnotation(OptionalAnnotationNameParameter typeAnnotation) {
        this.typeAnnotation = typeAnnotation;
    }

    @JIPipeDocumentation(name = "Annotate with last modification time", description = "If enabled, the path is annotated with its last modification time. The format is ISO 8601: <code>YYYY-MM-DDThh:mm:ss[.s+]Z</code>")
    @JIPipeParameter("last-modified-time-annotation")
    public OptionalAnnotationNameParameter getLastModifiedTime() {
        return lastModifiedTime;
    }

    @JIPipeParameter("last-modified-time-annotation")
    public void setLastModifiedTime(OptionalAnnotationNameParameter lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }
}
