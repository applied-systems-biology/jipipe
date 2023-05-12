package org.hkijena.jipipe.extensions.nodetemplate;

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
