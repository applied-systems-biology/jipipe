package edu.kit.datamanager.ro_crate.externalproviders;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.externalproviders.dataentities.ImportFromDataCite;
import edu.kit.datamanager.ro_crate.objectmapper.MyObjectMapper;
import edu.kit.datamanager.ro_crate.validation.JsonSchemaValidation;
import edu.kit.datamanager.ro_crate.validation.Validator;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DataCiteImportTest {

  @Test
  void dataCiteImportNewCrate() {
    var crate = ImportFromDataCite
        .createCrateFromDataCiteResource(
            "https://api.datacite.org/application/vnd.datacite.datacite+json/10.1594/pangaea.149669",
            Optional.of("importedCrate"),
            Optional.of("description"),
            Optional.of("2024"),
            Optional.of("https://creativecommons.org/licenses/by-nc-sa/3.0/au/"));
    Validator validator = new Validator(new JsonSchemaValidation());
    assertTrue(validator.validate(crate));
  }

  @Test
  void dataCiteImportToExistingCrate() {
    var crate = new RoCrate.RoCrateBuilder("name", "description", "2024", "https://creativecommons.org/licenses/by-nc-sa/3.0/au/").build();
    ImportFromDataCite.addDataCiteToCrate("https://api.datacite.org/application/vnd.datacite.datacite+json/10.1594/pangaea.149669", crate);
    Validator validator = new Validator(new JsonSchemaValidation());
    assertTrue(validator.validate(crate));
  }

  @Test
  void dataCiteImportFromExistingJson() {
    String url = "https://api.datacite.org/application/vnd.datacite.datacite+json/10.5061/dryad.91741";
    CloseableHttpClient httpClient = HttpClients.createDefault();
    HttpGet request = new HttpGet(url);
    ObjectMapper objectMapper = MyObjectMapper.getMapper();
    JsonNode jsonNode;
    try {
      CloseableHttpResponse response = httpClient.execute(request);
      jsonNode = objectMapper.readValue(response.getEntity().getContent(),
          JsonNode.class);

      var crate = ImportFromDataCite.createCrateFromDataCiteJson(
        jsonNode,
        Optional.of("importedCrate"),
        Optional.of("description"),
        Optional.of("2024"),
        Optional.of("https://creativecommons.org/licenses/by-nc-sa/3.0/au/"));
      Validator validator = new Validator(new JsonSchemaValidation());
      assertTrue(validator.validate(crate));

      String anotherUrl = "https://api.datacite.org/application/vnd.datacite.datacite+json/10.5061/dryad.dr554m2";
      request = new HttpGet(anotherUrl);
      response = httpClient.execute(request);
      jsonNode = objectMapper.readValue(response.getEntity().getContent(),
          JsonNode.class);
      assertTrue(validator.validate(crate));

      ImportFromDataCite.addDataCiteToCrateFromJson(jsonNode, crate);

    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
