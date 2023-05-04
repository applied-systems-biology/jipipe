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

package org.hkijena.jipipe.extensions.imagejdatatypes.algorithms.datasources;

import ij.IJ;
import ij.ImagePlus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.*;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.OMEImageData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;
import org.hkijena.jipipe.extensions.parameters.library.references.JIPipeDataInfoRef;
import org.hkijena.jipipe.extensions.parameters.library.references.JIPipeDataParameterSettings;
import org.hkijena.jipipe.utils.IJLogToJIPipeProgressInfoPump;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads an image data from a file via IJ.openFile()
 */
@JIPipeDocumentation(name = "Import image", description = "Loads an image via the native ImageJ functions.")
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeInputSlot(value = FileData.class, slotName = "Files", description = "The image file", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Image", description = "Imported image", autoCreate = true)
@JIPipeCitation("Melissa Linkert, Curtis T. Rueden, Chris Allan, Jean-Marie Burel, Will Moore, Andrew Patterson, Brian Loranger, Josh Moore, " +
        "Carlos Neves, Donald MacDonald, Aleksandra Tarkowska, Caitlin Sticco, Emma Hill, Mike Rossner, Kevin W. Eliceiri, " +
        "and Jason R. Swedlow (2010) Metadata matters: access to image data in the real world. The Journal of Cell Biology 189(5), 777-782")
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "File", aliasName = "Open (image)")
public class ImagePlusFromFile extends JIPipeSimpleIteratingAlgorithm {

    private JIPipeDataInfoRef generatedImageType = new JIPipeDataInfoRef("imagej-imgplus");
    private OptionalAnnotationNameParameter titleAnnotation = new OptionalAnnotationNameParameter();
    private boolean removeLut = false;
    private boolean removeOverlay = false;

    /**
     * @param info algorithm info
     */
    public ImagePlusFromFile(JIPipeNodeInfo info) {
        super(info);
        titleAnnotation.setContent("Image title");
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public ImagePlusFromFile(ImagePlusFromFile other) {
        super(other);
        setGeneratedImageType(new JIPipeDataInfoRef(other.generatedImageType));
        this.titleAnnotation = new OptionalAnnotationNameParameter(other.titleAnnotation);
        this.removeLut = other.removeLut;
        this.removeOverlay = other.removeOverlay;
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
            importer.getFirstInputSlot().addData(new FileData(fileName), progressInfo);
            importer.run(progressInfo);
            image = importer.getFirstOutputSlot().getData(0, OMEImageData.class, progressInfo).getImage();
        } else {
            try (IJLogToJIPipeProgressInfoPump pump = new IJLogToJIPipeProgressInfoPump(progressInfo)) {
                image = IJ.openImage(fileName.toString());
            }
        }
        if (image == null) {
            // Try Bioformats again?
            // Pass to bioformats
            progressInfo.log("Using BioFormats importer. Please use the Bio-Formats importer node for more settings.");
            BioFormatsImporter importer = JIPipe.createNode(BioFormatsImporter.class);
            importer.getFirstInputSlot().addData(new FileData(fileName), progressInfo);
            importer.run(progressInfo);
            image = importer.getFirstOutputSlot().getData(0, OMEImageData.class, progressInfo).getImage();
        }
        if (image == null) {
            throw new NullPointerException("Image could not be loaded!");
        }
        return image;
    }

    @JIPipeDocumentation(name = "Remove overlays", description = "If enabled, remove overlay ROIs from the imported image")
    @JIPipeParameter("remove-overlay")
    public boolean isRemoveOverlay() {
        return removeOverlay;
    }

    @JIPipeParameter("remove-overlay")
    public void setRemoveOverlay(boolean removeOverlay) {
        this.removeOverlay = removeOverlay;
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
        ImagePlusData outputData;
        ImagePlus image = readImageFrom(fileData.toPath(), progressInfo);
        if (removeLut) {
            ImageJUtils.removeLUT(image, null);
        }
        if (removeOverlay) {
            ImageJUtils.removeOverlay(image);
        }
        outputData = (ImagePlusData) JIPipe.createData(generatedImageType.getInfo().getDataClass(), image);
        List<JIPipeTextAnnotation> annotations = new ArrayList<>();
        if (titleAnnotation.isEnabled()) {
            annotations.add(new JIPipeTextAnnotation(titleAnnotation.getContent(), outputData.getImage().getTitle()));
        }
        dataBatch.addOutputData(getFirstOutputSlot(), outputData, annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
    }

    @JIPipeDocumentation(name = "Title annotation", description = "Optional annotation type where the image title is written.")
    @JIPipeParameter("title-annotation")
    public OptionalAnnotationNameParameter getTitleAnnotation() {
        return titleAnnotation;
    }

    @JIPipeParameter("title-annotation")
    public void setTitleAnnotation(OptionalAnnotationNameParameter titleAnnotation) {
        this.titleAnnotation = titleAnnotation;
    }

    @Override
    public void reportValidity(JIPipeIssueReport report) {
        if (titleAnnotation.isEnabled()) {
            report.resolve("Title annotation").checkNonEmpty(titleAnnotation.getContent(), this);
        }
    }

    @JIPipeDocumentation(name = "Generated image type", description = "The image type that is generated.")
    @JIPipeParameter("generated-image-type")
    @JIPipeDataParameterSettings(dataBaseClass = ImagePlusData.class)
    public JIPipeDataInfoRef getGeneratedImageType() {
        return generatedImageType;
    }

    @JIPipeParameter("generated-image-type")
    public void setGeneratedImageType(JIPipeDataInfoRef generatedImageType) {
        this.generatedImageType = generatedImageType;
        getFirstOutputSlot().setAcceptedDataType(generatedImageType.getInfo().getDataClass());
        getEventBus().post(new JIPipeGraph.NodeSlotsChangedEvent(this));
    }
}
