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

package org.hkijena.jipipe.extensions.clij2;

import net.haesleinhuepf.clij.macro.CLIJMacroPlugin;
import net.imagej.updater.UpdateSite;
import org.apache.commons.compress.utils.Sets;
import org.hkijena.jipipe.*;
import org.hkijena.jipipe.api.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.compat.DefaultImageJDataExporterUI;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.extensions.clij2.algorithms.Clij2ExecuteKernelIterating;
import org.hkijena.jipipe.extensions.clij2.algorithms.Clij2ExecuteKernelSimpleIterating;
import org.hkijena.jipipe.extensions.clij2.algorithms.Clij2PullAlgorithm;
import org.hkijena.jipipe.extensions.clij2.algorithms.Clij2PushAlgorithm;
import org.hkijena.jipipe.extensions.clij2.compat.CLIIJ2DataToImageWindowImageJExporter;
import org.hkijena.jipipe.extensions.clij2.compat.CLIJ2DataFromImageWindowImageJImporter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageToImagePlusDataConverter;
import org.hkijena.jipipe.extensions.clij2.datatypes.ImagePlusDataToCLIJImageDataConverter;
import org.hkijena.jipipe.extensions.clij2.parameters.OpenCLKernelScript;
import org.hkijena.jipipe.extensions.clij2.ui.CLIJControlPanelJIPipeDesktopMenuExtension;
import org.hkijena.jipipe.extensions.core.CorePlugin;
import org.hkijena.jipipe.extensions.imagejdatatypes.ImageJDataTypesPlugin;
import org.hkijena.jipipe.extensions.imagejdatatypes.compat.ImagePlusWindowImageJImporterUI;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.parameters.library.images.ImageParameter;
import org.hkijena.jipipe.extensions.parameters.library.jipipe.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.utils.JIPipeResourceManager;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Integrates CLIJ
 */
