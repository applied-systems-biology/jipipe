package edu.kit.datamanager.ro_crate.examples;

import edu.kit.datamanager.ro_crate.HelpFunctions;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.entities.contextual.ContextualEntity;
import edu.kit.datamanager.ro_crate.entities.contextual.OrganizationEntity;
import edu.kit.datamanager.ro_crate.entities.contextual.PersonEntity;
import edu.kit.datamanager.ro_crate.entities.data.DataEntity;
import edu.kit.datamanager.ro_crate.entities.data.FileEntity;
import edu.kit.datamanager.ro_crate.externalproviders.organizationprovider.RorProvider;
import edu.kit.datamanager.ro_crate.externalproviders.personprovider.OrcidProvider;
import edu.kit.datamanager.ro_crate.preview.AutomaticPreview;
import edu.kit.datamanager.ro_crate.preview.StaticPreview;
import edu.kit.datamanager.ro_crate.reader.CrateReader;
import edu.kit.datamanager.ro_crate.reader.GenericReaderStrategy;
import edu.kit.datamanager.ro_crate.reader.Readers;
import edu.kit.datamanager.ro_crate.validation.JsonSchemaValidation;
import edu.kit.datamanager.ro_crate.validation.Validator;
import edu.kit.datamanager.ro_crate.writer.CrateWriter;
import edu.kit.datamanager.ro_crate.writer.WriteFolderStrategy;
import edu.kit.datamanager.ro_crate.writer.GenericWriterStrategy;
import edu.kit.datamanager.ro_crate.writer.Writers;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This class is meant to be a small example-driven introduction to the ro-crate-java library.
 * It is meant to be read from top to bottom.
 */
public class LearnByExampleTest {

    /**
     * This creates a valid, empty RO-Crate builder.
     */
    static RoCrate.RoCrateBuilder NEW_STARTER_CRATE() {
        return new RoCrate.RoCrateBuilder(
                "name",
                "description",
                "2025",
                "licenseIdentifier"
        );
    }

    /**
     * Calling the `build()` method on the builder creates a valid RO-Crate.
     * Run this test to view the NEW_STARTER_CRATE() JSON in the console.
     */
    @Test
    void aSimpleCrate() {
        RoCrate almostEmptyCrate = NEW_STARTER_CRATE().build();
        assertNotNull(almostEmptyCrate);
        HelpFunctions.prettyPrintJsonString(almostEmptyCrate.getJsonMetadata());
    }

    /**
     * This is how we can add things to a crate.
     * <p>
     * Note that methods starting with `add` can be used multiple times to add more.
     * For example, we can add multiple files or multiple contexts.
     * <p>
     * On the other hand, methods starting with `set` will override previous calls.
     * <p>
     * There may be inconsistencies yet, which are tracked here: <a href="https://github.com/kit-data-manager/ro-crate-java/issues/242">Issue #242</a>
     */
    @Test
    void addingYourFirstEntity() {
        RoCrate myFirstCrate = NEW_STARTER_CRATE()
                // We can add new terms to our crate. The terms we can use are called "context".
                .addValuePairToContext("Station", "www.station.com")
                // We can also add whole contexts to our crate.
                .addUrlToContext("contextUrl")
                // Let's add a file to our crate.
                .addDataEntity(
                        new FileEntity.FileEntityBuilder()
                                // For files (or folders, which are DataSetEntities),
                                // the ID determines the file name in the crate.
                                .setId("survey-responses-2019.csv")
                                // This is where we get the file from. The path will not be part of the metadata.
                                .setLocation(Paths.get("copy/from/this/file-and-rename-it.csv"))
                                // And now, the remaining metadata.
                                // Note that "name", "contentSize", and "encodingFormat"
                                // are already defined in our default context.
                                .addProperty("name", "Survey responses")
                                .addProperty("contentSize", "26452")
                                .addProperty("encodingFormat", "text/csv")
                                .build()
                )
                // We could add more, but let's keep it simple for now.
                //.addDataEntity(...)
                //.addContextualEntity(...)
                //...
                .build();

        assertNotNull(myFirstCrate);
        HelpFunctions.prettyPrintJsonString(myFirstCrate.getJsonMetadata());
    }

