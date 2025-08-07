package edu.kit.datamanager.ro_crate.singlecratebenchmarks;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import org.apache.commons.io.FileUtils;

import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.entities.data.DataEntity;
import java.nio.file.Paths;

/**
 * Benchmark for adding data entities to a crate.
 * The data entities are such ones that have a physical file
 * locally present.
 */
public class LocalDataEntitiesPerformance {

  /**
   * The main class of the benchmark that should be called for running it.
   *
   * @param args contains both the amount of entities to add,
   *             and the location they are at.
   * @throws IOException if the writing of the results to a file fails.
   */
  public static void main(String[] args) throws IOException {
    int times = Integer.parseInt(args[0]);
    String baseLocation = args[1];
    localDataEntitiesTest(times, baseLocation);
  }

  /**
   * The method that creates and runs the benchmark.
   *
   * @param numEntities the amount of entities.
   * @param baseLocation their base location (where are the files present).
   * @throws IOException if writing the results at the end fails.
   */
  public static void localDataEntitiesTest(int numEntities, String baseLocation)
      throws IOException {

    Instant start = Instant.now();
    RoCrate crate = new RoCrate.RoCrateBuilder("name", "description", "datePublished", "licenseId").build();
    for (int i = 0; i < numEntities; i++) {
      DataEntity person = new DataEntity.DataEntityBuilder()
          .setLocationWithExceptions(Paths.get(baseLocation + "file" + i))
          .setId(baseLocation + "file" + i)
          .addType("File")
          .build();
      crate.addDataEntity(person);
    }
    Instant end = Instant.now();
    String duration = String.valueOf(Duration.between(start, end).toMillis() / 1000.f);
    System.out.println(
        numEntities + " number of Data local Entities added in: " + duration + " seconds");
    FileUtils.writeStringToFile(
        new File("data_local_java.txt"),
        duration + '\n',
        Charset.defaultCharset(),
        true);
  }
}
