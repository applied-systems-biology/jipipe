package org.hkijena.jipipe.extensions.cellpose;

import com.google.common.collect.Sets;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.JIPipeMutableDependency;
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
import org.hkijena.jipipe.extensions.cellpose.algorithms.CellposeAlgorithm;
import org.hkijena.jipipe.extensions.cellpose.algorithms.CellposeTrainingAlgorithm;
import org.hkijena.jipipe.extensions.cellpose.algorithms.ImportCellposeModelAlgorithm;
import org.hkijena.jipipe.extensions.cellpose.algorithms.ImportCellposeSizeModelAlgorithm;
import org.hkijena.jipipe.extensions.cellpose.algorithms.deprecated.CellposeAlgorithm_Old;
import org.hkijena.jipipe.extensions.cellpose.algorithms.deprecated.CellposeTrainingAlgorithm_Old;
import org.hkijena.jipipe.extensions.cellpose.compat.CellposeModelImageJExporter;
import org.hkijena.jipipe.extensions.cellpose.compat.CellposeModelImageJImporter;
import org.hkijena.jipipe.extensions.cellpose.compat.CellposeSizeModelImageJExporter;
import org.hkijena.jipipe.extensions.cellpose.compat.CellposeSizeModelImageJImporter;
import org.hkijena.jipipe.extensions.cellpose.datatypes.CellposeModelData;
import org.hkijena.jipipe.extensions.cellpose.datatypes.CellposeSizeModelData;
import org.hkijena.jipipe.extensions.cellpose.installers.*;
import org.hkijena.jipipe.extensions.core.CoreExtension;
import org.hkijena.jipipe.extensions.imagejalgorithms.ImageJAlgorithmsExtension;
import org.hkijena.jipipe.extensions.imagejdatatypes.ImageJDataTypesExtension;
import org.hkijena.jipipe.extensions.parameters.library.images.ImageParameter;
import org.hkijena.jipipe.extensions.parameters.library.jipipe.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.extensions.python.PythonEnvironment;
import org.hkijena.jipipe.extensions.python.PythonExtension;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.running.JIPipeRunExecuterUI;
import org.hkijena.jipipe.ui.settings.JIPipeApplicationSettingsUI;
import org.hkijena.jipipe.utils.JIPipeResourceManager;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Plugin(type = JIPipeJavaExtension.class)
public class CellposeExtension extends JIPipePrepackagedDefaultJavaExtension {

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:cellpose",
            JIPipe.getJIPipeVersion(),
            "Cellpose integration");

    public static final JIPipeResourceManager RESOURCES = new JIPipeResourceManager(CellposeExtension.class, "org/hkijena/jipipe/extensions/cellpose");

    public CellposeExtension() {
        getMetadata().addCategories(PluginCategoriesEnumParameter.CATEGORY_DEEP_LEARNING, PluginCategoriesEnumParameter.CATEGORY_SEGMENTATION, PluginCategoriesEnumParameter.CATEGORY_MACHINE_LEARNING);
        getMetadata().setThumbnail(new ImageParameter(ResourceUtils.getPluginResource("thumbnails/cellpose.png")));
    }

    private static void easyInstallCellpose(JIPipeWorkbench workbench) {
        CellposeSettings settings = CellposeSettings.getInstance();
        JIPipeParameterTree tree = new JIPipeParameterTree(settings);
        JIPipeParameterAccess parameterAccess = tree.getParameters().get("python-environment");
        CellposeEasyInstaller installer = new CellposeEasyInstaller(workbench, parameterAccess);
        JIPipeRunExecuterUI.runInDialog(workbench.getWindow(), installer);
    }

    private static void configureCellpose(JIPipeWorkbench workbench) {
        DocumentTabPane.DocumentTab tab = workbench.getDocumentTabPane().selectSingletonTab(JIPipeProjectWorkbench.TAB_APPLICATION_SETTINGS);
        JIPipeApplicationSettingsUI applicationSettingsUI = (JIPipeApplicationSettingsUI) tab.getContent();
        applicationSettingsUI.selectNode("/Extensions/Cellpose");
    }

    public static void createMissingPythonNotificationIfNeeded(JIPipeNotificationInbox inbox) {
        if (!CellposeSettings.pythonSettingsAreValid()) {
            JIPipeNotification notification = new JIPipeNotification(AS_DEPENDENCY.getDependencyId() + ":python-not-configured");
            notification.setHeading("Cellpose is not configured");
            notification.setDescription("You need to setup a Python environment that contains Cellpose." + "Click 'Install Cellpose' to let JIPipe setup a Python distribution with Cellpose automatically. " +
                    "You can choose between the standard CPU and GPU-accelerated installation (choose CPU if you are unsure). " +
                    "Alternatively, click 'Configure' to visit the settings page with more options, including the selection of an existing Python environment.\n\n" +
                    "For more information, please visit https://www.jipipe.org/installation/third-party/cellpose/");
            notification.getActions().add(new JIPipeNotificationAction("Install Cellpose",
                    "Installs Cellpose via the EasyInstaller",
                    UIUtils.getIconInvertedFromResources("actions/browser-download.png"),
                    JIPipeNotificationAction.Style.Success,
                    CellposeExtension::easyInstallCellpose));
            notification.getActions().add(new JIPipeNotificationAction("Open settings",
                    "Opens the applications settings page",
                    UIUtils.getIconFromResources("actions/configure.png"),
                    CellposeExtension::configureCellpose));
            inbox.push(notification);
        }
    }

    @Override
    public Set<JIPipeDependency> getDependencies() {
        return Sets.newHashSet(CoreExtension.AS_DEPENDENCY, ImageJDataTypesExtension.AS_DEPENDENCY, PythonExtension.AS_DEPENDENCY, ImageJAlgorithmsExtension.AS_DEPENDENCY);
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
        registerSettingsSheet(CellposeSettings.ID,
                "Cellpose",
                "Connect existing Cellpose installations to JIPipe or automatically install a new Cellpose environment if none is available",
                UIUtils.getIconFromResources("apps/cellpose.png"),
                "Extensions",
                UIUtils.getIconFromResources("actions/plugins.png"),
                new CellposeSettings());
        registerEnvironmentInstaller(PythonEnvironment.class, MinicondaCellposeEnvInstaller.class, UIUtils.getIconFromResources("apps/cellpose.png"));
        registerEnvironmentInstaller(PythonEnvironment.class, MinicondaCellposeGPUEnvInstaller.class, UIUtils.getIconFromResources("apps/cellpose.png"));
        registerEnvironmentInstaller(PythonEnvironment.class, PortableCellposeEnvInstaller.class, UIUtils.getIconFromResources("apps/cellpose.png"));
        registerEnvironmentInstaller(PythonEnvironment.class, PortableCellposeGPUEnvInstaller.class, UIUtils.getIconFromResources("apps/cellpose.png"));
        registerEnvironmentInstaller(PythonEnvironment.class, CellposeEasyInstaller.class, UIUtils.getIconFromResources("emblems/vcs-normal.png"));

        registerEnumParameterType("cellpose-model", CellposeModel.class, "Cellpose model", "A Cellpose model");
        registerEnumParameterType("cellpose-pretrained-model", CellposePretrainedModel.class, "Cellpose pre-trained model", "A pretrained model for Cellpose");

        registerDatatype("cellpose-model", CellposeModelData.class, UIUtils.getIconURLFromResources("data-types/cellpose-model.png"));
        registerImageJDataImporter("cellpose-model-from-file", new CellposeModelImageJImporter(), FileImageJDataImporterUI.class);
        registerImageJDataExporter("cellpose-model-to-directory", new CellposeModelImageJExporter(), FolderImageJDataExporterUI.class);
        registerDatatype("cellpose-size-model", CellposeSizeModelData.class, UIUtils.getIconURLFromResources("data-types/cellpose-size-model.png"));
        registerImageJDataImporter("cellpose-size-model-from-file", new CellposeSizeModelImageJImporter(), FileImageJDataImporterUI.class);
        registerImageJDataExporter("cellpose-size-model-to-directory", new CellposeSizeModelImageJExporter(), FolderImageJDataExporterUI.class);

        registerNodeType("cellpose", CellposeAlgorithm_Old.class, UIUtils.getIconURLFromResources("emblems/vcs-conflicting.png"));
        registerNodeType("cellpose-2", CellposeAlgorithm.class, UIUtils.getIconURLFromResources("apps/cellpose.png"));
        registerNodeType("cellpose-training", CellposeTrainingAlgorithm_Old.class, UIUtils.getIconURLFromResources("emblems/vcs-conflicting.png"));
        registerNodeType("cellpose-training-2", CellposeTrainingAlgorithm.class, UIUtils.getIconURLFromResources("apps/cellpose.png"));
        registerNodeType("import-cellpose-model", ImportCellposeModelAlgorithm.class);
        registerNodeType("import-cellpose-size-model", ImportCellposeSizeModelAlgorithm.class);

        registerProjectTemplatesFromResources(RESOURCES, "templates");
    }

    @Override
    public void postprocess(JIPipeProgressInfo progressInfo) {
        createMissingPythonNotificationIfNeeded(JIPipeNotificationInbox.getInstance());
    }
}
