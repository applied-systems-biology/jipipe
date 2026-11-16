package org.hkijena.jipipe.api.servers;

import java.util.ArrayList;
import java.util.List;

/**
 * Bounded, thread-safe ring buffer of {@link ServerLogEntry} instances used to serve
 * recent server logs over a {@code /control/logs} endpoint.
 */
public class ServerLogRingBuffer {

    private final ServerLogEntry[] buffer;
    private final int capacity;
    private int head = 0;
    private int count = 0;
    private int nextIndex = 0;

    public ServerLogRingBuffer(int capacity) {
        this.capacity = capacity;
        this.buffer = new ServerLogEntry[capacity];
    }

    public synchronized void add(String level, String message) {
        ServerLogEntry entry = new ServerLogEntry(nextIndex, System.currentTimeMillis(), level, message);
        buffer[head] = entry;
        head = (head + 1) % capacity;
        if (count < capacity) {
            count++;
        }
        nextIndex++;
    }

    public synchronized List<ServerLogEntry> getSince(int index) {
        List<ServerLogEntry> result = new ArrayList<>();
        if (count == 0) {
            return result;
        }
        int start = (head - count + capacity) % capacity;
        for (int i = 0; i < count; i++) {
            int pos = (start + i) % capacity;
            ServerLogEntry entry = buffer[pos];
            if (entry.getIndex() > index) {
                result.add(entry);
            }
        }
        return result;
    }

    public synchronized int getNextIndex() {
        return nextIndex;
    }

    public synchronized List<ServerLogEntry> getAll() {
        return getSince(-1);
    }
}
