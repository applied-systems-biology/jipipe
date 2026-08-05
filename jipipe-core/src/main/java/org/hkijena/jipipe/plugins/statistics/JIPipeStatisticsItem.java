package org.hkijena.jipipe.plugins.statistics;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.api.service.components.JIPipeStatisticsServiceComponent;

public interface JIPipeStatisticsItem {
    String getId();

    String getName();

    String getDescription();

    StatisticsPrivacyLevel getRequiredPrivacyLevel();

    JsonNode serialize();

    void deserialize(JsonNode node);

    void reset();

    default void initialize(JIPipeStatisticsServiceComponent service) {
    }

    default boolean isTimeTracked() {
        return false;
    }
}
