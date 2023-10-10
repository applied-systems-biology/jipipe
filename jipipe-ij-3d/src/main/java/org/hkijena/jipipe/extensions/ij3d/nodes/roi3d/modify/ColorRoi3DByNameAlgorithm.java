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

package org.hkijena.jipipe.extensions.ij3d.nodes.roi3d.modify;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeSingleDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3D;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.extensions.parameters.api.enums.EnumParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.colors.ColorMapEnumItemInfo;
import org.hkijena.jipipe.extensions.parameters.library.colors.OptionalColorMapParameter;
import org.hkijena.jipipe.utils.NaturalOrderComparator;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@JIPipeDocumentation(name = "Color 3D ROI by name", description = "Sets the 3D ROI item colors by their name.")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Modify")
@JIPipeInputSlot(value = ROI3DListData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ROI3DListData.class, slotName = "Output", autoCreate = true)
public class ColorRoi3DByNameAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    private OptionalColorMapParameter mapFillColor = new OptionalColorMapParameter();

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public ColorRoi3DByNameAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public ColorRoi3DByNameAlgorithm(ColorRoi3DByNameAlgorithm other) {
        super(other);
        this.mapFillColor = new OptionalColorMapParameter(other.mapFillColor);
    }

    @Override
    protected void runIteration(JIPipeSingleDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ROI3DListData rois = new ROI3DListData(dataBatch.getInputData(getFirstInputSlot(), ROI3DListData.class, progressInfo));
        Set<String> names = new HashSet<>();
        for (ROI3D roi : rois) {
            names.add(StringUtils.nullToEmpty(roi.getObject3D().getName()));
        }
        ArrayList<String> sortedNames = new ArrayList<>(names);
        sortedNames.sort(NaturalOrderComparator.INSTANCE);
        for (ROI3D roi : rois) {
            double relativeName = 1.0 * sortedNames.indexOf(StringUtils.nullToEmpty(roi.getObject3D().getName())) / (sortedNames.size() - 1);
            if (mapFillColor.isEnabled()) {
                roi.setFillColor(mapFillColor.getContent().apply(relativeName));
            }
        }
        dataBatch.addOutputData(getFirstOutputSlot(), rois, progressInfo);
    }

    @JIPipeDocumentation(name = "Map fill color", description = "Allows you to map the 3D ROI fill color to the order generated by the sorting. " +
            "The color is rendered when converting into a RGB visualization.")
    @JIPipeParameter("map-fill-color")
    @EnumParameterSettings(itemInfo = ColorMapEnumItemInfo.class)
    public OptionalColorMapParameter getMapFillColor() {
        return mapFillColor;
    }

    @JIPipeParameter("map-fill-color")
    public void setMapFillColor(OptionalColorMapParameter mapFillColor) {
        this.mapFillColor = mapFillColor;
    }

}
