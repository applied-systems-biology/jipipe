package edu.kit.datamanager.ro_crate.externalproviders.organizationprovider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import edu.kit.datamanager.ro_crate.entities.contextual.OrganizationEntity;
import edu.kit.datamanager.ro_crate.objectmapper.MyObjectMapper;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class providing possibility to import organization entities from ror.com.
 */
public class RorProvider {

    private static final Logger logger = LoggerFactory.getLogger(RorProvider.class);

    private RorProvider() {
    }

    /**
     * The method that parses a ror entry to a crate entity.
     *
     * @param url the url of the ror entry.
     * @return the created Organization entity.
     */
    public static OrganizationEntity getOrganization(String url) {
        if (!url.startsWith("https://ror.org/")) {
            throw new IllegalArgumentException("Should provide ror url");
        }
        String newUrl = "https://api.ror.org/v2/organizations/" + url.replaceAll("https://ror.org/", "");
        HttpGet request = new HttpGet(newUrl);

        try (
                CloseableHttpClient httpClient = HttpClients.createDefault(); CloseableHttpResponse response = httpClient.execute(request)) {
            boolean isError = response.getStatusLine().getStatusCode() != HttpStatus.SC_OK;
            if (isError) {
                String errorMessage = String.format("Identifier not found: %s", response.getStatusLine().toString());
                logger.error(errorMessage);
                return null;
            }
            ObjectNode jsonNode = MyObjectMapper.getMapper().readValue(response.getEntity().getContent(),
                    ObjectNode.class);

            return new OrganizationEntity.OrganizationEntityBuilder()
                    .setId(jsonNode.path("id").asText())
                    .addProperty("name", getOrganizationNameV2(jsonNode.path("names")))
                    .addProperty("email", jsonNode.path("email_address"))
                    .addProperty("url", jsonNode.path("id").asText())
                    .build();
        } catch (IOException e) {
            String errorMessage = String.format("IO error: %s", e.getMessage());
            logger.error(errorMessage);
        }
        return null;
    }

    private static String getOrganizationNameV2(JsonNode node) {
        if (node.isArray()) {
            for (JsonNode n : node) {
                if (n.has("types") && n.path("types").isArray()) {
                    ArrayList<?> l = new ObjectMapper().convertValue(n.path("types"), ArrayList.class);
                    if (l.contains("ror_display")) {
                        return n.path("value").asText();
                    }
                }
            }
            //fallback
            return node.path(0).path("value").asText();
        } else {
            return node.asText();
        }
    }

}
