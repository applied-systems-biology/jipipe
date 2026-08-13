package org.hkijena.jipipe.api.service.components;

import org.hkijena.jipipe.api.instrumentation.InstrumentationOperationRegistry;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.api.service.JIPipeServiceComponent;

/**
 * Service component that holds the instrumentation operation registry.
 */
public class JIPipeInstrumentationServiceComponent extends JIPipeServiceComponent {
    private final InstrumentationOperationRegistry operationRegistry = new InstrumentationOperationRegistry();

    public JIPipeInstrumentationServiceComponent(JIPipeService service) {
        super(service);
    }

    public InstrumentationOperationRegistry getOperationRegistry() {
        return operationRegistry;
    }
}
