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

package org.hkijena.jipipe.api.servers;

import org.hkijena.jipipe.api.environments.JIPipeEnvironment;

/**
 * Functional interface for creating server instances from environments.
 * 
 * <p>Each factory knows:</p>
 * <ul>
 *     <li>Which environment class it accepts</li>
 *     <li>How to create a {@link JIPipeServerInstance} from that environment</li>
 * </ul>
 *
 * @param <TEnv> the environment type
 * @param <TInst> the server instance type
 */
@FunctionalInterface
public interface ServerInstanceFactory<TEnv extends JIPipeEnvironment, TInst extends JIPipeServerInstance<TEnv>> {
    /**
     * Creates a new server instance from the given environment and port.
     *
     * @param environment the environment configuration
     * @param port        the port to use
     * @return a new server instance (not yet started)
     */
    TInst create(TEnv environment, int port);
}
