package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.datasources;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.OMEImageData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.parameters.OMEExporterSettings;

@JIPipeDocumentation(name = "Image to OME Image", description = "Converts an image into an OME image. Optionally allows the attachment of ROI that are then stored within the OME image")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Image", autoCreate = true)
@JIPipeInputSlot(value = ROIListData.class, slotName = "ROI", autoCreate = true, optional = true)
@JIPipeOutputSlot(value = OMEImageData.class, slotName = "OME Image", autoCreate = true)
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlusData imagePlusData = dataBatch.getInputData("Image", ImagePlusData.class, progressInfo);
        ROIListData rois = dataBatch.getInputRow("ROI") >= 0 ? dataBatch.getInputData("ROI", ROIListData.class, progressInfo) : new ROIListData();
        OMEImageData omeImageData = new OMEImageData(imagePlusData.getImage(), rois, null);
        omeImageData.setExporterSettings(new OMEExporterSettings(exporterSettings));
        dataBatch.addOutputData(getFirstOutputSlot(), omeImageData, progressInfo);
    }

    @JIPipeDocumentation(name = "Exporter settings", description = "The following settings control how files are exported:")
    @JIPipeParameter("ome-exporter-settings")
    public OMEExporterSettings getExporterSettings() {
        return exporterSettings;
    }
}
