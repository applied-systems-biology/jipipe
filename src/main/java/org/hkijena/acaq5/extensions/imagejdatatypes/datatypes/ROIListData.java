package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes;

import com.google.common.collect.ImmutableList;
import ij.gui.Roi;
import ij.gui.RoiListener;
import ij.gui.ShapeRoi;
import ij.io.RoiDecoder;
import ij.io.RoiEncoder;
import ij.plugin.frame.RoiManager;
import ij.process.FloatPolygon;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.utils.PathUtils;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Contains {@link Roi}
 */
@ACAQDocumentation(name = "ROI list", description = "Collection of ROI")
public class ROIListData extends ArrayList<Roi> implements ACAQData {

    /**
     * Creates an empty set of ROI
     */
    public ROIListData() {
    }

    /**
     * Loads {@link Roi} from a path that contains a zip file
     *
     * @param storageFilePath path that contains a zip file
     */
    public ROIListData(Path storageFilePath) {
        addAll(loadRoiListFromFile(PathUtils.findFileByExtensionIn(storageFilePath, ".zip")));
    }

    /**
     * Creates a deep copy
     *
     * @param other the original
     */
    public ROIListData(List<Roi> other) {
        for (Roi roi : other) {
            add((Roi) roi.clone());
        }
    }

    /**
     * Initializes from a RoiManager
     *
     * @param roiManager the ROI manager
     */
    public ROIListData(RoiManager roiManager) {
        this.addAll(Arrays.asList(roiManager.getRoisAsArray()));
    }

    @Override
    public void saveTo(Path storageFilePath, String name) {
        // Code adapted from ImageJ RoiManager class
        try {
            ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(storageFilePath.resolve(name + ".zip").toFile())));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(zos));
            RoiEncoder re = new RoiEncoder(out);
            for (int i = 0; i < this.size(); i++) {
                String label = name + "-" + i;
                Roi roi = this.get(i);
                if (roi == null) continue;
                if (!label.endsWith(".roi")) label += ".roi";
                zos.putNextEntry(new ZipEntry(label));
                re.write(roi);
                out.flush();
            }
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ACAQData duplicate() {
        return new ROIListData(this);
    }

    /**
     * Adds the ROI to an existing ROI manager instance
     *
     * @param roiManager the ROI manager
     */
    public void addToRoiManager(RoiManager roiManager) {
        for (Roi roi : this) {
            roiManager.add(roi, -1);
        }
    }

    /**
     * Merges the ROI from another data into this one.
     * Creates copies of the merged {@link Roi}
     *
     * @param other the other data. The entries are copied.
     */
    public void mergeWith(ROIListData other) {
        for (Roi item : other) {
            add((Roi) item.clone());
        }
    }

    /**
     * Splits all {@link ij.gui.ShapeRoi} that consist of multiple sub-Roi into their individual sub-Roi.
     * The original {@link ShapeRoi} is removed.
     */
    public void splitAll() {
        for (Roi target : ImmutableList.copyOf(this)) {
            if(target instanceof ShapeRoi) {
                ShapeRoi shapeRoi = (ShapeRoi)target;
                if(shapeRoi.getRois().length > 1) {
                    remove(shapeRoi);
                    this.addAll(Arrays.asList(shapeRoi.getRois()));
                }
            }
        }
    }

    /**
     * Splits the two {@link ROIListData} into sets of operands. No copies are created.
     * @param lhs the left operands
     * @param rhs the right operands
     * @param perLhs if true, lhs is split into single-component {@link ROIListData}
     * @param perRhs if true, rhs is split into single-component {@link ROIListData}
     * @return List of operands. The key is the left operand and the value is the right operand
     */
    public static List<Map.Entry<ROIListData, ROIListData>> createOperands(ROIListData lhs, ROIListData rhs, boolean perLhs, boolean perRhs) {
        List<Map.Entry<ROIListData, ROIListData>> result = new ArrayList<>();
        List<ROIListData> leftOperands = new ArrayList<>();
        if(perLhs) {
            for (Roi lh : lhs) {
                ROIListData data = new ROIListData();
                data.add(lh);
                leftOperands.add(data);
            }
        }
        else {
            leftOperands.add(lhs);
        }
        if(perRhs) {
            for (Roi rh : rhs) {
                ROIListData data = new ROIListData();
                data.add(rh);
                for (ROIListData leftOperand : leftOperands) {
                    result.add(new AbstractMap.SimpleEntry<>(leftOperand, data));
                }
            }
        }
        else {
            for (ROIListData leftOperand : leftOperands) {
                result.add(new AbstractMap.SimpleEntry<>(leftOperand, rhs));
            }
        }
        return result;
    }


    /**
     * Returns if this ROI list only contains ROI of given type
     *
     * @param type the ROI type. Can be RECTANGLE=0, OVAL=1, POLYGON=2, FREEROI=3, TRACED_ROI=4, LINE=5, POLYLINE=6, FREELINE=7, ANGLE=8, COMPOSITE=9, or POINT=10
     * @return if this ROI list only contains ROI of given type
     */
    public boolean containsOnlyRoisOfType(int type) {
        return size() == countRoisOfType(type);
    }

    /**
     * Counts all ROIs of given type
     *
     * @param type the ROI type. Can be RECTANGLE=0, OVAL=1, POLYGON=2, FREEROI=3, TRACED_ROI=4, LINE=5, POLYLINE=6, FREELINE=7, ANGLE=8, COMPOSITE=9, or POINT=10
     * @return number of ROI with type Roi.POINT
     */
    public int countRoisOfType(int type) {
        int nPointRois = 0;
        for (Roi roi : this)
            if (roi.getType() == type)
                nPointRois++;
        return nPointRois;
    }

    /**
     * Loads a set of ROI from a zip file
     *
     * @param fileName the zip file
     * @return the Roi list
     */
    public static List<Roi> loadRoiListFromFile(Path fileName) {
        // Code adapted from ImageJ RoiManager
        List<Roi> result = new ArrayList<>();
        ZipInputStream in = null;
        ByteArrayOutputStream out = null;
        int nRois = 0;
        try {
            in = new ZipInputStream(new FileInputStream(fileName.toFile()));
            byte[] buf = new byte[1024];
            int len;
            ZipEntry entry = in.getNextEntry();
            while (entry != null) {
                String name = entry.getName();
                if (name.endsWith(".roi")) {
                    out = new ByteArrayOutputStream();
                    while ((len = in.read(buf)) > 0)
                        out.write(buf, 0, len);
                    out.close();
                    byte[] bytes = out.toByteArray();
                    RoiDecoder rd = new RoiDecoder(bytes, name);
                    Roi roi = rd.getRoi();
                    if (roi != null) {
                        name = name.substring(0, name.length() - 4);
                        result.add(roi);
                        nRois++;
                    }
                }
                entry = in.getNextEntry();
            }
            in.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (in != null)
                try {
                    in.close();
                } catch (IOException e) {
                }
            if (out != null)
                try {
                    out.close();
                } catch (IOException e) {
                }
        }
        return result;
    }
}
