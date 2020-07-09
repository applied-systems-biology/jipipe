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

package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.roi;

import ij.ImagePlus;
import ij.process.FloatPolygon;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQDefaultMutableSlotConfiguration;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.acaq5.extensions.parameters.roi.Margin;

import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.acaq5.api.algorithm.ACAQIteratingAlgorithm.ITERATING_ALGORITHM_DESCRIPTION;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@ACAQDocumentation(name = "Remove ROI at borders", description = "Removes all ROI that intersect with image borders. Use the 'Border' parameter " +
        "to define a rectangle inside of the image dimensions. If a ROI is not contained within this region, it is removed." + "\n\n" + ITERATING_ALGORITHM_DESCRIPTION)
@ACAQOrganization(menuPath = "ROI", algorithmCategory = ACAQAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ROIListData.class, slotName = "ROI")
@AlgorithmInputSlot(value = ImagePlusData.class, slotName = "Image")
@AlgorithmOutputSlot(value = ROIListData.class, slotName = "Cleaned ROI")
public class RemoveBorderRoisAlgorithm extends ACAQIteratingAlgorithm {

    private Margin borderDefinition = new Margin();
    private RoiOutline outline = RoiOutline.ClosedPolygon;

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public RemoveBorderRoisAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQDefaultMutableSlotConfiguration.builder().addInputSlot("ROI", ROIListData.class)
                .addInputSlot("Image", ImagePlusData.class)
                .addOutputSlot("Cleaned ROI", ROIListData.class, null)
                .seal()
                .build());
        borderDefinition.setAnchor(Margin.Anchor.CenterCenter);
    }

    /**
     * Instantiates a new algorithm.
     *
     * @param other the other
     */
    public RemoveBorderRoisAlgorithm(RemoveBorderRoisAlgorithm other) {
        super(other);
        this.borderDefinition = new Margin(other.borderDefinition);
        this.outline = other.outline;
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ROIListData data = (ROIListData) dataInterface.getInputData("ROI", ROIListData.class).duplicate();
        data.outline(outline);
        ImagePlus reference = dataInterface.getInputData("Image", ImagePlusData.class).getImage();
        Rectangle inside = borderDefinition.apply(new Rectangle(0, 0, reference.getWidth(), reference.getHeight()));

        data.removeIf(roi -> {
            FloatPolygon fp = roi.getFloatPolygon();
            for (int i = 0; i < fp.npoints; i++) {
                if (!inside.contains((int) fp.xpoints[i], (int) fp.ypoints[i])) {
                    return true;
                }
            }
            return false;
        });

        dataInterface.addOutputData(getFirstOutputSlot(), data);
    }


    @Override
    public void reportValidity(ACAQValidityReport report) {
    }

    @ACAQDocumentation(name = "Border", description = "Defines the rectangle that is created within the image boundaries separate inside and outside. " +
            "If a ROI intersects with the outside area (meaning that it is not contained within the rectangle), it is removed.")
    @ACAQParameter("border-definition")
    public Margin getBorderDefinition() {
        return borderDefinition;
    }

    @ACAQParameter("border-definition")
    public void setBorderDefinition(Margin borderDefinition) {
        this.borderDefinition = borderDefinition;
    }

    @ACAQDocumentation(name = "Outline method", description = "Determines how ROI are preprocessed to obtain the extreme points (i.e. polygon stops)")
    @ACAQParameter("outline")
    public RoiOutline getOutline() {
        return outline;
    }

    @ACAQParameter("outline")
    public void setOutline(RoiOutline outline) {
        this.outline = outline;
    }
}
