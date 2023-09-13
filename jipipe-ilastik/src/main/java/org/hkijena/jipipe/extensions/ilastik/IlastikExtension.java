package org.hkijena.jipipe.extensions.ilastik;

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
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Plugin(type = JIPipeJavaExtension.class)
public class IlastikExtension extends JIPipePrepackagedDefaultJavaExtension {

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:ilastik",
            JIPipe.getJIPipeVersion(),
            "Ilastik integration");

    public static final JIPipeResourceManager RESOURCES = new JIPipeResourceManager(IlastikExtension.class, "org/hkijena/jipipe/extensions/ilastik");

    public IlastikExtension() {
        getMetadata().addCategories(PluginCategoriesEnumParameter.CATEGORY_DEEP_LEARNING,
                PluginCategoriesEnumParameter.CATEGORY_SEGMENTATION,
                PluginCategoriesEnumParameter.CATEGORY_CLASSIFICATION,
                PluginCategoriesEnumParameter.CATEGORY_MACHINE_LEARNING);
        getMetadata().setThumbnail(new ImageParameter(RESOURCES.getResourceURL("thumbnail.png")));
    }

    private static void easyInstallOmnipose(JIPipeWorkbench workbench) {

    }

    private static void configureOmnipose(JIPipeWorkbench workbench) {
        DocumentTabPane.DocumentTab tab = workbench.getDocumentTabPane().selectSingletonTab(JIPipeProjectWorkbench.TAB_APPLICATION_SETTINGS);
        JIPipeApplicationSettingsUI applicationSettingsUI = (JIPipeApplicationSettingsUI) tab.getContent();
        applicationSettingsUI.selectNode("/Extensions/Ilastik");
    }

    public static void createMissingPythonNotificationIfNeeded(JIPipeNotificationInbox inbox) {
//        if (!OmniposeSettings.pythonSettingsAreValid()) {
//            JIPipeNotification notification = new JIPipeNotification(AS_DEPENDENCY.getDependencyId() + ":python-not-configured");
//            notification.setHeading("Omnipose is not configured");
//            notification.setDescription("You need to setup a Python environment that contains Omnipose." + "Click 'Install Omnipose' to let JIPipe setup a Python distribution with Omnipose automatically. " +
//                    "You can choose between the standard CPU and GPU-accelerated installation (choose CPU if you are unsure). " +
//                    "Alternatively, click 'Configure' to visit the settings page with more options, including the selection of an existing Python environment.\n\n" +
//                    "For more information, please visit https://www.jipipe.org/installation/third-party/omnipose/");
//            notification.getActions().add(new JIPipeNotificationAction("Install Omnipose",
//                    "Installs Omnipose via the EasyInstaller",
//                    UIUtils.getIconInvertedFromResources("actions/browser-download.png"),
//                    JIPipeNotificationAction.Style.Success,
//                    IlastikExtension::easyInstallOmnipose));
//            notification.getActions().add(new JIPipeNotificationAction("Open settings",
//                    "Opens the applications settings page",
//                    UIUtils.getIconFromResources("actions/configure.png"),
//                    IlastikExtension::configureOmnipose));
//            inbox.push(notification);
//        }
    }

    @Override
    public Set<JIPipeDependency> getDependencies() {
        return Sets.newHashSet(CoreExtension.AS_DEPENDENCY, ImageJDataTypesExtension.AS_DEPENDENCY, ImageJAlgorithmsExtension.AS_DEPENDENCY);
    }

    @Override
    public boolean isBeta() {
        return true;
    }

    @Override
    public JIPipeAuthorMetadata.List getAcknowledgements() {
        return new JIPipeAuthorMetadata.List(new JIPipeAuthorMetadata("",
                "Stuart",
                "Berg",
                new StringList("HHMI Janelia Research Campus, Ashburn, Virginia, USA"),
                "",
                "",
                true,
                false),
                new JIPipeAuthorMetadata("",
                        "Dominik",
                        "Kutra",
                        new StringList("HCI/IWR, Heidelberg University, Heidelberg, Germany",
                                "European Molecular Biology Laboratory, Heidelberg, Germany"),
                        "",
                        "",
                        false,
                        false),
                new JIPipeAuthorMetadata("",
                        "Thorben",
                        "Kroeger",
                        new StringList("HCI/IWR, Heidelberg University, Heidelberg, Germany"),
                        "",
                        "",
                        false,
                        false),
                new JIPipeAuthorMetadata("",
                        "Christoph N.",
                        "Straehle",
                        new StringList("HCI/IWR, Heidelberg University, Heidelberg, Germany"),
                        "",
                        "",
                        false,
                        false),
                new JIPipeAuthorMetadata("",
                        "Bernhard X.",
                        "Kausler",
                        new StringList("HCI/IWR, Heidelberg University, Heidelberg, Germany"),
                        "",
                        "",
                        false,
                        false),
                new JIPipeAuthorMetadata("",
                        "Carsten",
                        "Haubold",
                        new StringList("HCI/IWR, Heidelberg University, Heidelberg, Germany"),
                        "",
                        "",
                        false,
                        false),
                new JIPipeAuthorMetadata("",
                        "Martin",
                        "Schiegg",
                        new StringList("HCI/IWR, Heidelberg University, Heidelberg, Germany"),
                        "",
                        "",
                        false,
                        false),
                new JIPipeAuthorMetadata("",
                        "Janez",
                        "Ales",
                        new StringList("HCI/IWR, Heidelberg University, Heidelberg, Germany"),
                        "",
                        "",
                        false,
                        false),
                new JIPipeAuthorMetadata("",
                        "Thorsten",
                        "Beier",
                        new StringList("HCI/IWR, Heidelberg University, Heidelberg, Germany"),
                        "",
                        "",
                        false,
                        false),
                new JIPipeAuthorMetadata("",
                        "Markus",
                        "Rudy",
                        new StringList("HCI/IWR, Heidelberg University, Heidelberg, Germany"),
                        "",
                        "",
                        false,
                        false),
                new JIPipeAuthorMetadata("",
                        "Kemal",
                        "Eren",
                        new StringList("HCI/IWR, Heidelberg University, Heidelberg, Germany"),
                        "",
                        "",
                        false,
                        false),
                new JIPipeAuthorMetadata("",
                        "Jaime I",
                        "Cervantes",
                        new StringList("HCI/IWR, Heidelberg University, Heidelberg, Germany"),
                        "",
                        "",
                        false,
                        false),
                new JIPipeAuthorMetadata("",
                        "Buote",
                        "Xu",
                        new StringList("HCI/IWR, Heidelberg University, Heidelberg, Germany"),
                        "",
                        "",
                        false,
                        false),
                new JIPipeAuthorMetadata("",
                        "Fynn",
                        "Beuttenmueller",
                        new StringList("HCI/IWR, Heidelberg University, Heidelberg, Germany",
                                "European Molecular Biology Laboratory, Heidelberg, Germany"),
                        "",
                        "",
                        false,
                        false),
                new JIPipeAuthorMetadata("",
                        "Adrian",
                        "Wolny",
                        new StringList("HCI/IWR, Heidelberg University, Heidelberg, Germany"),
                        "",
                        "",
                        false,
                        false),
                new JIPipeAuthorMetadata("",
                        "Chong",
                        "Zhang",
                        new StringList("HCI/IWR, Heidelberg University, Heidelberg, Germany"),
                        "",
                        "",
                        false,
                        false),
                new JIPipeAuthorMetadata("",
                        "Ullrich",
                        "Koethe",
                        new StringList("HCI/IWR, Heidelberg University, Heidelberg, Germany"),
                        "",
                        "",
                        false,
                        false),
                new JIPipeAuthorMetadata("",
                        "Fred A.",
                        "Hamprecht",
                        new StringList("HCI/IWR, Heidelberg University, Heidelberg, Germany"),
                        "",
                        "fred.hamprecht@iwr.uni-heidelberg.de",
                        false,
                        true),
                new JIPipeAuthorMetadata("",
                        "Anna",
                        "Kreshuk",
                        new StringList("HCI/IWR, Heidelberg University, Heidelberg, Germany",
                                "European Molecular Biology Laboratory, Heidelberg, Germany"),
                        "",
                        "anna.kreshuk@embl.de",
                        false,
                        true));
    }

    @Override
    public StringList getDependencyCitations() {
        StringList strings = new StringList();
        strings.add("Berg, S., Kutra, D., Kroeger, T., Straehle, C. N., Kausler, B. X., Haubold, C., ... & Kreshuk, A. (2019). Ilastik: interactive machine learning for (bio) image analysis. Nature methods, 16(12), 1226-1232.");
        return strings;
    }

    @Override
    public String getName() {
        return "Ilastik integration";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Integrates Ilastik");
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:ilastik";
    }

    @Override
    public List<ImageIcon> getSplashIcons() {
        return Arrays.asList(RESOURCES.getIcon32FromResources("ilastik.png"));
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
//        registerSettingsSheet(OmniposeSettings.ID,
//                "Omnipose",
//                "Connect existing Omnipose installations to JIPipe or automatically install a new Omnipose environment if none is available",
//                RESOURCES.getIconFromResources("omnipose.png"),
//                "Extensions",
//                UIUtils.getIconFromResources("actions/plugins.png"),
//                new OmniposeSettings());
//        registerEnumParameterType("omnipose-model", OmniposeModel.class, "Omnipose model", "An Omnipose model");
//        registerEnumParameterType("omnipose-pretrained-model", OmniposePretrainedModel.class, "Omnipose pre-trained model", "A pretrained model for Omnipose");
//
//        registerNodeType("omnipose", OmniposeAlgorithm.class, RESOURCES.getIcon16URLFromResources("omnipose.png"));
//        registerNodeType("omnipose-training", OmniposeTrainingAlgorithm.class, RESOURCES.getIcon16URLFromResources("omnipose.png"));
//
//        registerEnvironmentInstaller(PythonEnvironment.class, OmniposeEasyInstaller.class, UIUtils.getIconFromResources("emblems/vcs-normal.png"));
    }

    @Override
    public void postprocess(JIPipeProgressInfo progressInfo) {
        createMissingPythonNotificationIfNeeded(JIPipeNotificationInbox.getInstance());
    }
}
