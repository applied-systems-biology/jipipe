package org.hkijena.jipipe.extensions.omero;

import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.extensions.parameters.api.optional.OptionalParameter;

public class OptionalOMEROCredentialsEnvironment extends OptionalParameter<OMEROCredentialsEnvironment> {
    public OptionalOMEROCredentialsEnvironment() {
        super(OMEROCredentialsEnvironment.class);
    }

    public OptionalOMEROCredentialsEnvironment(OptionalParameter<OMEROCredentialsEnvironment> other) {
        super(other);
        this.setContent(new OMEROCredentialsEnvironment(other.getContent()));
    }

    @Override
    public OMEROCredentialsEnvironment getContentOrDefault(OMEROCredentialsEnvironment defaultValue) {
        if(getContent() != null && getContent().generateValidityReport(new UnspecifiedValidationReportContext()).isValid()) {
            return getContent();
        }
        else {
            return defaultValue;
        }
    }
}
