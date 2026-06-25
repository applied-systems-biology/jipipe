package org.hkijena.jipipe.plugins.embeddingserver;

import org.hkijena.jipipe.api.ai.JIPipeOnnxEmbeddingAIModelRunner;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Minimal standalone Java application that loads an ONNX embedding model and serves an
 * OpenAI-compatible HTTP API.
 *
 * <p>This class deliberately does NOT initialize the full JIPipe runtime (no
 * {@code JIPipe.getInstance()}, no SciJava context, no plugin system). It is designed to be
 * spawned as a separate JVM process that only loads the model and answers HTTP requests.</p>
 */
public class EmbeddingServerProcess {

    private final JIPipeOnnxEmbeddingAIModelRunner runner;
    private final String modelId;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private volatile boolean modelReady = false;
    private final long parentPid;

    /**
     * Creates and starts the embedding server process.
     *
     * @param modelPath    the path to the ONNX model file
     * @param tokenizerPath the path to the tokenizer file
     * @param port         the TCP port to listen on
     * @param modelId      the model identifier reported by the API
     * @param parentPid    the PID of the parent process to monitor (0 to disable monitoring)
     * @throws Exception if the model or HTTP server cannot be started
     */
    public EmbeddingServerProcess(Path modelPath, Path tokenizerPath, int port, String modelId, long parentPid) throws Exception {
        this.modelId = modelId;
        this.parentPid = parentPid;

        // Load the ONNX model
        this.runner = new JIPipeOnnxEmbeddingAIModelRunner(modelPath, tokenizerPath, modelId);
        runner.start();
        modelReady = true;

        // Start the HTTP server
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/v1/embeddings", new EmbeddingsHandler());
        server.createContext("/health", new HealthHandler());
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();

        // Start monitoring the parent process so this server self-terminates
        // if the parent is killed (e.g., via SIGKILL, which cannot run shutdown hooks).
        if (parentPid > 0) {
            startParentMonitor();
        }
    }

    /**
     * Starts a daemon thread that periodically checks whether the parent process
     * (identified by {@link #parentPid}) is still alive. If the parent is gone,
     * this server self-terminates via {@link System#exit(int)}.
     *
     * <p>This handles the case where the parent JIPipe process is killed with
     * {@code kill -9} (SIGKILL), which prevents shutdown hooks from running and
     * thus prevents the parent from telling this server to stop.</p>
     */
    private void startParentMonitor() {
        Thread monitor = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(2000); // Check every 2 seconds
                } catch (InterruptedException e) {
                    break;
                }
                if (!ProcessHandle.of(parentPid).isPresent()) {
                    // Parent process is gone — self-terminate
                    System.exit(0);
                }
            }
        }, "Parent-Process-Monitor");
        monitor.setDaemon(true);
        monitor.start();
    }

    /**
     * Entry point for the standalone server process.
     *
     * <p>Supported arguments:</p>
     * <ul>
     *     <li>{@code --model <path>} - path to the ONNX model file</li>
     *     <li>{@code --tokenizer <path>} - path to the tokenizer file</li>
     *     <li>{@code --port <int>} - TCP port to listen on</li>
     *     <li>{@code --model-id <string>} - model identifier (default: "unknown")</li>
     *     <li>{@code --parent-pid <long>} - PID of the parent process to monitor (optional)</li>
     * </ul>
     *
     * @param args the command-line arguments
     * @throws Exception if the server fails to start
     */
    public static void main(String[] args) throws Exception {
        Path modelPath = null;
        Path tokenizerPath = null;
        int port = 0;
        String modelId = "unknown";
        long parentPid = 0;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--model":
                    if (i + 1 < args.length) {
                        modelPath = Paths.get(args[++i]);
                    }
                    break;
                case "--tokenizer":
                    if (i + 1 < args.length) {
                        tokenizerPath = Paths.get(args[++i]);
                    }
                    break;
                case "--port":
                    if (i + 1 < args.length) {
                        port = Integer.parseInt(args[++i]);
                    }
                    break;
                case "--model-id":
                    if (i + 1 < args.length) {
                        modelId = args[++i];
                    }
                    break;
                case "--parent-pid":
                    if (i + 1 < args.length) {
                        parentPid = Long.parseLong(args[++i]);
                    }
                    break;
                default:
                    // Ignore unknown arguments
                    break;
            }
        }

        if (modelPath == null || tokenizerPath == null || port == 0) {
            System.err.println("Usage: EmbeddingServerProcess --model <path> --tokenizer <path> --port <int> [--model-id <string>] [--parent-pid <long>]");
            System.exit(1);
        }

        new EmbeddingServerProcess(modelPath, tokenizerPath, port, modelId, parentPid);

        // Keep the process running until killed
        Thread.currentThread().join();
    }

    /**
     * Handles OpenAI-compatible embedding requests on {@code /v1/embeddings}.
     */
    private class EmbeddingsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                // Reject non-POST requests
                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    String response = "{\"error\":\"Method Not Allowed\"}";
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(405, response.getBytes(StandardCharsets.UTF_8).length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes(StandardCharsets.UTF_8));
                    }
                    return;
                }

                // Parse the request body
                String bodyJson = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                JsonNode rootNode = objectMapper.readTree(bodyJson);

                // Read the input node - handle both a single string and an array of strings
                JsonNode inputNode = rootNode.get("input");
                List<String> inputs = new ArrayList<>();
                if (inputNode != null) {
                    if (inputNode.isArray()) {
                        for (JsonNode element : inputNode) {
                            inputs.add(element.asText());
                        }
                    } else {
                        inputs.add(inputNode.asText());
                    }
                }

                // Build the response data array
                ArrayNode dataArray = objectMapper.createArrayNode();
                int totalTokens = 0;

                for (int i = 0; i < inputs.size(); i++) {
                    float[] embedding = runner.embed(inputs.get(i));

                    ObjectNode embeddingObject = objectMapper.createObjectNode();
                    embeddingObject.put("object", "embedding");

                    ArrayNode embeddingArray = objectMapper.createArrayNode();
                    for (float value : embedding) {
                        embeddingArray.add(value);
                    }
                    embeddingObject.set("embedding", embeddingArray);
                    embeddingObject.put("index", i);

                    dataArray.add(embeddingObject);

                    // Token estimate (~4 chars per token)
                    totalTokens += inputs.get(i).length() / 4;
                }

                ObjectNode responseNode = objectMapper.createObjectNode();
                responseNode.put("object", "list");
                responseNode.set("data", dataArray);
                responseNode.put("model", modelId);

                ObjectNode usageNode = objectMapper.createObjectNode();
                usageNode.put("prompt_tokens", totalTokens);
                usageNode.put("total_tokens", totalTokens);
                responseNode.set("usage", usageNode);

                String response = objectMapper.writeValueAsString(responseNode);
                byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);

                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            } catch (Exception e) {
                ObjectNode errorNode = objectMapper.createObjectNode();
                errorNode.put("error", e.getMessage());
                String response = objectMapper.writeValueAsString(errorNode);
                byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);

                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(500, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            } finally {
                exchange.close();
            }
        }
    }

    /**
     * Handles health check requests on {@code /health}.
     */
    private class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if (modelReady) {
                    String response = "{\"status\":\"ready\"}";
                    byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, responseBytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(responseBytes);
                    }
                } else {
                    exchange.sendResponseHeaders(503, -1);
                }
            } finally {
                exchange.close();
            }
        }
    }
}
