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

package org.hkijena.jipipe.plugins.imagejdatatypes.algorithms;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ExportNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.OMEImageData;
import org.hkijena.jipipe.plugins.imagejdatatypes.parameters.OMEExporterSettings;

@SetJIPipeDocumentation(name = "Setup Bio-Formats exporter", description = "Sets the settings of an OME image that will be used to write the image into an *.ome.tif file.")
@ConfigureJIPipeNode(nodeTypeCategory = ExportNodeTypeCategory.class, menuPath = "Images")
@AddJIPipeInputSlot(value = OMEImageData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = OMEImageData.class, name = "Output", create = true)
public class SetBioFormatsExporterSettings extends JIPipeSimpleIteratingAlgorithm {

    private OMEExporterSettings exporterSettings = new OMEExporterSettings();

    public SetBioFormatsExporterSettings(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(exporterSettings);
    }

    public SetBioFormatsExporterSettings(SetBioFormatsExporterSettings other) {
        super(other);
        this.exporterSettings = new OMEExporterSettings(other.exporterSettings);
        registerSubParameter(exporterSettings);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        OMEImageData input = iterationStep.getInputData(getFirstInputSlot(), OMEImageData.class, progressInfo);
        OMEImageData output = (OMEImageData) input.duplicate(progressInfo);
        output.setExporterSettings(new OMEExporterSettings(exporterSettings));
        iterationStep.addOutputData(getFirstOutputSlot(), output, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Exporter settings", description = "The following settings control how files are exported:")
    @JIPipeParameter("ome-exporter-settings")
    public OMEExporterSettings getExporterSettings() {
        return exporterSettings;
    }
}
