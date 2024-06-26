package io.jenkins.update_center.json;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.google.common.collect.Maps;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jenkins.update_center.Deprecations;
import io.jenkins.update_center.MavenRepository;
import io.jenkins.update_center.Plugin;
import io.jenkins.update_center.PluginUpdateCenterEntry;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class UpdateCenterRoot extends WithSignature {
    @JSONField
    @SuppressFBWarnings(value = "SS_SHOULD_BE_STATIC", justification = "Accessed by JSON serializer")
    public final String updateCenterVersion = "1";

    @JSONField
    public String connectionCheckUrl = "http://www.google.com/"; // TODO pass in command line arg

    @JSONField
    public String id = "default"; // TODO pass in command line arg

    @JSONField
    public UpdateCenterCore core;

    @JSONField
    public Map<String, PluginUpdateCenterEntry> plugins = new TreeMap<>();

    @JSONField
    public List<UpdateCenterWarning> warnings;

    @JSONField
    public Map<String, UpdateCenterDeprecation> deprecations;

    public UpdateCenterRoot(MavenRepository repo, File warningsJsonFile) throws IOException {
        // load warnings
        final String warningsJsonText = String.join("", Files.readAllLines(warningsJsonFile.toPath(), StandardCharsets.UTF_8));
        warnings = Arrays.asList(JSON.parseObject(warningsJsonText, UpdateCenterWarning[].class));

        // load deprecations
        deprecations = Maps.newHashMap();

        for (Plugin plugin : repo.listJenkinsPlugins()) {
            PluginUpdateCenterEntry entry = PluginUpdateCenterEntry.create(plugin);
            plugins.put(plugin.getArtifactId(), entry);
        }

        core = new UpdateCenterCore(repo.getJenkinsWarsByVersionNumber());
    }

    private static UpdateCenterDeprecation deprecationForPlugin(String artifactId) {
        return new UpdateCenterDeprecation(Deprecations.getCustomDeprecationUri(artifactId));
    }
}
