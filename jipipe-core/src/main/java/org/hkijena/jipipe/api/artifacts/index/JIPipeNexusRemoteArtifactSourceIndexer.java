/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.artifacts.index;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifactRepositoryReference;
import org.hkijena.jipipe.api.artifacts.JIPipeRemoteArtifact;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Stack;

public class JIPipeNexusRemoteArtifactSourceIndexer implements JIPipeRemoteArtifactSourceIndexer {
    @Override
    public void rebuild(JIPipeArtifactRepositoryReference repositoryReference, JIPipeProgressInfo progressInfo) {

    }

    @Override
    public void query(String groupId, String artifactId, String version, JIPipeProgressInfo progressInfo, JIPipeArtifactRepositoryReference repositoryReference, Map<String, JIPipeRemoteArtifact> downloadMap) {
        Stack<String> tokens = new Stack<>();
        tokens.add(null);
        while (!tokens.isEmpty()) {
            try {
                String token = tokens.pop();
                String urlString = repositoryReference.getUrl() + "/service/rest/v1/search/assets?repository=" + repositoryReference.getRepository();
                if (groupId != null) {
                    urlString += "&group=" + groupId;
                }
                if (artifactId != null) {
                    urlString += "&name=" + artifactId;
                }
                if (version != null) {
                    urlString += "&version=" + version;
                }
                if (token != null) {
                    urlString += "&continuationToken" + token;
                }
                progressInfo.log("Contacting " + urlString);
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(1000);
                conn.setReadTimeout(5000);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");

                if (conn.getResponseCode() != 200) {
                    progressInfo.log("Failed : HTTP error code : " + conn.getResponseCode());
                    continue;
                }

                // Read JSON string data
                StringBuilder textBuilder = new StringBuilder();
                try (Reader reader = new BufferedReader(new InputStreamReader
                        (conn.getInputStream(), StandardCharsets.UTF_8))) {
                    int c = 0;
                    while ((c = reader.read()) != -1) {
                        textBuilder.append((char) c);
                    }
                }
                conn.disconnect();

                // Read as JSON
                JsonNode rootNode = JsonUtils.readFromString(textBuilder.toString(), JsonNode.class);

                // Read items
                if (rootNode.has("items")) {
                    for (JsonNode item : ImmutableList.copyOf(rootNode.get("items").elements())) {
                        JIPipeRemoteArtifact download = new JIPipeRemoteArtifact();
                        download.setUrl(item.get("downloadUrl").asText());
                        download.setSize(item.get("fileSize").asLong());
                        download.setArtifactId(item.get("maven2").get("artifactId").asText());
                        download.setGroupId(item.get("maven2").get("groupId").asText());
                        download.setClassifier(item.get("maven2").get("classifier").asText());
                        download.setVersion(item.get("maven2").get("version").asText());

                        if (!downloadMap.containsKey(download.getFullId())) {
                            downloadMap.put(download.getFullId(), download);
                            progressInfo.log("Found " + download.getFullId());
                        }
                    }
                }

                // Continuation token
                if (rootNode.has("continuationToken") && !rootNode.get("continuationToken").isNull()) {
                    tokens.push(rootNode.get("continuationToken").asText());
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
