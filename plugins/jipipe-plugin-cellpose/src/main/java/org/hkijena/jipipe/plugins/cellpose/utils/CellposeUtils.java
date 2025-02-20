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
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemWriteDataStorage;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.plugins.cellpose.datatypes.CellposeModelData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageSliceIndex;
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

    public static void saveInputImages(JIPipeInputDataSlot inputSlot, Set<Integer> inputRows, boolean enable3D, JIPipeProgressInfo progressInfo, Path io2DPath, Path io3DPath, List<CellposeImageInfo> runWith2D, List<CellposeImageInfo> runWith3D, JIPipeGraphNode executingNode) {
        for (int row : inputRows) {
            JIPipeProgressInfo rowProgress = progressInfo.resolve("Data row " + row);

            ImagePlus img = inputSlot.getData(row, ImagePlusData.class, rowProgress).getImage();
            if (img.getNFrames() > 1) {
                throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                        new GraphNodeValidationReportContext(executingNode),
                        "Cellpose does not support time series!",
                        "Please ensure that the image dimensions are correctly assigned.",
                        "Remove the frames or reorder the dimensions before applying Cellpose"));
            }
            if (img.getNSlices() == 1) {
                // Output the image as-is
                String baseName = row + "_";
                Path outputPath = io2DPath.resolve(baseName + ".tif");
                IJ.saveAsTiff(img, outputPath.toString());

                // Create info
                CellposeImageInfo info = new CellposeImageInfo(row);
                info.getSliceBaseNames().put(new ImageSliceIndex(-1, -1, -1), baseName);
                runWith2D.add(info);
            } else {
                if (enable3D) {
                    // Cannot have channels AND RGB
                    if (img.getNChannels() > 1 && img.getType() == ImagePlus.COLOR_RGB) {
                        throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                                new GraphNodeValidationReportContext(executingNode),
                                "Cellpose does not support 3D multichannel images with RGB!",
                                "Python will convert the RGB channels into greyscale slices, thus conflicting with the channel slices defined in the input image",
                                "Convert the image from RGB to greyscale or remove the additional channel slices."));
                    }

                    // Output the image as-is
                    String baseName = row + "_";
                    Path outputPath = io3DPath.resolve(baseName + ".tif");
                    IJ.saveAsTiff(img, outputPath.toString());

                    // Create info
                    CellposeImageInfo info = new CellposeImageInfo(row);
                    info.getSliceBaseNames().put(new ImageSliceIndex(-1, -1, -1), baseName);
                    runWith3D.add(info);
                } else {
                    rowProgress.log("3D mode not active, but 3D image detected -> Image will be split into 2D slices.");

                    CellposeImageInfo info = new CellposeImageInfo(row);

                    // Split the 3D image into slices
                    ImageJUtils.forEachIndexedZCTSlice(img, (ip, index) -> {
                        ImagePlus sliceImage = new ImagePlus(index.toString(), ip);
                        String baseName = row + "_z" + index.getZ() + "_c" + index.getC() + "_t" + index.getT() + "_";
                        Path outputPath = io2DPath.resolve(baseName + ".tif");
                        IJ.saveAsTiff(sliceImage, outputPath.toString());

                        // Create info
                        info.getSliceBaseNames().put(index, baseName);
                    }, rowProgress);

                    runWith2D.add(info);
                }
            }
        }
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
                modelInfo.setSizeModelNameOrPath(tempDirectory.resolve(modelData.getMetadata().getName()).toString());
            }
        }
        return modelInfo;
    }
}
