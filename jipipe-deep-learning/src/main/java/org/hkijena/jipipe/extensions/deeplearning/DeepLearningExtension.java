package org.hkijena.jipipe.extensions.deeplearning;

import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.api.notifications.JIPipeNotification;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationAction;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.deeplearning.datatypes.DeepLearningModelData;
import org.hkijena.jipipe.extensions.deeplearning.enums.EvaluationMethod;
import org.hkijena.jipipe.extensions.deeplearning.enums.ModelType;
import org.hkijena.jipipe.extensions.deeplearning.enums.NetworkArchitecture;
import org.hkijena.jipipe.extensions.deeplearning.enums.NormalizationMethod;
import org.hkijena.jipipe.extensions.deeplearning.enums.RegularizationMethod;
import org.hkijena.jipipe.extensions.deeplearning.environments.DeepLearningDeviceEnvironment;
import org.hkijena.jipipe.extensions.deeplearning.environments.DeepLearningToolkitLibraryEnvironment;
import org.hkijena.jipipe.extensions.deeplearning.environments.DeepLearningToolkitLibraryEnvironmentInstaller;
import org.hkijena.jipipe.extensions.deeplearning.environments.OptionalDeepLearningDeviceEnvironment;
import org.hkijena.jipipe.extensions.deeplearning.environments.TensorFlowEnvInstaller;
import org.hkijena.jipipe.extensions.deeplearning.nodes.CreateModelAlgorithm;
import org.hkijena.jipipe.extensions.deeplearning.nodes.ImportModelAlgorithm;
import org.hkijena.jipipe.extensions.deeplearning.nodes.PredictClassifierAlgorithm;
import org.hkijena.jipipe.extensions.deeplearning.nodes.PredictImageAlgorithm;
import org.hkijena.jipipe.extensions.deeplearning.nodes.TrainClassifierModelAlgorithm;
import org.hkijena.jipipe.extensions.deeplearning.nodes.TrainImageModelAlgorithm;
import org.hkijena.jipipe.extensions.parameters.primitives.HTMLText;
import org.hkijena.jipipe.extensions.parameters.primitives.StringList;
import org.hkijena.jipipe.extensions.python.PythonEnvironment;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.DocumentTabPane;
import org.hkijena.jipipe.ui.running.JIPipeRunExecuterUI;
import org.hkijena.jipipe.ui.settings.JIPipeApplicationSettingsUI;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

@Plugin(type = JIPipeJavaExtension.class)
public class DeepLearningExtension extends JIPipePrepackagedDefaultJavaExtension {
    @Override
    public StringList getDependencyCitations() {
        StringList strings = new StringList();
        strings.add("Martín Abadi, Ashish Agarwal, Paul Barham, Eugene Brevdo,\n" +
                "Zhifeng Chen, Craig Citro, Greg S. Corrado, Andy Davis,\n" +
                "Jeffrey Dean, Matthieu Devin, Sanjay Ghemawat, Ian Goodfellow,\n" +
                "Andrew Harp, Geoffrey Irving, Michael Isard, Rafal Jozefowicz, Yangqing Jia,\n" +
                "Lukasz Kaiser, Manjunath Kudlur, Josh Levenberg, Dan Mané, Mike Schuster,\n" +
                "Rajat Monga, Sherry Moore, Derek Murray, Chris Olah, Jonathon Shlens,\n" +
                "Benoit Steiner, Ilya Sutskever, Kunal Talwar, Paul Tucker,\n" +
                "Vincent Vanhoucke, Vijay Vasudevan, Fernanda Viégas,\n" +
                "Oriol Vinyals, Pete Warden, Martin Wattenberg, Martin Wicke,\n" +
                "Yuan Yu, and Xiaoqiang Zheng.\n" +
                "TensorFlow: Large-scale machine learning on heterogeneous systems,\n" +
                "2015. Software available from tensorflow.org.");
        strings.add("Chollet, Francois and others. Keras, 2015. Software available from https://keras.io");
        return strings;
    }

    @Override
    public String getName() {
        return "Deep Learning integration";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Integrates Deep Learning");
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:deep-learning";
    }

    @Override
    public String getDependencyVersion() {
        return "1.52.1";
    }

    @Override
    public List<ImageIcon> getSplashIcons() {
        return Arrays.asList(UIUtils.getIcon32FromResources("apps/tensorflow.png"));
    }

