package org.hkijena.jipipe.extensions.cellpose;

import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.cellpose.algorithms.CellPoseAlgorithm;
import org.hkijena.jipipe.extensions.cellpose.algorithms.CellPoseTrainingAlgorithm;
import org.hkijena.jipipe.extensions.cellpose.algorithms.FileBasedCellPoseAlgorithm;
import org.hkijena.jipipe.extensions.cellpose.algorithms.FileBasedCellPoseTrainingAlgorithm;
import org.hkijena.jipipe.extensions.cellpose.algorithms.ImportCellPoseModelAlgorithm;
import org.hkijena.jipipe.extensions.cellpose.algorithms.ImportCellPoseSizeModelAlgorithm;
import org.hkijena.jipipe.extensions.cellpose.datatypes.CellPoseModelData;
import org.hkijena.jipipe.extensions.cellpose.datatypes.CellPoseSizeModelData;
import org.hkijena.jipipe.extensions.parameters.primitives.HTMLText;
import org.hkijena.jipipe.extensions.parameters.primitives.StringList;
import org.hkijena.jipipe.extensions.python.PythonEnvironment;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

@Plugin(type = JIPipeJavaExtension.class)
public class CellPoseExtension extends JIPipePrepackagedDefaultJavaExtension {
    @Override
    public StringList getDependencyCitations() {
        StringList strings = new StringList();
        strings.add("Stringer, C., Wang, T., Michaelos, M., & Pachitariu, M. (2021). Cellpose: a generalist algorithm for cellular segmentation. Nature Methods, 18(1), 100-106.");
        return strings;
    }

    @Override
    public String getName() {
        return "Cellpose integration";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Integrates Cellpose");
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:cellpose";
    }

    @Override
    public String getDependencyVersion() {
        return "2021.6";
    }

    @Override
    public List<ImageIcon> getSplashIcons() {
        return Arrays.asList(UIUtils.getIcon32FromResources("apps/cellpose.png"));
    }

    @Override
    public void register() {
        registerSettingsSheet(CellPoseSettings.ID,
                "Cellpose",
                UIUtils.getIconFromResources("apps/cellpose.png"),
                "Extensions",
                UIUtils.getIconFromResources("actions/plugins.png"),
                new CellPoseSettings());
        registerEnvironmentInstaller(PythonEnvironment.class, CellPoseEnvInstaller.class, UIUtils.getIconFromResources("apps/cellpose.png"));
        registerEnvironmentInstaller(PythonEnvironment.class, CellPoseGPUEnvInstaller.class, UIUtils.getIconFromResources("apps/cellpose.png"));

        registerEnumParameterType("cellpose-model", CellPoseModel.class, "Cellpose model", "A Cellpose model");
        registerEnumParameterType("cellpose-pretrained-model", CellPosePretrainedModel.class, "Cellpose pre-trained model", "A pretrained model for Cellpose");

        registerDatatype("cellpose-model", CellPoseModelData.class, UIUtils.getIconURLFromResources("data-types/cellpose-model.png"), null, null);
        registerDatatype("cellpose-size-model", CellPoseSizeModelData.class, UIUtils.getIconURLFromResources("data-types/cellpose-size-model.png"), null, null);

        registerNodeType("cellpose", CellPoseAlgorithm.class, UIUtils.getIconURLFromResources("apps/cellpose.png"));
        registerNodeType("cellpose-training", CellPoseTrainingAlgorithm.class, UIUtils.getIconURLFromResources("apps/cellpose.png"));
        registerNodeType("cellpose-file-based", FileBasedCellPoseAlgorithm.class, UIUtils.getIconURLFromResources("apps/cellpose.png"));
        registerNodeType("cellpose-training-file-based", FileBasedCellPoseTrainingAlgorithm.class, UIUtils.getIconURLFromResources("apps/cellpose.png"));
        registerNodeType("import-cellpose-model", ImportCellPoseModelAlgorithm.class);
        registerNodeType("import-cellpose-size-model", ImportCellPoseSizeModelAlgorithm.class);
    }
}
