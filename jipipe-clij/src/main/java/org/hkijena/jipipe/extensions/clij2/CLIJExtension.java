package org.hkijena.jipipe.extensions.clij2;

import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.clij2.algorithms.Clij2GaussianBlur2d;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageDataImageJAdapter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageToImagePlusDataConverter;
import org.hkijena.jipipe.extensions.clij2.datatypes.ImagePlusDataToCLIJImageDataConverter;
import org.hkijena.jipipe.extensions.imagejdatatypes.compat.ImagePlusDataImporterUI;
import org.hkijena.jipipe.extensions.imagejdatatypes.resultanalysis.ImageDataSlotRowUI;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.plugin.Plugin;

/**
 * Integrates CLIJ
 */
@Plugin(type = JIPipeJavaExtension.class)
public class CLIJExtension  extends JIPipePrepackagedDefaultJavaExtension {
    @Override
    public String getName() {
        return "CLIJ2 integration";
    }

    @Override
    public String getDescription() {
        return "Integrates data types and algorithms for GPU computing based on CLIJ2.";
    }

    @Override
    public void register() {
        registerDatatype("clij2-image",
                CLIJImageData.class,
                UIUtils.getIconURLFromResources("data-types/clij.png"),
                ImageDataSlotRowUI.class,
                null);
        registerDatatypeConversion(new CLIJImageToImagePlusDataConverter());
        registerDatatypeConversion(new ImagePlusDataToCLIJImageDataConverter());
        registerImageJDataAdapter(new CLIJImageDataImageJAdapter(), ImagePlusDataImporterUI.class);
        registerAlgorithms();
    }

    private void registerAlgorithms() {
        registerAlgorithm("clij2:gaussian-blur-2d", Clij2GaussianBlur2d.class, UIUtils.getAlgorithmIconURL("clij.png"));
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:clij2-integration";
    }

    @Override
    public String getDependencyVersion() {
        return "1.0.0";
    }
}