    /**
     * The library currently comes with three specialized DataEntities:
     * <p>
     * 1. `DataSetEntity`
     * 2. `FileEntity` (used in the example above)
     * 3. `WorkflowEntity`
     * <p>
     * If another type of `DataEntity` is required,
     * the base class `DataEntity` can be used. Example:
     */
    @Test
    void specializingYourFirstEntity() {
        RoCrate crate = NEW_STARTER_CRATE()
                .addDataEntity(
                        // Let's do something custom:
                        new DataEntity.DataEntityBuilder()
                                // You need to add the type of your `DataEntity`
                                // because for DataEntity, there is no default.
                                .addType("CreativeWork")
                                .setId("myEntityInstance")
                                // Now that we are a CreativeWork instance,
                                // it is fine to use some of its properties.
                                .addProperty("https://schema.org/award", "Wow-award")
                                .build()
                )
                .build();

        assertNotNull(crate);
        HelpFunctions.prettyPrintJsonString(crate.getJsonMetadata());
    }

    /**
     * A `DataEntity` and its subclasses can have a file located on the web.
     * In this case, it does not need to reside in a crate's folder.
     * This can be useful for large, publicly available files,
     * or in order to reuse or share files.
     * <p>
     * Note: Technically, an entity pointing to a file on the web is just an entity
     * that uses the URL as an ID.
     */
    @Test
    void referencingFilesOnTheWeb() {
        // Let's say this is the file we would like to point at with an entity.
        String lovelyFile = "https://github.com/kit-data-manager/ro-crate-java/issues/5";

        RoCrate crate = NEW_STARTER_CRATE()
                .addDataEntity(
                        // Build our entity to point to the file:
                        new FileEntity.FileEntityBuilder()
                                // Make it point to an external file.
                                .setLocation(URI.create(lovelyFile))
                                // This would do the same:
                                .setId(lovelyFile)
                                // don't forget to add metadata!
                                .addProperty("description", "my new file that I added")
                                .build()
                )
                .build();

        assertNotNull(crate);
        HelpFunctions.prettyPrintJsonString(crate.getJsonMetadata());
    }

    /**
     * A `DataEntity` and its subclasses can have a local file associated with them,
     * instead of one located on the web.
     *
     * @param tempDir We'll use this to create a temporary folder for our crate.
     * @throws IOException If the file cannot be created or written to.
     */
    @Test
    void includingFilesIntoTheCrateFolder(@TempDir Path tempDir) throws IOException {
        // Let's say this is the file we would like to point at with an entity.
        String lovelyFile = tempDir.resolve("my/experiment.csv").toString();
        {
            // (Let's quickly create a dummy file, but the rest will not make use of this knowledge.)
            File lovelyFilePointer = new File(lovelyFile);
            FileUtils.touch(lovelyFilePointer);
            FileUtils.write(lovelyFilePointer, "My great experiment 001", "UTF-8");
        }

        // But in the crate we want it to be
        String seriousExperimentFile = "fantastic-experiment/2025-01-01.csv";

        RoCrate crate = NEW_STARTER_CRATE()
                .addDataEntity(
                        // Build our entity to point to the file:
                        new FileEntity.FileEntityBuilder()
                                // Let's tell the library where to find and copy the file from.
                                .setLocation(Paths.get(lovelyFile))
                                // Let's tell it to adjust the file name and path in the crate.
                                .setId(seriousExperimentFile)
                                .addProperty("description", "my new local file that I added")
                                .build()
                )
                .build();

        assertNotNull(crate);
        HelpFunctions.prettyPrintJsonString(crate.getJsonMetadata());

        // Let's write it to disk and see if the file is there!
        // (We'll discuss writing and reading crates later on.)
        Path crateFolder = tempDir.resolve("myCrate");
        Writers.newFolderWriter().save(crate, crateFolder.toString());
        assertTrue(crateFolder.resolve(seriousExperimentFile).toFile().exists());
    }

