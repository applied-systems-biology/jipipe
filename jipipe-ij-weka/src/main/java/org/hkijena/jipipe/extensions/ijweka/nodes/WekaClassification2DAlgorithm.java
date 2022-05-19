package org.hkijena.jipipe.extensions.ijweka.nodes;

import com.google.common.eventbus.EventBus;
import ij.ImagePlus;
import ij.ImageStack;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.ijweka.datatypes.WekaModelData;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.transform.AddBorder2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.transform.BorderMode;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import trainableSegmentation.WekaSegmentation;

@JIPipeDocumentation(name = "Weka classifier 2D", description = "Classifies an image with a Weka model. If higher-dimensional data is provided, the classification is applied per slice. To obtain ROI from the generated labels, utilize the 'Labels to ROI' node.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Weka")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Image", description = "Image on which the classification should be applied", autoCreate = true)
@JIPipeInputSlot(value = WekaModelData.class, slotName = "Model", description = "The model", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Classified image", description = "The classified image", autoCreate = true)
public class WekaClassification2DAlgorithm extends JIPipeIteratingAlgorithm {

    private BorderParameters borderParameters = new BorderParameters();

    private TilingParameters tilingParameters = new TilingParameters();

    public WekaClassification2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(borderParameters);
        registerSubParameter(tilingParameters);
    }

    public WekaClassification2DAlgorithm(WekaClassification2DAlgorithm other) {
        super(other);
        this.borderParameters = new BorderParameters(other.borderParameters);
        this.tilingParameters = new TilingParameters(other.tilingParameters);
        registerSubParameter(borderParameters);
        registerSubParameter(tilingParameters);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlusData image = dataBatch.getInputData("Image", ImagePlusData.class, progressInfo);
        WekaModelData modelData = dataBatch.getInputData("Model", WekaModelData.class, progressInfo);
        WekaSegmentation segmentation = modelData.getSegmentation();

        ImageStack stack = new ImageStack(image.getWidth(), image.getHeight(), image.getNSlices() * image.getNChannels() * image.getNChannels());
        ImageJUtils.forEachIndexedZCTSlice(image.getImage(), (ip, index) -> {
            ImagePlus slice = new ImagePlus(image.getImage().getTitle() + " " + index, ip);
            ImagePlus classified = segmentation.applyClassifier(slice);
            stack.setProcessor(classified.getProcessor(), index.zeroSliceIndexToOneStackIndex(image.getImage()));
        }, progressInfo);

        dataBatch.addOutputData("Classified image", new ImagePlusData(new ImagePlus("Classified", stack)), progressInfo);
    }

    @JIPipeDocumentation(name = "Generate border", description = "The following settings allow the generation of a border around images/tiles to avoid issues with pixels close the the image limits.")
    @JIPipeParameter("border-parameters")
    public BorderParameters getBorderParameters() {
        return borderParameters;
    }

    @JIPipeDocumentation(name = "Generate tiles", description = "The following settings allow the generation of tiles to save memory.")
    @JIPipeParameter("tiling-parameters")
    public TilingParameters getTilingParameters() {
        return tilingParameters;
    }

    public static class BorderParameters  implements JIPipeParameterCollection {

        private final EventBus eventBus = new EventBus();
        private boolean applyBorder = false;

        private BorderMode borderMode = BorderMode.Mirror;

        private int borderSize = 16;

        public BorderParameters() {
        }

        public BorderParameters(BorderParameters other) {
            this.applyBorder = other.applyBorder;
            this.borderMode = other.borderMode;
            this.borderSize = other.borderSize;
        }

        @Override
        public EventBus getEventBus() {
            return eventBus;
        }

        @JIPipeDocumentation(name = "Add border around images", description = "If enabled, a border is generated around the input image to avoid border conditions. " +
                "If you enabled tiling, the border is applied per tile, instead. Borders are automatically removed after the segmentation.")
        @JIPipeParameter("apply-border")
        public boolean isApplyBorder() {
            return applyBorder;
        }

        @JIPipeParameter("apply-border")
        public void setApplyBorder(boolean applyBorder) {
            this.applyBorder = applyBorder;
        }

        @JIPipeDocumentation(name = "Border mode", description = "How the border is generated")
        @JIPipeParameter("border-mode")
        public BorderMode getBorderMode() {
            return borderMode;
        }

        @JIPipeParameter("border-mode")
        public void setBorderMode(BorderMode borderMode) {
            this.borderMode = borderMode;
        }

        @JIPipeDocumentation(name = "Border size", description = "The size of the border in pixels")
        @JIPipeParameter("border-size")
        public int getBorderSize() {
            return borderSize;
        }

        @JIPipeParameter("border-size")
        public void setBorderSize(int borderSize) {
            this.borderSize = borderSize;
        }
    }

    public static class TilingParameters implements JIPipeParameterCollection {

        private final EventBus eventBus = new EventBus();
        private boolean applyTiling = true;
        private int tileSizeX = 128;
        private int tileSizeY = 128;

        public TilingParameters() {

        }

        public TilingParameters(TilingParameters other) {
            this.applyTiling = other.applyTiling;
            this.tileSizeX = other.tileSizeX;
            this.tileSizeY = other.tileSizeY;
        }

        @JIPipeDocumentation(name = "Apply tiling", description = "If enabled, the input image is first split into tiles that are processed individually by the Weka segmentation. " +
                "JIPipe will then assemble the final label image. " +
                "This can greatly reduce the memory cost. If borders are an issue, consider enabling the generation of a border.")
        @JIPipeParameter(value = "apply-tiling", uiOrder = -100)
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

        @Override
        public EventBus getEventBus() {
            return eventBus;
        }
    }
}
