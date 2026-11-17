package org.hkijena.jipipe.api.servers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TestSharedServerDiscoveryFile {

    @Test
    void writeAndRead(@TempDir Path tempDir) {
        Path file = tempDir.resolve("discovery.json");
        SharedServerDiscoveryFile.write(file, 12345, 51842, "2026-07-16T10:30:00Z");

        SharedServerDiscoveryFile.DiscoveryData data = SharedServerDiscoveryFile.read(file);
        assertNotNull(data);
        assertEquals(12345, data.getPid());
        assertEquals(51842, data.getPort());
        assertEquals("2026-07-16T10:30:00Z", data.getStartedAt());
        assertEquals("1.0", data.getVersion());
    }

    @Test
    void readNonexistentReturnsNull(@TempDir Path tempDir) {
        Path file = tempDir.resolve("nonexistent.json");
        assertNull(SharedServerDiscoveryFile.read(file));
    }

    @Test
    void deleteRemovesFile(@TempDir Path tempDir) {
        Path file = tempDir.resolve("discovery.json");
        SharedServerDiscoveryFile.write(file, 12345, 51842, "2026-07-16T10:30:00Z");
        assertTrue(file.toFile().exists());
        SharedServerDiscoveryFile.delete(file);
        assertFalse(file.toFile().exists());
    }

    @Test
    void isPidAliveReturnsFalseForDeadPid() {
        assertFalse(SharedServerDiscoveryFile.isPidAlive(99999999));
    }

    @Test
    void isPidAliveReturnsTrueForCurrentPid() {
        long currentPid = ProcessHandle.current().pid();
        assertTrue(SharedServerDiscoveryFile.isPidAlive(currentPid));
    }
}
