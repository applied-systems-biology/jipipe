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
import org.hkijena.jipipe.extensions.deeplearning.DeepLearningPredictionType;

import java.nio.file.Path;
import java.nio.file.Paths;

public class DeepLearningPredictionConfiguration {
    private String inputImagesPattern = "raw/*.tif";
    private Path outputPath = Paths.get("");
    private Path inputModelPath = Paths.get("");
    private DeepLearningPredictionType predictionType = DeepLearningPredictionType.standard;

    @JsonGetter("input_dir")
    public String getInputImagesPattern() {
        return inputImagesPattern;
    }

    @JsonSetter("input_dir")
    public void setInputImagesPattern(String inputImagesPattern) {
        this.inputImagesPattern = inputImagesPattern;
    }

    @JsonGetter("output_dir")
    public Path getOutputPath() {
        return outputPath;
    }

    @JsonSetter("output_dir")
    public void setOutputPath(Path outputPath) {
        this.outputPath = outputPath;
    }

    @JsonGetter("input_model_path")
    public Path getInputModelPath() {
        return inputModelPath;
    }

    @JsonSetter("input_model_path")
    public void setInputModelPath(Path inputModelPath) {
        this.inputModelPath = inputModelPath;
    }

    @JsonGetter("prediction_type")
    public DeepLearningPredictionType getPredictionType() {
        return predictionType;
    }

    @JsonSetter("prediction_type")
    public void setPredictionType(DeepLearningPredictionType predictionType) {
        this.predictionType = predictionType;
    }
}
