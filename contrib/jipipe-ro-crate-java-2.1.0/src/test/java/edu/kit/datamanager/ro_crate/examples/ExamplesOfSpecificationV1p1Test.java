package edu.kit.datamanager.ro_crate.examples;

import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.entities.AbstractEntity;
import edu.kit.datamanager.ro_crate.entities.contextual.ContextualEntity;
import edu.kit.datamanager.ro_crate.entities.contextual.OrganizationEntity;
import edu.kit.datamanager.ro_crate.entities.contextual.PersonEntity;
import edu.kit.datamanager.ro_crate.entities.contextual.PlaceEntity;
import edu.kit.datamanager.ro_crate.entities.data.*;

import edu.kit.datamanager.ro_crate.writer.CrateWriter;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.file.Paths;

import static edu.kit.datamanager.ro_crate.HelpFunctions.printAndAssertEquals;

/**
 * This class contains examples of the RO-Crate specification version 1.1.
 * <p>
 * This is supposed to serve both as a user guide and as a test for the implementation.
 * Executing a test may also print some interesting information to the console.
 */
public class ExamplesOfSpecificationV1p1Test {

    /**
     * From: <a href="https://www.researchobject.org/ro-crate/specification/1.1/root-data-entity.html#minimal-example-of-ro-crate">
     *     Minimal Example
     * </a> (<a href="src/test/resources/spec-v1.1-example-json-files/minimal.json">location in repo</a>)
     * <p>
     * This example produces a minimal crate with a
     * name, description, date, license and identifier.
     * <p>
     * This example produces the same result as
     * {@link #testMinimalCrateWithoutCrateBuilder()}, but using more convenient APIs.
     */
    @Test
    void testMinimalCrateConvenient() {
        String licenseID = "https://creativecommons.org/licenses/by-nc-sa/3.0/au/";
        RoCrate minimal = new RoCrate.RoCrateBuilder(
                "Data files associated with the manuscript:Effects of facilitated family case conferencing for ...",
                "Palliative care planning for nursing home residents with advanced dementia ...",
                "2017",
                licenseID
        )
                // We already had to set the license ID in the builder,
                // but we can override it with more details to fit the example:
                .setLicense( new ContextualEntity.ContextualEntityBuilder()
                        .addType("CreativeWork")
                        .setId(licenseID)
                        .addProperty("description", "This work is licensed under the Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Australia License. To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-sa/3.0/au/ or send a letter to Creative Commons, PO Box 1866, Mountain View, CA 94042, USA.")
                        .addProperty("identifier", licenseID)
                        .addProperty("name", "Attribution-NonCommercial-ShareAlike 3.0 Australia (CC BY-NC-SA 3.0 AU)")
                        .build()
                )
                .addIdentifier("https://doi.org/10.4225/59/59672c09f4a4b")
                .build();

        printAndAssertEquals(minimal, "/spec-v1.1-example-json-files/minimal.json");
    }

    /**
     * From: <a href="https://www.researchobject.org/ro-crate/specification/1.1/root-data-entity.html#minimal-example-of-ro-crate">
     *     Minimal Example
     * </a> (<a href="src/test/resources/spec-v1.1-example-json-files/minimal.json">location in repo</a>)
     * <p>
     * In this example, the minimal crate is created without the builder.
     * This should only be done if necessary: Use the builder if possible.
     * This example produces the same result as {@link #testMinimalCrateConvenient()}.
     */
    @Test
    void testMinimalCrateWithoutCrateBuilder() {
        RoCrate minimal = new RoCrate();

        ContextualEntity license = new ContextualEntity.ContextualEntityBuilder()
                .addType("CreativeWork")
                .setId("https://creativecommons.org/licenses/by-nc-sa/3.0/au/")
                .addProperty("description", "This work is licensed under the Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Australia License. To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-sa/3.0/au/ or send a letter to Creative Commons, PO Box 1866, Mountain View, CA 94042, USA.")
                .addProperty("identifier", "https://creativecommons.org/licenses/by-nc-sa/3.0/au/")
                .addProperty("name", "Attribution-NonCommercial-ShareAlike 3.0 Australia (CC BY-NC-SA 3.0 AU)")
                .build();

        minimal.setRootDataEntity(new RootDataEntity.RootDataEntityBuilder()
                .addProperty("identifier", "https://doi.org/10.4225/59/59672c09f4a4b")
                .addProperty("datePublished", "2017")
                .addProperty("name", "Data files associated with the manuscript:Effects of facilitated family case conferencing for ...")
                .addProperty("description", "Palliative care planning for nursing home residents with advanced dementia ...")
                .setLicense(license)
                .build());

        // This is pretty low-level. We are considering hiding/replacing this detailed API in major versions,
        // so tell us (for example, open an issue) if you have a use case for it!
        minimal.setJsonDescriptor(new ContextualEntity.ContextualEntityBuilder()
                .setId("ro-crate-metadata.json")
                .addType("CreativeWork")
                .addIdProperty("about", "./")
                .addIdProperty("conformsTo", "https://w3id.org/ro/crate/1.1")
                .build()
        );
        minimal.addContextualEntity(license);

        printAndAssertEquals(minimal, "/spec-v1.1-example-json-files/minimal.json");
    }

