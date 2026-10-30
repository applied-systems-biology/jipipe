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

import java.util.function.Predicate;

/**
 * Decides whether a setting may be written in this process.
 * <p>
 * The settings service component is the owner and installer of the guard (see
 * {@code JIPipeSettingsServiceComponent#setWriteGuard}); it injects it into
 * every {@link SettingsBinding} (at bind time) and every
 * {@link SettingsSheetBridge} (at attach time), and both query it
 * <em>before</em> they mutate the document. A denied write throws
 * {@link SettingsAccessDeniedException} and leaves the document untouched.
 * <p>
 * The predicate semantics are "may write": {@code test(key) == true} permits
 * the write, {@code false} rejects it.
 */
@FunctionalInterface
public interface SettingsWriteGuard {
    /** A guard that permits every write (the default while no remote runtime owns a document). */
    SettingsWriteGuard ALLOW_ALL = key -> true;

    /**
     * @param key the setting about to be written
     * @return whether the write is permitted
     */
    boolean test(JIPipeSettingKey<?> key);

    /**
     * Adapts a predicate to a guard.
     *
     * @param predicate the predicate, or {@code null} for {@link #ALLOW_ALL}
     * @return the guard
     */
    static SettingsWriteGuard of(Predicate<JIPipeSettingKey<?>> predicate) {
        return predicate == null ? ALLOW_ALL : predicate::test;
    }
}