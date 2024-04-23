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

package org.hkijena.jipipe.api.registries;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.SystemUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifactDownload;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifactRepositoryReference;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.plugins.artifacts.ArtifactSettings;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.VersionUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class JIPipeArtifactsRegistry {
    private final JIPipe jiPipe;
    private final JIPipeRunnableQueue queue = new JIPipeRunnableQueue("Artifacts");

    public JIPipeArtifactsRegistry(JIPipe jiPipe) {
        this.jiPipe = jiPipe;
    }

    public JIPipe getJiPipe() {
        return jiPipe;
    }

    public JIPipeRunnableQueue getQueue() {
        return queue;
    }

    public List<JIPipeArtifactDownload> queryRepositories(String groupId, String artifactId, String version) {
        Map<String, JIPipeArtifactDownload> downloadMap = new HashMap<>();
        for (JIPipeArtifactRepositoryReference repository : ArtifactSettings.getInstance().getRepositories()) {
            Stack<String> tokens = new Stack<>();
            tokens.add(null);
            while (!tokens.isEmpty()) {
                try {
                    String token = tokens.pop();
                    String urlString = repository.getUrl() + "/service/rest/v1/search/assets?repository=" + repository.getRepository();
                    if (groupId != null) {
                        urlString += "&group=" + groupId;
                    }
                    if (artifactId != null) {
                        urlString += "&name=" + artifactId;
                    }
                    if (version != null) {
                        urlString += "&version=" + version;
                    }
                    if(token != null) {
                        urlString += "&continuationToken" + token;
                    }
                    URL url = new URL(urlString);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Accept", "application/json");

                    if (conn.getResponseCode() != 200) {
                        System.out.println("Failed : HTTP error code : " + conn.getResponseCode());
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
                    if(rootNode.has("items")) {
                        for (JsonNode item : ImmutableList.copyOf(rootNode.get("items").elements())) {
                            JIPipeArtifactDownload download = new JIPipeArtifactDownload();
                            download.setUrl(item.get("downloadUrl").asText());
                            download.setSize(item.get("fileSize").asLong());
                            download.setArtifactId(item.get("maven2").get("artifactId").asText());
                            download.setGroupId(item.get("maven2").get("groupId").asText());
                            download.setClassifier(item.get("maven2").get("classifier").asText());
                            download.setVersion(item.get("maven2").get("version").asText());

                            if(!downloadMap.containsKey(download.getFullId())) {
                                downloadMap.put(download.getFullId(), download);
                            }
                        }
                    }

                    // Continuation token
                    if(rootNode.has("continuationToken") && !rootNode.get("continuationToken").isNull()) {
                        tokens.push(rootNode.get("continuationToken").asText());
                    }

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return downloadMap.values().stream().sorted((o1, o2) -> VersionUtils.compareVersions(o1.getVersion(), o2.getVersion())).collect(Collectors.toList());
    }

    public Path getArtifactsPath() {
        if(ArtifactSettings.getInstance().getOverrideInstallationPath().isEnabled() && !ArtifactSettings.getInstance().getOverrideInstallationPath().getContent().toString().isEmpty()) {
            if(ArtifactSettings.getInstance().getOverrideInstallationPath().getContent().isAbsolute()) {
                return ArtifactSettings.getInstance().getOverrideInstallationPath().getContent();
            }
            else {
                return PathUtils.getJIPipeUserDir().resolve(ArtifactSettings.getInstance().getOverrideInstallationPath().getContent());
            }
        }
        else {
            if (SystemUtils.IS_OS_WINDOWS) {
               return Paths.get(System.getenv("APPDATA")).resolve("JIPipe")
                        .resolve("artifacts");
            } else if (SystemUtils.IS_OS_LINUX) {
                if (System.getProperties().containsKey("XDG_DATA_HOME") && !StringUtils.isNullOrEmpty(System.getProperty("XDG_DATA_HOME"))) {
                    return Paths.get(System.getProperty("XDG_DATA_HOME"))
                            .resolve("JIPipe")
                            .resolve("artifacts");
                } else {
                    return Paths.get(System.getProperty("user.home")).resolve(".local")
                            .resolve("share").resolve("JIPipe")
                            .resolve("artifacts");
                }
            } else if (SystemUtils.IS_OS_MAC_OSX) {
                return Paths.get(System.getProperty("user.home")).resolve("Library").resolve("Application Support")
                        .resolve("JIPipe").resolve("artifacts");
            } else {
                throw new UnsupportedOperationException("Unknown operating system!");
            }
        }
    }
}
