package edu.kit.datamanager.ro_crate.payload;

/**
 * Implementation of the Observer pattern, used by the deletion of entities.
 */
public class EntityObserver implements Observer {

  private final RoCratePayload payload;

  public EntityObserver(RoCratePayload payload) {
    this.payload = payload;
  }

  @Override
  public void update(String entityId) {
    this.payload.addToAssociatedItems(this.payload.getEntityById(entityId));
  }
}
