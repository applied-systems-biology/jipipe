package org.hkijena.jipipe.api.registries;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.hkijena.jipipe.JIPipe;

import java.util.HashSet;
import java.util.Set;

/**
 * A registry of additional utilities (e.g., installers for external environments)
 */
public class JIPipeUtilityRegistry {
    private final JIPipe jiPipe;
    private final Multimap<Class<?>, Class<?>> registeredItems = HashMultimap.create();

    public JIPipeUtilityRegistry(JIPipe jiPipe) {

        this.jiPipe = jiPipe;
    }

    public JIPipe getJIPipe() {
        return jiPipe;
    }

    public Set<Class<?>> getUtilitiesFor(Class<?> categoryClass) {
        return new HashSet<>(registeredItems.get(categoryClass));
    }

    /**
     * Registers a new utility associated to the category
     *
     * @param categoryClass the category
     * @param utilityClass  the utility
     */
    public void register(Class<?> categoryClass, Class<?> utilityClass) {
        registeredItems.put(categoryClass, utilityClass);
        getJIPipe().getProgressInfo().log("Registered utility " + utilityClass + " of type " + categoryClass);
    }

    /**
     * Returns a mutable map of all items
     *
     * @return map from category to utilities
     */
    public Multimap<Class<?>, Class<?>> getRegisteredItems() {
        return registeredItems;
    }
}
