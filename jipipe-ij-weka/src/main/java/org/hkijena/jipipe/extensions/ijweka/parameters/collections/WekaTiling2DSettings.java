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
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.transform.BorderMode;

public class WekaTiling2DSettings implements JIPipeParameterCollection {

    private final EventBus eventBus = new EventBus();
    private boolean applyTiling = true;

    private boolean useWekaNativeTiling = false;

    private BorderMode borderMode = BorderMode.Mirror;
    private int tileSizeX = 128;
    private int tileSizeY = 128;

    private int overlapX = 8;

    private int overlapY = 8;

    public WekaTiling2DSettings() {

    }

    public WekaTiling2DSettings(WekaTiling2DSettings other) {
        this.applyTiling = other.applyTiling;
        this.useWekaNativeTiling = other.useWekaNativeTiling;
        this.borderMode = other.borderMode;
        this.tileSizeX = other.tileSizeX;
        this.tileSizeY = other.tileSizeY;
        this.overlapX = other.overlapX;
        this.overlapY = other.overlapY;
    }

    @JIPipeDocumentation(name = "Use Weka's integrated tiling", description = "If enabled, the native Weka tiling algorithm is utilized. 'Border mode', 'Overlap (X)', and 'Overlap ('Y') do not have any effect in this case.")
    @JIPipeParameter(value = "use-weka-native-tiling", important = true)
    public boolean isUseWekaNativeTiling() {
        return useWekaNativeTiling;
    }

    @JIPipeParameter("use-weka-native-tiling")
    public void setUseWekaNativeTiling(boolean useWekaNativeTiling) {
        this.useWekaNativeTiling = useWekaNativeTiling;
    }

    @JIPipeDocumentation(name = "Apply tiling", description = "If enabled, the input image is first split into tiles that are processed individually by the Weka segmentation. " +
            "This can greatly reduce the memory cost. If borders are an issue, consider enabling the generation of a border.")
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

    @JIPipeDocumentation(name = "Border mode", description = "Determines how the border is generated if an overlap is applied. Has no effect if 'Use Weka's integrated tiling' is active.")
    @JIPipeParameter("border-mode")
    public BorderMode getBorderMode() {
        return borderMode;
    }

    @JIPipeParameter("border-mode")
    public void setBorderMode(BorderMode borderMode) {
        this.borderMode = borderMode;
    }

    @JIPipeDocumentation(name = "Overlap (X)", description = "If greater than zero, adds an overlap border around each tile to potentially remove issues when classification is applied close to the image border. Has no effect if 'Use Weka's integrated tiling' is active. ")
    @JIPipeParameter("overlap-x")
    public int getOverlapX() {
        return overlapX;
    }

    @JIPipeParameter("overlap-x")
    public boolean setOverlapX(int overlapX) {
        if (overlapX < 0)
            return false;
        this.overlapX = overlapX;
        return false;
    }

    @JIPipeDocumentation(name = "Overlap (Y)", description = "If greater than zero, adds an overlap border around each tile to potentially remove issues when classification is applied close to the image border. Has no effect if 'Use Weka's integrated tiling' is active.")
    @JIPipeParameter("overlap-y")
    public int getOverlapY() {
        return overlapY;
    }

    @JIPipeParameter("overlap-y")
    public boolean setOverlapY(int overlapY) {
        if (overlapY < 0)
            return false;
        this.overlapY = overlapY;
        return false;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }
}
