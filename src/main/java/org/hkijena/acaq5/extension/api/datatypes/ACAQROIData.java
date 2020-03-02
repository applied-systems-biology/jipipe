package org.hkijena.acaq5.extension.api.datatypes;

import ij.gui.Roi;
import ij.io.RoiDecoder;
import ij.io.RoiEncoder;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.utils.PathUtils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@ACAQDocumentation(name = "ROI", description = "Collection of ROI")
public class ACAQROIData implements ACAQData {
    private List<Roi> roi;

    public ACAQROIData(Path storageFilePath) throws IOException {
        this.roi = loadRoiListFromFile(PathUtils.findFileByExtensionIn(storageFilePath, ".zip"));
    }

    public ACAQROIData(List<Roi> roi) {
        this.roi = roi;
    }

    public List<Roi> getROI() {
        return roi;
    }

    @Override
    public void saveTo(Path storageFilePath, String name) {
        // Code adapted from ImageJ RoiManager class
        try {
            ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(storageFilePath.resolve(name + ".zip").toFile())));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(zos));
            RoiEncoder re = new RoiEncoder(out);
            for (int i = 0; i < this.roi.size(); i++) {
                String label = name + "-" + i;
                Roi roi = this.roi.get(i);
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

    /**
     * Loads a set of ROI from a zip file
     *
     * @param fileName
     * @return
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