    @Override
    public void register() {
        DeepLearningSettings settings = new DeepLearningSettings();

        // Deep learning toolkit
        registerEnvironment(DeepLearningToolkitLibraryEnvironment.class,
                DeepLearningToolkitLibraryEnvironment.List.class,
                settings,
                "deep-learning-toolkit-library",
                "Deep Learning Toolkit",
                "Library that provides Deep Learning capabilities",
                UIUtils.getIconFromResources("actions/plugins.png"));
        registerEnvironmentInstaller(DeepLearningToolkitLibraryEnvironment.class,
                DeepLearningToolkitLibraryEnvironmentInstaller.class,
                UIUtils.getIconFromResources("actions/browser-download.png"));

        // Deep learning device
        registerEnvironment(DeepLearningDeviceEnvironment.class,
                DeepLearningDeviceEnvironment.List.class,
                settings,
                "deep-learning-device",
                "Deep Learning device configuration",
                "Determines the devices used for processing",
                UIUtils.getIconFromResources("devices/cpu.png"));
        registerParameterType("optional-deep-learning-device",
                OptionalDeepLearningDeviceEnvironment.class,
                "Optional deep learning device configuration",
                "Determines the devices used for processing");

        registerSettingsSheet(DeepLearningSettings.ID,
                "Deep Learning",
                UIUtils.getIconFromResources("data-types/dl-model.png"),
                "Extensions",
                UIUtils.getIconFromResources("actions/plugins.png"),
                settings);
        registerEnvironmentInstaller(PythonEnvironment.class,
                TensorFlowEnvInstaller.class,
                UIUtils.getIconFromResources("data-types/dl-model.png"));
        registerEnumParameterType("deep-learning-architecture",
                NetworkArchitecture.class,
                "Deep Learning architecture",
                "Architecture to be used");
        registerEnumParameterType("deep-learning-regularization-method",
                RegularizationMethod.class,
                "Deep Learning regularization method",
                "Regularization method to be used");
        registerEnumParameterType("deep-learning-evaluation-method",
                EvaluationMethod.class,
                "Deep Learning evaluation method",
                "The evaluation method");
        registerEnumParameterType("deep-learning-model-type",
                ModelType.class,
                "Deep Learning model type",
                "A model type");
        registerEnumParameterType("deep-learning-preprocessing-type",
                NormalizationMethod.class,
                "Deep Leaning preprocessing type",
                "Preprocessing methods");

        registerDatatype("deep-learning-model",
                DeepLearningModelData.class,
                UIUtils.getIconURLFromResources("data-types/dl-model.png"),
                null,
                null);
        registerNodeType("create-deep-learning-model",
                CreateModelAlgorithm.class,
                UIUtils.getIconURLFromResources("data-types/dl-model.png"));
        registerNodeType("import-deep-learning-model",
                ImportModelAlgorithm.class,
                UIUtils.getIconURLFromResources("data-types/dl-model.png"));
        registerNodeType("deep-learning-train-image-model",
                TrainImageModelAlgorithm.class,
                UIUtils.getIconURLFromResources("data-types/dl-model.png"));
        registerNodeType("deep-learning-train-classifier-model",
                TrainClassifierModelAlgorithm.class,
                UIUtils.getIconURLFromResources("data-types/dl-model.png"));
        registerNodeType("deep-learning-predict-image",
                PredictImageAlgorithm.class,
                UIUtils.getIconURLFromResources("data-types/dl-model.png"));
        registerNodeType("deep-learning-predict-classifier",
                PredictClassifierAlgorithm.class,
                UIUtils.getIconURLFromResources("data-types/dl-model.png"));
    }

    @Override
    public void postprocess() {
        if (!DeepLearningSettings.pythonSettingsAreValid()) {
            JIPipeNotification notification = new JIPipeNotification(getDependencyId() + ":python-not-configured");
            notification.setHeading("Tensorflow is not installed");
            notification.setDescription("You need to setup a Python environment that comes with Tensorflow.");
            notification.getActions().add(new JIPipeNotificationAction("Install Tensorflow",
                    "Installs a Conda environment that contains all necessary dependencies for the Deep Learning toolkit used by JIPipe",
                    UIUtils.getIconFromResources("actions/browser-download.png"),
                    DeepLearningExtension::installDeepLearningConda));
            notification.getActions().add(new JIPipeNotificationAction("Configure Python",
                    "Opens the applications settings page",
                    UIUtils.getIconFromResources("actions/configure.png"),
                    DeepLearningExtension::openSettingsPage));
            JIPipeNotificationInbox.getInstance().push(notification);
        }
        if (!DeepLearningSettings.getInstance().getDeepLearningToolkit().isNewestVersion()) {
            JIPipeNotification notification = new JIPipeNotification(getDependencyId() + ":old-dltoolkit");
            notification.setHeading("Old library version");
            notification.setDescription("JIPipe has detected that the installed version of the Deep Learning Toolkit library is outdated. " +
                    "Please click the button below to install the newest version.");
            notification.getActions().add(new JIPipeNotificationAction("Install newest version",
                    "Installs the newest version of the Python library",
                    UIUtils.getIconFromResources("actions/run-install.png"),
                    DeepLearningExtension::installDeepLearningToolkitLibrary));
            notification.getActions().add(new JIPipeNotificationAction("Configure",
                    "Opens the applications settings page",
                    UIUtils.getIconFromResources("actions/configure.png"),
                    DeepLearningExtension::openSettingsPage));
            JIPipeNotificationInbox.getInstance().push(notification);
        }
    }

    private static void installDeepLearningToolkitLibrary(JIPipeWorkbench workbench) {
        DeepLearningSettings settings = DeepLearningSettings.getInstance();
        JIPipeParameterTree tree = new JIPipeParameterTree(settings);
        JIPipeParameterAccess parameterAccess = tree.getParameters().get("deep-learning-toolkit");
        DeepLearningToolkitLibraryEnvironmentInstaller installer = new DeepLearningToolkitLibraryEnvironmentInstaller(workbench, parameterAccess);
        JIPipeRunExecuterUI.runInDialog(workbench.getWindow(), installer);
    }

    private static void installDeepLearningConda(JIPipeWorkbench workbench) {
        DeepLearningSettings settings = DeepLearningSettings.getInstance();
        JIPipeParameterTree tree = new JIPipeParameterTree(settings);
        JIPipeParameterAccess parameterAccess = tree.getParameters().get("python-environment");
        TensorFlowEnvInstaller installer = new TensorFlowEnvInstaller(workbench, parameterAccess);
        JIPipeRunExecuterUI.runInDialog(workbench.getWindow(), installer);
    }

    private static void openSettingsPage(JIPipeWorkbench workbench) {
        DocumentTabPane.DocumentTab tab = workbench.getDocumentTabPane().selectSingletonTab(JIPipeProjectWorkbench.TAB_APPLICATION_SETTINGS);
        JIPipeApplicationSettingsUI applicationSettingsUI = (JIPipeApplicationSettingsUI) tab.getContent();
        applicationSettingsUI.selectNode("/Extensions/Deep Learning");
    }
}
