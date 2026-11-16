package org.hkijena.jipipe.api.service.components;

import org.hkijena.jipipe.api.automation.AutomationOperationRegistry;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.api.service.JIPipeServiceComponent;

/**
 * Service component that holds the automation operation registry.
 */
public class JIPipeAutomationServiceComponent extends JIPipeServiceComponent {
    private final AutomationOperationRegistry operationRegistry = new AutomationOperationRegistry();

    public JIPipeAutomationServiceComponent(JIPipeService service) {
        super(service);
    }

    public AutomationOperationRegistry getOperationRegistry() {
        return operationRegistry;
    }
}