    // https://www.researchobject.org/ro-crate/specification/1.1/data-entities.html#example-linking-to-a-file-and-folders

    /**
     * From: <a href="https://www.researchobject.org/ro-crate/specification/1.1/data-entities.html#example-linking-to-a-file-and-folders">
     *     "Example linking to a file and folders"
     * </a> (<a href="src/test/resources/spec-v1.1-example-json-files/files-and-folders.json.json">location in repo</a>)
     * <p>
     * This example adds a File(Entity) and a DataSet(Entity) to the crate.
     * The file and the folder are referenced by their location. This way
     * they will be copied to the crate when writing it using a
     * {@link CrateWriter}.
     * The name of the file and the folder will be implicitly set to the
     * ID of the respective entity in order to conform to the specification.
     * <p>
     * Here we use the inner builder classes for the construction of the
     * crate. In contrast to {@link #testMinimalCrateWithoutCrateBuilder()},
     * we do not have to care about specification details.
     */
    @Test
    void testLinkingToFileAndFolders() {
        RoCrate crate = new RoCrate.RoCrateBuilder()
                .addDataEntity(
                        new FileEntity.FileEntityBuilder()
                                // This will tell us where the file is located. It will be copied to the crate.
                                .setLocation(Paths.get("path to file"))
                                // If no ID is given explicitly, the ID will be set to the filename.
                                // Changing the ID means also to set the file name within the crate!
                                .setId("cp7glop.ai")
                                .addProperty("name", "Diagram showing trend to increase")
                                .addProperty("contentSize", "383766")
                                .addProperty("description", "Illustrator file for Glop Pot")
                                .setEncodingFormat("application/pdf")
                                .build()
                )
                .addDataEntity(
                        new DataSetEntity.DataSetBuilder()
                                .setLocation(Paths.get("path_to_files"))
                                .setId("lots_of_little_files/")
                                .addProperty("name", "Too many files")
                                .addProperty("description", "This directory contains many small files, that we're not going to describe in detail.")
                                .build()
                )
                .build();

        printAndAssertEquals(crate, "/spec-v1.1-example-json-files/files-and-folders.json");
    }

    /**
     * From: <a href="https://www.researchobject.org/ro-crate/specification/1.1/data-entities.html#web-based-data-entities">
     *     Example with web-based data entities
     * </a> (<a href="src/test/resources/spec-v1.1-example-json-files/web-based-data-entities.json">location in repo</a>)
     * <p>
     * This example adds twp FileEntities to the crate.
     * One is a local file, the other one is located in the web
     * and will not be copied to the crate.
     */
    @Test
    void testWebBasedDataEntities() {
        RoCrate crate = new RoCrate.RoCrateBuilder()
                .addDataEntity(
                        new FileEntity.FileEntityBuilder()
                                .setLocation(Paths.get("README.md"))
                                .setId("survey-responses-2019.csv")
                                .addProperty("name", "Survey responses")
                                .addProperty("contentSize", "26452")
                                .setEncodingFormat("text/csv")
                                .build()
                )
                .addDataEntity(
                        new FileEntity.FileEntityBuilder()
                                .setLocation(URI.create("https://zenodo.org/record/3541888/files/ro-crate-1.0.0.pdf"))
                                .addProperty("name", "RO-Crate specification")
                                .addProperty("contentSize", "310691")
                                .addProperty("description", "RO-Crate specification")
                                .setEncodingFormat("application/pdf")
                                .build()
                )
                .build();

        printAndAssertEquals(crate, "/spec-v1.1-example-json-files/web-based-data-entities.json");
    }

