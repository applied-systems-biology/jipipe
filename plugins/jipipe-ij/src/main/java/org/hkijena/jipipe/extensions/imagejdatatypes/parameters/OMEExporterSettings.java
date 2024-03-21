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

package org.hkijena.jipipe.extensions.imagejdatatypes.parameters;

import ome.xml.model.enums.DimensionOrder;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;

public class OMEExporterSettings extends AbstractJIPipeParameterCollection {
    private boolean splitZ = false;
    private boolean splitC = false;
    private boolean splitT = false;
    private boolean padded = false;
    private boolean saveROI = true;
    private boolean noLUT = true;
    private OMETIFFCompression compression = OMETIFFCompression.Uncompressed;
    private DimensionOrder dimensionOrder = DimensionOrder.XYCZT;

    public OMEExporterSettings() {

    }

    public OMEExporterSettings(OMEExporterSettings other) {
        this.splitZ = other.splitZ;
        this.splitC = other.splitC;
        this.splitT = other.splitT;
        this.padded = other.padded;
        this.saveROI = other.saveROI;
        this.noLUT = other.noLUT;
        this.compression = other.compression;
        this.dimensionOrder = other.dimensionOrder;
    }

    @SetJIPipeDocumentation(name = "Split Z into files", description = "If enabled, each Z is written into its own file")
    @JIPipeParameter("split-z")
    public boolean isSplitZ() {
        return splitZ;
    }

    @JIPipeParameter("split-z")
    public void setSplitZ(boolean splitZ) {
        this.splitZ = splitZ;
    }

    @SetJIPipeDocumentation(name = "Split channels into files", description = "If enabled, each channel is written into its own file")
    @JIPipeParameter("split-c")
    public boolean isSplitC() {
        return splitC;
    }

    @JIPipeParameter("split-c")
    public void setSplitC(boolean splitC) {
        this.splitC = splitC;
    }

    @SetJIPipeDocumentation(name = "Split frames into files", description = "If enabled, each time frame is written into its own file")
    @JIPipeParameter("split-t")
    public boolean isSplitT() {
        return splitT;
    }

    @JIPipeParameter("split-t")
    public void setSplitT(boolean splitT) {
        this.splitT = splitT;
    }

    @SetJIPipeDocumentation(name = "Zero-pad file names", description = "If enabled, file names are zero-padded")
    @JIPipeParameter("zero-padding")
    public boolean isPadded() {
        return padded;
    }

    @JIPipeParameter("zero-padding")
    public void setPadded(boolean padded) {
        this.padded = padded;
    }

    @SetJIPipeDocumentation(name = "Save ROIs", description = "If enabled, ROI are saved into the OME TIFF file")
    @JIPipeParameter("save-roi")
    public boolean isSaveROI() {
        return saveROI;
    }

    @JIPipeParameter("save-roi")
    public void setSaveROI(boolean saveROI) {
        this.saveROI = saveROI;
    }

    @SetJIPipeDocumentation(name = "No LUT", description = "If enabled, no LUT information is saved into the OME TIFF file")
    @JIPipeParameter("no-lut")
    public boolean isNoLUT() {
        return noLUT;
    }

    @JIPipeParameter("no-lut")
    public void setNoLUT(boolean noLUT) {
        this.noLUT = noLUT;
    }

    @SetJIPipeDocumentation(name = "Compression", description = "The compression method that should be applied")
    @JIPipeParameter("compression")
    public OMETIFFCompression getCompression() {
        return compression;
    }

    @JIPipeParameter("compression")
    public void setCompression(OMETIFFCompression compression) {
        this.compression = compression;
    }

    @SetJIPipeDocumentation(name = "Stack order", description = "In which order stacks are saved")
    @JIPipeParameter("stack-order")
    public DimensionOrder getDimensionOrder() {
        return dimensionOrder;
    }

    @JIPipeParameter("stack-order")
    public void setDimensionOrder(DimensionOrder dimensionOrder) {
        this.dimensionOrder = dimensionOrder;
    }
}
