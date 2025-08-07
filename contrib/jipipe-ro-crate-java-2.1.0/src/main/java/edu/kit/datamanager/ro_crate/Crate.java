package edu.kit.datamanager.ro_crate;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import edu.kit.datamanager.ro_crate.context.CrateMetadataContext;
import edu.kit.datamanager.ro_crate.entities.AbstractEntity;
import edu.kit.datamanager.ro_crate.entities.contextual.ContextualEntity;
import edu.kit.datamanager.ro_crate.entities.data.DataEntity;
import edu.kit.datamanager.ro_crate.entities.data.RootDataEntity;
import edu.kit.datamanager.ro_crate.preview.CratePreview;
import edu.kit.datamanager.ro_crate.special.CrateVersion;

/**
 * An interface describing an ROCrate.
 *
 * @author Nikola Tzotchev on 6.2.2022 г.
 * @version 1
 */
public interface Crate {

  /**
   * Mark the crate as imported, i.e. it has been read from a file
   * or is for other reasons not considered a new crate.
   * <p>
   * This is useful mostly for readers to indicate this in case
   * the crate may not have any provenance information yet and
   * should still be recognized as an imported crate.
   *
   * @return this crate, for convenience.
   */
  Crate markAsImported();

  /**
   * Check if the crate is marked as imported.
   * <p>
   * If true, it indicates that the crate has been read from a file
   * or is for other reasons not considered a new crate.
   *
   * @return true if the crate is marked as imported, false otherwise.
   */
  boolean isImported();

  /**
   * Read version from the crate descriptor and return it as a class
   * representation.
   * <p>
   * NOTE: If there is no version in the crate, it does not comply with the
   * specification.
   * 
   * @return the class representation indication the version of this crate, if
   *         available.
   */
  Optional<CrateVersion> getVersion();

  /**
   * Returns strings indicating the conformance of a crate with other
   * specifications than the RO-Crate version.
   * <p>
   * If you need the crate version too, refer to {@link #getVersion()}.
   * <p>
   * This corresponds technically to all conformsTo values, excluding the RO crate
   * version / specification.
   * 
   * @return a collection of the profiles or specifications this crate conforms
   *         to.
   */
  Collection<String> getProfiles();

  CratePreview getPreview();

  void setMetadataContext(CrateMetadataContext metadataContext);

  /**
   * Get the value of a key from the metadata context.
   * @param key the key to be searched
   * @return the value of the key, null if not found
   */
  String getMetadataContextValueOf(String key);

  /**
   * Get an immutable collection of the keys in the metadata context.
   * @return the keys in the metadata context
   */
  Set<String> getMetadataContextKeys();

  /**
   * Get an immutable map of the context.
   * @return an immutable map containing the context key-value pairs
   */
  Map<String, String> getMetadataContextPairs();

  RootDataEntity getRootDataEntity();

  void setRootDataEntity(RootDataEntity rootDataEntity);

  ContextualEntity getJsonDescriptor();

  void setJsonDescriptor(ContextualEntity jsonDescriptor);

  String getJsonMetadata();

  DataEntity getDataEntityById(java.lang.String id);

  Set<DataEntity> getAllDataEntities();

  ContextualEntity getContextualEntityById(java.lang.String id);

  Set<ContextualEntity> getAllContextualEntities();

  AbstractEntity getEntityById(java.lang.String id);

  /**
   * Adds a data entity to the crate.
   *
   * @param entity the DataEntity to add to this crate.
   */
  void addDataEntity(DataEntity entity);

  void addContextualEntity(ContextualEntity entity);

  void deleteEntityById(String entityId);

  void setUntrackedFiles(Collection<File> files);

  void addFromCollection(Collection<? extends AbstractEntity> entities);

  void addItemFromDataCite(String locationUrl);

  void deleteValuePairFromContext(String key);

  void deleteUrlFromContext(String url);

  Collection<File> getUntrackedFiles();
}
