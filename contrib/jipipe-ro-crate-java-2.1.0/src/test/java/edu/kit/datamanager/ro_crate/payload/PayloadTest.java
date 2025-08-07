package edu.kit.datamanager.ro_crate.payload;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.kit.datamanager.ro_crate.entities.AbstractEntity;
import edu.kit.datamanager.ro_crate.entities.contextual.ContextualEntity;
import edu.kit.datamanager.ro_crate.entities.contextual.PersonEntity;
import edu.kit.datamanager.ro_crate.entities.data.DataEntity;
import edu.kit.datamanager.ro_crate.entities.data.DataSetEntity;

import java.util.Collection;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

public class PayloadTest {

  private RoCratePayload payload;

  @BeforeEach
  void setPayload() {
    this.payload = new RoCratePayload();
  }

  @Test
  void testAddContextualEntity() {
    var contextualEntity = new PersonEntity.PersonEntityBuilder()
        .setGivenName("petko")
        .setFamilyName("samo")
        .setEmail("email.com")
        .build();
    this.payload.addContextualEntity(contextualEntity);

    var person= payload.getEntityById(contextualEntity.getId());
    assertEquals(person, contextualEntity);
    var personAgain = payload.getContextualEntityById(contextualEntity.getId());
    assertEquals(personAgain, contextualEntity);
  }

  @Test
  void testAddDataEntity() {
    var dataEntity = new DataEntity.DataEntityBuilder()
        .setId("https://www.example.com/entity")
        .addProperty("description", "yes")
        .build();

    this.payload.addDataEntity(dataEntity);

    var e= payload.getEntityById("https://www.example.com/entity");
    assertEquals(e, dataEntity);
    var element2 = payload.getDataEntityById("https://www.example.com/entity");
    assertEquals(element2, dataEntity);
  }


  @Test
  void addAnyEntity() {
    var dataEntity = new DataEntity.DataEntityBuilder()
        .setId("https://www.example.com/entity1")
        .addType("CreativeWork")
        .addProperty("description", "yes")
        .build();

    var contextualEntity= new PersonEntity.PersonEntityBuilder()
        .setId("https://www.example.com/entity2")
        .addProperty("description", "yes")
        .build();

    this.payload.addEntity(dataEntity);
    this.payload.addEntity(contextualEntity);

    var e = payload.getEntityById("https://www.example.com/entity1");
    assertEquals(e, dataEntity);
    assertTrue(e instanceof DataEntity);
    var context = payload.getEntityById("https://www.example.com/entity2");
    assertEquals(context, contextualEntity);
    assertTrue(context instanceof ContextualEntity);
  }

  @Test
  void testAddEntitiesFromCollection() {

    var contextualEntity1= new PersonEntity.PersonEntityBuilder()
        .setId("https://www.example.com/entity1")
        .addProperty("description", "yes")
        .build();
    var contextualEntity2= new PersonEntity.PersonEntityBuilder()
        .setId("https://www.example.com/entity2")
        .addProperty("description", "yes")
        .build();

    Collection<AbstractEntity> set = new HashSet<>();
    set.add(contextualEntity1);
    set.add(contextualEntity2);
    this.payload.addEntities(set);
    var en1 = this.payload.getEntityById("https://www.example.com/entity1");
    assertEquals(en1, contextualEntity1);
    var en2 = this.payload.getEntityById("https://www.example.com/entity2");
    assertEquals(en2, contextualEntity2);
  }

  @Test
  void addSameIdTest() {
    var dataEntity1 = new DataEntity.DataEntityBuilder()
        .setId("https://www.example.com/entity1")
        .addProperty("description", "yes")
        .build();

    var dataEntity2 = new DataEntity.DataEntityBuilder()
        .setId("https://www.example.com/entity1")
        .addProperty("description", "different")
        .build();

    this.payload.addDataEntity(dataEntity1);
    this.payload.addDataEntity(dataEntity2);

    assertEquals(1, this.payload.getAllEntities().size());
    var retrieve = this.payload.getEntityById("https://www.example.com/entity1");
    assertEquals(retrieve, dataEntity2);
  }
  @Test
  void removeEntity() {
    var dataEntity1 = new DataEntity.DataEntityBuilder()
        .setId("https://www.example.com/entity1")
        .addProperty("description", "yes")
        .build();

    var dataEntity2 = new DataEntity.DataEntityBuilder()
        .setId("https://www.example.com/entity2")
        .addProperty("description", "yes")
        .build();

    this.payload.addDataEntity(dataEntity1);
    this.payload.addDataEntity(dataEntity2);

    assertEquals(2, this.payload.getAllEntities().size());

    this.payload.removeEntityById("https://www.example.com/entity1");
    assertEquals(1, this.payload.getAllEntities().size());

    this.payload.removeEntityById("https://www.example.com/entity2");
    assertEquals(0, this.payload.getAllEntities().size());
  }

  @Test
  void testAssociatedItems() {
    var dataEntity1= new DataEntity.DataEntityBuilder()
        .setId("https://www.example.com/entity1")
        .addProperty("description", "yes")
        .build();

    this.payload.addDataEntity(dataEntity1);
    assertEquals(0, dataEntity1.getLinkedTo().size() );

    var entity2 = new DataSetEntity.DataSetBuilder()
        .setId("set")
        .addToHasPart(dataEntity1)
        .addProperty("description", "yes")
        .build();

    this.payload.addDataEntity(entity2);
    assertEquals(1, entity2.getLinkedTo().size());

    // we delete the first entity
    this.payload.removeEntityById("https://www.example.com/entity1");
    var hasTobeNull = this.payload.getEntityById("https://www.example.com/entity1");
    assertNull(hasTobeNull);
    var setWithoutHasPart = this.payload.getDataEntityById("set");
    // when the first entity is deleted its occurrence in hasPart has to removed as well
    assertNull(setWithoutHasPart.getProperty("hasPart"));
  }
}
