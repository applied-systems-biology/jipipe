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

package org.hkijena.jipipe.plugins.imagejdatatypes.algorithms.datasources;

import ij.IJ;
import ij.ImagePlus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.AddJIPipeCitation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.filesystem.dataypes.FileData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.OMEImageData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;
import org.hkijena.jipipe.plugins.parameters.library.references.JIPipeDataInfoRef;
import org.hkijena.jipipe.plugins.parameters.library.references.JIPipeDataParameterSettings;
import org.hkijena.jipipe.utils.CoreImageJUtils;
import org.hkijena.jipipe.utils.IJLogToJIPipeProgressInfoPump;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads an image data from a file via IJ.openFile()
 */
@SetJIPipeDocumentation(name = "Import image", description = "Loads an image via the native ImageJ functions.")
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@AddJIPipeInputSlot(value = FileData.class, name = "Files", description = "The image file", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Image", description = "Imported image", create = true)
@AddJIPipeCitation("Melissa Linkert, Curtis T. Rueden, Chris Allan, Jean-Marie Burel, Will Moore, Andrew Patterson, Brian Loranger, Josh Moore, " +
        "Carlos Neves, Donald MacDonald, Aleksandra Tarkowska, Caitlin Sticco, Emma Hill, Mike Rossner, Kevin W. Eliceiri, " +
        "and Jason R. Swedlow (2010) Metadata matters: access to image data in the real world. The Journal of Cell Biology 189(5), 777-782")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "File", aliasName = "Open (image)")
public class ImagePlusFromFile extends JIPipeSimpleIteratingAlgorithm {

    private JIPipeDataInfoRef generatedImageType = new JIPipeDataInfoRef("imagej-imgplus");
    private OptionalTextAnnotationNameParameter titleAnnotation = new OptionalTextAnnotationNameParameter();
    private boolean removeLut = false;
    private boolean removeOverlay = false;
    private boolean forceNativeImport = false;

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
        this.titleAnnotation = new OptionalTextAnnotationNameParameter(other.titleAnnotation);
        this.removeLut = other.removeLut;
        this.removeOverlay = other.removeOverlay;
        this.forceNativeImport = other.forceNativeImport;
    }

    /**
     * Loads an image from a file path
     *
     * @param fileName          the image file name
     * @param forceNativeImport forces the native IJ.open command. otherwise, Bio-Formats might be used
     * @param runContext        the run context
     * @param progressInfo      progress
     * @return the generated data
     */
    public static ImagePlus readImageFrom(Path fileName, boolean forceNativeImport, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus image;
        if (!forceNativeImport && !CoreImageJUtils.supportsNativeImageImport(fileName)) {
            // Pass to bioformats
            progressInfo.log("Using BioFormats importer. Please use the Bio-Formats importer node for more settings.");
            BioFormatsImporter importer = JIPipe.createNode(BioFormatsImporter.class);
            importer.getFirstInputSlot().addData(new FileData(fileName), progressInfo);
            importer.run(runContext, progressInfo);
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
            importer.run(runContext, progressInfo);
            image = importer.getFirstOutputSlot().getData(0, OMEImageData.class, progressInfo).getImage();
        }
        if (image == null) {
            throw new NullPointerException("Image could not be loaded!");
        }
        return image;
    }

    @SetJIPipeDocumentation(name = "Force native ImageJ importer", description = "If enabled, always use the native ImageJ file importer, even if the file looks like it can only be read by Bio-Formats")
    @JIPipeParameter("force-native-import")
    public boolean isForceNativeImport() {
        return forceNativeImport;
    }

    @JIPipeParameter("force-native-import")
    public void setForceNativeImport(boolean forceNativeImport) {
        this.forceNativeImport = forceNativeImport;
    }

    @SetJIPipeDocumentation(name = "Remove overlays", description = "If enabled, remove overlay ROIs from the imported image")
    @JIPipeParameter("remove-overlay")
    public boolean isRemoveOverlay() {
        return removeOverlay;
    }

    @JIPipeParameter("remove-overlay")
    public void setRemoveOverlay(boolean removeOverlay) {
        this.removeOverlay = removeOverlay;
    }

    @SetJIPipeDocumentation(name = "Remove LUT", description = "If enabled, remove the LUT information if present")
    @JIPipeParameter("remove-lut")
    public boolean isRemoveLut() {
        return removeLut;
    }

    @JIPipeParameter("remove-lut")
    public void setRemoveLut(boolean removeLut) {
        this.removeLut = removeLut;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        FileData fileData = iterationStep.getInputData(getFirstInputSlot(), FileData.class, progressInfo);
        ImagePlusData outputData;
        ImagePlus image = readImageFrom(fileData.toPath(), forceNativeImport, runContext, progressInfo);
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
        iterationStep.addOutputData(getFirstOutputSlot(), outputData, annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Title annotation", description = "Optional annotation type where the image title is written.")
    @JIPipeParameter("title-annotation")
    public OptionalTextAnnotationNameParameter getTitleAnnotation() {
        return titleAnnotation;
    }

    @JIPipeParameter("title-annotation")
    public void setTitleAnnotation(OptionalTextAnnotationNameParameter titleAnnotation) {
        this.titleAnnotation = titleAnnotation;
    }

    @SetJIPipeDocumentation(name = "Generated image type", description = "The image type that is generated.")
    @JIPipeParameter("generated-image-type")
    @JIPipeDataParameterSettings(dataBaseClass = ImagePlusData.class)
    public JIPipeDataInfoRef getGeneratedImageType() {
        return generatedImageType;
    }

    @JIPipeParameter("generated-image-type")
    public void setGeneratedImageType(JIPipeDataInfoRef generatedImageType) {
        this.generatedImageType = generatedImageType;
        getFirstOutputSlot().setAcceptedDataType(generatedImageType.getInfo().getDataClass());
        emitNodeSlotsChangedEvent();
    }
}
