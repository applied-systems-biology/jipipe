package org.hkijena.jipipe.extensions.r;

import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.parameters.primitives.HTMLText;
import org.hkijena.jipipe.extensions.parameters.primitives.StringList;
import org.hkijena.jipipe.extensions.r.algorithms.ImportRDatasetAlgorithm;
import org.hkijena.jipipe.extensions.r.algorithms.IteratingRScriptAlgorithm;
import org.hkijena.jipipe.extensions.r.algorithms.MergingRScriptAlgorithm;
import org.hkijena.jipipe.extensions.r.installers.REnvInstaller;
import org.hkijena.jipipe.extensions.r.parameters.RScriptParameter;
import org.hkijena.jipipe.extensions.r.ui.RTokenMaker;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

@Plugin(type = JIPipeJavaExtension.class)
public class RExtension extends JIPipePrepackagedDefaultJavaExtension {
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
        return "2021.5";
    }

    @Override
    public List<ImageIcon> getSplashIcons() {
        return Arrays.asList(UIUtils.getIcon32FromResources("apps/rlogo_icon.png"));
    }

    @Override
    public void register() {
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
}
