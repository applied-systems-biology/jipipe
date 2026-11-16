package org.hkijena.jipipe.api.service.components;

import org.hkijena.jipipe.api.automation.AutomationOperationRegistry;
import org.hkijena.jipipe.api.automation.permissions.AutomationPermissionService;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.api.service.JIPipeServiceComponent;

/**
 * Service component that holds the automation operation registry and the shared automation
 * permission service.
 * <p>
 * The {@link AutomationPermissionService} is held here (not as a static singleton) so every
 * consumer reaches the same instance through the service lifecycle: the instance endpoint,
 * the MCP lifecycle in the GUI launcher, and the automation permission UI.
 */
public class JIPipeAutomationServiceComponent extends JIPipeServiceComponent {
    private final AutomationOperationRegistry operationRegistry = new AutomationOperationRegistry();
    private final AutomationPermissionService permissionService = new AutomationPermissionService();

    public JIPipeAutomationServiceComponent(JIPipeService service) {
        super(service);
    }

    public AutomationOperationRegistry getOperationRegistry() {
        return operationRegistry;
    }

    /**
     * The shared permission service (user states, CLI overrides, grants).
     */
    public AutomationPermissionService getPermissionService() {
        return permissionService;
    }
}
