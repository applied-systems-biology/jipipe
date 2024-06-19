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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.datasources;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.OMEImageData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.parameters.OMEExporterSettings;

@SetJIPipeDocumentation(name = "Image to OME Image", description = "Converts an image into an OME image. Optionally allows the attachment of ROI that are then stored within the OME image")
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Image", create = true)
@AddJIPipeInputSlot(value = ROIListData.class, name = "ROI", create = true, optional = true)
@AddJIPipeOutputSlot(value = OMEImageData.class, slotName = "OME Image", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Plugins\nBio-Formats", aliasName = "Bio-Formats Exporter (automated export)")
public class OMEImageFromImagePlus extends JIPipeIteratingAlgorithm {

    private OMEExporterSettings exporterSettings = new OMEExporterSettings();

    public OMEImageFromImagePlus(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(exporterSettings);
    }

    public OMEImageFromImagePlus(OMEImageFromImagePlus other) {
        super(other);
        this.exporterSettings = new OMEExporterSettings(other.exporterSettings);
        registerSubParameter(exporterSettings);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData imagePlusData = iterationStep.getInputData("Image", ImagePlusData.class, progressInfo);
        ROIListData rois = iterationStep.getInputRow("ROI") >= 0 ? iterationStep.getInputData("ROI", ROIListData.class, progressInfo) : new ROIListData();
        OMEImageData omeImageData = new OMEImageData(imagePlusData.getImage(), rois, null);
        omeImageData.setExporterSettings(new OMEExporterSettings(exporterSettings));
        iterationStep.addOutputData(getFirstOutputSlot(), omeImageData, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Exporter settings", description = "The following settings control how files are exported:")
    @JIPipeParameter("ome-exporter-settings")
    public OMEExporterSettings getExporterSettings() {
        return exporterSettings;
    }
}
