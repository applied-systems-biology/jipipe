package org.hkijena.jipipe.plugins.embeddingserver;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.servers.JIPipeServerInstance;
import org.hkijena.jipipe.api.servers.ProcessSupervisor;
import org.hkijena.jipipe.api.servers.ServerStartException;
import org.hkijena.jipipe.plugins.ai.environments.EmbeddingModelEnvironment;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A spawned-process embedding server instance.
 *
 * <p>{@link #start()} spawns a Java process running {@link EmbeddingServerProcess} via a
 * {@link ProcessSupervisor}. The {@link #embed(String)} and {@link #embedBatch(List)} methods
 * perform HTTP POST requests against the spawned server's OpenAI-compatible
 * {@code /v1/embeddings} endpoint.</p>
 */
public class EmbeddingServerInstance extends JIPipeServerInstance<EmbeddingModelEnvironment> {

    /**
     * Factory ID for embedding servers.
     */
    public static final String FACTORY_ID = "embedding-server";

    private final ProcessSupervisor processSupervisor = new ProcessSupervisor();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Creates a new embedding server instance.
     *
     * @param environment the embedding model environment configuration
     * @param port        the TCP port the server will listen on
     */
    public EmbeddingServerInstance(EmbeddingModelEnvironment environment, int port) {
        super(environment, port);
    }

    /**
     * Spawns the embedding server process.
     *
     * @throws ServerStartException if the process cannot be spawned
     */
    @Override
    protected void startProcess() throws ServerStartException {
        try {
            List<String> command = buildSpawnCommand(getEnvironment());
            Process process = processSupervisor.spawnProcess(command, Collections.emptyMap(),
                    getDisplayName(), new JIPipeProgressInfo());
            setProcess(process);
        } catch (IOException e) {
            throw new ServerStartException("Failed to spawn embedding server process: " + e.getMessage(),
                    ServerStartException.Reason.Unknown, e);
        }
    }

    /**
     * Stops the embedding server process.
     */
    @Override
    protected void stopProcess() {
        Process process = getProcess();
        if (process != null && process.isAlive()) {
            processSupervisor.stopProcess(process, new JIPipeProgressInfo());
        }
    }

    /**
     * Checks whether the server is healthy by issuing an HTTP GET to {@code /health}.
     *
     * @return {@code true} if the health check returns status 200
     */
    @Override
    public boolean isHealthy() {
        try {
            URL url = new URL("http://127.0.0.1:" + getPort() + "/health");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(2000);
            int status = connection.getResponseCode();
            connection.disconnect();
            return status == 200;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Returns the server type identifier.
     *
     * @return {@link #FACTORY_ID}
     */
    @Override
    public String getServerTypeId() {
        return FACTORY_ID;
    }

    /**
     * Embeds a single text.
     *
     * @param text the text to embed
     * @return the embedding vector
     * @throws IOException if the request fails
     */
    public float[] embed(String text) throws IOException {
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("model", deriveModelId());
        ArrayNode inputArray = objectMapper.createArrayNode();
        inputArray.add(text);
        requestNode.set("input", inputArray);

        String requestBody = objectMapper.writeValueAsString(requestNode);
        String responseBody = httpPost("/v1/embeddings", requestBody);
        return parseEmbeddingResponse(responseBody, 0);
    }

    /**
     * Embeds a batch of texts.
     *
     * @param texts the texts to embed
     * @return an array of embedding vectors, one per input text
     * @throws IOException if the request fails
     */
    public float[][] embedBatch(List<String> texts) throws IOException {
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("model", deriveModelId());
        ArrayNode inputArray = objectMapper.createArrayNode();
        for (String text : texts) {
            inputArray.add(text);
        }
        requestNode.set("input", inputArray);

        String requestBody = objectMapper.writeValueAsString(requestNode);
        String responseBody = httpPost("/v1/embeddings", requestBody);

        float[][] result = new float[texts.size()][];
        for (int i = 0; i < texts.size(); i++) {
            result[i] = parseEmbeddingResponse(responseBody, i);
        }
        return result;
    }

    /**
     * Builds the command used to spawn the embedding server process.
     *
     * <p>To avoid exceeding the operating system's command-line length limit (notably
     * Windows' ~32,768 character limit, which the full {@code java.class.path} can
     * easily surpass), all JVM arguments are written to a temporary argument file that
     * is referenced via Java's {@code @argfile} syntax. The {@code @argfile} feature
     * (available since Java 9) expands the file contents as if the arguments were
     * passed on the command line.</p>
     *
     * @param env the embedding model environment
     * @return the command and arguments
     * @throws IOException if the temporary argument file cannot be created or written
     */
    private List<String> buildSpawnCommand(EmbeddingModelEnvironment env) throws IOException {
        String javaHome = System.getProperty("java.home");
        String javaExecutable = javaHome + File.separator + "bin" + File.separator + "java";
        String classPath = System.getProperty("java.class.path");
        String modelId = env.deriveModelId();
        if (modelId == null) {
            modelId = "unknown";
        }

        // Write all JVM arguments to a temporary argfile to avoid command-line length
        // limits (e.g. Windows "File name or extension too long" when the classpath is
        // very long). Each argument is written on its own line.
        Path argFile = Files.createTempFile("embedding-server-args", ".txt");
        argFile.toFile().deleteOnExit();

        List<String> jvmArgs = new ArrayList<>();
        jvmArgs.add("-cp");
        jvmArgs.add(classPath);
        jvmArgs.add(EmbeddingServerProcess.class.getName());
        jvmArgs.add("--model");
        jvmArgs.add(env.getLocalModelFile().toString());
        jvmArgs.add("--tokenizer");
        jvmArgs.add(env.getLocalTokenizerFile().toString());
        jvmArgs.add("--port");
        jvmArgs.add(String.valueOf(getPort()));
        jvmArgs.add("--model-id");
        jvmArgs.add(modelId);

        // Pass the current JVM's PID so the spawned server can self-terminate
        // if this parent process is killed (e.g., via SIGKILL).
        jvmArgs.add("--parent-pid");
        jvmArgs.add(String.valueOf(ProcessHandle.current().pid()));

        // Write the arguments to the file, one per line
        Files.write(argFile, jvmArgs, StandardCharsets.UTF_8);

        List<String> command = new ArrayList<>();
        command.add(javaExecutable);
        command.add("@" + argFile.toString());
        return command;
    }

    /**
     * Performs an HTTP POST request to the given path.
     *
     * @param path the URL path (e.g., "/v1/embeddings")
     * @param body the request body
     * @return the response body as a string
     * @throws IOException if the request fails
     */
    private String httpPost(String path, String body) throws IOException {
        URL url = new URL("http://127.0.0.1:" + getPort() + path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        try (OutputStream os = connection.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        return readResponse(connection);
    }

    /**
     * Parses an embedding response and extracts the vector at the given index.
     *
     * @param responseBody the raw response body
     * @param index        the index of the embedding to extract
     * @return the embedding vector
     * @throws IOException if the response cannot be parsed
     */
    private float[] parseEmbeddingResponse(String responseBody, int index) throws IOException {
        JsonNode rootNode = objectMapper.readTree(responseBody);
        JsonNode dataNode = rootNode.get("data");
        JsonNode elementNode = dataNode.get(index);
        JsonNode embeddingNode = elementNode.get("embedding");

        float[] embedding = new float[embeddingNode.size()];
        for (int i = 0; i < embeddingNode.size(); i++) {
            embedding[i] = (float) embeddingNode.get(i).asDouble();
        }
        return embedding;
    }

    /**
     * Reads the response from an HTTP connection, using the error stream if the status is >= 400.
     *
     * @param connection the HTTP connection
     * @return the response body as a UTF-8 string
     * @throws IOException if reading the response fails
     */
    private String readResponse(HttpURLConnection connection) throws IOException {
        int status = connection.getResponseCode();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        status >= 400
                                ? connection.getErrorStream()
                                : connection.getInputStream(),
                        StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Derives the model ID from the environment, falling back to "unknown".
     *
     * @return the model ID, or "unknown" if it cannot be determined
     */
    private String deriveModelId() {
        String modelId = getEnvironment().deriveModelId();
        if (modelId == null) {
            modelId = "unknown";
        }
        return modelId;
    }
}
