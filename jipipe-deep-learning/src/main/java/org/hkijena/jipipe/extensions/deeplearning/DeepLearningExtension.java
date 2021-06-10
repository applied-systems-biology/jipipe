package org.hkijena.jipipe.extensions.deeplearning;

import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.deeplearning.datatypes.DeepLearningModelData;
import org.hkijena.jipipe.extensions.deeplearning.nodes.CreateModelAlgorithm;
import org.hkijena.jipipe.extensions.deeplearning.nodes.ImportModelAlgorithm;
import org.hkijena.jipipe.extensions.deeplearning.nodes.PredictAlgorithm;
import org.hkijena.jipipe.extensions.deeplearning.nodes.TrainModelAlgorithm;
import org.hkijena.jipipe.extensions.parameters.primitives.HTMLText;
import org.hkijena.jipipe.extensions.parameters.primitives.StringList;
import org.hkijena.jipipe.extensions.python.PythonEnvironment;
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
        return "2021.6";
    }

    @Override
    public List<ImageIcon> getSplashIcons() {
        return Arrays.asList(UIUtils.getIcon32FromResources("apps/tensorflow.png"),
                UIUtils.getIcon32FromResources("apps/keras.png"));
    }

    @Override
    public void register() {
        DeepLearningSettings settings = new DeepLearningSettings();
        registerEnvironment(DeepLearningToolkitLibraryEnvironment.class,
                DeepLearningToolkitLibraryEnvironment.List.class,
                settings,
                "deep-learning-toolkit-library",
                "Deep Learning Toolkit",
                "Library that provides Deep Learning capabilities",
                UIUtils.getIconFromResources("actions/plugins.png"));
        registerSettingsSheet(DeepLearningSettings.ID,
                "Deep Learning",
                UIUtils.getIconFromResources("data-types/dl-model.png"),
                "Extensions",
                UIUtils.getIconFromResources("actions/plugins.png"),
                settings);
        registerEnvironmentInstaller(PythonEnvironment.class,
                DeepLearningToolkitEnvInstaller.class,
                UIUtils.getIconFromResources("data-types/dl-model.png"));
        registerEnumParameterType("deep-learning-architecture",
                DeepLearningArchitecture.class,
                "Deep Learning architecture",
                "Architecture to be used");
        registerEnumParameterType("deep-learning-regularization-method",
                RegularizationMethod.class,
                "Deep Learning regularization method",
                "Regularization method to be used");
        registerEnumParameterType("deep-learning-monitor-loss",
                MonitorLoss.class,
                "Deep Learning monitor loss",
                "Monitor loss method to be used");

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
        registerNodeType("train-deep-learning-model",
                TrainModelAlgorithm.class,
                UIUtils.getIconURLFromResources("data-types/dl-model.png"));
        registerNodeType("deep-learning-predict",
                PredictAlgorithm.class,
                UIUtils.getIconURLFromResources("data-types/dl-model.png"));
    }
}