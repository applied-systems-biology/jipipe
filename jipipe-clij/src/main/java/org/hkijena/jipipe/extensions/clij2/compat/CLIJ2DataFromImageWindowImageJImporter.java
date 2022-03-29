package org.hkijena.jipipe.extensions.clij2.compat;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.compat.ImageJDataImporter;
import org.hkijena.jipipe.api.compat.ImageJImportParameters;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;
import org.hkijena.jipipe.extensions.imagejdatatypes.compat.ImagePlusDataFromImageWindowImageJImporter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;

import java.util.List;

@JIPipeDocumentation(name = "Import IJ2 dataset from ImageJ window", description = "Imports an image window into JIPipe")
public class CLIJ2DataFromImageWindowImageJImporter implements ImageJDataImporter {
    @Override
    public JIPipeDataTable importData(List<Object> objects, ImageJImportParameters parameters, JIPipeProgressInfo progressInfo) {
        ImagePlusDataFromImageWindowImageJImporter importer = new ImagePlusDataFromImageWindowImageJImporter(ImagePlusData.class);
        JIPipeDataTable dataTable = importer.importData(objects, parameters, progressInfo);
        dataTable.convert(CLIJImageData.class, new JIPipeProgressInfo());
        return dataTable;
    }

    @Override
    public Class<? extends JIPipeData> getImportedJIPipeDataType() {
        return CLIJImageData.class;
    }

    @Override
    public Class<?> getImportedImageJDataType() {
        return ImagePlus.class;
    }
}
