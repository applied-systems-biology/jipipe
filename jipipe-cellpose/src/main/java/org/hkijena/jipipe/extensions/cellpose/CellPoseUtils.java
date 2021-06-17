package org.hkijena.jipipe.extensions.cellpose;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.extensions.cellpose.parameters.EnhancementParameters;
import org.hkijena.jipipe.extensions.cellpose.parameters.ModelParameters;
import org.hkijena.jipipe.extensions.cellpose.parameters.OutputParameters;
import org.hkijena.jipipe.extensions.cellpose.parameters.PerformanceParameters;
import org.hkijena.jipipe.extensions.cellpose.parameters.ThresholdParameters;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.color.ImagePlus3DColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleData;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalDoubleParameter;
import org.hkijena.jipipe.extensions.python.PythonUtils;
import org.hkijena.jipipe.utils.JsonUtils;
import org.hkijena.jipipe.utils.MacroUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class CellPoseUtils {
    private CellPoseUtils() {

    }

    /**
     * Converts a Cellpose ROI to {@link ROIListData} according to https://github.com/MouseLand/cellpose/blob/master/imagej_roi_converter.py
     *
     * @param file the Cellpose ROI
     * @return ImageJ ROI
     */
    public static ROIListData cellPoseROIToImageJ(Path file) {
        ROIListData rois = new ROIListData();
        try {
            for (String line : Files.readAllLines(file)) {
                TIntList xList = new TIntArrayList();
                TIntList yList = new TIntArrayList();
                List<Integer> xyList = Arrays.stream(line.trim().split(",")).map(Integer::parseInt).collect(Collectors.toList());
                for (int i = 0; i < xyList.size(); i++) {
                    if (i % 2 == 0) {
                        xList.add(xyList.get(i));
                    } else {
                        yList.add(xyList.get(i));
                    }
                }
                rois.add(new PolygonRoi(xList.toArray(), yList.toArray(), xList.size(), Roi.POLYGON));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return rois;
    }

    /**
     * Converts ROI in a custom Json format to {@link ROIListData}
     *
     * @param file the ROI file
     * @return ImageJ ROI
     */
    public static ROIListData cellPoseROIJsonToImageJ(Path file) {
        ROIListData rois = new ROIListData();
        try {
            JsonNode node = JsonUtils.getObjectMapper().readerFor(JsonNode.class).readValue(file.toFile());
            for (JsonNode roiItem : ImmutableList.copyOf(node.elements())) {
                TIntList xList = new TIntArrayList();
                TIntList yList = new TIntArrayList();

                for (JsonNode coordItem : ImmutableList.copyOf(roiItem.get("coords"))) {
                    int x = coordItem.get("x").asInt();
                    int y = coordItem.get("y").asInt();
                    xList.add(x);
                    yList.add(y);
                }

                if (xList.size() != yList.size()) {
                    System.err.println("Error: Different X and  Y array sizes");
                    continue;
                }
                if (xList.size() < 3) {
                    // Empty ROIs are not allowed
                    continue;
                }

                PolygonRoi roi = new PolygonRoi(xList.toArray(), yList.toArray(), xList.size(), Roi.POLYGON);
                int z = -1;
                JsonNode zEntry = roiItem.path("z");
                if (!zEntry.isMissingNode())
                    z = zEntry.asInt();
                roi.setPosition(0, z + 1, 0);
                rois.add(roi);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return rois;
    }

    /**
     * Injects an extended Cellpose runner into Python.
     * It receives a pretrained model
     *
     * @param code                the code
     * @param customModelPaths    custom model paths
     * @param customSizeModelPath custom size model path
     */
    public static void setupCustomCellposeModel(StringBuilder code, List<Path> customModelPaths, Path customSizeModelPath, EnhancementParameters enhancementParameters, ModelParameters modelParameters) {
        // This is code that allows to embed a custom model
        code.append("\n\nclass CellposeCustom():\n" +
                "    def __init__(self, gpu=False, pretrained_model=None, diam_mean=None, pretrained_size=None, net_avg=True, device=None, torch=True):\n" +
                "        super(CellposeCustom, self).__init__()\n" +
                "        from cellpose.core import UnetModel, assign_device, check_mkl, use_gpu, MXNET_ENABLED, parse_model_string\n" +
                "        from cellpose.models import CellposeModel, SizeModel\n\n" +
                "        if not torch:\n" +
                "            if not MXNET_ENABLED:\n" +
                "                torch = True\n" +
                "        self.torch = torch\n" +
                "        torch_str = ['','torch'][self.torch]\n" +
                "        \n" +
                "        # assign device (GPU or CPU)\n" +
                "        sdevice, gpu = assign_device(self.torch, gpu)\n" +
                "        self.device = device if device is not None else sdevice\n" +
                "        self.gpu = gpu\n" +
                "        self.pretrained_model = pretrained_model\n" +
                "        self.pretrained_size = pretrained_size\n" +
                "        self.diam_mean = diam_mean\n" +
                "        \n" +
                "        if not net_avg:\n" +
                "            self.pretrained_model = self.pretrained_model[0]\n" +
                "\n" +
                "        self.cp = CellposeModel(device=self.device, gpu=self.gpu,\n" +
                "                                pretrained_model=self.pretrained_model,\n" +
                "                                diam_mean=self.diam_mean, torch=self.torch)\n" +
                "        if pretrained_size is not None:\n" +
                "            self.sz = SizeModel(device=self.device, pretrained_size=self.pretrained_size,\n" +
                "                            cp_model=self.cp)\n" +
                "        else:\n" +
                "            self.sz = None\n" +
                "\n" +
                "    def eval(self, x, batch_size=8, channels=None, channel_axis=None, z_axis=None,\n" +
                "             invert=False, normalize=True, diameter=30., do_3D=False, anisotropy=None,\n" +
                "             net_avg=True, augment=False, tile=True, tile_overlap=0.1, resample=False, interp=True,\n" +
                "             flow_threshold=0.4, cellprob_threshold=0.0, min_size=15, \n" +
                "              stitch_threshold=0.0, rescale=None, progress=None):\n" +
                "        from cellpose.models import models_logger\n" +
                "        tic0 = time.time()\n" +
                "\n" +
                "        estimate_size = True if (diameter is None or diameter==0) else False\n" +
                "        models_logger.info('Estimate size: ' + str(estimate_size))\n" +
                "        if estimate_size and self.pretrained_size is not None and not do_3D and x[0].ndim < 4:\n" +
                "            tic = time.time()\n" +
                "            models_logger.info('~~~ ESTIMATING CELL DIAMETER(S) ~~~')\n" +
                "            diams, _ = self.sz.eval(x, channels=channels, channel_axis=channel_axis, invert=invert, batch_size=batch_size, \n" +
                "                                    augment=augment, tile=tile)\n" +
                "            rescale = self.diam_mean / np.array(diams)\n" +
                "            diameter = None\n" +
                "            models_logger.info('estimated cell diameter(s) in %0.2f sec'%(time.time()-tic))\n" +
                "            models_logger.info('>>> diameter(s) = ')\n" +
                "            if isinstance(diams, list) or isinstance(diams, np.ndarray):\n" +
                "                diam_string = '[' + ''.join(['%0.2f, '%d for d in diams]) + ']'\n" +
                "            else:\n" +
                "                diam_string = '[ %0.2f ]'%diams\n" +
                "            models_logger.info(diam_string)\n" +
                "        elif estimate_size:\n" +
                "            if self.pretrained_size is None:\n" +
                "                reason = 'no pretrained size model specified in model Cellpose'\n" +
                "            else:\n" +
                "                reason = 'does not work on non-2D images'\n" +
                "            models_logger.warning(f'could not estimate diameter, {reason}')\n" +
                "            diams = self.diam_mean \n" +
                "        else:\n" +
                "            diams = diameter\n" +
                "\n" +
                "        tic = time.time()\n" +
                "        models_logger.info('~~~ FINDING MASKS ~~~')\n" +
                "        masks, flows, styles = self.cp.eval(x, \n" +
                "                                            batch_size=batch_size, \n" +
                "                                            invert=invert, \n" +
                "                                            diameter=diameter,\n" +
                "                                            rescale=rescale, \n" +
                "                                            anisotropy=anisotropy, \n" +
                "                                            channels=channels,\n" +
                "                                            channel_axis=channel_axis, \n" +
                "                                            z_axis=z_axis,\n" +
                "                                            augment=augment, \n" +
                "                                            tile=tile, \n" +
                "                                            do_3D=do_3D, \n" +
                "                                            net_avg=net_avg, \n" +
                "                                            progress=progress,\n" +
                "                                            tile_overlap=tile_overlap,\n" +
                "                                            resample=resample,\n" +
                "                                            interp=interp,\n" +
                "                                            flow_threshold=flow_threshold, \n" +
                "                                            cellprob_threshold=cellprob_threshold,\n" +
                "                                            min_size=min_size, \n" +
                "                                            stitch_threshold=stitch_threshold)\n" +
                "        models_logger.info('>>>> TOTAL TIME %0.2f sec'%(time.time()-tic0))\n" +
                "    \n" +
                "        return masks, flows, styles, diams\n\n");
        Map<String, Object> modelParameterMap = new HashMap<>();
        modelParameterMap.put("pretrained_model", customModelPaths.stream().map(Objects::toString).collect(Collectors.toList()));
        modelParameterMap.put("net_avg", enhancementParameters.isNetAverage());
        modelParameterMap.put("gpu", modelParameters.isEnableGPU());
        modelParameterMap.put("diam_mean", modelParameters.getMeanDiameter());
        if (customSizeModelPath != null)
            modelParameterMap.put("pretrained_size", customSizeModelPath.toString());
        code.append(String.format("model = CellposeCustom(%s)\n", PythonUtils.mapToPythonArguments(modelParameterMap)));
    }

    public static void setupCombinedCellposeModel(StringBuilder code, EnhancementParameters enhancementParameters, ModelParameters modelParameters) {
        Map<String, Object> modelParameterMap = new HashMap<>();
        modelParameterMap.put("model_type", modelParameters.getModel().getId());
        modelParameterMap.put("net_avg", enhancementParameters.isNetAverage());
        modelParameterMap.put("gpu", modelParameters.isEnableGPU());
        code.append(String.format("model = models.Cellpose(%s)\n", PythonUtils.mapToPythonArguments(modelParameterMap)));
    }

    public static void setupGenerateOutputStyles(Path outputStyles, StringBuilder code) {
        code.append("if data_is_3d and not enable_3d_segmentation:\n" +
                "    styles = np.stack(styles, 0)\n");
        code.append("io.imsave(").append(PythonUtils.objectToPython(outputStyles)).append(", styles)\n");
    }

    public static void setupGenerateOutputProbabilities(Path outputProbabilities, StringBuilder code) {
        code.append("if data_is_3d and not enable_3d_segmentation:\n" +
                "    probs = np.stack([x[2] for x in flows], 0)\n" +
                "else:\n" +
                "    probs = flows[2]\n");
        code.append("io.imsave(").append(PythonUtils.objectToPython(outputProbabilities)).append(", probs)\n");
    }

    public static void setupGenerateOutputFlows(Path outputFlows, StringBuilder code) {
        code.append("if data_is_3d and not enable_3d_segmentation:\n" +
                "    flows_rgb = np.stack([x[0] for x in flows], 0)\n" +
                "else:\n" +
                "    flows_rgb = flows[0]\n");
        code.append("io.imsave(").append(PythonUtils.objectToPython(outputFlows)).append(", flows_rgb)\n");
    }

    public static void setupGenerateOutputLabels(Path outputLabels, StringBuilder code) {
        code.append("if masks.dtype != np.short and masks.dtype != np.uint8:\n" +
                "    masks = masks.astype(np.float32)\n");
        code.append("io.imsave(").append(PythonUtils.objectToPython(outputLabels)).append(", masks)\n");
    }

    public static void setupGenerateOutputROI(Path outputRoiOutline, StringBuilder code) {
        code.append("roi_list = []\n" +
                "if masks.ndim == 3:\n" +
                "    for z in range(masks.shape[0]):\n" +
                "        coords_list = utils.outlines_list(masks[z,:,:])\n" +
                "        for coords in coords_list:\n" +
                "            roi = dict(z=z, coords=[ dict(x=int(x[0]), y=int(x[1])) for x in coords ])\n" +
                "            roi_list.append(roi)\n" +
                "else:\n" +
                "    coords_list = utils.outlines_list(masks)\n" +
                "    for coords in coords_list:\n" +
                "        roi = dict(coords=[ dict(x=int(x[0]), y=int(x[1])) for x in coords ])\n" +
                "        roi_list.append(roi)\n");
        code.append(String.format("with open(\"%s\", \"w\") as f:\n" +
                        "    json.dump(roi_list, f, indent=4)\n\n",
                MacroUtils.escapeString(outputRoiOutline.toString())));
    }

    public static void extractCellposeOutputs(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo, Path outputRoiOutline, Path outputLabels, Path outputFlows, Path outputProbabilities, Path outputStyles, List<JIPipeAnnotation> annotationList, OutputParameters outputParameters) {
        if (outputParameters.isOutputROI()) {
            ROIListData rois = cellPoseROIJsonToImageJ(outputRoiOutline);
            dataBatch.addOutputData("ROI", rois, annotationList, JIPipeAnnotationMergeStrategy.OverwriteExisting, progressInfo);
        }
        if (outputParameters.isOutputLabels()) {
            ImagePlus labels = IJ.openImage(outputLabels.toString());
            dataBatch.addOutputData("Labels", new ImagePlus3DGreyscaleData(labels), annotationList, JIPipeAnnotationMergeStrategy.OverwriteExisting, progressInfo);
        }
        if (outputParameters.isOutputFlows()) {
            ImagePlus flows = IJ.openImage(outputFlows.toString());
            dataBatch.addOutputData("Flows", new ImagePlus3DColorRGBData(flows), annotationList, JIPipeAnnotationMergeStrategy.OverwriteExisting, progressInfo);
        }
        if (outputParameters.isOutputProbabilities()) {
            ImagePlus probabilities = IJ.openImage(outputProbabilities.toString());
            dataBatch.addOutputData("Probabilities", new ImagePlus3DGreyscale32FData(probabilities), annotationList, JIPipeAnnotationMergeStrategy.OverwriteExisting, progressInfo);
        }
        if (outputParameters.isOutputStyles()) {
            ImagePlus styles = IJ.openImage(outputStyles.toString());
            dataBatch.addOutputData("Styles", new ImagePlus3DGreyscale32FData(styles), annotationList, JIPipeAnnotationMergeStrategy.OverwriteExisting, progressInfo);
        }
    }

    public static void extractCellposeFileOutputs(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo, Path outputRoiOutline, Path outputLabels, Path outputFlows, Path outputProbabilities, Path outputStyles, List<JIPipeAnnotation> annotationList, OutputParameters outputParameters) {
        if (outputParameters.isOutputROI()) {
            ROIListData rois = cellPoseROIJsonToImageJ(outputRoiOutline);
            dataBatch.addOutputData("ROI", rois, annotationList, JIPipeAnnotationMergeStrategy.OverwriteExisting, progressInfo);
        }
        if (outputParameters.isOutputLabels()) {
            dataBatch.addOutputData("Labels", new FileData(outputLabels), annotationList, JIPipeAnnotationMergeStrategy.OverwriteExisting, progressInfo);
        }
        if (outputParameters.isOutputFlows()) {
            dataBatch.addOutputData("Flows", new FileData(outputFlows), annotationList, JIPipeAnnotationMergeStrategy.OverwriteExisting, progressInfo);
        }
        if (outputParameters.isOutputProbabilities()) {
            dataBatch.addOutputData("Probabilities", new FileData(outputProbabilities), annotationList, JIPipeAnnotationMergeStrategy.OverwriteExisting, progressInfo);
        }
        if (outputParameters.isOutputStyles()) {
            dataBatch.addOutputData("Styles", new FileData(outputStyles), annotationList, JIPipeAnnotationMergeStrategy.OverwriteExisting, progressInfo);
        }
    }

    public static void setupModelEval(StringBuilder code, OptionalDoubleParameter diameter, EnhancementParameters enhancementParameters, PerformanceParameters performanceParameters, ThresholdParameters thresholdParameters) {
        Map<String, Object> evalParameterMap = new HashMap<>();
        evalParameterMap.put("x", PythonUtils.rawPythonCode("img"));
        evalParameterMap.put("diameter", diameter.isEnabled() ? diameter.getContent() : null);
        evalParameterMap.put("channels", PythonUtils.rawPythonCode("[[0, 0]]"));
        evalParameterMap.put("do_3D", PythonUtils.rawPythonCode("enable_3d_segmentation"));
        evalParameterMap.put("normalize", enhancementParameters.isNormalize());
        evalParameterMap.put("anisotropy", enhancementParameters.getAnisotropy().isEnabled() ?
                enhancementParameters.getAnisotropy().getContent() : null);
        evalParameterMap.put("net_avg", enhancementParameters.isNetAverage());
        evalParameterMap.put("augment", enhancementParameters.isAugment());
        evalParameterMap.put("tile", performanceParameters.isTile());
        evalParameterMap.put("tile_overlap", performanceParameters.getTileOverlap());
        evalParameterMap.put("resample", performanceParameters.isResample());
        evalParameterMap.put("interp", enhancementParameters.isInterpolate());
        evalParameterMap.put("flow_threshold", thresholdParameters.getFlowThreshold());
        evalParameterMap.put("cellprob_threshold", thresholdParameters.getCellProbabilityThreshold());
        evalParameterMap.put("min_size", thresholdParameters.getMinSize());
        evalParameterMap.put("stitch_threshold", thresholdParameters.getStitchThreshold());

        code.append(String.format("masks, flows, styles, diams = model.eval(%s)\n",
                PythonUtils.mapToPythonArguments(evalParameterMap)
        ));
    }

    public static void setupCellposeImports(StringBuilder code) {
        code.append("from cellpose import models\n");
        code.append("from cellpose import utils, io\n");
        code.append("import json\n");
        code.append("import time\n");
        code.append("import numpy as np\n\n");
    }
}
