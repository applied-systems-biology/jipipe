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
import org.hkijena.jipipe.extensions.deeplearning.enums.EvaluationMethod;
import org.hkijena.jipipe.extensions.deeplearning.enums.NormalizationMethod;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class DeepLearningEvaluationConfiguration {
    private String inputImagesPattern = "raw/*.tif";
    private Path outputFigurePath = Paths.get("");
    private Path inputModelPath = Paths.get("");
    private Path inputModelJsonPath = Paths.get("");
    private String inputLabelsPattern = "labels/*.tif";
    private EvaluationMethod evaluationMethod = EvaluationMethod.plot_probabilities;
    private Map<String, Integer> labelDict = new HashMap<>();

    public DeepLearningEvaluationConfiguration() {
    }

    public DeepLearningEvaluationConfiguration(DeepLearningEvaluationConfiguration other) {
        this.inputImagesPattern = other.inputImagesPattern;
        this.outputFigurePath = other.outputFigurePath;
        this.inputModelPath = other.inputModelPath;
        this.inputModelJsonPath = other.inputModelJsonPath;
        this.inputLabelsPattern = other.inputLabelsPattern;
        this.evaluationMethod = other.evaluationMethod;
        this.labelDict = new HashMap<>(other.labelDict);
    }

    @JsonGetter("input_dir")
    public String getInputImagesPattern() {
        return inputImagesPattern;
    }

    @JsonSetter("input_dir")
    public void setInputImagesPattern(String inputImagesPattern) {
        this.inputImagesPattern = inputImagesPattern;
    }

    @JsonGetter("label_dir")
    public String getInputLabelsPattern() {
        return inputLabelsPattern;
    }

    @JsonSetter("label_dir")
    public void setInputLabelsPattern(String inputLabelsPattern) {
        this.inputLabelsPattern = inputLabelsPattern;
    }

    @JsonGetter("output_figure_path")
    public Path getOutputFigurePath() {
        return outputFigurePath;
    }

    @JsonSetter("output_figure_path")
    public void setOutputFigurePath(Path outputFigurePath) {
        this.outputFigurePath = outputFigurePath;
    }

    @JsonGetter("input_model_path")
    public Path getInputModelPath() {
        return inputModelPath;
    }

    @JsonSetter("input_model_path")
    public void setInputModelPath(Path inputModelPath) {
        this.inputModelPath = inputModelPath;
    }

    @JsonGetter("input_model_json_path")
    public Path getInputModelJsonPath() {
        return inputModelJsonPath;
    }

    @JsonSetter("input_model_json_path")
    public void setInputModelJsonPath(Path inputModelJsonPath) {
        this.inputModelJsonPath = inputModelJsonPath;
    }

    @JsonGetter("evaluation_method")
    public EvaluationMethod getEvaluationMethod() {
        return evaluationMethod;
    }

    @JsonSetter("evaluation_method")
    public void setEvaluationMethod(EvaluationMethod evaluationMethod) {
        this.evaluationMethod = evaluationMethod;
    }

    @JsonGetter("label_dict")
    public Map<String, Integer> getLabelDict() {
        return labelDict;
    }

    @JsonSetter("label_dict")
    public void setLabelDict(Map<String, Integer> labelDict) {
        this.labelDict = labelDict;
    }
}
