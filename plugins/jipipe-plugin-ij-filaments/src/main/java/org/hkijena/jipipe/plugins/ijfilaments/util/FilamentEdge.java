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

package org.hkijena.jipipe.plugins.ijfilaments.util;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@JsonSerialize
public class FilamentEdge {

    private UUID uuid = UUID.randomUUID();

    private Color color = new Color(0x3584E4);

    private Map<String, String> metadata = new HashMap<>();

    public FilamentEdge() {

    }

    public FilamentEdge(FilamentEdge other) {
        this.color = other.color;
        this.metadata = new HashMap<>(other.metadata);
    }

    @JsonGetter("metadata")
    public Map<String, String> getMetadata() {
        return metadata;
    }

    @JsonSetter("metadata")
    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    @JsonGetter("uuid")
    public UUID getUuid() {
        return uuid;
    }

    @JsonSetter("uuid")
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    @JsonGetter("color")
    public Color getColor() {
        return color;
    }

    @JsonSetter("color")
    public void setColor(Color color) {
        this.color = color;
    }


    public void setMetadata(String key, Object value) {
        metadata.put(key, StringUtils.nullToEmpty(value));
    }
}
