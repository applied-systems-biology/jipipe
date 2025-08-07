package edu.kit.datamanager.ro_crate.singlecratebenchmarks;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import org.apache.commons.io.FileUtils;

import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.entities.data.DataEntity;

/**
 * Benchmark for adding remote data entities to a crate.
 * Remote data entities are such that contain to local
 * physical file that should be added in the crate.
 */
public class RemoteDataEntitiesPerformance {

  /**
   * The main method of the benchmark from where it should be started.
   *
   * @param args contains both
   *             the amount of data entities to add
   *             and the baseId for these entities.
   * @throws IOException if writing the result to a file fails.
   */
  public static void main(String[] args) throws IOException {
    int times = Integer.parseInt(args[0]);
    String baseId = args[1];
    remoteDataEntitiesTest(times, baseId);
  }

  private static void remoteDataEntitiesTest(int numEntities, String baseId) throws IOException {

    Instant start = Instant.now();
    RoCrate crate = new RoCrate.RoCrateBuilder("name", "description", "datePublished", "licenseId").build();
    for (int i = 0; i < numEntities; i++) {
      DataEntity person = new DataEntity.DataEntityBuilder()
          .setId(baseId + i)
          .addType("File")
          .build();
      crate.addDataEntity(person);
    }
    Instant end = Instant.now();
    String duration = String.valueOf(Duration.between(start, end).toMillis() / 1000.f);
    System.out.println(
        numEntities + " number of Data Remote Entities added in: " + duration + " seconds");
    FileUtils.writeStringToFile(
        new File("data_remote_java.txt"),
        duration + '\n',
        Charset.defaultCharset(),
        true);
  }

}