    /**
     * From: <a href="https://www.researchobject.org/ro-crate/specification/1.1/appendix/jsonld.html">
     *     Example with file, author, and location
     * </a> (<a href="src/test/resources/spec-v1.1-example-json-files/file-author-location.json">location in repo</a>)
     * <p>
     * This example shows how to connect entities. If there is no specific method like
     * {@link DataEntity.DataEntityBuilder#addAuthor(String)} for referencing other
     * entities, one can use the more generic
     * {@link AbstractEntity.AbstractEntityBuilder#addIdProperty(String, AbstractEntity)}
     * or {@link AbstractEntity.AbstractEntityBuilder#addIdProperty(String, String)}.
     * <p>
     * <b>Important Note!</b> If you connect entities, make sure all entities are being
     * added to the crate. We currently can't enforce this properly yet.
     */
    @Test
    void testWithFileAuthorLocation() {
        // These two entities will be connected to others later on. Therefore, we make
        // them easier referencable. Referencing can be done using the whole entity or
        // its ID.
        final PersonEntity alice = new PersonEntity.PersonEntityBuilder()
                .setId("#alice")
                .addProperty("name", "Alice")
                .addProperty("description", "One of hopefully many Contextual Entities")
                .build();
        final PlaceEntity park = new PlaceEntity.PlaceEntityBuilder()
                .setId(URI.create("http://sws.geonames.org/8152662/").toString())
                .addProperty("name", "Catalina Park")
                .build();
        final String licenseId = "https://spdx.org/licenses/CC-BY-NC-SA-4.0";

        final RoCrate crate = new RoCrate.RoCrateBuilder(
                "Example RO-Crate",
                "The RO-Crate Root Data Entity",
                "2020",
                licenseId
        )
                .addContextualEntity(park)
                .addContextualEntity(alice)
                .addDataEntity(
                        new FileEntity.FileEntityBuilder()
                                .setLocation(Paths.get("......."))
                                .setId("data2.txt")
                                .build()
                )
                .addDataEntity(
                        new FileEntity.FileEntityBuilder()
                                .setLocation(Paths.get("......."))
                                .setId("data1.txt")
                                .addProperty("description", "One of hopefully many Data Entities")
                                // ↓ This is the specific way to add an author
                                .addAuthor(alice.getId())
                                // ↓ This is the generic way to add a location or other relations
                                .addIdProperty("contentLocation", park)
                                .build()
                )
                .build();

        /*
         The builder enforces to provide a license and a publishing date,
         but the example does not have them. So we have to remove them below:
         */

        // **Note**: When you add a license, even if only by a string, the crate will
        // implicitly also get a small ContextEntity for this license. When we remove
        // this (any) entity, all references to it will be removed as well to ensure
        // consistency within the crate. Therefore, there will be no trace left of
        // the license.
        crate.deleteEntityById(licenseId);

        // The datePublished property is a simple property and simple to remove without
        // any further internal checks.
        crate.getRootDataEntity().removeProperty("datePublished");

        printAndAssertEquals(crate, "/spec-v1.1-example-json-files/file-author-location.json");
    }

