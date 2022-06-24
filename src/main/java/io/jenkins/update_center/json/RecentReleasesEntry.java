package io.jenkins.update_center.json;

import io.jenkins.update_center.HPI;

public class RecentReleasesEntry {
    private HPI hpi;
    public RecentReleasesEntry(HPI hpi) {
        this.hpi = hpi;
    }

    public String getName() {
        return hpi.artifact.getArtifactName();
    }

    public String getVersion() {
        return hpi.version;
    }
}
