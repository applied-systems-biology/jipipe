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
 *
 */

package org.hkijena.jipipe.extensions.ijweka.parameters.collections;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;

public class WekaTiling3DSettings implements JIPipeParameterCollection {

    private final EventBus eventBus = new EventBus();
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

    @JIPipeDocumentation(name = "Apply tiling", description = "If enabled, the input image is first split into tiles that are processed individually by the Weka segmentation. " +
            "This can greatly reduce the memory cost.")
    @JIPipeParameter(value = "apply-tiling", uiOrder = -100, important = true)
    public boolean isApplyTiling() {
        return applyTiling;
    }

    @JIPipeParameter("apply-tiling")
    public void setApplyTiling(boolean applyTiling) {
        this.applyTiling = applyTiling;
    }

    @JIPipeDocumentation(name = "Tile width", description = "Width of each tile")
    @JIPipeParameter("tile-size-x")
    public int getTileSizeX() {
        return tileSizeX;
    }

    @JIPipeParameter("tile-size-x")
    public void setTileSizeX(int tileSizeX) {
        this.tileSizeX = tileSizeX;
    }

    @JIPipeDocumentation(name = "Tile height", description = "Height of each tile")
    @JIPipeParameter("tile-size-y")
    public int getTileSizeY() {
        return tileSizeY;
    }

    @JIPipeParameter("tile-size-y")
    public void setTileSizeY(int tileSizeY) {
        this.tileSizeY = tileSizeY;
    }

    @JIPipeDocumentation(name = "Tile depth", description = "Depth of each tile")
    @JIPipeParameter("tile-size-z")
    public int getTileSizeZ() {
        return tileSizeZ;
    }

    @JIPipeParameter("tile-size-z")
    public void setTileSizeZ(int tileSizeZ) {
        this.tileSizeZ = tileSizeZ;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }
}
