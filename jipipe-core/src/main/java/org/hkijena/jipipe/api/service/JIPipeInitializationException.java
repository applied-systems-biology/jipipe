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

package org.hkijena.jipipe.api.service;

/**
 * A fatal failure during JIPipe initialization.
 * <p>
 * Thrown when continuing would corrupt or silently lose user data — most
 * importantly when the one-time settings migration fails after legacy files
 * may already have been moved into the profile's
 * {@code settings-migration-backup/} directory: a later save would write a
 * new-format {@code settings.json} from the (empty or partial) settings
 * document, after which the migration would never be retried and the user's
 * values would only exist in the backup.
 * <p>
 * Starting up must therefore stop <em>loudly</em>: {@link JIPipeService#ensureInitialized()}
 * puts the service into {@link JIPipeServiceState#Error} and logs the
 * exception, so the failure is visible instead of a silently emptied profile.
 * The profile itself is left as it was found (the legacy files are restored
 * where possible, and preserved in the backup otherwise), so the next start
 * retries the migration.
 */
public class JIPipeInitializationException extends RuntimeException {

    /**
     * @param message what failed and what the user should do
     */
    public JIPipeInitializationException(String message) {
        super(message);
    }

    /**
     * @param message what failed and what the user should do
     * @param cause the underlying error
     */
    public JIPipeInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}