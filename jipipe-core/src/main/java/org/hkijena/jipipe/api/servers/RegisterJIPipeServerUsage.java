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

import java.lang.annotation.*;

/**
 * Annotates a {@link org.hkijena.jipipe.api.nodes.JIPipeGraphNode} as using a specific
 * server instance type. This enables the framework to:
 * <ul>
 *     <li>Track which algorithms depend on which server types</li>
 *     <li>Automatically resolve and acquire server instances via {@code getServerInstance()}</li>
 *     <li>Provide appropriate UI for server configuration</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(RegisterJIPipeServerUsages.class)
public @interface RegisterJIPipeServerUsage {
    Class<? extends JIPipeServerInstance<?>> value();
}
