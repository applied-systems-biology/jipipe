package org.hkijena.jipipe.extensions.cellpose;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.api.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.compat.ui.FileImageJDataImporterUI;
import org.hkijena.jipipe.api.compat.ui.FolderImageJDataExporterUI;
import org.hkijena.jipipe.api.notifications.JIPipeNotification;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationAction;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.cellpose.algorithms.CellPoseAlgorithm;
import org.hkijena.jipipe.extensions.cellpose.algorithms.CellPoseTrainingAlgorithm;
import org.hkijena.jipipe.extensions.cellpose.algorithms.ImportCellPoseModelAlgorithm;
import org.hkijena.jipipe.extensions.cellpose.algorithms.ImportCellPoseSizeModelAlgorithm;
import org.hkijena.jipipe.extensions.cellpose.compat.CellPoseModelImageJExporter;
import org.hkijena.jipipe.extensions.cellpose.compat.CellPoseModelImageJImporter;
import org.hkijena.jipipe.extensions.cellpose.compat.CellPoseSizeModelImageJExporter;
import org.hkijena.jipipe.extensions.cellpose.compat.CellPoseSizeModelImageJImporter;
import org.hkijena.jipipe.extensions.cellpose.datatypes.CellPoseModelData;
import org.hkijena.jipipe.extensions.cellpose.datatypes.CellPoseSizeModelData;
import org.hkijena.jipipe.extensions.cellpose.installers.CellPoseEasyInstaller;
import org.hkijena.jipipe.extensions.cellpose.installers.MinicondaCellPoseEnvInstaller;
import org.hkijena.jipipe.extensions.cellpose.installers.MinicondaCellPoseGPUEnvInstaller;
import org.hkijena.jipipe.extensions.cellpose.installers.PortableCellPoseEnvInstaller;
import org.hkijena.jipipe.extensions.cellpose.installers.PortableCellPoseGPUEnvInstaller;
import org.hkijena.jipipe.extensions.parameters.library.enums.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.extensions.parameters.library.images.ImageParameter;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.extensions.python.PythonEnvironment;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.running.JIPipeRunExecuterUI;
import org.hkijena.jipipe.ui.settings.JIPipeApplicationSettingsUI;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

@Plugin(type = JIPipeJavaExtension.class)
public class CellPoseExtension extends JIPipePrepackagedDefaultJavaExtension {

    public CellPoseExtension() {
        getMetadata().addCategories(PluginCategoriesEnumParameter.CATEGORY_DEEP_LEARNING, PluginCategoriesEnumParameter.CATEGORY_SEGMENTATION, PluginCategoriesEnumParameter.CATEGORY_MACHINE_LEARNING);
        getMetadata().setThumbnail(new ImageParameter(ResourceUtils.getPluginResource("thumbnails/cellpose.png")));
    }

    private static void installCellposeCPU(JIPipeWorkbench workbench) {
        CellPoseSettings settings = CellPoseSettings.getInstance();
        JIPipeParameterTree tree = new JIPipeParameterTree(settings);
        JIPipeParameterAccess parameterAccess = tree.getParameters().get("python-environment");
        PortableCellPoseEnvInstaller installer = new PortableCellPoseEnvInstaller(workbench, parameterAccess);
        JIPipeRunExecuterUI.runInDialog(workbench.getWindow(), installer);
    }

    private static void installCellposeGPU(JIPipeWorkbench workbench) {
        CellPoseSettings settings = CellPoseSettings.getInstance();
        JIPipeParameterTree tree = new JIPipeParameterTree(settings);
        JIPipeParameterAccess parameterAccess = tree.getParameters().get("python-environment");
        PortableCellPoseGPUEnvInstaller installer = new PortableCellPoseGPUEnvInstaller(workbench, parameterAccess);
        JIPipeRunExecuterUI.runInDialog(workbench.getWindow(), installer);
    }

    private static void configureCellpose(JIPipeWorkbench workbench) {
        DocumentTabPane.DocumentTab tab = workbench.getDocumentTabPane().selectSingletonTab(JIPipeProjectWorkbench.TAB_APPLICATION_SETTINGS);
        JIPipeApplicationSettingsUI applicationSettingsUI = (JIPipeApplicationSettingsUI) tab.getContent();
        applicationSettingsUI.selectNode("/Extensions/Cellpose");
    }

