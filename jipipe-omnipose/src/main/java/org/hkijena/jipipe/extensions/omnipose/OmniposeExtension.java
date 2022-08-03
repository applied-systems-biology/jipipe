package org.hkijena.jipipe.extensions.omnipose;

import com.google.common.collect.Sets;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.notifications.JIPipeNotification;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationAction;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.cellpose.CellposePretrainedModel;
import org.hkijena.jipipe.extensions.cellpose.installers.CellposeEasyInstaller;
import org.hkijena.jipipe.extensions.core.CoreExtension;
import org.hkijena.jipipe.extensions.imagejalgorithms.ImageJAlgorithmsExtension;
import org.hkijena.jipipe.extensions.imagejdatatypes.ImageJDataTypesExtension;
import org.hkijena.jipipe.extensions.omnipose.algorithms.OmniposeAlgorithm;
import org.hkijena.jipipe.extensions.omnipose.algorithms.OmniposeTrainingAlgorithm;
import org.hkijena.jipipe.extensions.omnipose.installers.OmniposeEasyInstaller;
import org.hkijena.jipipe.extensions.parameters.library.enums.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.extensions.parameters.library.images.ImageParameter;
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
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Plugin(type = JIPipeJavaExtension.class)
public class OmniposeExtension extends JIPipePrepackagedDefaultJavaExtension {

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:omnipose",
            JIPipe.getJIPipeVersion(),
            "Omnipose integration");

    public static final JIPipeResourceManager RESOURCES = new JIPipeResourceManager(OmniposeExtension.class, "org/hkijena/jipipe/extensions/omnipose");

    public OmniposeExtension() {
        getMetadata().addCategories(PluginCategoriesEnumParameter.CATEGORY_DEEP_LEARNING, PluginCategoriesEnumParameter.CATEGORY_SEGMENTATION, PluginCategoriesEnumParameter.CATEGORY_MACHINE_LEARNING);
        getMetadata().setThumbnail(new ImageParameter(RESOURCES.getResourceURL("thumbnail.png")));
    }

    private static void easyInstallOmnipose(JIPipeWorkbench workbench) {
        OmniposeSettings settings = OmniposeSettings.getInstance();
        JIPipeParameterTree tree = new JIPipeParameterTree(settings);
        JIPipeParameterAccess parameterAccess = tree.getParameters().get("python-environment");
        OmniposeEasyInstaller installer = new OmniposeEasyInstaller(workbench, parameterAccess);
        JIPipeRunExecuterUI.runInDialog(workbench.getWindow(), installer);
    }

    private static void configureOmnipose(JIPipeWorkbench workbench) {
        DocumentTabPane.DocumentTab tab = workbench.getDocumentTabPane().selectSingletonTab(JIPipeProjectWorkbench.TAB_APPLICATION_SETTINGS);
        JIPipeApplicationSettingsUI applicationSettingsUI = (JIPipeApplicationSettingsUI) tab.getContent();
        applicationSettingsUI.selectNode("/Extensions/Omnipose");
    }

    @Override
    public Set<JIPipeDependency> getDependencies() {
        return Sets.newHashSet(CoreExtension.AS_DEPENDENCY, ImageJDataTypesExtension.AS_DEPENDENCY, PythonExtension.AS_DEPENDENCY, ImageJAlgorithmsExtension.AS_DEPENDENCY);
    }

    @Override
    public JIPipeAuthorMetadata.List getAcknowledgements() {
        return new JIPipeAuthorMetadata.List(new JIPipeAuthorMetadata("",
                "Kevin J.",
                "Cutler",
                new StringList("Department of Physics, University of Washington, Seattle, WA 98195, USA"),
                "",
                "",
                true,
                false),
                new JIPipeAuthorMetadata("",
                        "Carsen",
                        "Stringer",
                        new StringList("HHMI Janelia Research Campus, Ashburn, VA, USA"),
                        "",
                        "",
                        false,
                        false),
                new JIPipeAuthorMetadata("",
                        "Paul A.",
                        "Wiggins",
                        new StringList("Department of Physics, University of Washington, Seattle, WA 98195, USA",
                                "Department of Bioengineering, University of Washington, Seattle, WA 98195, USA"),
                        "",
                        "",
                        false,
                        false),
                new JIPipeAuthorMetadata("",
                        "Joseph D.",
                        "Mougous",
                        new StringList("Department of Microbiology, University of Washington, Seattle, WA 98109, USA",
                                "Howard Hughes Medical Institute, University of Washington, Seattle, WA 98195, USA"),
                        "",
                        "",
                        false,
                        true));
    }

    @Override
    public StringList getDependencyCitations() {
        StringList strings = new StringList();
        strings.add("Kevin J. Cutler, Carsen Stringer, Paul A. Wiggins, Joseph D. Mougous bioRxiv 2021.11.03.467199; doi: https://doi.org/10.1101/2021.11.03.467199");
        return strings;
    }

    @Override
    public String getName() {
        return "Omnipose integration";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Integrates Omnipose");
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:omnipose";
    }

    @Override
    public List<ImageIcon> getSplashIcons() {
        return Arrays.asList(RESOURCES.getIcon32FromResources("omnipose.png"));
    }


    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
        registerSettingsSheet(OmniposeSettings.ID,
                "Omnipose",
                RESOURCES.getIconFromResources("omnipose.png"),
                "Extensions",
                UIUtils.getIconFromResources("actions/plugins.png"),
                new OmniposeSettings());
        registerEnumParameterType("omnipose-model", OmniposeModel.class, "Omnipose model", "An Omnipose model");
        registerEnumParameterType("omnipose-pretrained-model", OmniposePretrainedModel.class, "Omnipose pre-trained model", "A pretrained model for Omnipose");

        registerNodeType("omnipose", OmniposeAlgorithm.class, RESOURCES.getIcon16URLFromResources("omnipose.png"));
        registerNodeType("omnipose-training", OmniposeTrainingAlgorithm.class, RESOURCES.getIcon16URLFromResources("omnipose.png"));

        registerEnvironmentInstaller(PythonEnvironment.class, OmniposeEasyInstaller.class, UIUtils.getIconFromResources("emblems/vcs-normal.png"));
    }

    @Override
    public void postprocess() {
        if (!OmniposeSettings.pythonSettingsAreValid()) {
            JIPipeNotification notification = new JIPipeNotification(getDependencyId() + ":python-not-configured");
            notification.setHeading("Omnipose is not configured");
            notification.setDescription("You need to setup a Python environment that contains Omnipose." + "Click 'Install Omnipose' to let JIPipe setup a Python distribution with Omnipose automatically. " +
                    "You can choose between the standard CPU and GPU-accelerated installation (choose CPU if you are unsure). " +
                    "Alternatively, click 'Configure' to visit the settings page with more options, including the selection of an existing Python environment.\n\n" +
                    "For more information, please visit https://www.jipipe.org/installation/third-party/omnipose/");
            notification.getActions().add(new JIPipeNotificationAction("Install Omnipose",
                    "Installs Omnipose via the EasyInstaller",
                    UIUtils.getIconFromResources("actions/browser-download.png"),
                    OmniposeExtension::easyInstallOmnipose));
            notification.getActions().add(new JIPipeNotificationAction("Configure Python",
                    "Opens the applications settings page",
                    UIUtils.getIconFromResources("actions/configure.png"),
                    OmniposeExtension::configureOmnipose));
            JIPipeNotificationInbox.getInstance().push(notification);
        }
    }
}
