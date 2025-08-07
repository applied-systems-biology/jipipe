package edu.kit.datamanager.ro_crate.entities.validation;


import com.fasterxml.jackson.databind.JsonNode;

/**
 * Class used for validation of entities before their creation.
 */
public class EntityValidation {

  private final EntityValidationStrategy strategy;

  public EntityValidation(EntityValidationStrategy strategy) {
    this.strategy = strategy;
  }

  public boolean entityValidation(JsonNode entity) {
    return strategy.validateEntity(entity);
  }

  public boolean fieldValidation(JsonNode entity) {
    return strategy.validateFieldOfEntity(entity);
  }
}
