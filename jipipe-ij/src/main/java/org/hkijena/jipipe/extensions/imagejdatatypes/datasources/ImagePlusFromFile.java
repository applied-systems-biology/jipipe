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

package org.hkijena.jipipe.extensions.imagejdatatypes.datasources;

import ij.IJ;
import ij.ImagePlus;
import org.apache.commons.lang3.SystemUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeVirtualData;
import org.hkijena.jipipe.api.events.NodeSlotsChangedEvent;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.OMEImageData;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalAnnotationNameParameter;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalStringParameter;
import org.hkijena.jipipe.extensions.parameters.references.JIPipeDataInfoRef;
import org.hkijena.jipipe.extensions.settings.VirtualDataSettings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads an image data from a file via IJ.openFile()
 */
@JIPipeDocumentation(name = "Import image", description = "Loads an image via the native ImageJ functions.")
@JIPipeOrganization(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeInputSlot(FileData.class)
@JIPipeOutputSlot(ImagePlusData.class)
public class ImagePlusFromFile extends JIPipeSimpleIteratingAlgorithm {

    private JIPipeDataInfoRef generatedImageType = new JIPipeDataInfoRef("imagej-imgplus");
    private OptionalAnnotationNameParameter titleAnnotation = new OptionalAnnotationNameParameter();
    private boolean removeLut = false;

    /**
     * @param info algorithm info
     */
    public ImagePlusFromFile(JIPipeNodeInfo info) {
        super(info,
                JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Files", FileData.class)
                        .addOutputSlot("Image", ImagePlusData.class, null)
                        .sealOutput()
                        .sealInput()
                        .build());
        titleAnnotation.setContent("Image title");
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public ImagePlusFromFile(ImagePlusFromFile other) {
        super(other);
        this.generatedImageType = new JIPipeDataInfoRef(other.generatedImageType);
        this.titleAnnotation = new OptionalAnnotationNameParameter(other.titleAnnotation);
        this.removeLut = other.removeLut;
    }

    @JIPipeDocumentation(name = "Remove LUT", description = "If enabled, remove the LUT information if present")
    @JIPipeParameter("remove-lut")
    public boolean isRemoveLut() {
        return removeLut;
    }

    @JIPipeParameter("remove-lut")
    public void setRemoveLut(boolean removeLut) {
        this.removeLut = removeLut;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        FileData fileData = dataBatch.getInputData(getFirstInputSlot(), FileData.class, progressInfo);
        boolean enableVirtual = VirtualDataSettings.getInstance().isVirtualMode();
        if (enableVirtual && !removeLut && fileData.getPath().toString().endsWith(".tif") && getFirstOutputSlot().isVirtual()) {
            // Alternative path for virtual data to get rid of load-saving-load
            // Only works for something that is directly compatible to the row storage format (TIFF)
            List<JIPipeAnnotation> traits = new ArrayList<>(dataBatch.getAnnotations().values());
            if (titleAnnotation.isEnabled()) {
                traits.add(new JIPipeAnnotation(titleAnnotation.getContent(), fileData.toPath().getFileName().toString()));
            }
            Path targetPath = VirtualDataSettings.generateTempDirectory("virtual");
            if (!SystemUtils.IS_OS_WINDOWS) {
                // Create a symlink
                try {
                    Files.createSymbolicLink(targetPath.resolve("image.tif"), fileData.toPath());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                // Make a copy
                try {
                    Files.copy(fileData.toPath(), targetPath.resolve("imag.tif"), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            JIPipeVirtualData virtualData = new JIPipeVirtualData(generatedImageType.getInfo().getDataClass(), targetPath, "VIRTUAL: " + fileData.toPath().getFileName());
            getFirstOutputSlot().addData(virtualData, traits, JIPipeAnnotationMergeStrategy.Merge);
        } else {
            ImagePlusData outputData;
            ImagePlus image = readImageFrom(fileData.toPath(), progressInfo);
            if (removeLut) {
                image.getProcessor().setLut(null);
            }
            outputData = (ImagePlusData) JIPipe.createData(generatedImageType.getInfo().getDataClass(), image);
            List<JIPipeAnnotation> traits = new ArrayList<>();
            if (titleAnnotation.isEnabled()) {
                traits.add(new JIPipeAnnotation(titleAnnotation.getContent(), outputData.getImage().getTitle()));
            }
            dataBatch.addOutputData(getFirstOutputSlot(), outputData, traits, JIPipeAnnotationMergeStrategy.Merge, progressInfo);
        }
    }

    @JIPipeDocumentation(name = "Title annotation", description = "Optional annotation type where the image title is written.")
    @JIPipeParameter("title-annotation")
    public OptionalStringParameter getTitleAnnotation() {
        return titleAnnotation;
    }

    @JIPipeParameter("title-annotation")
    public void setTitleAnnotation(OptionalAnnotationNameParameter titleAnnotation) {
        this.titleAnnotation = titleAnnotation;
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        if (titleAnnotation.isEnabled()) {
            report.forCategory("Title annotation").checkNonEmpty(titleAnnotation.getContent(), this);
        }
    }

    @JIPipeDocumentation(name = "Generated image type", description = "The image type that is generated.")
    @JIPipeParameter("generated-image-type")
    public JIPipeDataInfoRef getGeneratedImageType() {
        return generatedImageType;
    }

    @JIPipeParameter("generated-image-type")
    public void setGeneratedImageType(JIPipeDataInfoRef generatedImageType) {
        this.generatedImageType = generatedImageType;
        getFirstOutputSlot().setAcceptedDataType(generatedImageType.getInfo().getDataClass());
        getEventBus().post(new NodeSlotsChangedEvent(this));
    }

    /**
     * Loads an image from a file path
     *
     * @param fileName     the image file name
     * @param progressInfo progress
     * @return the generated data
     */
    public static ImagePlus readImageFrom(Path fileName, JIPipeProgressInfo progressInfo) {
        ImagePlus image;
        String fileNameString = fileName.getFileName().toString();
        if (fileNameString.endsWith(".ome.tiff") || fileNameString.endsWith(".ome.tif") || fileNameString.endsWith(".czi")) {
            // Pass to bioformats
            progressInfo.log("Using BioFormats importer. Please use the Bio-Formats importer node for more settings.");
            BioFormatsImporter importer = JIPipe.createNode(BioFormatsImporter.class);
            importer.setAllSlotsVirtual(false, false, null);
            importer.getFirstInputSlot().addData(new FileData(fileName), progressInfo);
            importer.run(progressInfo);
            image = importer.getFirstOutputSlot().getData(0, OMEImageData.class, progressInfo).getImage();
        } else {
            image = IJ.openImage(fileName.toString());
        }
        if (image == null) {
            // Try Bioformats again?
            // Pass to bioformats
            progressInfo.log("Using BioFormats importer. Please use the Bio-Formats importer node for more settings.");
            BioFormatsImporter importer = JIPipe.createNode(BioFormatsImporter.class);
            importer.setAllSlotsVirtual(false, false, null);
            importer.getFirstInputSlot().addData(new FileData(fileName), progressInfo);
            importer.run(progressInfo);
            image = importer.getFirstOutputSlot().getData(0, OMEImageData.class, progressInfo).getImage();
        }
        if (image == null) {
            throw new NullPointerException("Image could not be loaded!");
        }
        return image;
    }
}
