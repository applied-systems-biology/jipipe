package org.hkijena.jipipe.api.ai;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import org.hkijena.jipipe.api.microservice.MicroserviceState;
import org.hkijena.jipipe.utils.AIUtils;
import org.hkijena.jipipe.utils.MathUtils;

import java.nio.LongBuffer;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class JIPipeOnnxEmbeddingAIModelRunner implements JIPipeEmbeddingAIModelRunner {

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Path modelPath;
    private final Path tokenizerPath;
    private final String modelId;
    private MicroserviceState status = MicroserviceState.Stopped;
    private String lastError = null;

    private OrtEnvironment env;
    private OrtSession session;
    private HuggingFaceTokenizer tokenizer;

    public JIPipeOnnxEmbeddingAIModelRunner(Path modelPath, Path tokenizerPath, String modelId) {
        this.modelPath = modelPath;
        this.tokenizerPath = tokenizerPath;
        this.modelId = modelId;
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
    public MicroserviceState getStatus() {
        return status;
    }

    @Override
    public String getLastError() {
        return lastError;
    }

    @Override
    public void start() {
        lock.readLock().lock();
        if (status == MicroserviceState.Stopped) {
            lock.readLock().unlock();
            lock.writeLock().lock();
            status = MicroserviceState.Starting;
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

                status = MicroserviceState.Ready;
            } catch (Exception e) {
                status = MicroserviceState.Failed;
            } finally {
                lock.writeLock().unlock();
            }
        } else {
            lock.readLock().unlock();
        }
    }

    @Override
    public void shutdown() {
        lock.writeLock().lock();
        try {
            if (status == MicroserviceState.Stopped) {
                return;
            }
            status = MicroserviceState.Stopping;
            try {
                if (session != null) {
                    session.close();
                    session = null;
                }
                // Do NOT close env - it's shared/singleton
                tokenizer = null;
                status = MicroserviceState.Stopped;
            } catch (Exception e) {
                lastError = e.getMessage();
                status = MicroserviceState.Failed;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public String getModelId() {
        return modelId;
    }
}
