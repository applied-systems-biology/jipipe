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

package org.hkijena.jipipe.plugins.imagejdatatypes.algorithms.io;

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
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.filesystem.dataypes.FileData;
import org.hkijena.jipipe.plugins.imagejdatatypes.algorithms.io.backends.BioFormatsImageImageBackend;
import org.hkijena.jipipe.plugins.imagejdatatypes.algorithms.io.backends.ImageJImportImageBackend;
import org.hkijena.jipipe.plugins.imagejdatatypes.algorithms.io.backends.ImportImageBackend;
import org.hkijena.jipipe.plugins.imagejdatatypes.algorithms.io.backends.JavaImportImageBackend;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.OMEImageData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;
import org.hkijena.jipipe.plugins.parameters.library.references.JIPipeDataInfoRef;
import org.hkijena.jipipe.plugins.parameters.library.references.JIPipeDataParameterSettings;
import org.hkijena.jipipe.utils.CoreImageJUtils;
import org.hkijena.jipipe.utils.IJLogToJIPipeProgressInfoPump;
import org.hkijena.jipipe.utils.VersionUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@SetJIPipeDocumentation(name = "ImageJ importer", description = "Loads an image using the standard ImageJ importer. Will not use Bio-Formats or other alternatives.")
@AddJIPipeNodeAlias(nodeTypeCategory = DataSourceNodeTypeCategory.class, aliasName = "Import image (ImageJ)")
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@AddJIPipeInputSlot(value = FileData.class, name = "Files", description = "The image file", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Image", description = "Imported image", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "File", aliasName = "Open (image)")
public class NativeImageJImporterAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private JIPipeDataInfoRef generatedImageType = new JIPipeDataInfoRef("imagej-imgplus");
    private OptionalTextAnnotationNameParameter titleAnnotation = new OptionalTextAnnotationNameParameter();
    private boolean removeLut = false;
    private boolean removeOverlay = false;

    public NativeImageJImporterAlgorithm(JIPipeNodeInfo info) {
        super(info);
        titleAnnotation.setContent("Image title");
    }

    public NativeImageJImporterAlgorithm(NativeImageJImporterAlgorithm other) {
        super(other);
        setGeneratedImageType(new JIPipeDataInfoRef(other.generatedImageType));
        this.titleAnnotation = new OptionalTextAnnotationNameParameter(other.titleAnnotation);
        this.removeLut = other.removeLut;
        this.removeOverlay = other.removeOverlay;
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
        ImagePlus image = IJ.openImage(fileData.getPath());
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
