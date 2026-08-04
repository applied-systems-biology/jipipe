package org.hkijena.jipipe.plugins.statistics;

import com.fasterxml.jackson.databind.JsonNode;

public interface JIPipeStatisticsItem {
    String getId();

    String getName();

    String getDescription();

    JIPipeStatisticsItemCategory getCategory();

    StatisticsPrivacyLevel getRequiredPrivacyLevel();

    JsonNode serialize();

    void deserialize(JsonNode node);

    void reset();
}
