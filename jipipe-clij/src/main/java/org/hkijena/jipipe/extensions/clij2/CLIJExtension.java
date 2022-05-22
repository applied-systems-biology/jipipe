package org.hkijena.jipipe.extensions.clij2;

import net.haesleinhuepf.clij.macro.CLIJMacroPlugin;
import net.imagej.updater.UpdateSite;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeImageJUpdateSiteDependency;
import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.compat.DefaultImageJDataExporterUI;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
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
import org.hkijena.jipipe.extensions.clij2.ui.CLIJControlPanelJIPipeMenuExtension;
import org.hkijena.jipipe.extensions.imagejdatatypes.compat.ImagePlusWindowImageJImporterUI;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

/**
 * Integrates CLIJ
 */
@Plugin(type = JIPipeJavaExtension.class)
public class CLIJExtension extends JIPipePrepackagedDefaultJavaExtension {

    public static final Class[] ALLOWED_PARAMETER_TYPES = new Class[]{Boolean.class, Character.class, Short.class, Integer.class, Float.class, Double.class};

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
                UIUtils.getIconFromResources("apps/clij.png"),
                "Extensions",
                UIUtils.getIconFromResources("actions/plugins.png"),
                new CLIJSettings());
        registerMenuExtension(CLIJControlPanelJIPipeMenuExtension.class);
    }

    private void registerAlgorithms(JIPipeProgressInfo progressInfo) {
        PluginService service = getContext().getService(PluginService.class);
        for (PluginInfo<CLIJMacroPlugin> pluginInfo : service.getPluginsOfType(CLIJMacroPlugin.class)) {
            JIPipeProgressInfo moduleProgress = progressInfo.resolve(pluginInfo.getName() + " @ " + pluginInfo.getClassName());
            try {
                CLIJMacroPlugin instance = pluginInfo.createInstance();
                if(instance.getClass().getAnnotation(Deprecated.class) != null) {
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
