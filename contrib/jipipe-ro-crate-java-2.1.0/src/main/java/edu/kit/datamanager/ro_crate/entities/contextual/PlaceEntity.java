package edu.kit.datamanager.ro_crate.entities.contextual;

/**
 * A helping class for the Place entity creation.
 *
 * @author Nikola Tzotchev on 5.2.2022 г.
 * @version 1
 */
public class PlaceEntity extends ContextualEntity {

  private static final String TYPE = "Place";

  /**
   * Constructor for creating a place entity from a builder.
   *
   * @param entityBuilder the builder from which to create the entity.
   */
  public PlaceEntity(AbstractPlaceEntityBuilder<?> entityBuilder) {
    super(entityBuilder);
    this.addIdProperty("geo", entityBuilder.geo);
    this.addType(TYPE);
  }

  abstract static class AbstractPlaceEntityBuilder<T extends AbstractPlaceEntityBuilder<T>> extends
      AbstractContextualEntityBuilder<T> {

    String geo;

    // that is the geolocation property
    public T setGeo(String geo) {
      this.geo = geo;
      return self();
    }

    public T setGeo(ContextualEntity geo) {
      this.geo = geo.getId();
      return self();
    }

    public abstract PlaceEntity build();
  }

  /**
   * A builder class for the easier Place entity construction.
   */
  public static final class PlaceEntityBuilder
      extends AbstractPlaceEntityBuilder<PlaceEntityBuilder> {

    @Override
    public PlaceEntityBuilder self() {
      return this;
    }

    @Override
    public PlaceEntity build() {
      return new PlaceEntity(this);
    }
  }
}
