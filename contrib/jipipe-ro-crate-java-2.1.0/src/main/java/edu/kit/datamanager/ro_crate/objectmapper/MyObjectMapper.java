package edu.kit.datamanager.ro_crate.objectmapper;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Singleton pattern for the object mapper used everywhere in the library.
 *
 * @author Nikola Tzotchev on 4.2.2022 г.
 * @version 1
 */
public class MyObjectMapper {

  private static final ObjectMapper mapper = new ObjectMapper()
      .enable(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
      .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
      .enable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS);

  private MyObjectMapper() {

  }

  public static ObjectMapper getMapper() {
    return mapper;
  }
}
