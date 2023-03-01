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

package org.hkijena.jipipe.extensions.ij3d.datatypes;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import gnu.trove.set.TIntSet;
import ij.IJ;
import ij.ImagePlus;
import mcib3d.geom.*;
import mcib3d.image3d.ImageHandler;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.extensions.ij3d.IJ3DUtils;
import org.hkijena.jipipe.extensions.ij3d.utils.ExtendedObject3DVoxels;
import org.hkijena.jipipe.extensions.ij3d.utils.ROI3DOutline;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.BitDepth;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.UnclosableInputStream;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UnclosableOutputStream;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@JIPipeDocumentation(name = "3D ROI list", description = "Collection of 3D ROI")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains one file in *.zip format. " +
        "The *.zip contains multiple 3D ImageJ Suite ROI. Please note that if multiple *.zip files are present, only " +
        "one will be loaded.", jsonSchemaURL = "https://jipipe.org/schemas/datatypes/roi-list-data.schema.json")
public class ROI3DListData extends ArrayList<ROI3D> implements JIPipeData {
    public ROI3DListData() {

    }

    public ROI3DListData(ROI3DListData other) {
        for (ROI3D roi3D : other) {
            add(new ROI3D(roi3D));
        }
    }

    @Override
    public void exportData(JIPipeWriteDataStorage storage, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        saveObjectsToStream(storage.write(StringUtils.orElse(name, "rois") + ".roi3d.zip"), progressInfo);
    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        ROI3DListData result = new ROI3DListData();
        for (int i = 0; i < this.size(); i++) {
            progressInfo.resolveAndLog("Copy 3D Object", i, size());
            ROI3D roi3D = this.get(i);
            result.add(new ROI3D(roi3D));
        }
        return result;
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {

    }

    @Override
    public String toString() {
        return size() == 1 ? "1 3D ROI" : size() + " 3D ROI";
    }

    /**
     * Groups the ROI by their image positions
     *
     * @param perChannel group per channel
     * @param perFrame   group per frame
     * @return groups (grouped by zero-index positions)
     */
    public Map<ImageSliceIndex, List<ROI3D>> groupByPosition(boolean perChannel, boolean perFrame) {
        return this.stream().collect(Collectors.groupingBy(roi -> {
            ImageSliceIndex index = new ImageSliceIndex();
            if (perFrame)
                index.setT(roi.getFrame() - 1);
            if (perChannel)
                index.setC(roi.getChannel() - 1);
            return index;
        }));
    }

    public static ROI3DListData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        Path zipFile = storage.findFileByExtension(".zip").get();
        try(InputStream stream = storage.open(zipFile)) {
            ROI3DListData target = new ROI3DListData();
            target.loadObjectsFromStream(stream,  progressInfo);
            return target;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static ROI3DListData importData(Path zipFile, JIPipeProgressInfo progressInfo) {
        try(InputStream stream = Files.newInputStream(zipFile)) {
            ROI3DListData target = new ROI3DListData();
            target.loadObjectsFromStream(stream,  progressInfo);
            return target;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Copy of the original loadObjects() method adapted to loading from streams
     * @param inputStream the input stream
     * @param progressInfo the progress info
     */
    public void loadObjectsFromStream(InputStream inputStream, JIPipeProgressInfo progressInfo) {
        //ImagePlus plus = this.getImage();
        Map<String, Object3D> objectByNameMap = new HashMap<>();
        Map<String, ROI3D> roiByNameMap = new HashMap<>();
        try (ZipInputStream zipinputstream = new ZipInputStream(inputStream)) {
            ZipEntry zipentry = zipinputstream.getNextEntry();
            while (zipentry != null) {
                //for each entry to be extracted
                String entryName = zipentry.getName();

                if(entryName.endsWith(".3droi")) {
                    progressInfo.log("Loading 3D object " + entryName);
                    //IJ.log("entryname=" + entryName);

                    // create object
                    ExtendedObject3DVoxels obj = new ExtendedObject3DVoxels();
                    obj.setValue(1);
                    obj.loadObjectFromStream(new UnclosableInputStream(zipinputstream), entryName);
                    obj.setName(entryName.substring(0, entryName.length() - 6));

                    objectByNameMap.put(obj.getName(), obj);
                }
                else if(entryName.equals("jipipe-metadata.json")) {
                    JsonNode node = JsonUtils.getObjectMapper().readerFor(JsonNode.class).readValue(new UnclosableInputStream(zipinputstream));
                    for (Map.Entry<String, JsonNode> entry : ImmutableList.copyOf(node.fields())) {
                        ROI3D roi3D = JsonUtils.getObjectMapper().readerFor(ROI3D.class).readValue(entry.getValue());
                        roiByNameMap.put(entry.getKey(), roi3D);
                    }
                }

                zipinputstream.closeEntry();
                zipentry = zipinputstream.getNextEntry();

            }
        }catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (Map.Entry<String, ROI3D> entry : roiByNameMap.entrySet()) {
            Object3D object3D = objectByNameMap.get(entry.getKey());
            if(object3D != null) {
                entry.getValue().setObject3D(object3D);
            }
            else {
                progressInfo.log("Unable to find ROI " + entry.getKey() + " in object list!");
            }
            add(entry.getValue());
        }
        for (Map.Entry<String, Object3D> entry : objectByNameMap.entrySet()) {
            ROI3D roi3D = roiByNameMap.get(entry.getKey());
            if(roi3D == null) {
                roi3D = new ROI3D(entry.getValue());
                add(roi3D);
            }
        }

    }

    public void saveObjectsToStream(OutputStream stream, JIPipeProgressInfo progressInfo) {
        try {
            //  ZIP
            ZipOutputStream zip = new ZipOutputStream(stream);
            Map<ROI3D, String> nameMap = new IdentityHashMap<>();
            for (ROI3D roi3D : this) {
                Object3D object3D = roi3D.getObject3D();
                String name = StringUtils.makeUniqueString(StringUtils.makeFilesystemCompatible(object3D.getName()), "_", nameMap.values());
                nameMap.put(roi3D, name);

                progressInfo.log("Saving 3D object " + name);
                zip.putNextEntry(new ZipEntry(name + ".3droi"));

                // Convert to voxels if needed
                ExtendedObject3DVoxels voxels;
                if(object3D instanceof ExtendedObject3DVoxels) {
                    voxels = (ExtendedObject3DVoxels) object3D;
                }
                else {
                    voxels = new ExtendedObject3DVoxels(object3D);
                }


                voxels.saveObjectToStream(new UnclosableOutputStream(zip));
                zip.closeEntry();
            }

            // Save JIPipe metadata
            zip.putNextEntry(new ZipEntry("jipipe-metadata.json"));
            Map<String, ROI3D> metadataMap = new HashMap<>();
            for (ROI3D roi3D : this) {
                String name = nameMap.get(roi3D);
                metadataMap.put(name, roi3D);
            }
            JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(new UnclosableOutputStream(zip), metadataMap);
            zip.closeEntry();

            zip.close();

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public ResultsTableData measure(ImageHandler referenceImage, int measurements, boolean physicalUnits, String columnPrefix, JIPipeProgressInfo progressInfo) {
        ResultsTableData target = new ResultsTableData();
        IJ3DUtils.measureRoi3d(referenceImage, this, measurements, physicalUnits, columnPrefix, target, progressInfo);
        return target;
    }

    /**
     * Gets the bounds of the 3D ROI
     * @return array with two 3D vectors, the first one being the location and the other one containing the width, height, and depth
     */
    public Vector3D[] getBounds() {
        if(isEmpty()) {
            return new Vector3D[] {
              new Vector3D(0,0,0),
              new Vector3D(0,0,0)
            };
        }
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (ROI3D roi3D : this) {
            Object3D object3D = roi3D.getObject3D();
            minX = Math.min(minX, object3D.getXmin());
            maxX = Math.max(maxX, object3D.getXmax());
            minY = Math.min(minY, object3D.getYmin());
            maxY = Math.max(maxY, object3D.getYmax());
            minZ = Math.min(minZ, object3D.getZmin());
            maxZ = Math.max(maxZ, object3D.getZmax());
        }
        return new Vector3D[] {
          new Vector3D(minX, minY, minZ),
          new Vector3D(maxX - minX, maxY - minY, maxZ - minZ)
        };
    }

    public Objects3DPopulation toPopulation() {
        Objects3DPopulation population = new Objects3DPopulation();
        for (ROI3D roi3D : this) {
            population.addObject(roi3D.getObject3D());
        }
        return population;
    }

    public Objects3DPopulation toPopulation(int channel, int frame) {
        Objects3DPopulation population = new Objects3DPopulation();
        for (ROI3D roi3D : this) {
            if(roi3D.sameChannel(channel) && roi3D.sameFrame(frame)) {
                population.addObject(roi3D.getObject3D());
            }
        }
        return population;
    }

    public ROI3DListData filteredForFrameAndChannel(int channel, int frame) {
        ROI3DListData listData = new ROI3DListData();
        for (ROI3D roi3D : this) {
            if(roi3D.sameChannel(channel) && roi3D.sameFrame(frame)) {
                listData.add(roi3D);
            }
        }
        return listData;
    }

    /**
     * Adds ROI from a population
     * @param population the population
     * @param channel the channel (one-based)
     * @param frame the frame (one-based)
     */
    public void addFromPopulation(Objects3DPopulation population, int channel, int frame) {
        for (int i = 0; i < population.getNbObjects(); i++) {
            ROI3D roi3D = new ROI3D(population.getObject(i));
            roi3D.setChannel(channel);
            roi3D.setFrame(frame);
            add(roi3D);
        }
    }

    public void logicalAnd() {
        if(!isEmpty()) {
            Object3DVoxels voxels = new ExtendedObject3DVoxels();
            voxels.addVoxelsIntersection(new ArrayList<>(stream().map(ROI3D::getObject3D).collect(Collectors.toList())));
            clear();
            if(!voxels.isEmpty()) {
                add(new ROI3D(voxels));
            }
        }
    }

    public void logicalOr() {
        if(!isEmpty()) {
            Object3DVoxels voxels = new ExtendedObject3DVoxels();
            voxels.addVoxelsUnion(new ArrayList<>(stream().map(ROI3D::getObject3D).collect(Collectors.toList())));
            clear();
            add(new ROI3D(voxels));
        }
    }

    public void logicalXor() {
        ROI3DListData or = new ROI3DListData();
        ROI3DListData and = new ROI3DListData();
        or.addAll(this);
        and.addAll(this);
        or.logicalOr();
        and.logicalAnd();
        clear();
        if(!or.isEmpty() && !and.isEmpty()) {
            for (ROI3D r1 : or) {
                Object3DVoxels voxels = r1.getObject3D().getObject3DVoxels();
                for (ROI3D r2 : and) {
                    voxels.substractObject(r2.getObject3D());
                }
                if(!voxels.isEmpty()) {
                    add(new ROI3D(voxels));
                }
            }
        }
    }

    public ImagePlus createBlankCanvas(String title, BitDepth bitDepth) {
        int width = 1;
        int height = 1;
        int nSlices = 1;
        int nChannels = 1;
        int nFrames = 1;
        Vector3D[] bounds = getBounds();

        width = (int) Math.max(width, bounds[0].x + bounds[1].x);
        height = (int) Math.max(height, bounds[0].y + bounds[1].y);
        nSlices = (int) Math.max(nSlices, bounds[0].z + bounds[1].z);

        for (ROI3D roi3D : this) {
            nChannels = Math.max(nChannels, roi3D.getChannel());
            nFrames = Math.max(nFrames, roi3D.getFrame());
        }

        return IJ.createHyperStack(title, width, height, nChannels, nSlices, nFrames, bitDepth.getBitDepth());
    }

    public ImagePlus createBlankCanvas(String title, int bitDepth) {
        int width = 1;
        int height = 1;
        int nSlices = 1;
        int nChannels = 1;
        int nFrames = 1;
        Vector3D[] bounds = getBounds();

        width = (int) Math.max(width, bounds[0].x + bounds[1].x);
        height = (int) Math.max(height, bounds[0].y + bounds[1].y);
        nSlices = (int) Math.max(nSlices, bounds[0].z + bounds[1].z);

        for (ROI3D roi3D : this) {
            nChannels = Math.max(nChannels, roi3D.getChannel());
            nFrames = Math.max(nFrames, roi3D.getFrame());
        }

        return IJ.createHyperStack(title, width, height, nChannels, nSlices, nFrames, bitDepth);
    }

    public ImagePlus toMask(ImagePlus referenceImage, JIPipeProgressInfo progressInfo) {
        ImagePlus outputImage;
        if(referenceImage != null) {
            outputImage = IJ.createHyperStack("Mask",
                    referenceImage.getWidth(),
                    referenceImage.getHeight(),
                    referenceImage.getNChannels(),
                    referenceImage.getNSlices(),
                    referenceImage.getNFrames(),
                    8);
        }
        else {
            outputImage = createBlankCanvas("Mask", BitDepth.Grayscale8u);
        }
        Map<ImageSliceIndex, List<ROI3D>> groups = groupByPosition(true, true);
        IJ3DUtils.forEach3DIn5DWrite(outputImage, (ih, index, ctProgress) -> {
            ROI3DListData toRender = new ROI3DListData();
            toRender.addAll(groups.getOrDefault(new ImageSliceIndex(-1, -1, -1), Collections.emptyList()));
            toRender.addAll(groups.getOrDefault(new ImageSliceIndex(index.getC(), -1, index.getT()), Collections.emptyList()));
            toRender.toPopulation().draw(ih, 255);
        }, progressInfo);
        return outputImage;
    }

    public ImagePlus toLabels(ImagePlus referenceImage, JIPipeProgressInfo progressInfo) {
        int bitDepth;
        if(size() < 250) {
            bitDepth = 8;
        }
        else if(size() < 65000) {
            bitDepth = 16;
        }
        else {
            bitDepth = 32;
        }

        ImagePlus outputImage;
        if(referenceImage != null) {
            outputImage = IJ.createHyperStack("Labels",
                    referenceImage.getWidth(),
                    referenceImage.getHeight(),
                    referenceImage.getNChannels(),
                    referenceImage.getNSlices(),
                    referenceImage.getNFrames(),
                    bitDepth);
        }
        else {
            outputImage = createBlankCanvas("Labels", bitDepth);
        }

        Map<ROI3D, Integer> labelAssignments = new HashMap<>();
        for (int i = 0; i < size(); i++) {
            labelAssignments.put(get(i), i + 1);
        }
        Map<ImageSliceIndex, List<ROI3D>> groups = groupByPosition(true, true);
        IJ3DUtils.forEach3DIn5DWrite(outputImage, (ih, index, ctProgress) -> {
            ROI3DListData toRender = new ROI3DListData();
            toRender.addAll(groups.getOrDefault(new ImageSliceIndex(-1, -1, -1), Collections.emptyList()));
            toRender.addAll(groups.getOrDefault(new ImageSliceIndex(index.getC(), -1, index.getT()), Collections.emptyList()));
            for (ROI3D roi3D : toRender) {
                roi3D.getObject3D().draw(ih, labelAssignments.get(roi3D));
            }
        }, progressInfo);
        return outputImage;
    }

    public void outline(ROI3DOutline outline, boolean ignoreErrors, JIPipeProgressInfo progressInfo) {
        for (int i = 0; i < size(); i++) {
            if(progressInfo.isCancelled())
                return;
            JIPipeProgressInfo roiProgress = progressInfo.resolveAndLog("Generating outline", i, size());
            try {
                ROI3D roi3D = get(i);
                switch (outline) {
                    case BoundingBox: {
                        int[] boundingBox = roi3D.getObject3D().getBoundingBox();
                        int xMin = boundingBox[0];
                        int xMax = boundingBox[1];
                        int yMin = boundingBox[2];
                        int yMax = boundingBox[3];
                        int zMin = boundingBox[4];
                        int zMax = boundingBox[5];
                        ObjectCreator3D creator3D = new ObjectCreator3D(xMax + 1, yMax + 1, zMax + 1);
                        creator3D.createBrick(xMin, xMax, yMin, yMax, zMin, zMax, roi3D.getObject3D().getValue());
                        roi3D.setObject3D(creator3D.getObject3DVoxels(roi3D.getObject3D().getValue()));
                    }
                    break;
                    case BoundingBoxOriented: {
                        ArrayList<Voxel3D> voxelList = roi3D.getObject3D().getBoundingOriented();
                        Object3DVoxels voxels = new ExtendedObject3DVoxels(voxelList);
                        roi3D.setObject3D(voxels);
                    }
                    break;
                    case ConvexHull: {
                        Object3DVoxels convexObject = roi3D.getObject3D().getConvexObject();
                        roi3D.setObject3D(convexObject);
                    }
                    break;
                    case Surface: {
                        Object3DSurface object3DSurface = roi3D.getObject3D().getObject3DSurface();
                        roi3D.setObject3D(object3DSurface);
                    }
                    break;
                    case ConvexSurface: {
                        Object3DSurface convexSurface = roi3D.getObject3D().getConvexSurface();
                        roi3D.setObject3D(convexSurface);
                    }
                    break;
                    default:
                        throw new UnsupportedOperationException("Unsupported outline: " + outline);
                }
            }
            catch (Throwable e) {
                if(!ignoreErrors) {
                    throw e;
                }
                else {
                    roiProgress.log(e.toString());
                    remove(i);
                    --i;
                }
            }
        }
    }
}
