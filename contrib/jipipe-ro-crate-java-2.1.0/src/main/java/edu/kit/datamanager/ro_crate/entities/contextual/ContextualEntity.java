package edu.kit.datamanager.ro_crate.entities.contextual;

import edu.kit.datamanager.ro_crate.entities.AbstractEntity;

/**
 * This class represents the base class of the contextual entities.
 *
 * @author Nikola Tzotchev on 5.2.2022 г.
 * @version 1
 */
public class ContextualEntity extends AbstractEntity {

  public ContextualEntity(AbstractContextualEntityBuilder<?> entityBuilder) {
    super(entityBuilder);
  }

  abstract static class AbstractContextualEntityBuilder
      <T extends AbstractContextualEntityBuilder<T>> extends AbstractEntityBuilder<T> {

    @Override
    public abstract ContextualEntity build();
  }

  /**
   * This is the Contextual entity builder base class,
   * as of right now it does not contain any methods.
   */
  public static final class ContextualEntityBuilder extends
      AbstractContextualEntityBuilder<ContextualEntityBuilder> {

    @Override
    public ContextualEntityBuilder self() {
      return this;
    }

    @Override
    public ContextualEntity build() {
      return new ContextualEntity(this);
    }
  }
}
