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

package org.hkijena.jipipe.plugins.omnipose;

import com.google.common.collect.Sets;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.core.CorePlugin;
import org.hkijena.jipipe.plugins.imagejalgorithms.ImageJAlgorithmsPlugin;
import org.hkijena.jipipe.plugins.imagejdatatypes.ImageJDataTypesPlugin;
import org.hkijena.jipipe.plugins.omnipose.algorithms.OmniposeInferenceAlgorithm;
import org.hkijena.jipipe.plugins.omnipose.algorithms.OmniposeTrainingAlgorithm;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.plugins.python.OptionalPythonEnvironment;
import org.hkijena.jipipe.plugins.python.PythonEnvironment;
import org.hkijena.jipipe.plugins.python.PythonPlugin;
import org.hkijena.jipipe.utils.JIPipeResourceManager;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Plugin(type = JIPipeJavaPlugin.class)
public class OmniposePlugin extends JIPipePrepackagedDefaultJavaPlugin {

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:omnipose",
            JIPipe.getJIPipeVersion(),
            "Omnipose integration");

    public static final JIPipeResourceManager RESOURCES = new JIPipeResourceManager(OmniposePlugin.class, "org/hkijena/jipipe/plugins/omnipose");

    public OmniposePlugin() {
        getMetadata().addCategories(PluginCategoriesEnumParameter.CATEGORY_DEEP_LEARNING, PluginCategoriesEnumParameter.CATEGORY_SEGMENTATION, PluginCategoriesEnumParameter.CATEGORY_MACHINE_LEARNING);
    }

    public static PythonEnvironment getEnvironment(JIPipeProject project, OptionalPythonEnvironment nodeEnvironment) {
        if (nodeEnvironment.isEnabled()) {
            return nodeEnvironment.getContent();
        }
        if (project != null && project.getSettingsSheet(OmniposePluginProjectSettings.class).getProjectDefaultEnvironment().isEnabled()) {
            return project.getSettingsSheet(OmniposePluginProjectSettings.class).getProjectDefaultEnvironment().getContent();
        }
        return OmniposePluginApplicationSettings.getInstance().getDefaultOmniposeEnvironment();
    }

    @Override
    public StringList getDependencyProvides() {
        return new StringList();
    }

    @Override
    public Set<JIPipeDependency> getDependencies() {
        return Sets.newHashSet(CorePlugin.AS_DEPENDENCY, ImageJDataTypesPlugin.AS_DEPENDENCY, PythonPlugin.AS_DEPENDENCY, ImageJAlgorithmsPlugin.AS_DEPENDENCY);
    }

    @Override
    public boolean isBeta() {
        return true;
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
        registerApplicationSettingsSheet(new OmniposePluginApplicationSettings());
        registerProjectSettingsSheet(OmniposePluginProjectSettings.class);

        registerEnumParameterType("omnipose-model", OmniposeModel.class, "Omnipose model", "An Omnipose model");
        registerEnumParameterType("omnipose-pretrained-model", OmniposePretrainedModel.class, "Omnipose pre-trained model", "A pretrained model for Omnipose");

        registerNodeType("omnipose", OmniposeInferenceAlgorithm.class, RESOURCES.getIcon16URLFromResources("omnipose.png"));
        registerNodeType("omnipose-training", OmniposeTrainingAlgorithm.class, RESOURCES.getIcon16URLFromResources("omnipose.png"));

    }

}
