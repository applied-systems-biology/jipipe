package org.hkijena.jipipe.extensions.r;

import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.notifications.JIPipeNotification;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationAction;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.extensions.r.algorithms.ImportRDatasetAlgorithm;
import org.hkijena.jipipe.extensions.r.algorithms.IteratingRScriptAlgorithm;
import org.hkijena.jipipe.extensions.r.algorithms.MergingRScriptAlgorithm;
import org.hkijena.jipipe.extensions.r.installers.REnvInstaller;
import org.hkijena.jipipe.extensions.r.parameters.RScriptParameter;
import org.hkijena.jipipe.extensions.r.ui.RTokenMaker;
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
public class RExtension extends JIPipePrepackagedDefaultJavaExtension {
    private static void installR(JIPipeWorkbench workbench) {
        RExtensionSettings settings = RExtensionSettings.getInstance();
        JIPipeParameterTree tree = new JIPipeParameterTree(settings);
        JIPipeParameterAccess parameterAccess = tree.getParameters().get("r-environment");
        REnvInstaller installer = new REnvInstaller(workbench, parameterAccess);
        JIPipeRunExecuterUI.runInDialog(workbench.getWindow(), installer);
    }

    private static void configureR(JIPipeWorkbench workbench) {
        DocumentTabPane.DocumentTab tab = workbench.getDocumentTabPane().selectSingletonTab(JIPipeProjectWorkbench.TAB_APPLICATION_SETTINGS);
        JIPipeApplicationSettingsUI applicationSettingsUI = (JIPipeApplicationSettingsUI) tab.getContent();
        applicationSettingsUI.selectNode("/Extensions/R integration");
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
    public String getDependencyVersion() {
        return "1.72.2";
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
    }

    @Override
    public void postprocess() {
        if (!RExtensionSettings.RSettingsAreValid()) {
            JIPipeNotification notification = new JIPipeNotification(getDependencyId() + ":r-not-configured");
            notification.setHeading("R is not configured");
            notification.setDescription("To make use of R within JIPipe, you need to either provide JIPipe with an " +
                    "existing R installation or let JIPipe install a R distribution for you. Please note that we cannot provide you with an R " +
                    "setup tool for Linux and Mac.");
            notification.getActions().add(new JIPipeNotificationAction("Install R",
                    "Installs R (Currently only Windows)",
                    UIUtils.getIconFromResources("actions/browser-download.png"),
                    RExtension::installR));
            notification.getActions().add(new JIPipeNotificationAction("Configure R",
                    "Opens the applications settings page",
                    UIUtils.getIconFromResources("actions/configure.png"),
                    RExtension::configureR));
            JIPipeNotificationInbox.getInstance().push(notification);
        }
    }
}
