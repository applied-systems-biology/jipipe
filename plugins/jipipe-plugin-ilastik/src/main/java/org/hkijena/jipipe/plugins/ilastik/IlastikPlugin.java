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

package org.hkijena.jipipe.plugins.ilastik;

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
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.ilastik.datatypes.IlastikModelData;
import org.hkijena.jipipe.plugins.ilastik.nodes.*;
import org.hkijena.jipipe.plugins.ilastik.parameters.IlastikProjectValidationMode;
import org.hkijena.jipipe.plugins.imagejalgorithms.ImageJAlgorithmsPlugin;
import org.hkijena.jipipe.plugins.imagejdatatypes.ImageJDataTypesPlugin;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.plugins.processes.ProcessEnvironment;
import org.hkijena.jipipe.utils.JIPipeResourceManager;
import org.hkijena.jipipe.utils.ProcessUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.util.*;

@Plugin(type = JIPipeJavaPlugin.class)
public class IlastikPlugin extends JIPipePrepackagedDefaultJavaPlugin {

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:ilastik",
            JIPipe.getJIPipeVersion(),
            "Ilastik integration");

    public static final JIPipeResourceManager RESOURCES = new JIPipeResourceManager(IlastikPlugin.class, "org/hkijena/jipipe/plugins/ilastik");

    public IlastikPlugin() {
        getMetadata().addCategories(PluginCategoriesEnumParameter.CATEGORY_DEEP_LEARNING,
                PluginCategoriesEnumParameter.CATEGORY_SEGMENTATION,
                PluginCategoriesEnumParameter.CATEGORY_CLASSIFICATION,
                PluginCategoriesEnumParameter.CATEGORY_MACHINE_LEARNING);
    }

    /**
     * Runs Ilastik
     *
     * @param environment  the environment. can be null (then the {@link IlastikPluginApplicationSettings} environment is taken)
     * @param parameters   the cli parameters
     * @param progressInfo the progress info
     * @param detached     if the process is launched detached
     */
    public static void runIlastik(IlastikEnvironment environment, List<String> parameters, JIPipeProgressInfo progressInfo, boolean detached) {
        if (environment == null) {
            environment = IlastikPluginApplicationSettings.getInstance().getDefaultEnvironment();
        }

        // CLI
        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.set("cli_parameters", parameters);

        // Environment variables
        Map<String, String> environmentVariables = new HashMap<>();
        if (IlastikPluginApplicationSettings.getInstance().getMaxThreads() > 0) {
            environmentVariables.put("LAZYFLOW_THREADS", String.valueOf(IlastikPluginApplicationSettings.getInstance().getMaxThreads()));
        }
        environmentVariables.put("LAZYFLOW_TOTAL_RAM_MB", String.valueOf(Math.max(256, IlastikPluginApplicationSettings.getInstance().getMaxMemory())));
        environmentVariables.put("LANG", "en_US.UTF-8");
        environmentVariables.put("LC_ALL", "en_US.UTF-8");
        environmentVariables.put("LC_CTYPE", "en_US.UTF-8");

        ProcessEnvironment processEnvironment = new ProcessEnvironment();
        if (true)
            throw new UnsupportedOperationException("Not implemented yet");

        if (detached) {
            ProcessUtils.launchProcess(processEnvironment, variables, environmentVariables, false, progressInfo);
        } else {
            ProcessUtils.runProcess(processEnvironment, variables, environmentVariables, false, progressInfo);
        }
    }

    public static IlastikEnvironment getEnvironment(JIPipeProject project, OptionalIlastikEnvironment nodeEnvironment) {
        if (nodeEnvironment.isEnabled()) {
            return nodeEnvironment.getContent();
        }
        if (project != null && project.getSettingsSheet(IlastikPluginProjectSettings.class).getProjectDefaultEnvironment().isEnabled()) {
            return project.getSettingsSheet(IlastikPluginProjectSettings.class).getProjectDefaultEnvironment().getContent();
        }
        return IlastikPluginApplicationSettings.getInstance().getDefaultEnvironment();
    }

    @Override
    public StringList getDependencyProvides() {
        return new StringList();
    }

    @Override
    public Set<JIPipeDependency> getDependencies() {
        return Sets.newHashSet(CorePlugin.AS_DEPENDENCY, ImageJDataTypesPlugin.AS_DEPENDENCY, ImageJAlgorithmsPlugin.AS_DEPENDENCY);
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
        IlastikPluginApplicationSettings settings = new IlastikPluginApplicationSettings();
        registerApplicationSettingsSheet(settings);
        registerProjectSettingsSheet(IlastikPluginProjectSettings.class);
        registerMenuExtension(RunIlastikDesktopMenuExtension.class);
        registerDatatype("ilastik-model", IlastikModelData.class, RESOURCES.getIcon16URLFromResources("ilastik-model.png"));

        registerEnvironment(IlastikEnvironment.class,
                IlastikEnvironment.List.class,
                settings,
                "ilastik-environment",
                "Ilastik environment",
                "An Ilastik environment",
                RESOURCES.getIconFromResources("ilastik.png"));
        registerParameterType("optional-ilastik-environment",
                OptionalIlastikEnvironment.class,
                null,
                null,
                "Optional Ilastik environment",
                "An optional Ilastik environment",
                null);
        registerEnumParameterType("ilastik-project-validation-mode",
                IlastikProjectValidationMode.class,
                "Ilastik project validation mode",
                "Determines how Ilastik projects are checked");


        registerNodeType("import-ilastik-model", ImportIlastikModel.class);
        registerNodeType("import-ilastik-hdf5-image", ImportIlastikHDF5ImageAlgorithm.class);
        registerNodeType("export-ilastik-hdf5-image", ExportIlastikHDF5ImageAlgorithm.class, UIUtils.getIconURLFromResources("actions/document-export.png"));
        registerNodeType("ilastik-pixel-classification", IlastikPixelClassificationAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ilastik-autocontext", IlastikAutoContextAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ilastik-object-classification", IlastikObjectClassificationAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
    }
}
