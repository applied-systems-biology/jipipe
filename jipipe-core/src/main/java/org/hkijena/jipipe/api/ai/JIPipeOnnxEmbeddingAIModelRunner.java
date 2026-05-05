package org.hkijena.jipipe.api.ai;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import org.hkijena.jipipe.utils.AIUtils;
import org.hkijena.jipipe.utils.MathUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.nio.LongBuffer;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class JIPipeOnnxEmbeddingAIModelRunner implements JIPipeEmbeddingAIModelRunner {

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Path modelPath;
    private final Path tokenizerPath;
    private JIPipeAIModelRunnerStatus status = JIPipeAIModelRunnerStatus.Unloaded;

    private OrtEnvironment env;
    private OrtSession session;
    private HuggingFaceTokenizer tokenizer;

    public JIPipeOnnxEmbeddingAIModelRunner(Path modelFolder) {
        this(modelFolder.resolve("model.onnx"), modelFolder.resolve("tokenizer.json"));
    }

    public JIPipeOnnxEmbeddingAIModelRunner(Path modelPath, Path tokenizerPath) {
        this.modelPath = modelPath;
        this.tokenizerPath = tokenizerPath;
    }

    @Override
    public float[] embed(String text) {
        Encoding encoding = tokenizer.encode(text);

        long[] inputIds = encoding.getIds();
        long[] typeIds = encoding.getTypeIds();
        long[] attentionMask = encoding.getAttentionMask();

        long[] shape = new long[]{1, inputIds.length};

        try (
                OnnxTensor inputIdsTensor = OnnxTensor.createTensor(
                        env,
                        LongBuffer.wrap(inputIds),
                        shape
                );
                OnnxTensor attentionMaskTensor = OnnxTensor.createTensor(
                        env,
                        LongBuffer.wrap(attentionMask),
                        shape
                );
                OnnxTensor typeIdsTensor = OnnxTensor.createTensor(
                        env,
                        LongBuffer.wrap(typeIds),
                        shape
                )
        ) {
            Map<String, OnnxTensor> inputs = Map.of(
                    "input_ids", inputIdsTensor,
                    "token_type_ids", typeIdsTensor,
                    "attention_mask", attentionMaskTensor
            );

            try (OrtSession.Result result = session.run(inputs)) {
                Object raw = result.get(0).getValue();

                // Depends on model output:
                // common shapes:
                // [1, hidden_size]
                // [1, sequence_length, hidden_size]
                //
                // For sentence-transformers-style models, you may need mean pooling
                // over token embeddings using attention_mask, then L2 normalization.

                return extractEmbedding(raw, attentionMask);
            }
        } catch (OrtException e) {
            throw new RuntimeException(e);
        }
    }

    private static float[] extractEmbedding(Object raw, long[] attentionMask) {
        if (raw instanceof float[][] matrix) {
            return MathUtils.l2Normalize(matrix[0]);
        }

        if (raw instanceof float[][][] tokenEmbeddings) {
            return AIUtils.meanPoolAndNormalize(tokenEmbeddings[0], attentionMask);
        }

        throw new IllegalStateException("Unsupported embedding output: " + raw.getClass());
    }

    @Override
    public JIPipeAIModelRunnerStatus getStatus() {
        return status;
    }

    @Override
    public void start() {
        lock.readLock().lock();
        if (status == JIPipeAIModelRunnerStatus.Unloaded) {
            lock.readLock().unlock();
            lock.writeLock().lock();
            status = JIPipeAIModelRunnerStatus.Loading;
            try {
                this.env = OrtEnvironment.getEnvironment();
                OrtSession.SessionOptions options = new OrtSession.SessionOptions();
                options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);

                // TODO: configurable
                options.setIntraOpNumThreads(Math.max(1, Runtime.getRuntime().availableProcessors() / 2));
                options.setInterOpNumThreads(1);

                this.session = env.createSession(
                        modelPath.toString(),
                        options
                );

                this.tokenizer = HuggingFaceTokenizer.newInstance(
                        tokenizerPath
                );

                status = JIPipeAIModelRunnerStatus.Idle;
            } catch (Exception e) {
                status = JIPipeAIModelRunnerStatus.Failed;
            } finally {
                lock.writeLock().unlock();
            }
        } else {
            lock.readLock().unlock();
        }
    }

    @Override
    public void shutdown() {

    }

    public static void main(String[] args) {
        JIPipeOnnxEmbeddingAIModelRunner model = new JIPipeOnnxEmbeddingAIModelRunner(Path.of("/data/src/bge-small-en-v1.5-ONNX/onnx/model.onnx"),
                Path.of("/data/src/bge-small-en-v1.5-ONNX/tokenizer.json"));
        System.out.println("Loading ...");
        model.start();
        System.out.println("Loading finished");
        float[] embed = model.embed("His La Hee performance still lives rent free in my head. What a guy. I'm glad he's better now");
        System.out.println(JsonUtils.toJsonString(embed));
        model.shutdown();
    }
}
