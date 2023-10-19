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
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ExportNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.OMEImageData;
import org.hkijena.jipipe.extensions.imagejdatatypes.parameters.OMEExporterSettings;

@JIPipeDocumentation(name = "Setup Bio-Formats exporter", description = "Sets the settings of an OME image that will be used to write the image into an *.ome.tif file.")
@JIPipeNode(nodeTypeCategory = ExportNodeTypeCategory.class, menuPath = "Images")
@JIPipeInputSlot(value = OMEImageData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = OMEImageData.class, slotName = "Output", autoCreate = true)
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        OMEImageData input = iterationStep.getInputData(getFirstInputSlot(), OMEImageData.class, progressInfo);
        OMEImageData output = (OMEImageData) input.duplicate(progressInfo);
        output.setExporterSettings(new OMEExporterSettings(exporterSettings));
        iterationStep.addOutputData(getFirstOutputSlot(), output, progressInfo);
    }

    @JIPipeDocumentation(name = "Exporter settings", description = "The following settings control how files are exported:")
    @JIPipeParameter("ome-exporter-settings")
    public OMEExporterSettings getExporterSettings() {
        return exporterSettings;
    }
}