    /**
     * Contextual entities cannot be associated with a file: they are pure metadata
     * To add a contextual entity to a crate you use the function
     * {@link RoCrate.RoCrateBuilder#addContextualEntity(ContextualEntity)}.
     * <p>
     * Some types of derived/specializes entities are:
     * <p>
     * 1. `OrganizationEntity`
     * 2. `PersonEntity`
     * 3. `PlaceEntity`
     * <p>
     * If you need another type of contextual entity, use the base class
     * {@link ContextualEntity}, similar to how we did it in
     * {@link #specializingYourFirstEntity()}.
     * <p>
     * The library provides a way to automatically create contextual entities from
     * external providers. Currently, support for [ORCID](https://orcid.org/) and
     * [ROR](https://ror.org/) is implemented.
     * Check the module {@link edu.kit.datamanager.ro_crate.externalproviders} for
     * more implementations.
     */
    @Test
    void addingContextualEntities() {
        PersonEntity person = OrcidProvider.getPerson("https://orcid.org/0000-0001-6575-1022");
        OrganizationEntity organization = RorProvider.getOrganization("https://ror.org/04t3en479");

        RoCrate crate = NEW_STARTER_CRATE()
                .addContextualEntity(person)
                .addContextualEntity(organization)
                .build();

        assertNotNull(crate);
        HelpFunctions.prettyPrintJsonString(crate.getJsonMetadata());
    }

    /**
     * RO-Crates are file based, but in your application you may want to create a crate
     * on the fly and directly send it somewhere else without storing it on disk.
     * This is why we can't only write to a folder or a zip file, but also to a stream
     * (containing the zip file).
     * <p>
     * There is a generic interface to implement Writers (and Readers), so even more
     * exotic use cases should be possible. The readers work the same way.
     * <p>
     * - {@link GenericWriterStrategy}
     * - {@link GenericReaderStrategy}
     */
    @Test
    void writingAndReadingCrates(@TempDir Path tempDir) throws IOException {
        // Ok lets make a small, but not fully boring crate.
        PersonEntity person = OrcidProvider.getPerson("https://orcid.org/0000-0001-6575-1022");
        OrganizationEntity organization = RorProvider.getOrganization("https://ror.org/04t3en479");

        RoCrate crate = NEW_STARTER_CRATE()
                .addContextualEntity(person)
                .addContextualEntity(organization)
                .build();

        assertNotNull(crate);
        HelpFunctions.prettyPrintJsonString(crate.getJsonMetadata());

        {
            // Now, let's write it to a folder.
            Path folder = tempDir.resolve("folderCrate");
            Writers.newFolderWriter()
                    .save(crate, folder.toString());
            // and read it back.
            RoCrate read = Readers.newFolderReader()
                    .readCrate(folder.toAbsolutePath().toString());

            HelpFunctions.compareTwoCrateJson(crate, read);
        }

        {
            // Now, let's write it to a zip file.
            Path zipFile = tempDir.resolve("zipCrate.zip");
            Writers.newZipPathWriter()
                    .save(crate, zipFile.toString());
            // and read it back.
            RoCrate read = Readers.newZipPathReader()
                    .readCrate(zipFile.toAbsolutePath().toString());

            HelpFunctions.compareTwoCrateJson(crate, read);
        }

        {
            // Now, let's write it to a zip stream.
            Path zipStreamFile = tempDir.resolve("zipStreamCrate.zip");
            try (OutputStream outputStream = new FileOutputStream(zipStreamFile.toFile())) {
                Writers.newZipStreamWriter().save(crate, outputStream);
            }
            // and read it back.
            try (InputStream inputStream = new FileInputStream(zipStreamFile.toFile())) {
                RoCrate read = Readers.newZipStreamReader()
                        .readCrate(inputStream);

                HelpFunctions.compareTwoCrateJson(crate, read);
            }
        }
    }

