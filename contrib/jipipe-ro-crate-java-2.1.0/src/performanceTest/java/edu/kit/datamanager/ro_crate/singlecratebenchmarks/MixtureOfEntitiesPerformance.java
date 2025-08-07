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
 * A benchmark for the creation of single crate with a mixture of entities.
 */
public class MixtureOfEntitiesPerformance {

  /**
   * The main file of the benchmark from which it should be run.
   *
   * @param args contains the amount of entities to add
   *             as well as the base location of the data entities.
   * @throws IOException if the writing of the results to a file fails.
   */
  public static void main(String[] args) throws IOException {
    int times = Integer.parseInt(args[0]);
    String baseLocation = args[1];
    mixEntitiesTest(times, baseLocation);
  }

  /**
   * The benchmark method which runs it and times it.
   *
   * @param numEntities the amount of entities to add
   *                    (this amount of data and contextual entities, in total 2 * numEntities)
   * @param baseLocation the base location of the data entities.
   * @throws IOException if the results fail to be written to a file.
   */
  public static void mixEntitiesTest(int numEntities, String baseLocation) throws IOException {
    Instant start = Instant.now();
    RoCrate crate = new RoCrate.RoCrateBuilder("name", "description", "datePublished", "licenseId").build();
    for (int i = 0; i < numEntities; i++) {
      PersonEntity person = new PersonEntity.PersonEntityBuilder()
          .setId("#id" + i)
          .addProperty("name", "Joe")
          .build();
      DataEntity file = new DataEntity.DataEntityBuilder()
          .setLocationWithExceptions(Paths.get(baseLocation + "file" + i))
          .setId(baseLocation + "file" + i)
          .addType("File")
          .addIdProperty("author", person)
          .build();
      crate.addDataEntity(file);
    }
    Instant end = Instant.now();
    String duration = String.valueOf(Duration.between(start, end).toMillis() / 1000.f);
    System.out.println("everything added in: " + duration + " seconds");
    FileUtils.writeStringToFile(
        new File("mix_java.txt"),
        duration + '\n',
        Charset.defaultCharset(),
        true);
  }
}
