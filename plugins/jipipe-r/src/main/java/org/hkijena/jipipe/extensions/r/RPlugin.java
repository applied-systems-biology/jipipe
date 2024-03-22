/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.r;

import org.apache.commons.compress.utils.Sets;
import org.apache.commons.lang3.SystemUtils;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.notifications.JIPipeNotification;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationAction;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.running.JIPipeDesktopRunExecuterUI;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.extensions.core.CorePlugin;
import org.hkijena.jipipe.extensions.imagejdatatypes.ImageJDataTypesPlugin;
import org.hkijena.jipipe.extensions.parameters.library.images.ImageParameter;
import org.hkijena.jipipe.extensions.parameters.library.jipipe.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.extensions.r.algorithms.ImportRDatasetAlgorithm;
import org.hkijena.jipipe.extensions.r.algorithms.IteratingRScriptAlgorithm;
import org.hkijena.jipipe.extensions.r.algorithms.MergingRScriptAlgorithm;
import org.hkijena.jipipe.extensions.r.installers.REasyInstaller;
import org.hkijena.jipipe.extensions.r.installers.REnvInstaller;
import org.hkijena.jipipe.extensions.r.parameters.RScriptParameter;
import org.hkijena.jipipe.extensions.r.ui.RTokenMaker;
import org.hkijena.jipipe.utils.JIPipeResourceManager;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Plugin(type = JIPipeJavaPlugin.class)
public class RPlugin extends JIPipePrepackagedDefaultJavaPlugin {

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:r",
            JIPipe.getJIPipeVersion(),
            "R integration");

    public static final JIPipeResourceManager RESOURCES = new JIPipeResourceManager(RPlugin.class, "org/hkijena/jipipe/extensions/r");

    public RPlugin() {
        getMetadata().addCategories(PluginCategoriesEnumParameter.CATEGORY_SCRIPTING);
        getMetadata().setThumbnail(new ImageParameter(ResourceUtils.getPluginResource("thumbnails/r.png")));
    }

    private static void installR(JIPipeWorkbench workbench) {
        RExtensionSettings settings = RExtensionSettings.getInstance();
        JIPipeParameterTree tree = new JIPipeParameterTree(settings);
        JIPipeParameterAccess parameterAccess = tree.getParameters().get("r-environment");
        REnvInstaller installer = new REnvInstaller(workbench, parameterAccess);
        JIPipeDesktopRunExecuterUI.runInDialog((JIPipeDesktopWorkbench) workbench, ((JIPipeDesktopWorkbench) workbench).getWindow(), installer);
    }

    private static void easyInstallR(JIPipeWorkbench workbench) {
        RExtensionSettings settings = RExtensionSettings.getInstance();
        JIPipeParameterTree tree = new JIPipeParameterTree(settings);
        JIPipeParameterAccess parameterAccess = tree.getParameters().get("r-environment");
        REasyInstaller installer = new REasyInstaller((JIPipeDesktopWorkbench) workbench, parameterAccess);
        JIPipeDesktopRunExecuterUI.runInDialog((JIPipeDesktopWorkbench) workbench, ((JIPipeDesktopWorkbench) workbench).getWindow(), installer);
    }

    private static void configureR(JIPipeWorkbench workbench) {
        if (workbench instanceof JIPipeDesktopProjectWorkbench) {
            ((JIPipeDesktopProjectWorkbench) workbench).openApplicationSettings("/Extensions/R integration");
        }
    }

    public static void createMissingRNotificationIfNeeded(JIPipeNotificationInbox inbox) {
        if (!RExtensionSettings.RSettingsAreValid()) {
            JIPipeNotification notification = new JIPipeNotification(AS_DEPENDENCY.getDependencyId() + ":r-not-configured");
            notification.setHeading("R is not configured");
            notification.setDescription("To make use of R within JIPipe, you need to either provide JIPipe with an " +
                    "existing R installation or let JIPipe install a R distribution for you. Please note that we cannot provide you with an R " +
                    "setup tool for Linux and Mac.\n\n" +
                    "For more information, please visit https://www.jipipe.org/installation/third-party/r/");
            if (SystemUtils.IS_OS_WINDOWS) {
                notification.getActions().add(new JIPipeNotificationAction("Install R",
                        "Installs a prepackaged version of R",
                        UIUtils.getIconInvertedFromResources("actions/download.png"),
                        JIPipeNotificationAction.Style.Success,
                        RPlugin::easyInstallR));
            }
            notification.getActions().add(new JIPipeNotificationAction("Open settings",
                    "Opens the applications settings page",
                    UIUtils.getIconFromResources("actions/configure.png"),
                    RPlugin::configureR));
            inbox.push(notification);
        }
    }

    @Override
    public boolean isBeta() {
        return true;
    }

    @Override
    public Set<JIPipeDependency> getDependencies() {
        return Sets.newHashSet(CorePlugin.AS_DEPENDENCY, ImageJDataTypesPlugin.AS_DEPENDENCY);
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
                REnvironment.ENVIRONMENT_ID,
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
        registerEnvironmentInstaller(REnvironment.class, REnvInstaller.class, UIUtils.getIconFromResources("actions/download.png"));
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
                "Connect existing R installations to JIPipe or automatically install a new R environment if none is available",
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
    public void postprocess(JIPipeProgressInfo progressInfo) {
        createMissingRNotificationIfNeeded(JIPipeNotificationInbox.getInstance());
    }
}
