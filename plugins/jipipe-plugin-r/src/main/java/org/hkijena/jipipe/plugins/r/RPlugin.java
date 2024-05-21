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

package org.hkijena.jipipe.plugins.r;

import org.apache.commons.compress.utils.Sets;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.core.CorePlugin;
import org.hkijena.jipipe.plugins.imagejdatatypes.ImageJDataTypesPlugin;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.plugins.r.algorithms.ImportRDatasetAlgorithm;
import org.hkijena.jipipe.plugins.r.algorithms.IteratingRScriptAlgorithm;
import org.hkijena.jipipe.plugins.r.algorithms.MergingRScriptAlgorithm;
import org.hkijena.jipipe.plugins.r.parameters.RScriptParameter;
import org.hkijena.jipipe.plugins.r.ui.RTokenMaker;
import org.hkijena.jipipe.utils.JIPipeResourceManager;
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

    public static final JIPipeResourceManager RESOURCES = new JIPipeResourceManager(RPlugin.class, "org/hkijena/jipipe/plugins/r");

    public RPlugin() {
        getMetadata().addCategories(PluginCategoriesEnumParameter.CATEGORY_SCRIPTING);
    }

    public static REnvironment getEnvironment(JIPipeProject project, OptionalREnvironment nodeEnvironment) {
        if (nodeEnvironment.isEnabled()) {
            return nodeEnvironment.getContent();
        }
        if (project != null && project.getSettingsSheet(RPluginProjectSettings.class).getProjectDefaultEnvironment().isEnabled()) {
            return project.getSettingsSheet(RPluginProjectSettings.class).getProjectDefaultEnvironment().getContent();
        }
        return RPluginApplicationSettings.getInstance().getDefaultEnvironment();
    }

    @Override
    public boolean isBeta() {
        return true;
    }

    @Override
    public StringList getDependencyProvides() {
        return new StringList();
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
        RPluginApplicationSettings extensionSettings = new RPluginApplicationSettings();

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


        AbstractTokenMakerFactory atmf = (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();
        atmf.putMapping("text/x-r-script", RTokenMaker.class.getName());

        registerParameterType("r-script",
                RScriptParameter.class,
                null,
                null,
                "R script",
                "An R script",
                null);
        registerApplicationSettingsSheet(extensionSettings);
        registerProjectSettingsSheet(RPluginProjectSettings.class);


        registerNodeType("r-script-iterating", IteratingRScriptAlgorithm.class, UIUtils.getIconURLFromResources("apps/rlogo_icon.png"));
        registerNodeType("r-script-merging", MergingRScriptAlgorithm.class, UIUtils.getIconURLFromResources("apps/rlogo_icon.png"));

        registerEnumParameterType("r-import-dataset:dataset", ImportRDatasetAlgorithm.Dataset.class, "R dataset", "A dataset from the R datasets package");
        registerNodeType("r-import-dataset", ImportRDatasetAlgorithm.class, UIUtils.getIconURLFromResources("apps/rlogo_icon.png"));

        registerNodeExamplesFromResources(RESOURCES, "examples");
    }
}
