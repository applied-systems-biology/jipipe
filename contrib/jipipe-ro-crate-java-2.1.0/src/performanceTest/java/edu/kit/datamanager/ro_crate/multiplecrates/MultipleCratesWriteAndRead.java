package edu.kit.datamanager.ro_crate.multiplecrates;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import org.apache.commons.io.FileUtils;

import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.entities.contextual.PersonEntity;
import edu.kit.datamanager.ro_crate.entities.data.DataEntity;
import edu.kit.datamanager.ro_crate.reader.FolderReader;
import edu.kit.datamanager.ro_crate.reader.RoCrateReader;
import edu.kit.datamanager.ro_crate.writer.FolderWriter;
import edu.kit.datamanager.ro_crate.writer.RoCrateWriter;
import java.nio.file.Paths;

/**
 * Benchmark for creating multiple crates then writing
 * them and the reading them again.
 */
public class MultipleCratesWriteAndRead {

  /**
   * The main class of the benchmark from where it should be started.
   *
   * @param args contains
   *             the amount of crates
   *             the amount of entities pro crate
   *             the base location of the data entities.
   * @throws IOException if the writing of the results of the crate fails.
   */
  public static void main(String[] args) throws IOException {
    int crates = Integer.parseInt(args[0]);
    int entities = Integer.parseInt(args[1]);
    String baseLocation = args[2];
    readWriteMultiple(crates, entities, baseLocation);
  }

  /**
   * The method creating the benchmark and timing the results.
   *
   * @param numCrates           the amount of crates to be created.
   * @param numEntitiesProCrate the number of entities pro crate.
   * @param baseLocation        the location of the data entities.
   * @throws IOException if writing the result to a file fails.
   */
  public static void readWriteMultiple(int numCrates, int numEntitiesProCrate, String baseLocation)
      throws IOException {

    for (int i = 0; i < numCrates; i++) {
      FileUtils.deleteDirectory(new File("crate" + i));
    }
    Instant start = Instant.now();
    for (int i = 0; i < numCrates; i++) {
      RoCrate crate = new RoCrate.RoCrateBuilder("name", "description", "datePublished", "licenseId").build();
      for (int j = 0; j < numEntitiesProCrate; j++) {
        PersonEntity person = new PersonEntity.PersonEntityBuilder()
            .setId("#id" + i + j)
            .addProperty("name", "Joe")
            .build();
        DataEntity file = new DataEntity.DataEntityBuilder()
            .setLocationWithExceptions(Paths.get(baseLocation + "file" + j))
            .setId(baseLocation + "file" + j)
            .addType("File")
            .addIdProperty("author", person)
            .build();
        crate.addContextualEntity(person);
        crate.addDataEntity(file);
      }
      RoCrateWriter writer = new RoCrateWriter(new FolderWriter());
      writer.save(crate, "crate" + i);
      RoCrateReader reader = new RoCrateReader(new FolderReader());
      @SuppressWarnings("unused")
      RoCrate copy = (RoCrate) reader.readCrate("crate" + i);
    }
    Instant end = Instant.now();
    for (int i = 0; i < numCrates; i++) {
      FileUtils.deleteDirectory(new File("crate" + i));
    }
    String duration = String.valueOf(Duration.between(start, end).toMillis() / 1000.f);
    System.out.println("time taken: " + duration + " seconds");
    FileUtils.writeStringToFile(
        new File("mul_mix_java.txt"),
        duration + '\n',
        Charset.defaultCharset(),
        true);
  }
}
