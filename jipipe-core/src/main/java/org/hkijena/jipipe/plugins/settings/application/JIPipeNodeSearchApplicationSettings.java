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

package org.hkijena.jipipe.plugins.settings.application;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseSearchImplementation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.service.components.JIPipeApplicationSettingsServiceComponent;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationsSettingsSheet;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

/**
 * All settings for the node search system (#1409): the search implementation
 * (the classic lexicon searches), the AI contribution of the merged ranking,
 * the typo tolerance of the enhanced search, and the reset of the recorded
 * personal node usage counts.
 * <p>
 * The search implementation setting moved here from the general UI sheet
 * ({@link JIPipeGeneralUIApplicationSettings}, parameter key
 * {@code search-implementation-v2}). The constructor adopts a value that is
 * stored under the old location, so a user who chose Legacy keeps Legacy
 * after the update; a value stored under this sheet's own id always wins.
 */
public class JIPipeNodeSearchApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet {

    private static final Logger LOGGER = LoggerFactory.getLogger(JIPipeNodeSearchApplicationSettings.class);

    public static final String ID = "org.hkijena.jipipe:node-search";

    private JIPipeNodeDatabaseSearchImplementation searchImplementation = JIPipeNodeDatabaseSearchImplementation.Enhanced;
    private boolean aiContribution = true;
    private boolean typoTolerance = true;
    private NodeUsageResetParameter usageReset = new NodeUsageResetParameter();

    /**
     * Creates a new instance and applies the compat migration: the search
     * implementation stored under the old general UI location is adopted when
     * this sheet has no stored value of its own. This runs before the sheet
     * is registered, so the adopted value is part of the state that the
     * settings bridge seeds into the document for a previously unknown sheet
     * id: the migration persists with the next settings save.
     */
    public JIPipeNodeSearchApplicationSettings() {
        try {
            JIPipeNodeDatabaseSearchImplementation adopted =
                    adoptLegacySearchImplementation(JIPipeApplicationSettingsServiceComponent.getRawSheetsNode());
            if (adopted != null) {
                this.searchImplementation = adopted;
            }
        } catch (Exception e) {
            // Fail-closed: keep the default. The raw read failing must not
            // block the sheet registration.
            LOGGER.debug("Could not adopt the legacy search implementation from the general UI sheet; keeping the default", e);
        }
    }

    public static JIPipeNodeSearchApplicationSettings getInstance() {
        return JIPipe.getSettings().getById(ID, JIPipeNodeSearchApplicationSettings.class);
    }

    /**
     * The compat migration of the search implementation setting (public to be
     * feedable by the migration test with synthetic raw nodes; the constructor
     * applies it against the real raw sheets node).
     * <p>
     * Returns the implementation to adopt from the old general UI sheet's
     * {@code search-implementation-v2} key, or null when there is nothing to
     * adopt: this sheet's own key is present in the raw sheets (its stored
     * value always wins, whatever its age), the old key is absent or null,
     * or the stored value is not a valid implementation name.
     *
     * @param rawSheets the raw sheets node ({@code getRawSheetsNode()} shape,
     *                  keyed by sheet id); may be null
     * @return the implementation to adopt, or null
     */
    public static JIPipeNodeDatabaseSearchImplementation adoptLegacySearchImplementation(JsonNode rawSheets) {
        if (rawSheets == null || !rawSheets.isObject()) {
            return null;
        }
        JsonNode own = rawSheets.path(ID).path("search-implementation-v2");
        if (!own.isMissingNode() && !own.isNull()) {
            return null;
        }
        JsonNode legacy = rawSheets.path(JIPipeGeneralUIApplicationSettings.ID).path("search-implementation-v2");
        if (legacy.isMissingNode() || legacy.isNull()) {
            return null;
        }
        try {
            return JsonUtils.getObjectMapper().readerFor(JIPipeNodeDatabaseSearchImplementation.class).readValue(legacy);
        } catch (Exception e) {
            return null;
        }
    }

    @SetJIPipeDocumentation(name = "Node search implementation", description = "The method responsible for handling the node search")
    @JIPipeParameter("search-implementation-v2")
    public JIPipeNodeDatabaseSearchImplementation getSearchImplementation() {
        return searchImplementation;
    }

    @JIPipeParameter("search-implementation-v2")
    public void setSearchImplementation(JIPipeNodeDatabaseSearchImplementation searchImplementation) {
        this.searchImplementation = searchImplementation;
    }

    @SetJIPipeDocumentation(name = "AI contribution", description = "If enabled, the node search blends the classic ranking with the AI (embedding) contribution when the AI model is available. Disable to always use the classic ranking alone.")
    @JIPipeParameter("ai-contribution")
    public boolean isAiContribution() {
        return aiContribution;
    }

    @JIPipeParameter("ai-contribution")
    public void setAiContribution(boolean aiContribution) {
        this.aiContribution = aiContribution;
    }

    @SetJIPipeDocumentation(name = "Typo tolerance", description = "If enabled, the node search tolerates typos in query words (fuzzy matching). Disable for strictly literal matching.")
    @JIPipeParameter("typo-tolerance")
    public boolean isTypoTolerance() {
        return typoTolerance;
    }

    @JIPipeParameter("typo-tolerance")
    public void setTypoTolerance(boolean typoTolerance) {
        this.typoTolerance = typoTolerance;
    }

    @SetJIPipeDocumentation(name = "Reset usage data", description = "Clears the recorded personal node usage counts; the shipped community prior stays active.")
    @JIPipeParameter("usage-reset")
    public NodeUsageResetParameter getUsageReset() {
        return usageReset;
    }

    @JIPipeParameter("usage-reset")
    public void setUsageReset(NodeUsageResetParameter usageReset) {
        this.usageReset = usageReset;
    }

    @Override
    public JIPipeDefaultApplicationSettingsSheetCategory getDefaultCategory() {
        return JIPipeDefaultApplicationSettingsSheetCategory.General;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Icon getIcon() {
        return JIPipe.RESOURCES.getIcon16("actions/search.png");
    }

    @Override
    public String getName() {
        return "Node search";
    }

    @Override
    public String getDescription() {
        return "Settings for the node search system (implementation, AI contribution, typo tolerance, usage data)";
    }
}
