/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.deeplearning.configs;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.deeplearning.enums.NetworkArchitecture;
import org.hkijena.jipipe.extensions.deeplearning.enums.ModelType;
import org.hkijena.jipipe.extensions.deeplearning.enums.RegularizationMethod;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * POJO class for the model configuration
 */
public class DeepLearningModelConfiguration implements JIPipeParameterCollection {
    private final EventBus eventBus = new EventBus();
    private NetworkArchitecture architecture = NetworkArchitecture.SegNet;
    private ModelType modelType = ModelType.segmentation;
    private RegularizationMethod regularizationMethod = RegularizationMethod.GaussianDropout;
    private double learningRate = 0.001;
    private double regularizationLambda = 0.1;
    private int imageWidth = 256;
    private int imageHeight = 256;
    private int imageChannels = 3;
    private int numClasses = 2;
    private Path outputModelPath = Paths.get("");
    private Path outputModelJsonPath = Paths.get("");

    public DeepLearningModelConfiguration() {
    }

    public DeepLearningModelConfiguration(DeepLearningModelConfiguration other) {
        this.architecture = other.architecture;
        this.regularizationMethod = other.regularizationMethod;
        this.regularizationLambda = other.regularizationLambda;
        this.imageWidth = other.imageWidth;
        this.imageHeight = other.imageHeight;
        this.imageChannels = other.imageChannels;
        this.numClasses = other.numClasses;
        this.outputModelPath = other.outputModelPath;
        this.outputModelJsonPath = other.outputModelJsonPath;
        this.modelType = other.modelType;
        this.learningRate = other.learningRate;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @JIPipeDocumentation(name = "Learning rate", description = "Initial learning rate for the underlying optimizer")
    @JIPipeParameter("learning_rate")
    @JsonGetter("learning_rate")
    public double getLearningRate() {
        return learningRate;
    }

    @JIPipeParameter("learning_rate")
    @JsonSetter("learning_rate")
    public void setLearningRate(double learningRate) {
        this.learningRate = learningRate;
    }

    @JIPipeDocumentation(name = "Architecture", description = "The model architecture")
    @JIPipeParameter("architecture")
    @JsonGetter("architecture")
    public NetworkArchitecture getArchitecture() {
        return architecture;
    }

    @JIPipeParameter("architecture")
    @JsonSetter("architecture")
    public void setArchitecture(NetworkArchitecture architecture) {
        this.architecture = architecture;
    }

    @JIPipeDocumentation(name = "Model type", description = "The type of this model")
    @JIPipeParameter("model_type")
    @JsonGetter("model_type")
    public ModelType getModelType() {
        return modelType;
    }

    @JIPipeParameter("model_type")
    @JsonSetter("model_type")
    public void setModelType(ModelType modelType) {
        this.modelType = modelType;
    }

    @JIPipeDocumentation(name = "Regularization method", description = "Additional methods to regularize within the model:   Dropout, GaussianDropout, GaussianNoise")
    @JIPipeParameter("regularization-method")
    @JsonGetter("regularization_method")
    public RegularizationMethod getRegularizationMethod() {
        return regularizationMethod;
    }

    @JIPipeParameter("regularization-method")
    @JsonSetter("regularization_method")
    public void setRegularizationMethod(RegularizationMethod regularizationMethod) {
        this.regularizationMethod = regularizationMethod;
    }

    @JIPipeDocumentation(name = "Regularization lambda", description = "Factorizing lambda in the interval [0,1] to reduce/reinforce the regularization")
    @JIPipeParameter("regularization-lambda")
    @JsonGetter("regularization_lambda")
    public double getRegularizationLambda() {
        return regularizationLambda;
    }

    @JIPipeParameter("regularization-lambda")
    @JsonSetter("regularization_lambda")
    public void setRegularizationLambda(double regularizationLambda) {
        this.regularizationLambda = regularizationLambda;
    }

    @JIPipeDocumentation(name = "Image width", description = "The width of the image")
    @JIPipeParameter("image-width")
    public int getImageWidth() {
        return imageWidth;
    }

    @JIPipeParameter("image-width")
    public void setImageWidth(int imageWidth) {
        this.imageWidth = imageWidth;
    }

    @JIPipeDocumentation(name = "Image height", description = "The height of the image")
    @JIPipeParameter("image-height")
    public int getImageHeight() {
        return imageHeight;
    }

    @JIPipeParameter("image-height")
    public void setImageHeight(int imageHeight) {
        this.imageHeight = imageHeight;
    }

    @JIPipeDocumentation(name = "Image channels", description = "The number of channels in the image")
    @JIPipeParameter("image-channels")
    public int getImageChannels() {
        return imageChannels;
    }

    @JIPipeParameter("image-channels")
    public void setImageChannels(int imageChannels) {
        this.imageChannels = imageChannels;
    }

//    @JIPipeDocumentation(name = "Image depth", description = "The number of Z slices in the image")
//    @JIPipeParameter("image-depth")
//    public int getImageDepth() {
//        return imageDepth;
//    }
//
//    @JIPipeParameter("image-depth")
//    public void setImageDepth(int imageDepth) {
//        this.imageDepth = imageDepth;
//    }
//
//    @JIPipeDocumentation(name = "Image frames", description = "The number of frame slices in the image")
//    @JIPipeParameter("image-frames")
//    public int getImageFrames() {
//        return imageFrames;
//    }
//
//    @JIPipeParameter("image-frames")
//    public void setImageFrames(int imageFrames) {
//        this.imageFrames = imageFrames;
//    }

    public int getImageDimensions() {
        int dimensions = 1;
        if (getImageHeight() > 1)
            ++dimensions;
//        if (getImageDepth() > 1)
//            ++dimensions;
        if (getImageChannels() > 1)
            ++dimensions;
//        if (getImageFrames() > 1)
//            ++dimensions;
        return dimensions;
    }

    @JsonGetter("image_shape")
    public List<Integer> getImageShape() {
        // Order is WHZCT
        List<Integer> shape = new ArrayList<>();
        shape.add(getImageWidth());
        shape.add(getImageHeight());
        shape.add(getImageChannels());
        return shape;
    }

    @JsonSetter("image_shape")
    public void setImageShape(List<Integer> shape) {
        setImageWidth(shape.get(0));
        if (shape.size() > 1)
            setImageHeight(shape.get(1));
//        if (shape.size() > 2)
//            setImageDepth(shape.get(2));
        if (shape.size() > 3)
            setImageChannels(shape.get(3));
//        if (shape.size() > 4)
//            setImageFrames(shape.get(4));
    }

    @JIPipeDocumentation(name = "Number of classes", description = "The number of classes")
    @JIPipeParameter("num-classes")
    @JsonGetter("n_classes")
    public int getNumClasses() {
        return numClasses;
    }

    @JIPipeParameter("num-classes")
    @JsonSetter("n_classes")
    public void setNumClasses(int numClasses) {
        this.numClasses = numClasses;
    }

    @JsonGetter("output_model_path")
    public Path getOutputModelPath() {
        return outputModelPath;
    }

    @JsonSetter("output_model_path")
    public void setOutputModelPath(Path outputModelPath) {
        this.outputModelPath = outputModelPath;
    }

    @JsonGetter("output_model_json_path")
    public Path getOutputModelJsonPath() {
        return outputModelJsonPath;
    }

    @JsonSetter("output_model_json_path")
    public void setOutputModelJsonPath(Path outputModelJsonPath) {
        this.outputModelJsonPath = outputModelJsonPath;
    }
}
