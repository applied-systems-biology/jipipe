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

package org.hkijena.jipipe.plugins.nodetemplate;

import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;

/**
 * This event should always be triggered into NodeTemplateSettings.getInstance().getEventBus()
 * (even if non-global). Will refresh the {@link NodeTemplateBox} instances.
 * Should be triggered if the user added any templates
 */
public class NodeTemplatesRefreshedEvent extends AbstractJIPipeEvent {
    public NodeTemplatesRefreshedEvent() {
        super(null);
    }
}
