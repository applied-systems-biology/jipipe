package org.hkijena.acaq5.extension.api.datatypes;

import ij.gui.Roi;
import ij.io.RoiEncoder;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.data.ACAQData;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@ACAQDocumentation(name = "ROI", description = "Collection of ROI")
public class ACAQROIData implements ACAQData {
    private List<Roi> roi;

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
}
