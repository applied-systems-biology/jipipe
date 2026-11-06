/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.progress;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * One record of the NDJSON progress log (one JSON object per line; spec §Record types).
 * <p>
 * Records are the parseable contract of the progress store: every record carries the discriminator field
 * {@code t} (obtained via {@link #type()}), and all other fields use the short keys defined by the
 * {@link JsonProperty} annotations. Timestamps are epoch millis. The JSON keys and the {@code t} values
 * are persisted in log files and must not change.
 */
public sealed interface JIPipeProgressRecord {

    /**
     * Returns the value of the discriminator field {@code t}.
     *
     * @return the record type identifier
     */
    @JsonProperty("t")
    String type();

    /**
     * File header, always line 1 of a log file.
     *
     * @param v     schema version ({@code 1})
     * @param run   run UUID
     * @param label run label
     * @param app   JIPipe version
     * @param ts    epoch millis
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Head(@JsonProperty("v") int v, @JsonProperty("run") String run, @JsonProperty("label") String label, @JsonProperty("app") String app, @JsonProperty("ts") long ts) implements JIPipeProgressRecord {

        @Override
        public String type() {
            return "head";
        }
    }

    /**
     * Node created ({@code k} is the name of a {@link JIPipeProgressNodeKind}).
     *
     * @param id  node id
     * @param p   parent node id ({@code 0} = root)
     * @param l   node label
     * @param k   node kind name
     * @param loc optional location URI ({@code node://<uuid>} today; free-form for future schemes; {@code null} = no location)
     * @param ts  epoch millis
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Node(@JsonProperty("id") int id, @JsonProperty("p") int p, @JsonProperty("l") String l, @JsonProperty("k") String k, @JsonProperty("loc") @JsonInclude(JsonInclude.Include.NON_NULL) String loc, @JsonProperty("ts") long ts) implements JIPipeProgressRecord {

        @Override
        public String type() {
            return "n";
        }
    }

    /**
     * Node finished ({@code s} is one of {@code ok|fail|cancel}).
     *
     * @param id node id
     * @param s  finish state
     * @param ts epoch millis
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record NodeFinished(@JsonProperty("id") int id, @JsonProperty("s") String s, @JsonProperty("ts") long ts) implements JIPipeProgressRecord {

        @Override
        public String type() {
            return "N";
        }
    }

    /**
     * Log message attached to a node ({@code lv} is a {@link JIPipeLogLevel#value()}).
     *
     * @param id node id
     * @param lv numeric log level
     * @param m  message
     * @param ts epoch millis
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Log(@JsonProperty("id") int id, @JsonProperty("lv") int lv, @JsonProperty("m") String m, @JsonProperty("ts") long ts) implements JIPipeProgressRecord {

        @Override
        public String type() {
            return "l";
        }
    }

    /**
     * Progress state attached to a node ({@code mx} negative = indeterminate).
     *
     * @param id node id
     * @param v  progress value
     * @param mx progress maximum
     * @param ts epoch millis
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Progress(@JsonProperty("id") int id, @JsonProperty("v") int v, @JsonProperty("mx") int mx, @JsonProperty("ts") long ts) implements JIPipeProgressRecord {

        @Override
        public String type() {
            return "p";
        }
    }

    /**
     * Error attached to a node.
     *
     * @param id node id
     * @param c  exception class name
     * @param m  exception message
     * @param st stack trace (optional)
     * @param ts epoch millis
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Error(@JsonProperty("id") int id, @JsonProperty("c") String c, @JsonProperty("m") String m, @JsonProperty("st") String st, @JsonProperty("ts") long ts) implements JIPipeProgressRecord {

        @Override
        public String type() {
            return "e";
        }
    }

    /**
     * Run status change ({@code s} is one of {@code running|finished|failed|cancelled}).
     *
     * @param s  run status
     * @param ts epoch millis
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Status(@JsonProperty("s") String s, @JsonProperty("ts") long ts) implements JIPipeProgressRecord {

        @Override
        public String type() {
            return "s";
        }
    }

    /**
     * User-facing notification.
     *
     * @param h heading
     * @param d description
     * @param ts epoch millis
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Notification(@JsonProperty("h") String h, @JsonProperty("d") String d, @JsonProperty("ts") long ts) implements JIPipeProgressRecord {

        @Override
        public String type() {
            return "w";
        }
    }

    /**
     * Creates a {@link Head} record (file header, always line 1).
     *
     * @param v     schema version ({@code 1})
     * @param run   run UUID
     * @param label run label
     * @param app   JIPipe version
     * @param ts    epoch millis
     * @return the record
     */
    static JIPipeProgressRecord head(int v, String run, String label, String app, long ts) {
        return new Head(v, run, label, app, ts);
    }

    /**
     * Creates a {@link Node} record.
     *
     * @param id node id
     * @param p  parent node id ({@code 0} = root)
     * @param l  node label
     * @param k  node kind name ({@link JIPipeProgressNodeKind} name)
     * @param ts epoch millis
     * @return the record
     */
    static JIPipeProgressRecord node(int id, int p, String l, String k, long ts) {
        return new Node(id, p, l, k, null, ts);
    }

    /**
     * Creates a {@link Node} record with an optional location URI.
     *
     * @param id  node id
     * @param p   parent node id ({@code 0} = root)
     * @param l   node label
     * @param k   node kind name ({@link JIPipeProgressNodeKind} name)
     * @param loc the location URI (e.g. {@code node://<uuid>}), or {@code null} for no location
     * @param ts  epoch millis
     * @return the record
     */
    static JIPipeProgressRecord node(int id, int p, String l, String k, String loc, long ts) {
        return new Node(id, p, l, k, loc, ts);
    }

    /**
     * Creates a {@link NodeFinished} record.
     *
     * @param id node id
     * @param s  finish state ({@code ok|fail|cancel})
     * @param ts epoch millis
     * @return the record
     */
    static JIPipeProgressRecord nodeFinished(int id, String s, long ts) {
        return new NodeFinished(id, s, ts);
    }

    /**
     * Creates a {@link Log} record.
     *
     * @param id node id
     * @param lv numeric log level ({@link JIPipeLogLevel#value()})
     * @param m  message
     * @param ts epoch millis
     * @return the record
     */
    static JIPipeProgressRecord log(int id, int lv, String m, long ts) {
        return new Log(id, lv, m, ts);
    }

    /**
     * Creates a {@link Progress} record.
     *
     * @param id node id
     * @param v  progress value
     * @param mx progress maximum ({@code mx < 0} = indeterminate)
     * @param ts epoch millis
     * @return the record
     */
    static JIPipeProgressRecord progress(int id, int v, int mx, long ts) {
        return new Progress(id, v, mx, ts);
    }

    /**
     * Creates an {@link Error} record.
     *
     * @param id node id
     * @param c  exception class name
     * @param m  exception message
     * @param st stack trace (optional)
     * @param ts epoch millis
     * @return the record
     */
    static JIPipeProgressRecord error(int id, String c, String m, String st, long ts) {
        return new Error(id, c, m, st, ts);
    }

    /**
     * Creates a {@link Status} record.
     *
     * @param s  run status ({@code running|finished|failed|cancelled})
     * @param ts epoch millis
     * @return the record
     */
    static JIPipeProgressRecord status(String s, long ts) {
        return new Status(s, ts);
    }

    /**
     * Creates a {@link Notification} record.
     *
     * @param h  heading
     * @param d  description
     * @param ts epoch millis
     * @return the record
     */
    static JIPipeProgressRecord notification(String h, String d, long ts) {
        return new Notification(h, d, ts);
    }
}
