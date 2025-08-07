package edu.kit.datamanager.ro_crate.singlecratebenchmarks;

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
 * Deletion of entities performance Benchmark.
 */
public class DeletionEntitiesPerformance {

  /**
   * Main class that is called when the benchmark should be started.
   *
   * @param args contain both the amount of entities
   *             as well the location of the Files for the Data entities.
   * @throws IOException when writing the results to a file fails.
   */
  public static void main(String[] args) throws IOException {
    int times = Integer.parseInt(args[0]);
    String baseLocation = args[1];
    deletionEntitiesTest(times, baseLocation);
  }

  /**
   * The method that creates the benchmark.
   *
   * @param numEntities amount of entities in the benchmark.
   * @param baseLocation the base location of the data files.
   * @throws IOException if writing of the results fails.
   */
  public static void deletionEntitiesTest(int numEntities, String baseLocation) throws IOException {

    final Instant start = Instant.now();
    RoCrate crate = new RoCrate.RoCrateBuilder("name", "description", "datePublished", "licenseId").build();
    for (int i = 0; i < numEntities; i++) {
      PersonEntity person = new PersonEntity.PersonEntityBuilder()
          .setId("#id" + i)
          .addProperty("name", "Joe")
          .build();
      DataEntity file = new DataEntity.DataEntityBuilder()
          .setLocationWithExceptions(Paths.get(baseLocation + "file" + i))
          .setId(baseLocation + "file" + i )
          .addType("File")
          .addIdProperty("author", person)
          .build();
      crate.addDataEntity(file);
    }

    for (int i = 0; i < numEntities; i++) {
      crate.deleteEntityById("#id" + i);
    }
    for (int i = 0; i < numEntities; i++) {
      crate.deleteEntityById("file" + i);
    }

    Instant end = Instant.now();
    String duration = String.valueOf(Duration.between(start, end).toMillis() / 1000.f);
    System.out.println("everything deleted in: " + duration + " seconds");
    FileUtils.writeStringToFile(
        new File("deletion_java.txt"), duration + '\n', Charset.defaultCharset(), true);
  }
}
