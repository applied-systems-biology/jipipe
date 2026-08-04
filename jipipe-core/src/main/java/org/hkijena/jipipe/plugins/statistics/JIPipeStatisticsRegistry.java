package org.hkijena.jipipe.plugins.statistics;

import java.util.*;
import java.util.stream.Collectors;

public class JIPipeStatisticsRegistry {
    private final Map<String, JIPipeStatisticsItem> registeredItems = new LinkedHashMap<>();

    public void registerItem(JIPipeStatisticsItem item) {
        if (registeredItems.containsKey(item.getId())) {
            throw new IllegalArgumentException("Statistics item with ID '" + item.getId() + "' is already registered");
        }
        registeredItems.put(item.getId(), item);
    }

    public JIPipeStatisticsItem getItem(String id) {
        return registeredItems.get(id);
    }

    public List<JIPipeStatisticsItem> getItems() {
        return new ArrayList<>(registeredItems.values());
    }

    public List<JIPipeStatisticsItem> getItemsForLevel(StatisticsPrivacyLevel level) {
        return registeredItems.values().stream()
                .filter(item -> item.getRequiredPrivacyLevel().getLevel() <= level.getLevel())
                .collect(Collectors.toList());
    }

    public Map<JIPipeStatisticsItemCategory, List<JIPipeStatisticsItem>> getItemsByCategory() {
        return registeredItems.values().stream()
                .collect(Collectors.groupingBy(JIPipeStatisticsItem::getCategory, LinkedHashMap::new, Collectors.toList()));
    }
}
