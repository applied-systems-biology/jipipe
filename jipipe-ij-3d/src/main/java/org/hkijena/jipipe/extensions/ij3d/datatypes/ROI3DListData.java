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

import mcib3d.geom.Object3D;
import mcib3d.geom.Objects3DPopulation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.extensions.ij3d.IJ3DUtils;
import org.hkijena.jipipe.extensions.ij3d.utils.ExtendedObject3DVoxels;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.UnclosableInputStream;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UnclosableOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@JIPipeDocumentation(name = "3D ROI list", description = "Collection of 3D ROI")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains one file in *.zip format. " +
        "The *.zip contains multiple 3D ImageJ Suite ROI. Please note that if multiple *.zip files are present, only " +
        "one will be loaded.", jsonSchemaURL = "https://jipipe.org/schemas/datatypes/roi-list-data.schema.json")
public class ROI3DListData extends Objects3DPopulation implements JIPipeData {
    public ROI3DListData() {

    }

    public ROI3DListData(ROI3DListData other) {
        setCalibration(other.getScaleXY(), other.getScaleZ(), other.getUnit());
        for (int i = 0; i < other.getNbObjects(); i++) {
            addObject(IJ3DUtils.duplicateObject3D(other.getObject(i)));
        }
    }

    public ROI3DListData newWithSameCalibration() {
        ROI3DListData result = new ROI3DListData();
        setCalibration(getScaleXY(), getScaleZ(), getUnit());
        return result;
    }

    @Override
    public void exportData(JIPipeWriteDataStorage storage, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        saveObjectsToStream(storage.write(StringUtils.orElse(name, "rois") + ".roi3d.zip"), progressInfo);
    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        ROI3DListData result = new ROI3DListData();
        for (int i = 0; i < getNbObjects(); i++) {
            progressInfo.resolveAndLog("Copy 3D Object", i, getNbObjects());
            result.addObject(IJ3DUtils.duplicateObject3D(getObject(i)));
        }
        return result;
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {

    }

    @Override
    public String toString() {
        return getNbObjects() == 1 ? "1 3D object" : getNbObjects() + " 3D objects";
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
        try (ZipInputStream zipinputstream = new ZipInputStream(inputStream)) {
            ZipEntry zipentry = zipinputstream.getNextEntry();
            while (zipentry != null) {
                //for each entry to be extracted
                String entryName = zipentry.getName();
                progressInfo.log("Loading 3D object " + entryName);
                //IJ.log("entryname=" + entryName);

                // create object
                ExtendedObject3DVoxels obj = new ExtendedObject3DVoxels();
                obj.setValue(1);
                obj.loadObjectFromStream(new UnclosableInputStream(zipinputstream), entryName);
                obj.setName(entryName.substring(0, entryName.length() - 6));

                zipinputstream.closeEntry();
                zipentry = zipinputstream.getNextEntry();


                //Calibration cal = new Calibration();
                //cal.pixelWidth = obj.getResXY();
                //cal.pixelHeight = obj.getResXY();
                //cal.pixelDepth = obj.getResZ();
                //cal.setUnit(obj.getUnits());

                addObject(obj);
            }
            zipinputstream.close();
        }catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveObjectsToStream(OutputStream stream, JIPipeProgressInfo progressInfo) {
        int[] indexes = new int[getNbObjects()];
        for (int i = 0; i < indexes.length; i++) {
            indexes[i] = i;
        }
        saveObjectsToStream(stream, indexes, progressInfo);
    }

    public void saveObjectsToStream(OutputStream stream, int[] indexes, JIPipeProgressInfo progressInfo) {
        String name;

//        for (int i : indexes) {
//            obj = this.getObject(i);
//            obj.saveObject(dir + fs);
//        }
        try {
            //  ZIP
            ZipOutputStream zip = new ZipOutputStream(stream);
            for (int i : indexes) {
                name = this.getObject(i).getName();
                progressInfo.log("Saving 3D object " + name);
                zip.putNextEntry(new ZipEntry(name + ".3droi"));
                Object3D object = getObject(i);

                // Convert to voxels if needed
                ExtendedObject3DVoxels voxels;
                if(object instanceof ExtendedObject3DVoxels) {
                    voxels = (ExtendedObject3DVoxels) object;
                }
                else {
                    voxels = new ExtendedObject3DVoxels(object);
                }
                voxels.saveObjectToStream(new UnclosableOutputStream(zip));
                zip.closeEntry();
            }
            zip.close();

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }


}
