package edu.kit.datamanager.ro_crate.externalproviders.personprovider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import edu.kit.datamanager.ro_crate.entities.contextual.PersonEntity;
import edu.kit.datamanager.ro_crate.entities.validation.EntityValidation;
import edu.kit.datamanager.ro_crate.entities.validation.JsonSchemaValidation;
import edu.kit.datamanager.ro_crate.objectmapper.MyObjectMapper;

import java.io.IOException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for creating person entities from orcid uri.
 *
 * @author Nikola Tzotchev on 10.2.2022 г.
 * @version 1
 */
public class OrcidProvider {

  private static Logger logger = LoggerFactory.getLogger(OrcidProvider.class);

  private OrcidProvider() {}

  /**
   * Static method for importing a person entity from his ORCID id.
   *
   * @param url the url of the orcid identifier of the person.
   * @return the created PersonEntity.
   */
  public static PersonEntity getPerson(String url) {
    if (!url.startsWith("https://orcid.org")) {
      throw new IllegalArgumentException("Should provide orcid url");
    }
    HttpGet request = new HttpGet(url);
    request.addHeader(HttpHeaders.ACCEPT, "application/ld+json");

    try (
      CloseableHttpClient httpClient = HttpClients.createDefault();
      CloseableHttpResponse response = httpClient.execute(request);
    ) {
      boolean isError = response.getStatusLine().getStatusCode() != HttpStatus.SC_OK;
      String receivedMimeType = ContentType.parse(response.getFirstHeader(HttpHeaders.CONTENT_TYPE).getValue()).getMimeType();
      boolean isUnexpectedFormat = response.containsHeader(HttpHeaders.CONTENT_TYPE)
        && !receivedMimeType.equals("application/ld+json");
      if (isError || isUnexpectedFormat) {
        String errorMessage = String.format("Identifier not found: %s", response.getStatusLine().toString());
        logger.error(errorMessage);
        return null;
      }

      ObjectMapper objectMapper = MyObjectMapper.getMapper();
      ObjectNode jsonNode = objectMapper.readValue(response.getEntity().getContent(),
          ObjectNode.class);
      jsonNode.remove("@reverse");
      jsonNode.remove("@context");
      ObjectNode node = objectMapper.createObjectNode();
      EntityValidation entityValidation = new EntityValidation(new JsonSchemaValidation());
      var itr = jsonNode.fields();
      while (itr.hasNext()) {
        var element = itr.next();
        if (entityValidation.fieldValidation(element.getValue())) {
          node.set(element.getKey(), element.getValue());
        }
      }
      return new PersonEntity.PersonEntityBuilder().setAllUnsafe(node).build();
    } catch (IOException e) {
      String errorMessage = String.format("IO error: %s", e.getMessage());
      logger.error(errorMessage);
    }
    return null;
  }
}
