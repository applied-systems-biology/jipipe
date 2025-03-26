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

package org.hkijena.jipipe.plugins.cellpose.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemWriteDataStorage;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.plugins.cellpose.datatypes.CellposeModelData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.OMEImageData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.ImageSliceIndex;
import org.hkijena.jipipe.plugins.python.PythonEnvironment;
import org.hkijena.jipipe.plugins.python.PythonUtils;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class CellposeUtils {

    private static String CELLPOSE_CUSTOM_CODE;

    private CellposeUtils() {

    }

    public static void runCellpose(PythonEnvironment environment, List<String> arguments, boolean suppressLogs, JIPipeProgressInfo progressInfo) {
        Map<String, String> additionalEnvironmentVariables = new HashMap<>();

        // If the environment is provided via a cellpose artifact and has a model directory, override CELLPOSE_LOCAL_MODELS_PATH
        if (environment.isLoadFromArtifact() && environment.getLastConfiguredArtifact() != null) {
            Path modelPath = environment.getLastConfiguredArtifact().getLocalPath().resolve("models");
            if (Files.isDirectory(modelPath)) {
                progressInfo.log("Configuring CELLPOSE_LOCAL_MODELS_PATH as " + modelPath);
                additionalEnvironmentVariables.put("CELLPOSE_LOCAL_MODELS_PATH", modelPath.toString());
            }
        }

        // Run the module
        PythonUtils.runPython(arguments.toArray(new String[0]),
                environment,
                Collections.emptyList(),
                additionalEnvironmentVariables,
                suppressLogs,
                false, progressInfo);
    }

    public static String getCellposeCustomCode() {
        if (CELLPOSE_CUSTOM_CODE == null) {
            try {
                CELLPOSE_CUSTOM_CODE = Resources.toString(CellposeUtils.class.getResource("/org/hkijena/jipipe/plugins/cellpose/cellpose-custom.py"), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return CELLPOSE_CUSTOM_CODE;
    }

    /**
     * Converts ROI in a custom Json format to {@link ROI2DListData}
     *
     * @param file the ROI file
     * @return ImageJ ROI
     */
    public static ROI2DListData cellposeROIJsonToImageJ(Path file) {
        ROI2DListData rois = new ROI2DListData();
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

    public static void saveInputImages(JIPipeInputDataSlot inputSlot, Set<Integer> inputRows, boolean enable3D, boolean enableMultiChannel, Path io2DPath, Path io3DPath, List<CellposeImageInfo> runWith2D, List<CellposeImageInfo> runWith3D, JIPipeGraphNode executingNode, JIPipeProgressInfo progressInfo) {
        for (int row : inputRows) {
            JIPipeProgressInfo rowProgress = progressInfo.resolveAndLog("Saving input image @ row " + row);
            ImagePlus img = inputSlot.getData(row, ImagePlusData.class, rowProgress).getImage();

            // Save only one slice
            if (img.getStackSize() == 1) {

                CellposeImageInfo info = new CellposeImageInfo(row);
                saveInputImage(row, img, io2DPath, new ImageSliceIndex(-1, -1, -1), info);
                runWith2D.add(info);
                continue;
            }


            if (enable3D && img.getNSlices() > 1) {
                final boolean isRGB = img.getType() == ImagePlus.COLOR_RGB;
                final boolean isMultiChannel = img.getNChannels() > 1;

                if (enableMultiChannel) {
                    if (isRGB ^ isMultiChannel || !isRGB) {
                        rowProgress.log("3D mode, multichannel (multichannel XOR RGB) -> Image will be split by frame.");
                        CellposeImageInfo info = new CellposeImageInfo(row);
                        ImageJIterationUtils.forEachIndexedTHyperStack(img, (sliceImage, index, sliceProgress) -> {
                            saveInputImage(row, sliceImage, io3DPath, index, info);
                        }, progressInfo);
                        runWith3D.add(info);
                    } else {
                        rowProgress.log("3D mode, multichannel (multichannel !! RGB conflict) -> Image will be converted to greyscale.");
                        rowProgress.log("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv");
                        rowProgress.log("CONVERTING TO GREYSCALE, AS CHANNEL SLICES ARE CONSIDERED MORE IMPORTANT");
                        rowProgress.log("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
                        CellposeImageInfo info = new CellposeImageInfo(row);
                        ImageJIterationUtils.forEachIndexedTHyperStack(img, (sliceImage, index, sliceProgress) -> {
                            saveInputImage(row, ImageJUtils.convertToGreyscaleIfNeeded(sliceImage), io3DPath, index, info);
                        }, progressInfo);
                        runWith3D.add(info);
                    }
                } else {
                    CellposeImageInfo info = new CellposeImageInfo(row);
                    ImageJIterationUtils.forEachIndexedCTStack(img, (sliceImage, index, sliceProcess) -> {
                        saveInputImage(row, ImageJUtils.convertToGreyscaleIfNeeded(sliceImage), io3DPath, index, info);
                    }, progressInfo);
                    runWith3D.add(info);
                }
            } else {
                if (enableMultiChannel) {
                    // Split into stacks per frame
                    CellposeImageInfo info = new CellposeImageInfo(row);
                    rowProgress.log("3D mode not active, multichannel -> Image will be split into multi-channel images + split by frame.");
                    ImageJIterationUtils.forEachIndexedZTStack(img, (sliceImage, index, stackProgress) -> {
                        saveInputImage(row, sliceImage, io2DPath, index, info);
                    }, progressInfo);
                    runWith2D.add(info);

                } else {
                    // Split everything into 2D slices
                    rowProgress.log("3D mode not active, no multichannel -> Image will be split into 2D slices.");
                    CellposeImageInfo info = new CellposeImageInfo(row);
                    ImageJIterationUtils.forEachIndexedZCTSlice(img, (ip, index) -> {
                        saveInputImage(row, new ImagePlus("slice", ip), io2DPath, index, info);
                    }, rowProgress);
                }
            }

        }
    }

    private static void saveInputImage(int row, ImagePlus img, Path ioPath, ImageSliceIndex index, CellposeImageInfo info) {
        String baseName = row + "_z" + index.getZ() + "_c" + index.getC() + "_t" + index.getT() + "_";
        Path outputPath = ioPath.resolve(baseName + ".tif");
        IJ.saveAsTiff(img, outputPath.toString());

        info.getSliceBaseNames().put(index, baseName);
    }

    public static CellposeModelInfo createModelInfo(List<JIPipeTextAnnotation> textAnnotations, CellposeModelData modelData, Path workDirectory, JIPipeProgressInfo modelProgress) {
        CellposeModelInfo modelInfo = new CellposeModelInfo();
        modelInfo.setAnnotationList(textAnnotations);
        if (modelData != null) {
            if (modelData.isPretrained()) {
                modelInfo.setModelPretrained(true);
                modelInfo.setModelNameOrPath(modelData.getPretrainedModelName());
            } else {
                Path tempDirectory = PathUtils.createTempSubDirectory(workDirectory.resolve("models"), "model");
                modelData.exportData(new JIPipeFileSystemWriteDataStorage(modelProgress, tempDirectory), null, false, modelProgress);
                modelInfo.setModelPretrained(false);
                modelInfo.setModelNameOrPath(tempDirectory.resolve(modelData.getMetadata().getName()).toString());
            }
        }
        return modelInfo;
    }

    public static ImagePlus extractImageFromInfo(CellposeImageInfo imageInfo, Path ioPath, String basePathSuffix, boolean useBioFormats, JIPipeProgressInfo progressInfo) {
        Map<ImageSliceIndex, ImageProcessor> sliceMap = new HashMap<>();
        for (Map.Entry<ImageSliceIndex, String> entry : imageInfo.getSliceBaseNames().entrySet()) {
            ImageSliceIndex sourceIndex = entry.getKey();
            Path imageFile = ioPath.resolve(entry.getValue() + basePathSuffix);

            // Read the slice image
            progressInfo.log("Reading: " + imageFile);
            ImagePlus sliceImg;
            if (useBioFormats) {
                OMEImageData omeImageData = OMEImageData.simpleOMEImport(imageFile);
                sliceImg = omeImageData.getImage();
            } else {
                sliceImg = IJ.openImage(imageFile.toString());
            }
            if (sliceImg == null) {
                throw new NullPointerException("Unable to read image from " + imageFile + "! Bio-Formats: " + useBioFormats);
            }

            for (int c = 0; c < sliceImg.getNChannels(); c++) {
                for (int z = 0; z < sliceImg.getNSlices(); z++) {
                    for (int t = 0; t < sliceImg.getNFrames(); t++) {
                        int targetC = c;
                        int targetZ = z;
                        int targetT = t;
                        if (sourceIndex.getC() >= 0) {
                            targetC = sourceIndex.getC();
                        }
                        if (sourceIndex.getZ() >= 0) {
                            targetZ = sourceIndex.getZ();
                        }
                        if (sourceIndex.getT() >= 0) {
                            targetT = sourceIndex.getT();
                        }
                        progressInfo.log("Assigning local slice (c=" + c + ", z=" + z + ", t=" + t + ") to final location (c=" + targetC + ", z=" + targetZ + ", t=" + targetT + ")");
                        sliceMap.put(new ImageSliceIndex(targetC, targetZ, targetT), ImageJUtils.getSliceZero(sliceImg, c, z, t));
                    }
                }
            }
        }
        return ImageJUtils.mergeMappedSlices(sliceMap);
    }

    public static ROI2DListData extractROIFromInfo(CellposeImageInfo imageInfo, Path ioPath) {
        ROI2DListData rois = new ROI2DListData();
        for (Map.Entry<ImageSliceIndex, String> entry : imageInfo.getSliceBaseNames().entrySet()) {
            ROI2DListData sliceRoi = cellposeROIJsonToImageJ(ioPath.resolve(entry.getValue() + "_seg_roi.json"));
            if (imageInfo.getSliceBaseNames().size() > 1) {
                for (Roi roi : sliceRoi) {
                    roi.setPosition(entry.getKey().getC() + 1, entry.getKey().getZ() + 1, entry.getKey().getT() + 1);
                }
            }
            rois.addAll(sliceRoi);
        }
        return rois;
    }
}