    /**
     * From: <a href="https://www.researchobject.org/ro-crate/specification/1.1/workflows.html#complete-workflow-example">
     *     Example with complete workflow
     * </a> (<a href="src/test/resources/spec-v1.1-example-json-files/complete-workflow-example.json">location in repo</a>)
     * <p>
     * This example shows how to connect entities. If there is no specific method like
     * {@link DataEntity.DataEntityBuilder#addAuthor(String)} for referencing other
     * entities, one can use the more generic
     * {@link AbstractEntity.AbstractEntityBuilder#addIdProperty(String, AbstractEntity)}
     * or {@link AbstractEntity.AbstractEntityBuilder#addIdProperty(String, String)}.
     * <p>
     * <b>Important Note!</b> If you connect entities, make sure all entities are being
     * added to the crate. We currently can't enforce this properly yet.
     */
    @Test
    void testCompleteWorkflowExample() {
        final String licenseId = "https://spdx.org/licenses/CC-BY-NC-SA-4.0";
        ContextualEntity license = new ContextualEntity.ContextualEntityBuilder()
                .addType("CreativeWork")
                .setId(licenseId)
                .addProperty("name", "Creative Commons Attribution Non Commercial Share Alike 4.0 International")
                .addProperty("alternateName", "CC-BY-NC-SA-4.0")
                .build();
        ContextualEntity knime = new ContextualEntity.ContextualEntityBuilder()
                .setId("#knime")
                .addType("ComputerLanguage")
                .addProperty("name", "KNIME Analytics Platform")
                .addProperty("alternateName", "KNIME")
                .addProperty("url", "https://www.knime.com/whats-new-in-knime-41")
                .addProperty("version", "4.1.3")
                .build();
        OrganizationEntity workflowHub = new OrganizationEntity.OrganizationEntityBuilder()
                .setId("#workflow-hub")
                .addProperty("name", "Example Workflow Hub")
                .addProperty("url", "http://example.com/workflows/")
                .build();
        ContextualEntity fasta = new ContextualEntity.ContextualEntityBuilder()
                .setId("http://edamontology.org/format_1929")
                .addType("Thing")
                .addProperty("name", "FASTA sequence format")
                .build();
        ContextualEntity clustalW = new ContextualEntity.ContextualEntityBuilder()
                .setId("http://edamontology.org/format_1982")
                .addType("Thing")
                .addProperty("name", "ClustalW alignment format")
                .build();
        ContextualEntity ban = new ContextualEntity.ContextualEntityBuilder()
                .setId("http://edamontology.org/format_2572")
                .addType("Thing")
                .addProperty("name", "BAM format")
                .build();
        ContextualEntity nucSec = new ContextualEntity.ContextualEntityBuilder()
                .setId("http://edamontology.org/data_2977")
                .addType("Thing")
                .addProperty("name", "Nucleic acid sequence")
                .build();
        ContextualEntity nucAlign = new ContextualEntity.ContextualEntityBuilder()
                .setId("http://edamontology.org/data_1383")
                .addType("Thing")
                .addProperty("name", "Nucleic acid sequence alignment")
                .build();
        PersonEntity alice = new PersonEntity.PersonEntityBuilder()
                .setId("#alice")
                .addProperty("name", "Alice Brown")
                .build();
        ContextualEntity requiredParam = new ContextualEntity.ContextualEntityBuilder()
                .addType("FormalParameter")
                .setId("#36aadbd4-4a2d-4e33-83b4-0cbf6a6a8c5b")
                .addProperty("name", "genome_sequence")
                .addProperty("valueRequired", true)
                .addIdProperty("conformsTo", "https://bioschemas.org/profiles/FormalParameter/0.1-DRAFT-2020_07_21/")
                .addIdProperty("additionalType", nucSec)
                .addIdProperty("format", fasta)
                .build();
        ContextualEntity clnParam = new ContextualEntity.ContextualEntityBuilder()
                .addType("FormalParameter")
                .setId("#6c703fee-6af7-4fdb-a57d-9e8bc4486044")
                .addProperty("name", "cleaned_sequence")
                .addIdProperty("conformsTo", "https://bioschemas.org/profiles/FormalParameter/0.1-DRAFT-2020_07_21/")
                .addIdProperty("additionalType", nucSec)
                .addIdProperty("encodingFormat", ban)
                .build();
        ContextualEntity alignParam = new ContextualEntity.ContextualEntityBuilder()
                .addType("FormalParameter")
                .setId("#2f32b861-e43c-401f-8c42-04fd84273bdf")
                .addProperty("name", "sequence_alignment")
                .addIdProperty("conformsTo", "https://bioschemas.org/profiles/FormalParameter/0.1-DRAFT-2020_07_21/")
                .addIdProperty("additionalType", nucAlign)
                .addIdProperty("encodingFormat", clustalW)
                .build();

        RoCrate crate = new RoCrate.RoCrateBuilder(
                "Example RO-Crate",
                "The RO-Crate Root Data Entity",
                "2020",
                licenseId
        )
                .setLicense(license)
                .addContextualEntity(knime)
                .addContextualEntity(workflowHub)
                .addContextualEntity(fasta)
                .addContextualEntity(clustalW)
                .addContextualEntity(ban)
                .addContextualEntity(nucSec)
                .addContextualEntity(nucAlign)
                .addContextualEntity(alice)
                .addContextualEntity(requiredParam)
                .addContextualEntity(clnParam)
                .addContextualEntity(alignParam)
                .addDataEntity(
                        new WorkflowEntity.WorkflowEntityBuilder()
                                .setId("workflow/alignment.knime")
                                .setLocation(Paths.get("src"))
                                .addIdProperty("conformsTo", "https://bioschemas.org/profiles/ComputationalWorkflow/0.5-DRAFT-2020_07_21/")
                                .addProperty("name", "Sequence alignment workflow")
                                .addIdProperty("programmingLanguage", "#knime")
                                // This example does not use the term "author"...
                                //.addAuthor("#alice")
                                // instead, it uses "creator":
                                .addIdProperty("creator", "#alice")
                                .addProperty("dateCreated", "2020-05-23")
                                .setLicense(licenseId)
                                .addInput("#36aadbd4-4a2d-4e33-83b4-0cbf6a6a8c5b")
                                .addOutput("#6c703fee-6af7-4fdb-a57d-9e8bc4486044")
                                .addOutput("#2f32b861-e43c-401f-8c42-04fd84273bdf")
                                .addProperty("url", "http://example.com/workflows/alignment")
                                .addProperty("version", "0.5.0")
                                .addIdProperty("sdPublisher", "#workflow-hub")
                                .build()
                )
                .build();

        // Similar to the previous example, this example from the specification
        // spared out some details we now need to remove.
        // Here we do not want to remove the license, only the reference to our root data entity.
        // This is because (the way we constructed the crate) other entities use the license as well.
        crate.getRootDataEntity().removeProperty("license");
        crate.getRootDataEntity().removeProperty("datePublished");
        crate.getRootDataEntity().removeProperty("name");
        crate.getRootDataEntity().removeProperty("description");

        printAndAssertEquals(crate, "/spec-v1.1-example-json-files/complete-workflow-example.json");
    }
}
