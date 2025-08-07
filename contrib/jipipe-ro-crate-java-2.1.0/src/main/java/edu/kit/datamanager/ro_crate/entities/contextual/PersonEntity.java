package edu.kit.datamanager.ro_crate.entities.contextual;

/**
 * A helping class for creating a Person Entity.
 *
 * @author Nikola Tzotchev on 5.2.2022 г.
 * @version 1
 */
public class PersonEntity extends ContextualEntity {

  private static final String TYPE = "Person";

  /**
   * Constructor for creating the Person entity from a builder.
   *
   * @param entityBuilder the builder from which to create the entity.
   */
  public PersonEntity(AbstractPersonEntityBuilder<?> entityBuilder) {
    super(entityBuilder);
    this.addIdProperty("affiliation", entityBuilder.affiliation);
    this.addIdProperty("contactPoint", entityBuilder.contactPoint);
    this.addProperty("email", entityBuilder.email);
    this.addProperty("givenName", entityBuilder.givenName);
    this.addProperty("familyName", entityBuilder.familyName);
    this.addType(TYPE);
  }

  abstract static class AbstractPersonEntityBuilder
      <T extends AbstractPersonEntityBuilder<T>>
      extends AbstractContextualEntityBuilder<T> {

    String affiliation;
    String contactPoint;
    String email;
    String givenName;
    String familyName;

    public T setAffiliation(String organisation) {
      this.affiliation = organisation;
      return self();
    }

    public T setContactPoint(String contactPoint) {
      this.contactPoint = contactPoint;
      return self();
    }

    public T setEmail(String email) {
      this.email = email;
      return self();
    }

    public T setGivenName(String name) {
      this.givenName = name;
      return self();
    }

    public T setFamilyName(String familyName) {
      this.familyName = familyName;
      return self();
    }

    @Override
    public abstract PersonEntity build();
  }

  /**
   * Person Entity builder for easier creation of Person entities.
   */
  public static final class PersonEntityBuilder
      extends AbstractPersonEntityBuilder<PersonEntityBuilder> {

    @Override
    public PersonEntityBuilder self() {
      return this;
    }

    @Override
    public PersonEntity build() {
      return new PersonEntity(this);
    }
  }
}
