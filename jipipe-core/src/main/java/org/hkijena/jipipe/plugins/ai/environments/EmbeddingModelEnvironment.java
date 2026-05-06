package org.hkijena.jipipe.plugins.ai.environments;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ai.JIPipeAPIEmbeddingAIModelRunner;
import org.hkijena.jipipe.api.ai.JIPipeEmbeddingAIModelRunner;
import org.hkijena.jipipe.api.ai.JIPipeOnnxEmbeddingAIModelRunner;
import org.hkijena.jipipe.api.artifacts.JIPipeLocalArtifact;
import org.hkijena.jipipe.api.environments.JIPipeArtifactEnvironment;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportSettings;
import org.hkijena.jipipe.plugins.ai.EmbeddingModelType;
import org.hkijena.jipipe.plugins.parameters.library.auth.JIPipePasswordParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.StringUtils;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class EmbeddingModelEnvironment extends JIPipeArtifactEnvironment {

    private Path localModelFile = Paths.get("");
    private Path localTokenizerFile = Paths.get("");
    private EmbeddingModelType modelType = EmbeddingModelType.LocalOnnx;
    private String apiBase = "";
    private String apiModel = "";
    private JIPipePasswordParameter apiKey = new JIPipePasswordParameter();

    public EmbeddingModelEnvironment() {
    }

    public EmbeddingModelEnvironment(EmbeddingModelEnvironment other) {
        super(other);
        this.localModelFile = other.localModelFile;
        this.localTokenizerFile = other.localTokenizerFile;
        this.modelType = other.modelType;
        this.apiBase = other.apiBase;
        this.apiModel = other.apiModel;
        this.apiKey = other.apiKey;
    }

    @Override
    public boolean isAllowReadOnlyDeployment() {
        return true;
    }

    @SetJIPipeDocumentation(name = "Local model file", description = "Only for local models. The model file.")
    @JIPipeParameter("local-model-file")
    @JsonGetter("local-model-file")
    public Path getLocalModelFile() {
        return localModelFile;
    }

    @JIPipeParameter("local-model-file")
    @JsonSetter("local-model-file")
    public void setLocalModelFile(Path localModelFile) {
        this.localModelFile = localModelFile;
    }

    @SetJIPipeDocumentation(name = "Local model tokenizer", description = "Only for local models. The tokenizer configuration.")
    @JIPipeParameter("local-tokenizer-file")
    @JsonGetter("local-tokenizer-file")
    public Path getLocalTokenizerFile() {
        return localTokenizerFile;
    }

    @JIPipeParameter("local-tokenizer-file")
    @JsonSetter("local-tokenizer-file")
    public void setLocalTokenizerFile(Path localTokenizerFile) {
        this.localTokenizerFile = localTokenizerFile;
    }

    @SetJIPipeDocumentation(name = "Model type", description = "The type of model to be used")
    @JIPipeParameter("model-type")
    @JsonGetter("model-type")
    public EmbeddingModelType getModelType() {
        return modelType;
    }

    @JIPipeParameter("model-type")
    @JsonSetter("model-type")
    public void setModelType(EmbeddingModelType modelType) {
        this.modelType = modelType;
    }

    @SetJIPipeDocumentation(name = "API base", description = "Only for API. The base of the API.")
    @JIPipeParameter("api-base")
    @JsonGetter("api-base")
    @StringParameterSettings(monospace = true)
    public String getApiBase() {
        return apiBase;
    }

    @JIPipeParameter("api-base")
    @JsonSetter("api-base")
    public void setApiBase(String apiBase) {
        this.apiBase = apiBase;
    }

    @SetJIPipeDocumentation(name = "API model", description = "Only for API. The model ID.")
    @JIPipeParameter("api-model")
    @JsonGetter("api-model")
    @StringParameterSettings(monospace = true)
    public String getApiModel() {
        return apiModel;
    }

    @JIPipeParameter("api-model")
    @JsonSetter("api-model")
    public void setApiModel(String apiModel) {
        this.apiModel = apiModel;
    }

    @SetJIPipeDocumentation(name = "API key", description = "Only for API. The API key.")
    @JIPipeParameter("api-key")
    @JsonGetter("api-key")
    public JIPipePasswordParameter getApiKey() {
        return apiKey;
    }

    @JIPipeParameter("api-key")
    @JsonSetter("api-key")
    public void setApiKey(JIPipePasswordParameter apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public void applyConfigurationFromArtifact(JIPipeLocalArtifact artifact, JIPipeProgressInfo progressInfo) {
        setModelType(EmbeddingModelType.LocalOnnx);
        setLocalModelFile(artifact.getLocalPath().resolve("embedding").resolve("onnx").resolve("model.onnx"));
        setLocalTokenizerFile(artifact.getLocalPath().resolve("embedding").resolve("tokenizer.json"));
    }

    @Override
    public Icon getIcon() {
        return JIPipe.RESOURCES.getIcon16("actions/ai.png");
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReportSettings reportSettings, JIPipeValidationReport report, JIPipeProgressInfo progressInfo) {
        super.reportValidity(reportContext, reportSettings, report, progressInfo);
        if(!isLoadFromArtifact()) {
            if(!isValidLocalOnnx() && !isValidOpenAI()) {
                reportContext.error().title("Invalid embedding model configuration")
                        .explanation("A local model or remote API needs to be set up")
                        .solution("Setup a local model or remote API").report(report);
            }
        }
    }

    @Override
    public String getInfo() {
        if (isLoadFromArtifact()) {
            return StringUtils.orElse(getArtifactQuery().getQuery(), "<Not set>");
        } else if(getModelType() == EmbeddingModelType.LocalOnnx && isValidLocalOnnx()) {
            return "ONNX (Local)";
        }
        else if(getModelType() == EmbeddingModelType.OpenAIAPI && isValidOpenAI()) {
            return "Remote API";
        }
        return "<Not set>";
    }

    private boolean isValidOpenAI() {
        if(getModelType() == EmbeddingModelType.OpenAIAPI) {
            return !StringUtils.isNullOrEmpty(getApiBase());
        }
        return false;
    }

    public boolean isValidLocalOnnx() {
        if(getModelType() == EmbeddingModelType.LocalOnnx) {
            return Files.isRegularFile(getLocalModelFile()) && Files.isRegularFile(getLocalTokenizerFile());
        }
        return false;
    }

    public JIPipeEmbeddingAIModelRunner toRunner() {
        if(getModelType() == EmbeddingModelType.LocalOnnx) {
            return new JIPipeOnnxEmbeddingAIModelRunner(localModelFile, localTokenizerFile);
        }
        else if(getModelType() == EmbeddingModelType.OpenAIAPI) {
            return new JIPipeAPIEmbeddingAIModelRunner(apiBase, apiModel, apiKey.getPassword());
        }
    }
}