    /**
     * In {@link #writingAndReadingCrates(Path)} we already saw how to write or read
     * a crate. We used the Readers and Writers classes to get the available options.
     * But what if you want to write your own reader or writer strategy?
     * <p>
     * Let's see how you can make a reader or writer, manually configuring the strategy.
     */
    @Test
    void writingAndReadingStrategies(@TempDir Path tempDir) throws IOException {
        // Ok lets make a small, but not fully boring crate.
        PersonEntity person = OrcidProvider.getPerson("https://orcid.org/0000-0001-6575-1022");
        OrganizationEntity organization = RorProvider.getOrganization("https://ror.org/04t3en479");

        RoCrate crate = NEW_STARTER_CRATE()
                .addContextualEntity(person)
                .addContextualEntity(organization)
                .build();

        assertNotNull(crate);
        HelpFunctions.prettyPrintJsonString(crate.getJsonMetadata());

        // Now, let's write it to a folder. Note the used strategy could be replaced with your own.
        Path folder = tempDir.resolve("folderCrate");
        new CrateWriter<>(new WriteFolderStrategy())
                .save(crate, folder.toString());
        // and read it back.
        RoCrate read = new CrateReader<>(
                // Note: There are two WriteFolderStrategy implementations, one for reading and one for writing.
                // Java is a bit bad with imports, so we use the fully qualified name here.
                new edu.kit.datamanager.ro_crate.reader.ReadFolderStrategy()
        )
                .readCrate(folder.toAbsolutePath().toString());

        HelpFunctions.compareTwoCrateJson(crate, read);
    }

    /**
     * RO-Crate specified there should be a human-readable preview of the crate.
     * This is a HTML file that can be opened in a browser.
     * ro-crate-java offers three different ways to create this file:
     * <p>
     * - AutomaticPreview: Uses third-party library
     *   <a href="https://www.npmjs.com/package/ro-crate-html-js">ro-crate-html-js</a>,
     *   which must be installed separately via `npm install --global ro-crate-html-js`.
     *   <p>
     * - CustomPreview: Pure Java-based preview using an included template processed by
     *   the FreeMarker template engine. At the same time, CustomPreview is the fallback
     *   for AutomaticPreview if ro-crate-html-js is not installed.
     *   <p>
     * - StaticPreview: Allows to provide a static HTML page (including additional
     *   dependencies, e.g., CSS, JS) which is then shipped with the RO-Crate.
     * <p>
     * When creating a new RO-Crate using the builder, the default setting is to use
     * CustomPreview. This example shows you how to change it.
     */
    @Test
    void humanReadableContent() {
        RoCrate crate = NEW_STARTER_CRATE()
                .setPreview(new AutomaticPreview())
                .build();

        assertNotNull(crate);
    }

    /**
     * A static preview means you'll just add your own HTML file to the crate.
     * Therefore, the constructor is a bit more complicated.
     */
    @Test
    void staticPreview(@TempDir Path tempDir) throws IOException {
        File mainPreviewHtml = tempDir.resolve("mainPreview.html").toFile();
        File additionalFilesDirectory = tempDir.resolve("additionalFiles").toFile();
        FileUtils.forceMkdir(additionalFilesDirectory);
        FileUtils.touch(mainPreviewHtml);

        RoCrate crate = NEW_STARTER_CRATE()
                .setPreview(new StaticPreview(mainPreviewHtml, additionalFilesDirectory))
                .build();

        assertNotNull(crate);
    }

    /**
     * Crates can be validated.
     * Right now, the only implemented way of validating a RO-crate is to use a
     * [JSON-Schema](https://json-schema.org/) that the crate's metadata JSON file should
     * match. JSON-Schema is an established standard and therefore a good choice for a
     * crate profile. This example shows how to use it.
     * <p>
     * Note: If you happen to implement your own validator anyway, please consider
     * contributing your code!
     */
    @Test
    void validation() {
        // Let's find a schema file in the resources folder.
        URL schemaUrl = Objects.requireNonNull(this.getClass().getResource("/crates/validation/workflowschema.json"));
        String schemaPath = schemaUrl.getPath();

        // This crate for sure is not a workflow, so validation will fail.
        RoCrate crate = NEW_STARTER_CRATE().build();

        // And now do the validation.
        Validator validator = new Validator(new JsonSchemaValidation(schemaPath));
        assertFalse(validator.validate(crate));
    }
}
