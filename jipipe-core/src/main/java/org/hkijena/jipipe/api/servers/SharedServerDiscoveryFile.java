package org.hkijena.jipipe.api.servers;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SharedServerDiscoveryFile {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void write(Path file, long pid, int port, String startedAt) {
        DiscoveryData data = new DiscoveryData();
        data.setPid(pid);
        data.setPort(port);
        data.setStartedAt(startedAt);
        data.setVersion("1.0");
        try {
            Files.createDirectories(file.getParent());
            objectMapper.writeValue(file.toFile(), data);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write discovery file: " + e.getMessage(), e);
        }
    }

    public static DiscoveryData read(Path file) {
        if (!Files.exists(file)) {
            return null;
        }
        try {
            return objectMapper.readValue(file.toFile(), DiscoveryData.class);
        } catch (IOException e) {
            return null;
        }
    }

    public static void delete(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
        }
    }

    public static boolean isPidAlive(long pid) {
        return ProcessHandle.of(pid).isPresent();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DiscoveryData {
        private long pid;
        private int port;
        private String startedAt;
        private String version = "1.0";

        public DiscoveryData() {
        }

        @JsonGetter("pid")
        public long getPid() {
            return pid;
        }

        @JsonSetter("pid")
        public void setPid(long pid) {
            this.pid = pid;
        }

        @JsonGetter("port")
        public int getPort() {
            return port;
        }

        @JsonSetter("port")
        public void setPort(int port) {
            this.port = port;
        }

        @JsonGetter("started-at")
        public String getStartedAt() {
            return startedAt;
        }

        @JsonSetter("started-at")
        public void setStartedAt(String startedAt) {
            this.startedAt = startedAt;
        }

        @JsonGetter("version")
        public String getVersion() {
            return version;
        }

        @JsonSetter("version")
        public void setVersion(String version) {
            this.version = version;
        }
    }
}
