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

/**
 * The scopes that settings can live in.
 * <p>
 * The three scopes are separated by who owns the value:
 * {@link #APPLICATION} holds user-interface and user-preference state,
 * stored per user in the profile; {@link #PROJECT} holds per-project
 * settings, stored inside the project file; {@link #RUNTIME} holds
 * execution state (threads, environments, temp/cache dirs, acceleration),
 * stored per machine in the profile.
 * Each setting key is assigned to exactly one scope.
 */
public enum SettingsScope {
    APPLICATION,
    PROJECT,
    RUNTIME
}
