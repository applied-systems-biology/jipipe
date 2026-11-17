package org.hkijena.jipipe.api.servers;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * One entry in a {@link ServerLogRingBuffer}: a monotonically increasing index, a timestamp,
 * a level string, and a message.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServerLogEntry {

    private int index;
    private long timestamp;
    private String level;
    private String message;

    public ServerLogEntry() {
    }

    public ServerLogEntry(int index, long timestamp, String level, String message) {
        this.index = index;
        this.timestamp = timestamp;
        this.level = level;
        this.message = message;
    }

    @JsonGetter("index")
    public int getIndex() {
        return index;
    }

    @JsonSetter("index")
    public void setIndex(int index) {
        this.index = index;
    }

    @JsonGetter("timestamp")
    public long getTimestamp() {
        return timestamp;
    }

    @JsonSetter("timestamp")
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @JsonGetter("level")
    public String getLevel() {
        return level;
    }

    @JsonSetter("level")
    public void setLevel(String level) {
        this.level = level;
    }

    @JsonGetter("message")
    public String getMessage() {
        return message;
    }

    @JsonSetter("message")
    public void setMessage(String message) {
        this.message = message;
    }
}
