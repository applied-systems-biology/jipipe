package edu.kit.datamanager.ro_crate.externalproviders.dataentities;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.document.JsonDocument;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import edu.kit.datamanager.ro_crate.Crate;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.RoCrate.RoCrateBuilder;
import edu.kit.datamanager.ro_crate.entities.contextual.ContextualEntity;
import edu.kit.datamanager.ro_crate.entities.data.DataEntity;
import edu.kit.datamanager.ro_crate.objectmapper.MyObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/**
 * Class providing functionality for importing a resource from zenodo.
 */
public class ImportFromZenodo {

  /**
   * Class has only static methods, therefore forbid instance creation.
   */
  private ImportFromZenodo() {}

  /**
   * Creating a crate and adding the zenodo resource to it.
   *
   * @param url           the url of the zenodo resource.
   * @param name          the name of the crate created.
   * @param description   the description of the crate created.
   * @param datePublished the published date of the crate.
   * @param licenseId     the license identifier of the crate.
   * @return the created crate.
   */
  public static Crate createCrateWithItem(
      String url,
      Optional<String> name,
      Optional<String> description,
      Optional<String> datePublished,
      Optional<String> licenseId) {

    RoCrateBuilder builder = new RoCrate.RoCrateBuilder();
    name.ifPresent(builder::addName);
    description.ifPresent(builder::addDescription);
    datePublished.ifPresent(builder::addDatePublishedWithExceptions);
    licenseId.ifPresent(builder::setLicense);

    Crate crate = builder.build();
    addToCrateFromZotero(url, crate);
    return crate;
  }

  public static void addZenodoToCrate(String ulr, Crate crate) {
    addToCrateFromZotero(ulr, crate);
  }

  private static void addToCrateFromZotero(String url, Crate crate) {
    ObjectMapper objectMapper = MyObjectMapper.getMapper();
    CloseableHttpClient httpClient = HttpClients.createDefault();
    HttpGet requestZenodo = new HttpGet(url);

    // HttpGet requestSchema= new HttpGet("https://www.researchobject.org/ro-crate/1.1/context.jsonld");
    HttpGet requestSchema = new HttpGet("https://schema.org/docs/jsonldcontext.json");
    requestZenodo.addHeader(HttpHeaders.ACCEPT, "application/ld+json");
    ObjectNode jsonNode;
    String mainId;
    try {
      CloseableHttpResponse response = httpClient.execute(requestZenodo);
      var stream = response.getEntity().getContent();

      // get the item of the main entity
      jsonNode = (ObjectNode) objectMapper.readTree(stream);
      mainId = jsonNode.get("@id").asText();

      final var el = JsonLd.flatten(JsonDocument.of(
              new ByteArrayInputStream(
                  jsonNode.toString().getBytes(StandardCharsets.UTF_8)))).get();

      CloseableHttpResponse schema = httpClient.execute(requestSchema);
      ObjectNode doc = (ObjectNode) objectMapper.readTree(schema.getEntity().getContent());
      var con = (ObjectNode) doc.get("@context");
      con.remove("type");
      con.remove("id");
      doc.set("@context", con);
      var finalVersion = JsonLd.compact(
          JsonDocument.of(el),
          JsonDocument.of(new ByteArrayInputStream(
              doc.toString().getBytes(StandardCharsets.UTF_8)))).get();


      jsonNode = (ObjectNode) objectMapper.readTree(finalVersion.toString());
    } catch (IOException | JsonLdError e) {
      e.printStackTrace();
      return;
    }
    // we don't need this context since we are going to add it to a crate which allready has one
    jsonNode.remove("@context");
    var graph = jsonNode.get("@graph");
    if (graph != null) {
      for (var entity : graph) {
        if (entity.get("@id").asText().equals(mainId)) {
          var dataEntity = new DataEntity.DataEntityBuilder()
              .setAllUnsafe((ObjectNode) entity).build();
          crate.addDataEntity(dataEntity);
        } else {
          // here we have to think of a way to differentiate between data and contextual entities.
          var contextualEntity = new ContextualEntity.ContextualEntityBuilder()
              .setAllUnsafe((ObjectNode) entity).build();
          crate.addContextualEntity(contextualEntity);
        }
      }
    }
  }
}
