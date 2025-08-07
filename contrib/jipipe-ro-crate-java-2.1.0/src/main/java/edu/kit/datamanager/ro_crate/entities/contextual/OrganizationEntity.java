package edu.kit.datamanager.ro_crate.entities.contextual;

/**
 * A helping class for creating organization entities.
 *
 * @author Nikola Tzotchev on 5.2.2022 г.
 * @version 1
 */
public class OrganizationEntity extends ContextualEntity {

  private static final String TYPE = "Organization";

  /**
   * Constructor for creating an organization entity from a builder.
   *
   * @param entityBuilder the builder from which to create the entity.
   */
  public OrganizationEntity(AbstractOrganizationEntityBuilder<?> entityBuilder) {
    super(entityBuilder);
    this.addProperty("address", entityBuilder.address);
    this.addProperty("email", entityBuilder.email);
    this.addProperty("telephone", entityBuilder.telephone);
    this.addIdProperty("location", entityBuilder.locationId);
    this.addType(TYPE);
  }

  abstract static class AbstractOrganizationEntityBuilder<T
      extends AbstractOrganizationEntityBuilder<T>>
      extends AbstractContextualEntityBuilder<T> {

    String address;
    String email;
    String telephone;
    String locationId;

    public T setAddress(String address) {
      this.address = address;
      return self();
    }

    public T setEmail(String email) {
      this.email = email;
      return self();
    }

    public T setTelephone(String telephone) {
      this.telephone = telephone;
      return self();
    }

    public T setLocationId(String placeId) {
      this.locationId = placeId;
      return self();
    }

    @Override
    public abstract OrganizationEntity build();
  }

  /**
   * A builder for the organization entity.
   */
  public static final class OrganizationEntityBuilder extends
      AbstractOrganizationEntityBuilder<OrganizationEntityBuilder> {

    @Override
    public OrganizationEntityBuilder self() {
      return this;
    }

    @Override
    public OrganizationEntity build() {
      return new OrganizationEntity(this);
    }
  }
}
