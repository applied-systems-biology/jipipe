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
import org.hkijena.jipipe.extensions.deeplearning.DeepLearningArchitecture;
import org.hkijena.jipipe.extensions.deeplearning.MonitorLoss;
import org.hkijena.jipipe.extensions.deeplearning.RegularizationMethod;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * POJO class for the model configuration
 */
public class DeepLearningModelConfiguration implements JIPipeParameterCollection {
    private final EventBus eventBus = new EventBus();
    private DeepLearningArchitecture architecture = DeepLearningArchitecture.SegNet;
    private RegularizationMethod regularizationMethod = RegularizationMethod.GaussianDropout;
    private double regularizationLambda = 0.1;
    private int imageSize = 256;
    private int numClasses = 2;
    private Path outputModelPath = Paths.get("");
    private Path outputModelJsonPath = Paths.get("");

    public DeepLearningModelConfiguration() {
    }

    public DeepLearningModelConfiguration(DeepLearningModelConfiguration other) {
        this.architecture = other.architecture;
        this.regularizationMethod = other.regularizationMethod;
        this.regularizationLambda = other.regularizationLambda;
        this.imageSize = other.imageSize;
        this.numClasses = other.numClasses;
        this.outputModelPath = other.outputModelPath;
        this.outputModelJsonPath = other.outputModelJsonPath;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @JIPipeDocumentation(name = "Architecture", description = "The model architecture")
    @JIPipeParameter("architecture")
    @JsonGetter("architecture")
    public DeepLearningArchitecture getArchitecture() {
        return architecture;
    }

    @JIPipeParameter("architecture")
    @JsonSetter("architecture")
    public void setArchitecture(DeepLearningArchitecture architecture) {
        this.architecture = architecture;
    }

    @JIPipeDocumentation(name = "Regularization method", description = "The regularization method")
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

    @JIPipeDocumentation(name = "Regularization lambda", description = "The regularization lambda")
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

    @JIPipeDocumentation(name = "Image size", description = "The width and height of the image")
    @JIPipeParameter("image-size")
    @JsonGetter("img_size")
    public int getImageSize() {
        return imageSize;
    }

    @JIPipeParameter("image-size")
    @JsonSetter("img_size")
    public void setImageSize(int imageSize) {
        this.imageSize = imageSize;
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