@Plugin(type = JIPipeJavaPlugin.class)
public class CLIJPlugin extends JIPipePrepackagedDefaultJavaPlugin {

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:clij2-integration",
            JIPipe.getJIPipeVersion(),
            "CLIJ2 integration");

    public static final JIPipeResourceManager RESOURCES = new JIPipeResourceManager(CLIJPlugin.class, "org/hkijena/jipipe/extensions/clij2");

    public static final Class[] ALLOWED_PARAMETER_TYPES = new Class[]{Boolean.class, Character.class, Short.class, Integer.class, Float.class, Double.class};

    public CLIJPlugin() {
    }

    @Override
    public PluginCategoriesEnumParameter.List getCategories() {
        return new PluginCategoriesEnumParameter.List(PluginCategoriesEnumParameter.CATEGORY_3D, PluginCategoriesEnumParameter.CATEGORY_IMAGE_ANALYSIS, PluginCategoriesEnumParameter.CATEGORY_ANALYSIS, PluginCategoriesEnumParameter.CATEGORY_GPU);
    }

    @Override
    public ImageParameter getThumbnail() {
        return new ImageParameter(ResourceUtils.getPluginResource("thumbnails/clij.png"));
    }

    @Override
    public Set<JIPipeDependency> getDependencies() {
        return Sets.newHashSet(CorePlugin.AS_DEPENDENCY, ImageJDataTypesPlugin.AS_DEPENDENCY);
    }

    @Override
    public JIPipeAuthorMetadata.List getAcknowledgements() {
        return new JIPipeAuthorMetadata.List(new JIPipeAuthorMetadata("",
                "Robert",
                "Haase",
                new StringList("Max Planck Institute for Molecular Cell Biology and Genetics, Dresden, Germany", "Center for Systems Biology Dresden, Dresden, Germany"),
                "",
                "",
                true,
                true),
                new JIPipeAuthorMetadata("",
                        "Loic A.",
                        "Royer",
                        new StringList("Chan Zuckerberg Biohub, San Francisco, CA, USA"),
                        "",
                        "",
                        true,
                        true),
                new JIPipeAuthorMetadata("",
                        "Peter",
                        "Steinbach",
                        new StringList("Helmholtz-Zentrum Dresden-Rossendorf, Dresden, Germany",
                                "Max Planck Institute for Molecular Cell Biology and Genetics, Dresden, Germany", "Center for Systems Biology Dresden, Dresden, Germany"),
                        "",
                        "",
                        false,
                        false),
                new JIPipeAuthorMetadata("",
                        "Deborah",
                        "Schmidt",
                        new StringList("Max Planck Institute for Molecular Cell Biology and Genetics, Dresden, Germany", "Center for Systems Biology Dresden, Dresden, Germany"),
                        "",
                        "",
                        false,
                        false),
                new JIPipeAuthorMetadata("",
                        "Alexandr",
                        "Dibrov",
                        new StringList("Max Planck Institute for Molecular Cell Biology and Genetics, Dresden, Germany", "Center for Systems Biology Dresden, Dresden, Germany"),
                        "",
                        "",
                        false,
                        false),
                new JIPipeAuthorMetadata("",
                        "Uwe",
                        "Schmidt",
                        new StringList("Max Planck Institute for Molecular Cell Biology and Genetics, Dresden, Germany", "Center for Systems Biology Dresden, Dresden, Germany"),
                        "",
                        "",
                        false,
                        false),
                new JIPipeAuthorMetadata("",
                        "Martin",
                        "Weigert",
                        new StringList("Max Planck Institute for Molecular Cell Biology and Genetics, Dresden, Germany", "Center for Systems Biology Dresden, Dresden, Germany"),
                        "",
                        "",
                        false,
                        false),
                new JIPipeAuthorMetadata("",
                        "Nicola",
                        "Maghelli",
                        new StringList("Max Planck Institute for Molecular Cell Biology and Genetics, Dresden, Germany", "Center for Systems Biology Dresden, Dresden, Germany"),
                        "",
                        "",
                        false,
                        false),
                new JIPipeAuthorMetadata("",
                        "Pavel",
                        "Tomancak",
                        new StringList("Max Planck Institute for Molecular Cell Biology and Genetics, Dresden, Germany", "Center for Systems Biology Dresden, Dresden, Germany",
                                "IT4Innovations, VŠB - Technical University of Ostrava, Ostrava-Poruba, Czech Republic"),
                        "",
                        "",
                        false,
                        false),
                new JIPipeAuthorMetadata("",
                        "Florian",
                        "Jug",
                        new StringList("Max Planck Institute for Molecular Cell Biology and Genetics, Dresden, Germany", "Center for Systems Biology Dresden, Dresden, Germany"),
                        "",
                        "",
                        false,
                        false),
                new JIPipeAuthorMetadata("",
                        "Eugene W.",
                        "Myers",
                        new StringList("Max Planck Institute for Molecular Cell Biology and Genetics, Dresden, Germany", "Center for Systems Biology Dresden, Dresden, Germany"),
                        "",
                        "",
                        false,
                        false));
    }

    @Override
    public StringList getDependencyCitations() {
        StringList result = new StringList();
        result.add("Robert Haase, Loic Alain Royer, Peter Steinbach, Deborah Schmidt, Alexandr Dibrov, Uwe Schmidt, Martin Weigert, " +
                "Nicola Maghelli, Pavel Tomancak, Florian Jug, Eugene W Myers. CLIJ: GPU-accelerated image processing for everyone. Nat Methods 17, 5-6 (2020)");
        return result;
    }

    @Override
    public String getName() {
        return "CLIJ2 integration";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Integrates data types and algorithms for GPU computing based on CLIJ2.");
    }

    @Override
    public List<JIPipeImageJUpdateSiteDependency> getImageJUpdateSiteDependencies() {
        return Arrays.asList(
                new JIPipeImageJUpdateSiteDependency(new UpdateSite("clij", "https://sites.imagej.net/clij/", "", "", "", "", 0)),
                new JIPipeImageJUpdateSiteDependency(new UpdateSite("clij2", "https://sites.imagej.net/clij2/", "", "", "", "", 0))
        );
    }

    @Override
    public List<ImageIcon> getSplashIcons() {
        return Arrays.asList(UIUtils.getIcon32FromResources("apps/clij.png"));
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
        registerParameterType("clij2:opencl-kernel",
                OpenCLKernelScript.class,
                null,
                null,
                "OpenCL Kernel",
                "A OpenCL kernel",
                null);
        registerDatatype("clij2-image",
                CLIJImageData.class,
                UIUtils.getIconURLFromResources("data-types/clij2-image.png"));
        registerDatatypeConversion(new CLIJImageToImagePlusDataConverter(ImagePlusData.class));
        registerDatatypeConversion(new ImagePlusDataToCLIJImageDataConverter());
        registerImageJDataImporter("clij2-image-from-window", new CLIJ2DataFromImageWindowImageJImporter(), ImagePlusWindowImageJImporterUI.class);
        registerImageJDataExporter("clij2-to-window", new CLIIJ2DataToImageWindowImageJExporter(), DefaultImageJDataExporterUI.class);
        registerAlgorithms(progressInfo);
        registerNodeType("clij-execute-kernel-iterating", Clij2ExecuteKernelIterating.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij-execute-kernel-simple-iterating", Clij2ExecuteKernelSimpleIterating.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij-push-to-gpu", Clij2PushAlgorithm.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij-pull-from-gpu", Clij2PullAlgorithm.class, UIUtils.getIconURLFromResources("apps/clij.png"));

        registerSettingsSheet(CLIJSettings.ID,
                "CLIJ2",
                "Configure the GPU devices and how data is interchanged between ImageJ and CLIJ",
                UIUtils.getIconFromResources("apps/clij.png"),
                "Extensions",
                UIUtils.getIconFromResources("actions/plugins.png"),
                new CLIJSettings());
        registerMenuExtension(CLIJControlPanelJIPipeDesktopMenuExtension.class);

        registerNodeExamplesFromResources(RESOURCES, "examples");
    }

    private void registerAlgorithms(JIPipeProgressInfo progressInfo) {
        PluginService service = getContext().getService(PluginService.class);
        for (PluginInfo<CLIJMacroPlugin> pluginInfo : service.getPluginsOfType(CLIJMacroPlugin.class)) {
            JIPipeProgressInfo moduleProgress = progressInfo.resolve(pluginInfo.getName() + " @ " + pluginInfo.getClassName());
            try {
                CLIJMacroPlugin instance = pluginInfo.createInstance();
                if (instance.getClass().getAnnotation(Deprecated.class) != null) {
                    moduleProgress.log("Marked as deprecated. Skipping.");
                    continue;
                }
                CLIJCommandNodeInfo nodeInfo = new CLIJCommandNodeInfo(getContext(), pluginInfo, moduleProgress);
                if (nodeInfo.getInputSlots().isEmpty() && nodeInfo.getOutputSlots().isEmpty()) {
                    progressInfo.log("Node has no data slots. Skipping.");
                    continue;
                }
                registerNodeType(nodeInfo, UIUtils.getIconURLFromResources("apps/clij.png"));
            } catch (Exception e) {
                moduleProgress.log("Unable to register module:");
                moduleProgress.log(e.toString());
            }
        }
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:clij2-integration";
    }

}
