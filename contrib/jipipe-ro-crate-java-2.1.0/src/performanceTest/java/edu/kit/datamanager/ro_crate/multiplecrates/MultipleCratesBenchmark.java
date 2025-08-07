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
import java.nio.file.Paths;

/**
 * A benchmark for creating multiple crates with the same entities.
 */
public class MultipleCratesBenchmark {

  /**
   * The main method of the benchmark from where it should be started.
   *
   * @param args contains
   *             the amount of crates
   *             the amount of entities in a crate
   *             the base location of the data entities.
   * @throws IOException if the result fails to be written in the file.
   */
  public static void main(String[] args) throws IOException {
    int crates = Integer.parseInt(args[0]);
    int entities = Integer.parseInt(args[1]);
    String baseLocation = args[2];
    multipleCratesCreation(crates, entities, baseLocation);
  }

  /**
   * The method creating the crates and timing the result.
   *
   * @param numCrates the amount of crates.
   * @param numEntitiesProCrate the number of entities pro crate.
   * @param baseLocation the base location of the data entities.
   * @throws IOException if the writing of the result of a file fails.
   */
  public static void multipleCratesCreation(
      int numCrates, int numEntitiesProCrate, String baseLocation) throws IOException {

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
    }
    Instant end = Instant.now();
    String duration = String.valueOf(Duration.between(start, end).toMillis() / 1000.f);
    System.out.println("time taken: " + duration + " seconds");
    FileUtils.writeStringToFile(
        new File("mul_mix_java.txt"),
        duration + '\n',
        Charset.defaultCharset(),
        true);
  }
}