    @Override
    public JIPipeAuthorMetadata.List getAcknowledgements() {
        return new JIPipeAuthorMetadata.List(new JIPipeAuthorMetadata("",
                "Carsen",
                "Stringer",
                new StringList("HHMI Janelia Research Campus, Ashburn, VA, USA"),
                "",
                "",
                true,
                false),
                new JIPipeAuthorMetadata("",
                        "Tim",
                        "Wang",
                        new StringList("HHMI Janelia Research Campus, Ashburn, VA, USA"),
                        "",
                        "",
                        false,
                        false),
                new JIPipeAuthorMetadata("",
                        "Michalis",
                        "Michaelos",
                        new StringList("HHMI Janelia Research Campus, Ashburn, VA, USA"),
                        "",
                        "",
                        false,
                        false),
                new JIPipeAuthorMetadata("",
                        "Marius",
                        "Pachitariu",
                        new StringList("HHMI Janelia Research Campus, Ashburn, VA, USA"),
                        "",
                        "",
                        false,
                        true));
    }

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
    public List<ImageIcon> getSplashIcons() {
        return Arrays.asList(UIUtils.getIcon32FromResources("apps/cellpose.png"));
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
        registerSettingsSheet(CellPoseSettings.ID,
                "Cellpose",
                UIUtils.getIconFromResources("apps/cellpose.png"),
                "Extensions",
                UIUtils.getIconFromResources("actions/plugins.png"),
                new CellPoseSettings());
        registerEnvironmentInstaller(PythonEnvironment.class, MinicondaCellPoseEnvInstaller.class, UIUtils.getIconFromResources("apps/cellpose.png"));
        registerEnvironmentInstaller(PythonEnvironment.class, MinicondaCellPoseGPUEnvInstaller.class, UIUtils.getIconFromResources("apps/cellpose.png"));
        registerEnvironmentInstaller(PythonEnvironment.class, PortableCellPoseEnvInstaller.class, UIUtils.getIconFromResources("apps/cellpose.png"));
        registerEnvironmentInstaller(PythonEnvironment.class, PortableCellPoseGPUEnvInstaller.class, UIUtils.getIconFromResources("apps/cellpose.png"));
        registerEnvironmentInstaller(PythonEnvironment.class, CellPoseEasyInstaller.class, UIUtils.getIconFromResources("emblems/vcs-normal.png"));

        registerEnumParameterType("cellpose-model", CellPoseModel.class, "Cellpose model", "A Cellpose model");
        registerEnumParameterType("cellpose-pretrained-model", CellPosePretrainedModel.class, "Cellpose pre-trained model", "A pretrained model for Cellpose");

        registerDatatype("cellpose-model", CellPoseModelData.class, UIUtils.getIconURLFromResources("data-types/cellpose-model.png"));
        registerImageJDataImporter("cellpose-model-from-file", new CellPoseModelImageJImporter(), FileImageJDataImporterUI.class);
        registerImageJDataExporter("cellpose-model-to-directory", new CellPoseModelImageJExporter(), FolderImageJDataExporterUI.class);
        registerDatatype("cellpose-size-model", CellPoseSizeModelData.class, UIUtils.getIconURLFromResources("data-types/cellpose-size-model.png"));
        registerImageJDataImporter("cellpose-size-model-from-file", new CellPoseSizeModelImageJImporter(), FileImageJDataImporterUI.class);
        registerImageJDataExporter("cellpose-size-model-to-directory", new CellPoseSizeModelImageJExporter(), FolderImageJDataExporterUI.class);

        registerNodeType("cellpose", CellPoseAlgorithm.class, UIUtils.getIconURLFromResources("apps/cellpose.png"));
        registerNodeType("cellpose-training", CellPoseTrainingAlgorithm.class, UIUtils.getIconURLFromResources("apps/cellpose.png"));
        registerNodeType("import-cellpose-model", ImportCellPoseModelAlgorithm.class);
        registerNodeType("import-cellpose-size-model", ImportCellPoseSizeModelAlgorithm.class);
    }

    @Override
    public void postprocess() {
        if (!CellPoseSettings.pythonSettingsAreValid()) {
            JIPipeNotification notification = new JIPipeNotification(getDependencyId() + ":python-not-configured");
            notification.setHeading("Cellpose is not configured");
            notification.setDescription("You need to setup a Python environment that contains Cellpose." + "Click 'Install Cellpose' to let JIPipe setup a Python distribution with Cellpose automatically. You can choose between the standard CPU and GPU-accelerated installation (choose CPU if you are unsure). " +
                    "Alternatively, click 'Configure' to visit the settings page with more options, including the selection of an existing Python environment.");
            notification.getActions().add(new JIPipeNotificationAction("Install Cellpose (CPU)",
                    "Installs Cellpose (CPU version)",
                    UIUtils.getIconFromResources("actions/browser-download.png"),
                    CellPoseExtension::installCellposeCPU));
            notification.getActions().add(new JIPipeNotificationAction("Install Cellpose (GPU)",
                    "Installs Cellpose (GPU version)",
                    UIUtils.getIconFromResources("actions/browser-download.png"),
                    CellPoseExtension::installCellposeGPU));
            notification.getActions().add(new JIPipeNotificationAction("Configure Python",
                    "Opens the applications settings page",
                    UIUtils.getIconFromResources("actions/configure.png"),
                    CellPoseExtension::configureCellpose));
            JIPipeNotificationInbox.getInstance().push(notification);
        }
    }
}
