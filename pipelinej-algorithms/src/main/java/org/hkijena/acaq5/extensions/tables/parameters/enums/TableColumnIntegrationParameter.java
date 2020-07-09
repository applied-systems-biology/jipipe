/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.acaq5.extensions.tables.parameters.enums;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.html.HtmlEscapers;
import org.hkijena.acaq5.api.registries.ACAQTableRegistry;
import org.hkijena.acaq5.extensions.parameters.primitives.DynamicEnumParameter;
import org.hkijena.acaq5.extensions.tables.IntegratingColumnOperation;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Enum-like parameter that allows the selection of a {@link IntegratingColumnOperation}.
 * Contains ACAQTableRegistry.ColumnOperationEntry
 */
public class TableColumnIntegrationParameter extends DynamicEnumParameter {

    /**
     * Creates a new instance
     */
    public TableColumnIntegrationParameter() {
        List<Object> allowedValues = new ArrayList<>(ACAQTableRegistry.getInstance().getOperationsOfType(IntegratingColumnOperation.class).values()
                .stream().sorted(Comparator.comparing(ACAQTableRegistry.ColumnOperationEntry::getName)).collect(Collectors.toList()));
        setAllowedValues(allowedValues);
        setValue(allowedValues.get(0));
    }

    /**
     * Creates a copy
     *
     * @param value the original
     */
    public TableColumnIntegrationParameter(TableColumnIntegrationParameter value) {
        super(value);
    }

    @Override
    public String renderLabel(Object value) {
        if (value instanceof ACAQTableRegistry.ColumnOperationEntry) {
            ACAQTableRegistry.ColumnOperationEntry entry = (ACAQTableRegistry.ColumnOperationEntry) value;
            return entry.getName();
        } else {
            return "[None selected]";
        }
    }

    /**
     * The ID of the selected entry
     *
     * @return the id
     */
    @JsonGetter("id")
    public String getId() {
        if (getValue() == null)
            return null;
        return ((ACAQTableRegistry.ColumnOperationEntry) getValue()).getId();
    }

    /**
     * Sets the ID of the selected entry
     *
     * @param id the id
     */
    @JsonSetter("id")
    public void setId(String id) {
        if (id != null) {
            setValue(ACAQTableRegistry.getInstance().getColumnOperationById(id));
        }
    }

    @Override
    public String renderTooltip(Object value) {
        if (value instanceof ACAQTableRegistry.ColumnOperationEntry) {
            ACAQTableRegistry.ColumnOperationEntry entry = (ACAQTableRegistry.ColumnOperationEntry) value;
            return "<html><strong>" + HtmlEscapers.htmlEscaper().escape(entry.getName()) + "</strong><br/>" + HtmlEscapers.htmlEscaper().escape(entry.getDescription()) + "</html>";
        } else {
            return null;
        }
    }

    @Override
    public Icon renderIcon(Object value) {
        return UIUtils.getIconFromResources("statistics.png");
    }
}
