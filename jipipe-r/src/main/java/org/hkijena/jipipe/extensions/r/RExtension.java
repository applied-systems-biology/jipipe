package org.hkijena.jipipe.extensions.r;

import org.apache.commons.compress.utils.Sets;
import org.apache.commons.lang3.SystemUtils;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.notifications.JIPipeNotification;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationAction;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.core.CoreExtension;
import org.hkijena.jipipe.extensions.imagejdatatypes.ImageJDataTypesExtension;
import org.hkijena.jipipe.extensions.parameters.library.enums.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.extensions.parameters.library.images.ImageParameter;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.extensions.r.algorithms.ImportRDatasetAlgorithm;
import org.hkijena.jipipe.extensions.r.algorithms.IteratingRScriptAlgorithm;
import org.hkijena.jipipe.extensions.r.algorithms.MergingRScriptAlgorithm;
import org.hkijena.jipipe.extensions.r.installers.REasyInstaller;
import org.hkijena.jipipe.extensions.r.installers.REnvInstaller;
import org.hkijena.jipipe.extensions.r.parameters.RScriptParameter;
import org.hkijena.jipipe.extensions.r.ui.RTokenMaker;
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
public class RExtension extends JIPipePrepackagedDefaultJavaExtension {

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:r",
            JIPipe.getJIPipeVersion(),
            "R integration");

    public static final JIPipeResourceManager RESOURCES = new JIPipeResourceManager(RExtension.class, "org/hkijena/jipipe/extensions/r");

    public RExtension() {
        getMetadata().addCategories(PluginCategoriesEnumParameter.CATEGORY_SCRIPTING);
        getMetadata().setThumbnail(new ImageParameter(ResourceUtils.getPluginResource("thumbnails/r.png")));
    }

    private static void installR(JIPipeWorkbench workbench) {
        RExtensionSettings settings = RExtensionSettings.getInstance();
        JIPipeParameterTree tree = new JIPipeParameterTree(settings);
        JIPipeParameterAccess parameterAccess = tree.getParameters().get("r-environment");
        REnvInstaller installer = new REnvInstaller(workbench, parameterAccess);
        JIPipeRunExecuterUI.runInDialog(workbench.getWindow(), installer);
    }

    private static void easyInstallR(JIPipeWorkbench workbench) {
        RExtensionSettings settings = RExtensionSettings.getInstance();
        JIPipeParameterTree tree = new JIPipeParameterTree(settings);
        JIPipeParameterAccess parameterAccess = tree.getParameters().get("r-environment");
        REasyInstaller installer = new REasyInstaller(workbench, parameterAccess);
        JIPipeRunExecuterUI.runInDialog(workbench.getWindow(), installer);
    }

    private static void configureR(JIPipeWorkbench workbench) {
        DocumentTabPane.DocumentTab tab = workbench.getDocumentTabPane().selectSingletonTab(JIPipeProjectWorkbench.TAB_APPLICATION_SETTINGS);
        JIPipeApplicationSettingsUI applicationSettingsUI = (JIPipeApplicationSettingsUI) tab.getContent();
        applicationSettingsUI.selectNode("/Extensions/R integration");
    }

    @Override
    public Set<JIPipeDependency> getDependencies() {
        return Sets.newHashSet(CoreExtension.AS_DEPENDENCY, ImageJDataTypesExtension.AS_DEPENDENCY);
    }

    @Override
    public StringList getDependencyCitations() {
        StringList strings = new StringList();
        strings.add("R Core Team (2017). R: A language and environment for statistical computing. R Foundation for Statistical Computing, Vienna, Austria. URL https://www.R-project.org/.");
        return strings;
    }

    @Override
    public String getName() {
        return "R integration";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Integrates R scripts into JIPipe");
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:r";
    }

    @Override
    public List<ImageIcon> getSplashIcons() {
        return Arrays.asList(UIUtils.getIcon32FromResources("apps/rlogo_icon.png"));
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
        RExtensionSettings extensionSettings = new RExtensionSettings();

        registerEnvironment(REnvironment.class,
                REnvironment.List.class,
                extensionSettings,
                "r",
                "R environment",
                "A R environment",
                UIUtils.getIconFromResources("apps/rlogo_icon.png"));
        registerParameterType("optional-r-environment",
                OptionalREnvironment.class,
                null,
                null,
                "Optional R environment",
                "An optional R environment",
                null);
        registerEnvironmentInstaller(REnvironment.class, REnvInstaller.class, UIUtils.getIconFromResources("actions/browser-download.png"));
        registerEnvironmentInstaller(REnvironment.class, REasyInstaller.class, UIUtils.getIconFromResources("emblems/vcs-normal.png"));

        AbstractTokenMakerFactory atmf = (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();
        atmf.putMapping("text/x-r-script", RTokenMaker.class.getName());

        registerParameterType("r-script",
                RScriptParameter.class,
                null,
                null,
                "R script",
                "An R script",
                null);
        registerSettingsSheet(RExtensionSettings.ID,
                "R integration",
                UIUtils.getIconFromResources("apps/rlogo_icon.png"),
                "Extensions",
                UIUtils.getIconFromResources("actions/plugins.png"),
                extensionSettings);
        registerNodeType("r-script-iterating", IteratingRScriptAlgorithm.class, UIUtils.getIconURLFromResources("apps/rlogo_icon.png"));
        registerNodeType("r-script-merging", MergingRScriptAlgorithm.class, UIUtils.getIconURLFromResources("apps/rlogo_icon.png"));

        registerEnumParameterType("r-import-dataset:dataset", ImportRDatasetAlgorithm.Dataset.class, "R dataset", "A dataset from the R datasets package");
        registerNodeType("r-import-dataset", ImportRDatasetAlgorithm.class, UIUtils.getIconURLFromResources("apps/rlogo_icon.png"));

        registerNodeExamplesFromResources(RESOURCES, "examples");
    }

    @Override
    public void postprocess() {
        if (!RExtensionSettings.RSettingsAreValid()) {
            JIPipeNotification notification = new JIPipeNotification(getDependencyId() + ":r-not-configured");
            notification.setHeading("R is not configured");
            notification.setDescription("To make use of R within JIPipe, you need to either provide JIPipe with an " +
                    "existing R installation or let JIPipe install a R distribution for you. Please note that we cannot provide you with an R " +
                    "setup tool for Linux and Mac.\n\n" +
                    "For more information, please visit https://www.jipipe.org/installation/third-party/r/");
            if (SystemUtils.IS_OS_WINDOWS) {
                notification.getActions().add(new JIPipeNotificationAction("Install R",
                        "Installs a prepackaged version of R",
                        UIUtils.getIconFromResources("actions/browser-download.png"),
                        RExtension::easyInstallR));
            }
            notification.getActions().add(new JIPipeNotificationAction("Configure R",
                    "Opens the applications settings page",
                    UIUtils.getIconFromResources("actions/configure.png"),
                    RExtension::configureR));
            JIPipeNotificationInbox.getInstance().push(notification);
        }
    }
}
