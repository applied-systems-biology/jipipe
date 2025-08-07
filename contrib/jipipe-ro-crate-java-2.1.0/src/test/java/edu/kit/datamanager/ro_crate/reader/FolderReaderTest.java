package edu.kit.datamanager.ro_crate.reader;

import edu.kit.datamanager.ro_crate.Crate;
import edu.kit.datamanager.ro_crate.HelpFunctions;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.entities.contextual.PersonEntity;
import edu.kit.datamanager.ro_crate.writer.Writers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Nikola Tzotchev on 9.2.2022 г.
 * @version 1
 */
class FolderReaderTest implements CommonReaderTest<String, ReadFolderStrategy>
{
  @Override
  public void saveCrate(Crate crate, Path target) throws IOException {
    Writers.newFolderWriter()
            .withAutomaticProvenance(null)
            .save(crate, target.toAbsolutePath().toString());
    assertTrue(target.toFile().isDirectory());
  }

  @Override
  public Crate readCrate(Path source) throws IOException {
    return Readers.newFolderReader().readCrate(source.toAbsolutePath().toString());
  }

  @Override
  public ReadFolderStrategy newReaderStrategyWithTmp(Path tmpDirectory, boolean useUuidSubfolder) {
    // This strategy does not support a non-default temporary directory
    // and will always use the default one.
    // It also has no state we could make assertions on.
    return new ReadFolderStrategy();
  }

  @Override
  public Crate readCrate(ReadFolderStrategy strategy, Path source) throws IOException {
      return new CrateReader<>(strategy)
          .readCrate(source.toAbsolutePath().toString());
  }

  /**
   * The folder reader is state-less, so we should be able to read multiple crates
   * with the same instance.
   */
  @Test
  void testMultipleReads(@TempDir Path temp1, @TempDir Path temp2) throws IOException {
    String id = "https://orcid.org/0000-0001-6121-5409";
    PersonEntity person = new PersonEntity.PersonEntityBuilder()
            .setId(id)
            .setContactPoint("mailto:tim.luckett@uts.edu.au")
            .setAffiliation("https://ror.org/03f0f6041")
            .setFamilyName("Luckett")
            .setGivenName("Tim")
            .addProperty("name", "Tim Luckett")
            .build();
    RoCrate c1 = new RoCrate.RoCrateBuilder("mini", "test", "2024", "https://creativecommons.org/licenses/by-nc-sa/3.0/au/").build();
    RoCrate c2 = new RoCrate.RoCrateBuilder("other", "with file", "2024", "https://creativecommons.org/licenses/by-nc-sa/3.0/au/")
            .addContextualEntity(person)
            .build();
    this.saveCrate(c1, temp1);
    this.saveCrate(c2, temp2);
    // some first checks...
    assertEquals(0, c1.getAllContextualEntities().size());
    assertEquals(1, c2.getAllContextualEntities().size());
    // read both with the same reader
    CrateReader<String> reader = Readers.newFolderReader();
    RoCrate c1_read = reader.readCrate(temp1.toFile().toString());
    RoCrate c2_read = reader.readCrate(temp2.toFile().toString());
    // check that the reference is not the same
    assertNotEquals(c1, c1_read);
    assertNotEquals(c2, c2_read);
    assertNotEquals(c1_read, c2_read);
    assertEquals(0, c1_read.getAllContextualEntities().size());
    assertEquals(1, c2_read.getAllContextualEntities().size());
    HelpFunctions.compareTwoMetadataJsonNotEqual(c1_read, c2_read);
  }
}
