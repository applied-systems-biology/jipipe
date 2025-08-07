package edu.kit.datamanager.ro_crate.validation;

import edu.kit.datamanager.ro_crate.Crate;

/**
 * The validator class that provides method for validating a crate.
 * This class uses a strategy pattern to provide different validation options.
 */
public class Validator {

  private ValidatorStrategy strategy;

  public Validator(ValidatorStrategy strategy) {
    this.strategy = strategy;
  }

  public boolean validate(Crate crate) {
    return this.strategy.validate(crate);
  }
}
