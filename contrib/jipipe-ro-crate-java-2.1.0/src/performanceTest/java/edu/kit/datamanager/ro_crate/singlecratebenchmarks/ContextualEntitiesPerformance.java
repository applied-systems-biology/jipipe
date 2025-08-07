package edu.kit.datamanager.ro_crate.singlecratebenchmarks;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import org.apache.commons.io.FileUtils;

import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.entities.contextual.PersonEntity;

/**
 * Class for the contextual entity performance benchmark.
 */
public class ContextualEntitiesPerformance {

  /**
   * The main method of this benchmark.
   * It is the things that starts it.
   *
   * @param args contains both the amount of entities
   *            and the baseId of the contextual entities that will be added.
   * @throws IOException when reading the file where the results are stored fails.
   */
  public static void main(String[] args) throws IOException {
    int times =  Integer.parseInt(args[0]);
    String baseId =  args[1];
    contextualEntitiesTest(times, baseId);
  }

  /**
   * This is the method that executed the benchmark here both the crate and entities are created.
   *
   * @param numEntities the amount of entities that should be added in the Benchmark.
   * @param baseId the base id for the contextual entities ID's (often https://www.example.com)
   * @throws IOException when the writing of the results to a file fails.
   */
  public static void contextualEntitiesTest(int numEntities, String baseId) throws IOException {

    Instant start = Instant.now();
    RoCrate crate = new RoCrate.RoCrateBuilder("name", "description", "datePublished", "licenseId").build();
    for (int i = 0; i < numEntities; i++) {
      PersonEntity person = new PersonEntity.PersonEntityBuilder()
          .setId(baseId + i)
          .addProperty("name", "Joe Bloggs")
          .build();
      crate.addContextualEntity(person);
    }
    Instant end = Instant.now();
    String duration = String.valueOf(Duration.between(start, end).toMillis() / 1000.f);
    System.out.println(
        numEntities + " number of Contextual Entities added in: " + duration + " seconds");
    FileUtils.writeStringToFile(
        new File("contextual_java.txt"), duration + '\n', Charset.defaultCharset(), true);
  }
}
