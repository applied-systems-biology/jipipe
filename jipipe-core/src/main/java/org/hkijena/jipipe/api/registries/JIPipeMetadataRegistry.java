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

package org.hkijena.jipipe.api.registries;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeMetadataObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A registry of {@link org.hkijena.jipipe.api.JIPipeMetadataObject} types
 */
public class JIPipeMetadataRegistry {
    private final JIPipe jiPipe;
    private final BiMap<String, Class<? extends JIPipeMetadataObject>> registeredItems = HashBiMap.create();
    private final Map<String, Class<? extends JIPipeMetadataObject>> alternativeTypeIds = new HashMap<>();

    public JIPipeMetadataRegistry(JIPipe jiPipe) {

        this.jiPipe = jiPipe;
    }

    public JIPipe getJIPipe() {
        return jiPipe;
    }

    public Class<? extends JIPipeMetadataObject> findById(String id) {
        Class<? extends JIPipeMetadataObject> aClass = registeredItems.get(id);
        if(aClass != null) {
            return aClass;
        }
        else {
            return alternativeTypeIds.getOrDefault(id, null);
        }
    }

    public String getId(Class<? extends JIPipeMetadataObject> klass) {
        return registeredItems.inverse().get(klass);
    }

    public void register(Class<? extends JIPipeMetadataObject> objectClass, String id, String... alternativeIds) {
        registeredItems.put(id, objectClass);
        jiPipe.getProgressInfo().log("Registered MO " + objectClass + " as " + id);
        for (String alternativeId : alternativeIds) {
            alternativeTypeIds.put(alternativeId, objectClass);
            jiPipe.getProgressInfo().log("Registered MO " + objectClass + " readable as " + alternativeId);
        }
    }

    public void registerAlternativeIds(Class<? extends JIPipeMetadataObject> objectClass, String... alternativeIds) {
        for (String alternativeId : alternativeIds) {
            alternativeTypeIds.put(alternativeId, objectClass);
            jiPipe.getProgressInfo().log("Registered MO " + objectClass + " readable as " + alternativeId);
        }
    }
}
