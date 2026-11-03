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

package org.hkijena.jipipe.desktop.app.settings;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.api.service.components.JIPipeSettingsServiceComponent.ChangeOrigin;
import org.hkijena.jipipe.api.service.components.JIPipeSettingsServiceComponent.SettingsChangedEvent;
import org.hkijena.jipipe.api.settings.SettingsScope;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Pure (non-Swing) since-open audit of settings changes (modern settings
 * system, Task 21). The settings tab records every
 * {@link SettingsChangedEvent} it receives here; the changes strip renders the
 * entries of the active scope.
 * <p>
 * <b>Thread safety.</b> Change events are dispatched on whatever thread mutated
 * the settings document — the EDT for UI writes, the file-watcher thread for
 * external merges, whichever thread runs a merge. Every mutator and reader is
 * {@code synchronized} on this instance, and {@link #entries()} returns a
 * defensive copy, so the strip can read the log on the EDT while another
 * thread records.
 * <p>
 * <b>Scope.</b> The log records all scopes and does not filter by scope; the
 * strip filters by the active scope at display time.
 * <p>
 * <b>Bounded.</b> The log holds at most {@link #MAX_ENTRIES} entries: recording
 * beyond that drops the <em>oldest</em> entry, so a pathological stream of
 * changes cannot grow the log without bound.
 */
public final class SettingsChangeLog {

    /**
     * The maximum number of retained entries; recording beyond it drops the
     * oldest entry.
     */
    public static final int MAX_ENTRIES = 200;

    private final List<ChangeEntry> entries = new ArrayList<>();

    /**
     * @param origin a change origin
     * @return whether the origin is a change caused by another process: a
     * re-read of the file ({@link ChangeOrigin#EXTERNAL_FILE}) or a write by
     * another instance ({@link ChangeOrigin#EXTERNAL_PROCESS}). Exactly these
     * two origins count as foreign; a carry-over ({@link ChangeOrigin#CARRY_OVER})
     * is not foreign.
     */
    public static boolean isForeignOrigin(ChangeOrigin origin) {
        return origin == ChangeOrigin.EXTERNAL_FILE || origin == ChangeOrigin.EXTERNAL_PROCESS;
    }

    /**
     * Records one change.
     * <p>
     * <b>One source per side.</b> The two sides of a change are rendered by
     * exactly one place each: the <em>new</em> value is rendered by the caller
     * and passed in already formatted ({@code formattedValue}), the <em>old</em>
     * value is always rendered here from
     * {@link SettingsChangedEvent#getOldValue()} (the event carries the raw old
     * node). A caller must therefore not re-render or re-format the old value,
     * and the log never re-formats a non-null {@code formattedValue}.
     *
     * @param event          the change event
     * @param formattedValue the new value as rendered by the caller (an empty
     *                       string means the location was removed). It is only
     *                       normalized (null → empty), not re-rendered; the
     *                       caller is expected to cut it to the raw editor's
     *                       preview length via
     *                       {@link RawSettingsTableModel#truncate(String)} /
     *                       {@link RawSettingsTableModel#render(com.fasterxml.jackson.databind.JsonNode)}
     */
    public synchronized void record(SettingsChangedEvent event, String formattedValue) {
        Objects.requireNonNull(event, "event");
        if (entries.size() >= MAX_ENTRIES) {
            entries.remove(0);
        }
        entries.add(new ChangeEntry(event.getScope(),
                event.getPath(),
                format(event.getOldValue()),
                format(formattedValue),
                event.getOrigin()));
    }

    /**
     * @return a copy of the recorded entries, oldest first; modifying it does
     * not affect the log
     */
    public synchronized List<ChangeEntry> entries() {
        return new ArrayList<>(entries);
    }

    /**
     * Forgets every entry of the given scope that was caused by another
     * process (see {@link #isForeignOrigin(ChangeOrigin)}). Called when the
     * live-update banner is dismissed, so acknowledged foreign changes no longer
     * appear in the changes strip. Local changes and other scopes are kept.
     *
     * @param scope the scope whose foreign entries are acknowledged
     */
    public synchronized void acknowledgeForeign(SettingsScope scope) {
        entries.removeIf(entry -> entry.scope() == scope && entry.isForeign());
    }

    /**
     * Forgets every entry.
     */
    public synchronized void clear() {
        entries.clear();
    }

    /**
     * @return the number of recorded entries
     */
    public synchronized int size() {
        return entries.size();
    }

    private static String format(String value) {
        return value == null ? "" : value;
    }

    private static String format(JsonNode value) {
        if (value == null || value.isNull() || value.isMissingNode()) {
            return "";
        }
        return RawSettingsTableModel.truncate(RawSettingsTableModel.render(value));
    }

    /**
     * One recorded change.
     *
     * @param scope         the scope the change belongs to
     * @param path          the normalized {@code "<blockPath>/<subPath>"} that
     *                      changed (a bare block path for whole-block changes)
     * @param oldValue      the formatted old value (empty when there was none)
     * @param newValue      the formatted new value (empty when the location was
     *                      removed)
     * @param origin        the origin of the change
     */
    public record ChangeEntry(SettingsScope scope,
                              String path,
                              String oldValue,
                              String newValue,
                              ChangeOrigin origin) {
        public ChangeEntry {
            Objects.requireNonNull(scope, "scope");
            Objects.requireNonNull(path, "path");
            Objects.requireNonNull(origin, "origin");
            oldValue = oldValue == null ? "" : oldValue;
            newValue = newValue == null ? "" : newValue;
        }

        /**
         * @return whether the change was caused by another process (see
         * {@link #isForeignOrigin(ChangeOrigin)})
         */
        public boolean isForeign() {
            return isForeignOrigin(origin);
        }

        /**
         * @return the entry as shown in the changes strip:
         * {@code "<path>: <old> \u2192 <new>"}, or just the new value when there
         * is no old value
         */
        public String displayText() {
            if (oldValue.isEmpty()) {
                return path + ": " + newValue;
            }
            return path + ": " + oldValue + RawSettingsTableModel.PENDING_DEFAULT_SEPARATOR + newValue;
        }
    }
}