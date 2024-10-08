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

package org.hkijena.jipipe.plugins.ijweka.parameters.collections;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;

public class WekaTiling3DSettings extends AbstractJIPipeParameterCollection {
    private boolean applyTiling = true;

    private int tileSizeX = 64;
    private int tileSizeY = 64;
    private int tileSizeZ = 64;


    public WekaTiling3DSettings() {

    }

    public WekaTiling3DSettings(WekaTiling3DSettings other) {
        this.applyTiling = other.applyTiling;
        this.tileSizeX = other.tileSizeX;
        this.tileSizeY = other.tileSizeY;
        this.tileSizeZ = other.tileSizeZ;
    }

    @SetJIPipeDocumentation(name = "Apply tiling", description = "If enabled, the input image is first split into tiles that are processed individually by the Weka segmentation. " +
            "This can greatly reduce the memory cost.")
    @JIPipeParameter(value = "apply-tiling", uiOrder = -100, important = true)
    public boolean isApplyTiling() {
        return applyTiling;
    }

    @JIPipeParameter("apply-tiling")
    public void setApplyTiling(boolean applyTiling) {
        this.applyTiling = applyTiling;
    }

    @SetJIPipeDocumentation(name = "Tile width", description = "Width of each tile")
    @JIPipeParameter("tile-size-x")
    public int getTileSizeX() {
        return tileSizeX;
    }

    @JIPipeParameter("tile-size-x")
    public void setTileSizeX(int tileSizeX) {
        this.tileSizeX = tileSizeX;
    }

    @SetJIPipeDocumentation(name = "Tile height", description = "Height of each tile")
    @JIPipeParameter("tile-size-y")
    public int getTileSizeY() {
        return tileSizeY;
    }

    @JIPipeParameter("tile-size-y")
    public void setTileSizeY(int tileSizeY) {
        this.tileSizeY = tileSizeY;
    }

    @SetJIPipeDocumentation(name = "Tile depth", description = "Depth of each tile")
    @JIPipeParameter("tile-size-z")
    public int getTileSizeZ() {
        return tileSizeZ;
    }

    @JIPipeParameter("tile-size-z")
    public void setTileSizeZ(int tileSizeZ) {
        this.tileSizeZ = tileSizeZ;
    }
}
