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
import org.hkijena.jipipe.api.algorithm.JIPipeNodeCategory;
import org.hkijena.jipipe.api.algorithm.JIPipeNodeInfo;
import org.hkijena.jipipe.api.algorithm.JIPipeDataBatch;
import org.hkijena.jipipe.api.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalStringParameter;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
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
@JIPipeDocumentation(name = "Import 2D image")
@JIPipeOrganization(algorithmCategory = JIPipeNodeCategory.DataSource)
public class ImagePlusFromFile extends JIPipeSimpleIteratingAlgorithm {

    private Class<? extends JIPipeData> dataClass;
    private OptionalStringParameter titleAnnotation = new OptionalStringParameter();

    /**
     * @param info algorithm info
     * @param dataClass   loaded data class
     */
    public ImagePlusFromFile(JIPipeNodeInfo info, Class<? extends JIPipeData> dataClass) {
        super(info,
                JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Files", FileData.class)
                        .addOutputSlot("Image", dataClass, "")
                        .sealOutput()
                        .sealInput()
                        .build());
        this.dataClass = dataClass;
        titleAnnotation.setContent("Image title");
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public ImagePlusFromFile(ImagePlusFromFile other) {
        super(other);
        this.dataClass = other.dataClass;
        this.titleAnnotation = new OptionalStringParameter(other.titleAnnotation);
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
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/annotation.png")
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
            imagePlus.getProcessor().setLut(null);
            return dataClass.getConstructor(ImagePlus.class).newInstance(imagePlus);
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
}
