package org.hkijena.jipipe.contrib.ro_crate.validation;

import org.hkijena.jipipe.contrib.ro_crate.Crate;

/**
 * Interface for the validation strategy.
 */
public interface ValidatorStrategy {
    boolean validate(Crate crate);
}
