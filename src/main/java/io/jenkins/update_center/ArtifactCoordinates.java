package io.jenkins.update_center;

import com.qlangtech.tis.maven.plugins.tpi.PluginClassifier;

import java.util.Objects;
import java.util.Optional;

public class ArtifactCoordinates {

    public final String groupId;
    private final String artifactId;
    public final String version;
    public final String packaging;

    public boolean findParent;

    public final Optional<PluginClassifier> classifier;

    public String getArtifactName() {
        if (classifier.isPresent()) {
            return classifier.get().getTPIPluginName(this.artifactId);
        } else {
            return artifactId;
        }
    }

    public String getArtifactId() {
        return artifactId;
    }

    public static ArtifactCoordinates create(String groupId, String artifactId, String version, String packaging) {
        return new ArtifactCoordinates(groupId, artifactId, version, packaging);
    }

    public static ArtifactCoordinates create(ArtifactCoordinates artifact, String packaging) {
        return create(artifact, packaging, false);
    }

    public static ArtifactCoordinates create(ArtifactCoordinates artifact, String packaging, boolean findParent) {
        return new ArtifactCoordinates(artifact.groupId, artifact.artifactId, artifact.version, packaging, artifact.classifier, findParent);
    }

    private ArtifactCoordinates(String groupId, String artifactId, String version, String packaging) {
        this(groupId, artifactId, version, packaging, Optional.empty(), false);
    }

    public ArtifactCoordinates(String groupId, String artifactId
            , String version, String packaging, Optional<PluginClassifier> classifier, boolean findParent) {
        if (groupId == null) {
            throw new IllegalArgumentException("param groupId can not be null");
        }
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.packaging = packaging;
        this.findParent = findParent;
        this.classifier = classifier;
    }

    public String getGav() {
        StringBuffer gav = new StringBuffer();
        gav.append(groupId);
        gav.append(":");
        gav.append(artifactId);
        gav.append(":");
        gav.append(version);
        if (classifier.isPresent()) {
            gav.append(":").append(classifier.get().getClassifier());
        }
        return gav.toString();
    }

    public String toString() {
        // return groupId + ":" + artifactId + ":" + version + ":" + packaging;
        return getGav() + ":" + packaging;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArtifactCoordinates that = (ArtifactCoordinates) o;
        boolean equal = Objects.equals(groupId, that.groupId) &&
                Objects.equals(artifactId, that.artifactId) &&
                Objects.equals(version, that.version) &&
                Objects.equals(packaging, that.packaging);
        if (equal) {
            if (this.classifier.isPresent() && that.classifier.isPresent()) {
                return Objects.equals(this.classifier.get(), that.classifier.get());
            }
            if (this.classifier.isPresent() ^ that.classifier.isPresent()) {
                return false;
            }
        }

        return equal;
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, version, packaging);
    }
}
