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

package org.hkijena.jipipe.extensions.imagejdatatypes.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ExportNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.OMEImageData;
import org.hkijena.jipipe.extensions.imagejdatatypes.parameters.OMEExporterSettings;

@JIPipeDocumentation(name = "Bio-Formats exporter", description = "Exports an image with Bio-Formats")
@JIPipeNode(nodeTypeCategory = ExportNodeTypeCategory.class, menuPath = "Images")
@JIPipeInputSlot(value = OMEImageData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = OMEImageData.class, slotName = "Output", autoCreate = true)
public class BioFormatsExporter extends JIPipeSimpleIteratingAlgorithm {

    private OMEExporterSettings exporterSettings = new OMEExporterSettings();

    public BioFormatsExporter(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(exporterSettings);
    }

    public BioFormatsExporter(BioFormatsExporter other) {
        super(other);
        this.exporterSettings = new OMEExporterSettings(other.exporterSettings);
        registerSubParameter(exporterSettings);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        OMEImageData input = dataBatch.getInputData(getFirstInputSlot(), OMEImageData.class, progressInfo);
        OMEImageData output = (OMEImageData) input.duplicate();
        output.setExporterSettings(new OMEExporterSettings(exporterSettings));
        dataBatch.addOutputData(getFirstOutputSlot(), output, progressInfo);
    }

    @JIPipeDocumentation(name = "Exporter settings", description = "The following settings control how files are exported:")
    @JIPipeParameter("ome-exporter-settings")
    public OMEExporterSettings getExporterSettings() {
        return exporterSettings;
    }
}
