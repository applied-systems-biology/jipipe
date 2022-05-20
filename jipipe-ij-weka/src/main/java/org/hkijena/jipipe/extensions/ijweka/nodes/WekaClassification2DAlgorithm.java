package org.hkijena.jipipe.extensions.ijweka.nodes;

import com.google.common.eventbus.EventBus;
import ij.ImagePlus;
import ij.ImageStack;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.ijweka.datatypes.WekaModelData;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.transform.BorderMode;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.transform.TileImage2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.transform.UnTileImage2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;
import trainableSegmentation.WekaSegmentation;

@JIPipeDocumentation(name = "Weka classifier 2D", description = "Classifies an image with a Weka model. If higher-dimensional data is provided, the classification is applied per slice. To obtain ROI from the generated labels, utilize the 'Labels to ROI' node.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Weka")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Image", description = "Image on which the classification should be applied", autoCreate = true)
@JIPipeInputSlot(value = WekaModelData.class, slotName = "Model", description = "The model", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Classified image", description = "The classified image", autoCreate = true)
public class WekaClassification2DAlgorithm extends JIPipeIteratingAlgorithm {

    private TilingParameters tilingParameters = new TilingParameters();

    public WekaClassification2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(tilingParameters);
    }

    public WekaClassification2DAlgorithm(WekaClassification2DAlgorithm other) {
        super(other);
        this.tilingParameters = new TilingParameters(other.tilingParameters);
        registerSubParameter(tilingParameters);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlusData image = dataBatch.getInputData("Image", ImagePlusData.class, progressInfo);
        WekaModelData modelData = dataBatch.getInputData("Model", WekaModelData.class, progressInfo);
        WekaSegmentation segmentation = modelData.getSegmentation();

        TileImage2DAlgorithm tileImage2DAlgorithm = JIPipe.createNode(TileImage2DAlgorithm.class);
        tileImage2DAlgorithm.setOverlapX(tilingParameters.getOverlapX());
        tileImage2DAlgorithm.setOverlapY(tilingParameters.getOverlapY());
        tileImage2DAlgorithm.setTileSizeX(tilingParameters.getTileSizeX());
        tileImage2DAlgorithm.setTileSizeY(tilingParameters.getTileSizeY());
        tileImage2DAlgorithm.setBorderMode(tilingParameters.getBorderMode());
        tileImage2DAlgorithm.setTileInsetXAnnotation(new OptionalAnnotationNameParameter("inset_x", true));
        tileImage2DAlgorithm.setTileInsetYAnnotation(new OptionalAnnotationNameParameter("inset_y", true));
        tileImage2DAlgorithm.setTileRealXAnnotation(new OptionalAnnotationNameParameter("real_x", true));
        tileImage2DAlgorithm.setTileRealYAnnotation(new OptionalAnnotationNameParameter("real_y", true));
        tileImage2DAlgorithm.setImageWidthAnnotation(new OptionalAnnotationNameParameter("img_width", true));
        tileImage2DAlgorithm.setImageHeightAnnotation(new OptionalAnnotationNameParameter("img_height", true));

        UnTileImage2DAlgorithm unTileImage2DAlgorithm = JIPipe.createNode(UnTileImage2DAlgorithm.class);
        unTileImage2DAlgorithm.setTileInsetXAnnotation(new OptionalAnnotationNameParameter("inset_x", true));
        unTileImage2DAlgorithm.setTileInsetYAnnotation(new OptionalAnnotationNameParameter("inset_y", true));
        unTileImage2DAlgorithm.setTileRealXAnnotation(new OptionalAnnotationNameParameter("real_x", true));
        unTileImage2DAlgorithm.setTileRealYAnnotation(new OptionalAnnotationNameParameter("real_y", true));
        unTileImage2DAlgorithm.setImageWidthAnnotation(new OptionalAnnotationNameParameter("img_width", true));
        unTileImage2DAlgorithm.setImageHeightAnnotation(new OptionalAnnotationNameParameter("img_height", true));

        ImageStack stack = new ImageStack(image.getWidth(), image.getHeight(), image.getNSlices() * image.getNChannels() * image.getNChannels());
        ImageJUtils.forEachIndexedZCTSlice(image.getImage(), (ip, index) -> {
            ImagePlus wholeSlice = new ImagePlus(image.getImage().getTitle() + " " + index, ip);
            ImagePlus classified;
            if(tilingParameters.isApplyTiling()) {
                progressInfo.log("Generating tiles for " + wholeSlice);

                // Generate tiles
                tileImage2DAlgorithm.clearSlotData();
                tileImage2DAlgorithm.getFirstInputSlot().addData(new ImagePlusData(wholeSlice), progressInfo);
                tileImage2DAlgorithm.run(progressInfo.resolve("Generate tiles"));

                // Classify tiles
                JIPipeDataTable tileTable = tileImage2DAlgorithm.getFirstOutputSlot();
                for (int i = 0; i < tileTable.getRowCount(); i++) {
                    JIPipeProgressInfo tileProgress = progressInfo.resolveAndLog("Classify tiles", i, tileTable.getRowCount());
                    ImagePlus tileSlice = tileTable.getData(i, ImagePlusData.class, tileProgress).getImage();
                    ImagePlus classifiedTileSlice = segmentation.applyClassifier(tileSlice);
                    tileTable.setData(i, new ImagePlusData(classifiedTileSlice));
                }

                // Merge tiles
                unTileImage2DAlgorithm.clearSlotData();
                unTileImage2DAlgorithm.getFirstInputSlot().addFromTable(tileTable, progressInfo);
                unTileImage2DAlgorithm.run(progressInfo.resolve("Merge tiles"));

                classified = unTileImage2DAlgorithm.getFirstOutputSlot().getData(0, ImagePlusData.class, progressInfo).getImage();

                // Cleanup
                tileImage2DAlgorithm.clearSlotData();
                unTileImage2DAlgorithm.clearSlotData();
            }
            else {
                progressInfo.log("Classifying whole image " + wholeSlice);
                classified = segmentation.applyClassifier(wholeSlice);
            }
            stack.setProcessor(classified.getProcessor(), index.zeroSliceIndexToOneStackIndex(image.getImage()));
        }, progressInfo);

        dataBatch.addOutputData("Classified image", new ImagePlusData(new ImagePlus("Classified", stack)), progressInfo);
    }

    @JIPipeDocumentation(name = "Generate tiles", description = "The following settings allow the generation of tiles to save memory.")
    @JIPipeParameter("tiling-parameters")
    public TilingParameters getTilingParameters() {
        return tilingParameters;
    }

    public static class TilingParameters implements JIPipeParameterCollection {

        private final EventBus eventBus = new EventBus();
        private boolean applyTiling = true;

        private BorderMode borderMode = BorderMode.Mirror;
        private int tileSizeX = 128;
        private int tileSizeY = 128;

        private int overlapX = 8;

        private int overlapY = 8;

        public TilingParameters() {

        }

        public TilingParameters(TilingParameters other) {
            this.applyTiling = other.applyTiling;
            this.borderMode = other.borderMode;
            this.tileSizeX = other.tileSizeX;
            this.tileSizeY = other.tileSizeY;
            this.overlapX = other.overlapX;
            this.overlapY = other.overlapY;
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

        @JIPipeDocumentation(name = "Border mode", description = "Determines how the border is generated if an overlap is applied.")
        @JIPipeParameter("border-mode")
        public BorderMode getBorderMode() {
            return borderMode;
        }

        @JIPipeParameter("border-mode")
        public void setBorderMode(BorderMode borderMode) {
            this.borderMode = borderMode;
        }

        @JIPipeDocumentation(name = "Overlap (X)",description = "If greater than zero, adds an overlap border around each tile to potentially remove issues when classification is applied close to the image border.")
        @JIPipeParameter("overlap-x")
        public int getOverlapX() {
            return overlapX;
        }

        @JIPipeParameter("overlap-x")
        public boolean setOverlapX(int overlapX) {
            if(overlapX < 0)
                return false;
            this.overlapX = overlapX;
            return false;
        }

        @JIPipeDocumentation(name = "Overlap (Y)",description = "If greater than zero, adds an overlap border around each tile to potentially remove issues when classification is applied close to the image border.")
        @JIPipeParameter("overlap-y")
        public int getOverlapY() {
            return overlapY;
        }

        @JIPipeParameter("overlap-y")
        public boolean setOverlapY(int overlapY) {
            if(overlapY < 0)
                return false;
            this.overlapY = overlapY;
            return false;
        }

        @Override
        public EventBus getEventBus() {
            return eventBus;
        }
    }
}
