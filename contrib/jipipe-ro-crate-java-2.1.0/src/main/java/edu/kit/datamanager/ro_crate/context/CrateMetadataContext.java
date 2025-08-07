package edu.kit.datamanager.ro_crate.context;

import com.fasterxml.jackson.databind.node.ObjectNode;

import edu.kit.datamanager.ro_crate.entities.AbstractEntity;

import java.util.Map;
import java.util.Set;

/**
 * Interface for the metadata context.
 * Most often in an ROCrate this is the default context,
 * but allowed also additional entries can be added.
 *
 * @author Nikola Tzotchev on 6.2.2022 г.
 * @version 1
 */
public interface CrateMetadataContext {

  ObjectNode getContextJsonEntity();

  boolean checkEntity(AbstractEntity entity);

  void addToContextFromUrl(String url);

  void addToContext(String key, String value);

  /**
   * Get the value of a key from the context.
   * @param key the key to be searched
   * @return the value of the key, null if not found
   */
  String getValueOf(String key);

  /**
   * Get an immutable collection of the keys in the metadata context.
   * @return the keys in the metadata context
   */
  Set<String> getKeys();

  /**
  * Get an immutable map of the context.
  * @return an immutable map containing the context key-value pairs
  */
  Map<String, String> getPairs();

  void deleteValuePairFromContext(String key);

  void deleteUrlFromContext(String url);
}
