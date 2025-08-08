/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.contrib.cwl.cwl1_2.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SecondaryFilesDslLoader<T> implements Loader<T> {
  private final Loader<T> innerLoader;

  public SecondaryFilesDslLoader(final Loader<T> innerLoader) {
    this.innerLoader = innerLoader;
  }


  public T load(
      final Object doc_,
      final String baseUri,
      final LoadingOptions loadingOptions,
      final String docRoot) {
    Object doc = doc_;
    List<Map<String, Object>> r = new ArrayList<Map<String, Object>>();
    if (doc instanceof List) {
      final List<Object> docList = (List<Object>) doc;
      for (final Object d : docList) {
        Map<String, Object> entry = new HashMap<String, Object>();
        if (d instanceof String) {
	  String dString = (String) d;
	  if (dString.endsWith("?")) {
            entry.put("pattern", dString.substring(0, dString.length()-1));
	    entry.put("required", false);
	  } else {
            entry.put("pattern", dString);
	  }
	  r.add(entry);
	} else if (d instanceof Map) {
	  @SuppressWarnings("unchecked")
	  Map<String, Object> dMap = new HashMap<String, Object>((Map<String, Object>) d);
	  if (dMap.containsKey("pattern")) {
            entry.put("pattern", dMap.remove("pattern"));
	  } else {
            throw new ValidationException("Missing 'pattern' in secondaryFiles specification entry.");
	  }
	  if (dMap.containsKey("required")) {
            entry.put("required", dMap.remove("required"));
	  }
	  if (dMap.size() > 0) {
            throw new ValidationException("Unallowed values in secondaryFiles specification entry.");
	  }
	  r.add(entry);
	} else {
	  throw new ValidationException("Expected a string or sequence of (strings or mappings).");
	}
      }
    } else if (doc instanceof Map) {
      Map<String, Object> entry = new HashMap<String, Object>();
      @SuppressWarnings("unchecked")
      Map<String, Object> dMap = new HashMap<String, Object>((Map<String, Object>) doc);
      if (dMap.containsKey("pattern")) {
        entry.put("pattern", dMap.remove("pattern"));
      } else {
        throw new ValidationException("Missing 'pattern' in secondaryFiles specification entry.");
      }
      if (dMap.containsKey("required")) {
        entry.put("required", dMap.remove("required"));
      }
      if (dMap.size() > 0) {
        throw new ValidationException("Unallowed values in secondaryFiles specification entry.");
      }
      r.add(entry);
    } else if (doc instanceof String) {
	  String dString = (String) doc;
	  Map<String, Object> entry = new HashMap<String, Object>();
	  if (dString.endsWith("?")) {
        entry.put("pattern", dString.substring(0, dString.length()-1));
	    entry.put("required", false);
	  } else {
        entry.put("pattern", dString);
	  }
	  r.add(entry);
    } else {
        throw new ValidationException("Expected a string or sequence of (strings or mappings).");
    }
    return this.innerLoader.load(r, baseUri, loadingOptions, docRoot);
  }
}
