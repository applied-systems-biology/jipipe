package org.hkijena.jipipe.extensions.cellpose;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
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
import org.hkijena.jipipe.extensions.cellpose.datatypes.CellPoseModelData;
import org.hkijena.jipipe.extensions.cellpose.datatypes.CellPoseSizeModelData;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.extensions.python.PythonEnvironment;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.running.JIPipeRunExecuterUI;
import org.hkijena.jipipe.ui.settings.JIPipeApplicationSettingsUI;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

@Plugin(type = JIPipeJavaExtension.class)
public class CellPoseExtension extends JIPipePrepackagedDefaultJavaExtension {
    private static void installCellposeCPU(JIPipeWorkbench workbench) {
        CellPoseSettings settings = CellPoseSettings.getInstance();
        JIPipeParameterTree tree = new JIPipeParameterTree(settings);
        JIPipeParameterAccess parameterAccess = tree.getParameters().get("python-environment");
        MinicondaCellPoseEnvInstaller installer = new MinicondaCellPoseEnvInstaller(workbench, parameterAccess);
        JIPipeRunExecuterUI.runInDialog(workbench.getWindow(), installer);
    }

    private static void installCellposeGPU(JIPipeWorkbench workbench) {
        CellPoseSettings settings = CellPoseSettings.getInstance();
        JIPipeParameterTree tree = new JIPipeParameterTree(settings);
        JIPipeParameterAccess parameterAccess = tree.getParameters().get("python-environment");
        MinicondaCellPoseGPUEnvInstaller installer = new MinicondaCellPoseGPUEnvInstaller(workbench, parameterAccess);
        JIPipeRunExecuterUI.runInDialog(workbench.getWindow(), installer);
    }

    private static void configureCellpose(JIPipeWorkbench workbench) {
        DocumentTabPane.DocumentTab tab = workbench.getDocumentTabPane().selectSingletonTab(JIPipeProjectWorkbench.TAB_APPLICATION_SETTINGS);
        JIPipeApplicationSettingsUI applicationSettingsUI = (JIPipeApplicationSettingsUI) tab.getContent();
        applicationSettingsUI.selectNode("/Extensions/Cellpose");
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
    public String getDependencyVersion() {
        return "1.66.0";
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

        registerEnumParameterType("cellpose-model", CellPoseModel.class, "Cellpose model", "A Cellpose model");
        registerEnumParameterType("cellpose-pretrained-model", CellPosePretrainedModel.class, "Cellpose pre-trained model", "A pretrained model for Cellpose");

        registerDatatype("cellpose-model", CellPoseModelData.class, UIUtils.getIconURLFromResources("data-types/cellpose-model.png"));
        registerDatatype("cellpose-size-model", CellPoseSizeModelData.class, UIUtils.getIconURLFromResources("data-types/cellpose-size-model.png"));

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
            notification.setDescription("You need to setup a Python environment that contains Cellpose.");
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
