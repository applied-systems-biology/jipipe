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
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.events.NodeSlotsChangedEvent;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalStringParameter;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.parameters.references.JIPipeDataInfoRef;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Loads an image data from a file via IJ.openFile()
 */
@JIPipeDocumentation(name = "Import image", description = "Loads an image via the native ImageJ functions. Please note that you might run into issues " +
        "if you open a file that is imported via Bio-Formats (for example .czi files). In such cases, please use the Bio-Formats importer algorithm.")
@JIPipeOrganization(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeInputSlot(FileData.class)
@JIPipeOutputSlot(ImagePlusData.class)
public class ImagePlusFromFile extends JIPipeSimpleIteratingAlgorithm {

    private JIPipeDataInfoRef generatedImageType = new JIPipeDataInfoRef("imagej-imgplus");
    private OptionalStringParameter titleAnnotation = new OptionalStringParameter();
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
        this.titleAnnotation = new OptionalStringParameter(other.titleAnnotation);
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        FileData fileData = dataBatch.getInputData(getFirstInputSlot(), FileData.class);
        ImagePlusData data = (ImagePlusData) readImageFrom(fileData.getPath());
        List<JIPipeAnnotation> traits = new ArrayList<>();
        if (titleAnnotation.isEnabled()) {
            traits.add(new JIPipeAnnotation(titleAnnotation.getContent(), data.getImage().getTitle()));
        }
        dataBatch.addOutputData(getFirstOutputSlot(), data, traits);
    }

    @JIPipeDocumentation(name = "Title annotation", description = "Optional annotation type where the image title is written.")
    @JIPipeParameter("title-annotation")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/annotation.png")
    public OptionalStringParameter getTitleAnnotation() {
        return titleAnnotation;
    }

    @JIPipeParameter("title-annotation")
    public void setTitleAnnotation(OptionalStringParameter titleAnnotation) {
        this.titleAnnotation = titleAnnotation;
    }

    /**
     * Loads an image from a file path
     *
     * @param fileName the image file name
     * @return the generated data
     */
    protected JIPipeData readImageFrom(Path fileName) {
        try {
            ImagePlus imagePlus = IJ.openImage(fileName.toString());
            if (removeLut) {
                imagePlus.getProcessor().setLut(null);
            }
            return generatedImageType.getInfo().getDataClass().getConstructor(ImagePlus.class).newInstance(imagePlus);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
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
}
