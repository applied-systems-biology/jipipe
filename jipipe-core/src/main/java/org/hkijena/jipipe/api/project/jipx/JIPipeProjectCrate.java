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

package org.hkijena.jipipe.api.project.jipx;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.metadata.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.metadata.JIPipeOrganizationMetadata;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.contrib.ro_crate.RoCrate;
import org.hkijena.jipipe.contrib.ro_crate.entities.contextual.ContextualEntity;
import org.hkijena.jipipe.contrib.ro_crate.entities.contextual.JsonDescriptor;
import org.hkijena.jipipe.contrib.ro_crate.entities.contextual.OrganizationEntity;
import org.hkijena.jipipe.contrib.ro_crate.entities.contextual.PersonEntity;
import org.hkijena.jipipe.contrib.ro_crate.entities.data.FileEntity;
import org.hkijena.jipipe.contrib.ro_crate.entities.data.WorkflowEntity;
import org.hkijena.jipipe.contrib.ro_crate.objectmapper.MyObjectMapper;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.plugins.settings.application.JIPipeProjectIOApplicationSettings;
import org.hkijena.jipipe.utils.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds the Workflow RO-Crate metadata ({@code ro-crate-metadata.json})
 * of a JIPX project container.
 * <p>
 * The entity-building idioms mirror the export flow in
 * {@code CreateROCrateRun} (the same contrib library, the same profile),
 * with two deliberate differences:
 * <ul>
 * <li>No entity {@code location}s - the crate is serialized via
 * {@link RoCrate#getJsonMetadata()} and the container's ZIP is written by
 * {@link JIPipeXProjectWriter} itself, so the library's file-copying
 * writers are not used.</li>
 * <li>The main entity is the container's own {@code project/graph.json}
 * part (a JIPipe program), not an external CWL wrapper.</li>
 * <li>The Workflow RO-Crate profile descriptor is applied post-build via
 * {@code RoCrate.setJsonDescriptor} - the builder's internal descriptor
 * is not configurable through the builder API, and adding the descriptor
 * as a contextual entity would duplicate it in the {@code @graph} (the
 * library's own {@code CreateROCrateRun} addContextualEntity pattern has
 * that flaw; the profile is still present, twice).</li>
 * </ul>
 * <p>
 * <b>Compliance contract:</b> after auto-filling what the metadata does not
 * provide (license → settings default → MIT, the {@code mainEntity}
 * pointer), {@link #buildCrate} runs the
 * spec's profile checklist and reports every missing piece into
 * {@code violationsOut} - a well-formed save yields ZERO violations.
 */
public final class JIPipeProjectCrate {

    /**
     * The main entity id of a JIPX crate: the container's main graph part.
     * Equal to {@link JIPipeXManifest#MAIN_GRAPH} - the crate's
     * {@code mainEntity} and the manifest's {@code main-graph} must
     * always agree.
     */
    public static final String MAIN_ENTITY_ID = JIPipeXManifest.MAIN_GRAPH;

    /**
     * The id of the JIPipe programming-language contextual entity.
     * <p>
     * Stable URI placeholder: the spec's §6 follow-up work item publishes
     * the real language profile page at {@code https://jipipe.org};
     * {@code jipipe.org} is the registered domain, so the id is stable
     * across JIPipe versions (the profile <i>content</i> is not part of
     * the container format).
     */
    public static final String LANGUAGE_ENTITY_ID = "https://jipipe.org/jipipe";

    /**
     * The id prefix of Person entities for authors without an ORCID.
     * A URN (not a file path): it cannot collide with a crate data entity
     * and stays stable across re-saves of the same project.
     */
    public static final String PERSON_ID_PREFIX = "urn:person:";

    /**
     * The Bioschemas ComputationalWorkflow profile the main workflow
     * entity conforms to (same value as the reference
     * {@code CreateROCrateRun}).
     */
    public static final String BIOSCHEMAS_COMPUTATIONAL_WORKFLOW_PROFILE =
            "https://bioschemas.org/profiles/ComputationalWorkflow/1.0-RELEASE";

    private JIPipeProjectCrate() {
    }

    /**
     * Builds the Workflow RO-Crate for the given project and runs the
     * compliance checklist.
     *
     * @param project       the project to crate (its metadata drives the
     *                      entity graph)
     * @param violationsOut collects the compliance violations; a
     *                      well-formed save leaves it empty
     * @return the built crate (validated by the library's JSON-schema
     * validator - violations are printed to {@code System.err} by
     * the library and intentionally left there)
     * @throws JIPipeValidationRuntimeException if the crate cannot be built
     */
    public static RoCrate buildCrate(JIPipeProject project, List<String> violationsOut) {
        RoCrate.RoCrateBuilder builder;
        try {
            builder = newBuilder(project);
        } catch (RuntimeException e) {
            throw new JIPipeValidationRuntimeException(e, "Could not build the RO-Crate metadata",
                    "The Workflow RO-Crate builder rejected the project metadata: " + e.getMessage(),
                    "Check the project metadata (name, license, authors) in the project properties.");
        }

        RoCrate crate;
        try {
            crate = builder.build();
        } catch (RuntimeException e) {
            throw new JIPipeValidationRuntimeException(e, "Could not build the RO-Crate metadata",
                    "The Workflow RO-Crate builder failed: " + e.getMessage(),
                    "Check the project metadata (name, license, authors) in the project properties.");
        }

        // Apply the Workflow RO-Crate profile descriptor post-build: the
        // builder's internal JsonDescriptor cannot be configured through
        // the builder API (descriptorBuilder is final, no setter), and
        // the naive addContextualEntity pattern would create a DUPLICATE
        // contextual entity with the same @id (ro-crate-metadata.json)
        // in the @graph.
        crate.setJsonDescriptor(new JsonDescriptor.Builder()
                .addConformsTo("https://w3id.org/workflowhub/workflow-ro-crate/1.0")
                .build());

        // Point the root at the main entity (same post-build hook as
        // CreateROCrateRun.addROCrateMainEntity - the builder has no
        // mainEntity setter).
        crate.getRootDataEntity().addIdProperty("mainEntity", MAIN_ENTITY_ID);

        // Root keywords: "JIPipe" + the metadata's categories (the
        // library's addProperty overwrites single values, so the list is
        // built explicitly as an array - the schema allows arrays of
        // literals, which serialize as the RO-Crate list form).
        ArrayNode keywords = MyObjectMapper.getMapper().createArrayNode();
        keywords.add("JIPipe");
        for (PluginCategoriesEnumParameter category : project.getMetadata().getCategories()) {
            String value = StringUtils.nullToEmpty(category.getValue()).trim();
            if (!value.isEmpty()) {
                keywords.add(value);
            }
        }
        crate.getRootDataEntity().addProperty("keywords", keywords);

        checkCompliance(crate, violationsOut);
        return crate;
    }

    /**
     * Creates the crate builder with all entities attached (mirrors
     * {@code CreateROCrateRun.createROCrateBuilder()} + the entity-adding
     * runs, without {@code location}s).
     */
    private static RoCrate.RoCrateBuilder newBuilder(JIPipeProject project) {
        // Root entity: name / description / datePublished / license.
        // License resolution (a non-empty metadata license always wins):
        // metadata license → settings default → MIT.
        String metadataLicense = StringUtils.nullToEmpty(project.getMetadata().getLicense());
        String effectiveLicense = metadataLicense;
        if (StringUtils.isNullOrEmpty(effectiveLicense)) {
            effectiveLicense = JIPipeProjectIOApplicationSettings.getInstance().getDefaultRoCrateLicense();
        }
        RoCrate.RoCrateBuilder builder = new RoCrate.RoCrateBuilder(
                StringUtils.isNullOrEmpty(project.getMetadata().getName()) ? "JIPipe Project" : project.getMetadata().getName(),
                project.getMetadata().getSummary() == null ? "" : project.getMetadata().getSummary().toPlainText(),
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                toSpdxLicenseUri(effectiveLicense));

        // The JIPipe programming-language contextual entity (the CWL
        // language entity of CreateROCrateRun, replaced by JIPipe - the
        // stable URI placeholder, see LANGUAGE_ENTITY_ID).
        builder.addContextualEntity(new ContextualEntity.ContextualEntityBuilder()
                .setId(LANGUAGE_ENTITY_ID)
                .addType("ComputerLanguage")
                .addProperty("name", "JIPipe")
                .addProperty("version", JIPipe.getJIPipeVersion())
                .build());

        // The Bioschemas ComputationalWorkflow profile guide (mirrors
        // CreateROCrateRun's contextual entity - spec §6).
        builder.addContextualEntity(new ContextualEntity.ContextualEntityBuilder()
                .setId(BIOSCHEMAS_COMPUTATIONAL_WORKFLOW_PROFILE)
                .addType("Guide")
                .addProperty("name", "ComputationalWorkflow Profile")
                .addProperty("version", "1.0-RELEASE")
                .addProperty("description", "Bioschemas specification for describing a Computational Workflow")
                .build());

        // Organizations from the authors' affiliations (deduplicated by
        // unique id, mirroring CreateROCrateRun.addROCrateOrganizations).
        Set<String> organizationIds = new LinkedHashSet<>();
        for (JIPipeAuthorMetadata author : project.getMetadata().getAuthors()) {
            for (JIPipeOrganizationMetadata affiliation : author.getAffiliations()) {
                String organizationId = affiliation.getUniqueId();
                if (!StringUtils.isNullOrEmpty(organizationId) && organizationIds.add(organizationId)) {
                    OrganizationEntity.OrganizationEntityBuilder organizationEntityBuilder =
                            new OrganizationEntity.OrganizationEntityBuilder();
                    organizationEntityBuilder.setId(organizationId);
                    organizationEntityBuilder.addProperty("name", StringUtils.nullToEmpty(affiliation.getName()));
                    builder.addContextualEntity(organizationEntityBuilder.build());
                }
            }
        }

        // Persons from the project authors (mirrors
        // CreateROCrateRun.addROCrateAuthors). Authors without an ORCID
        // get a stable non-URL id (urn:person:<index> in author order)
        // instead - an empty id would be replaced by a random UUID by the
        // library, breaking id stability.
        int authorIndex = 0;
        for (JIPipeAuthorMetadata author : project.getMetadata().getAuthors()) {
            PersonEntity.PersonEntityBuilder entityBuilder = new PersonEntity.PersonEntityBuilder();
            entityBuilder.setId(StringUtils.isNullOrEmpty(author.getOrcidUrl())
                    ? PERSON_ID_PREFIX + authorIndex
                    : author.getOrcidUrl());
            if (!StringUtils.isNullOrEmpty(author.getFirstName())) {
                entityBuilder.setGivenName(author.getFirstName());
            }
            if (!StringUtils.isNullOrEmpty(author.getLastName())) {
                entityBuilder.setFamilyName(author.getLastName());
            }
            if (!StringUtils.isNullOrEmpty(author.getEmail())) {
                entityBuilder.setEmail(author.getEmail());
            }
            if (!StringUtils.isNullOrEmpty(author.getContact())) {
                entityBuilder.setEmail(author.getContact());
            }
            for (JIPipeOrganizationMetadata affiliation : author.getAffiliations()) {
                entityBuilder.addIdProperty("affiliation", affiliation.getUniqueId());
            }
            builder.addContextualEntity(entityBuilder.build());
            authorIndex++;
        }

        // The main entity: the container's own graph part as a
        // ComputationalWorkflow in JIPipe (mirrors
        // CreateROCrateRun.addROCrateCwlWorkflow, without the CWL
        // programming language and without a location).
        builder.addDataEntity(new WorkflowEntity.WorkflowEntityBuilder()
                .setId(MAIN_ENTITY_ID)
                .addType("File")
                .addIdProperty("programmingLanguage", LANGUAGE_ENTITY_ID)
                .addIdProperty("conformsTo", BIOSCHEMAS_COMPUTATIONAL_WORKFLOW_PROFILE)
                .addProperty("encodingFormat", "application/json")
                .addProperty("name", "JIPipe project graph")
                .build());

        // The README (mirrors CreateROCrateRun.createReadme, minus the
        // location - the file is written by the container writer).
        builder.addDataEntity(new FileEntity.FileEntityBuilder()
                .setId("README.md")
                .addType("File")
                .addProperty("encodingFormat", "text/markdown")
                .addProperty("about", "./")
                .build());

        return builder;
    }

    /**
     * The spec's profile checklist, run AFTER the auto-fill (so a
     * well-formed save yields zero violations):
     * (1) license present on the root, (2) mainEntity present on the
     * root, (3) README.md data entity present, (4) programmingLanguage
     * present on the main entity, (5) Bioschemas conformsTo present on
     * the main entity.
     */
    private static void checkCompliance(RoCrate crate, List<String> violationsOut) {
        if (crate.getRootDataEntity().getIdProperty("license") == null) {
            violationsOut.add("license missing on the RO-Crate root entity (the default license setting and metadata license are both empty?)");
        }
        if (crate.getRootDataEntity().getIdProperty("mainEntity") == null) {
            violationsOut.add("mainEntity missing from the RO-Crate root entity");
        }
        if (crate.getDataEntityById("README.md") == null) {
            violationsOut.add("README.md data entity missing from the RO-Crate");
        }
        if (crate.getDataEntityById(MAIN_ENTITY_ID) == null
                || crate.getDataEntityById(MAIN_ENTITY_ID).getIdProperty("programmingLanguage") == null) {
            violationsOut.add("programmingLanguage missing on the main workflow entity");
        }
        if (crate.getDataEntityById(MAIN_ENTITY_ID) == null
                || crate.getDataEntityById(MAIN_ENTITY_ID).getIdProperty("conformsTo") == null) {
            violationsOut.add("Bioschemas conformsTo missing on the main workflow entity");
        }
    }

    /**
     * Resolves a license value to an SPDX license URI (same convention as
     * {@code CreateROCrateRun.toSpdxLicenseUri}): empty → MIT, an http(s)
     * URL is kept as-is, everything else is treated as an SPDX
     * identifier.
     */
    private static String toSpdxLicenseUri(String license) {
        if (StringUtils.isNullOrEmpty(license)) {
            return "https://spdx.org/licenses/MIT";
        }
        if (license.startsWith("http://") || license.startsWith("https://")) {
            return license;
        }
        return "https://spdx.org/licenses/" + license;
    }
}
