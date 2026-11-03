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

package org.hkijena.jipipe.api.settings;

import org.hkijena.jipipe.api.settings.SettingsFileStore.FileFingerprint;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Public handle to a running {@link SettingsFileWatcher} for callers outside
 * the {@code org.hkijena.jipipe.api.settings} package (the settings service
 * component wires one per file-backed scope). The watcher itself stays
 * package-private; this handle exposes only the three operations a store
 * owner needs: observing foreign changes, recording own writes
 * (self-write suppression), and stopping the watch.
 * <p>
 * See {@link SettingsFileWatcher} for the detection contract, the callback
 * threading constraint (the consumer runs under the watcher's instance
 * monitor and must be a cheap hand-off), and the known own-fingerprint
 * suppression window that the store owner must close with a post-save
 * read-back verification.
 */
public final class SettingsFileWatching implements AutoCloseable {
    private final SettingsFileWatcher watcher;

    private SettingsFileWatching(SettingsFileWatcher watcher) {
        this.watcher = watcher;
    }

    /**
     * Starts watching the given settings file with the default poll
     * interval. The current file state is recorded as this owner's own
     * fingerprint (and as the watcher's baseline), so the pre-existing
     * content is not reported as a foreign change. In a headless JVM the
     * watch is inert (a no-op), matching {@link SettingsFileWatcher#start()}.
     *
     * @param file the settings file to watch
     * @param onForeignChange invoked (on the watcher's worker thread, under
     *                        the watcher's instance monitor) when the file
     *                        changes on disk due to a write this owner did
     *                        not perform; it must be a cheap hand-off
     * @return the handle for the running watch
     */
    public static SettingsFileWatching start(Path file, Consumer<FileFingerprint> onForeignChange) {
        SettingsFileWatcher watcher = new SettingsFileWatcher(file, onForeignChange);
        FileFingerprint current = null;
        try {
            current = FileFingerprint.of(file);
        } catch (IOException ignored) {
            // an unreadable or absent file has no fingerprint to record;
            // the watcher's baseline handles that case
        }
        if (current != null) {
            watcher.setOwnFingerprint(current);
        }
        watcher.start();
        return new SettingsFileWatching(watcher);
    }

    /**
     * Records the fingerprint this owner just wrote to the file, so the
     * watcher suppresses the corresponding change (self-write suppression).
     *
     * @param ownFingerprint the file's fingerprint after this owner's last
     *                       save, or {@code null} to clear the record
     */
    public void setOwnFingerprint(FileFingerprint ownFingerprint) {
        watcher.setOwnFingerprint(ownFingerprint);
    }

    /**
     * Stops the watch. Idempotent.
     */
    @Override
    public void close() {
        watcher.close();
    }
}
