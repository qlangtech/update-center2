package io.jenkins.update_center;

import com.alibaba.fastjson.annotation.JSONField;
import com.google.common.collect.Lists;
import com.qlangtech.tis.maven.plugins.tpi.PluginClassifier;
import hudson.util.VersionNumber;
import io.jenkins.update_center.util.JavaSpecificationVersion;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

/**
 * An entry of a plugin in the update center metadata.
 */
public abstract class PluginUpdateCenterEntry {
    /**
     * Plugin artifact ID.
     */
    @JSONField(name = "name")
    public final String artifactId;

    /**
     * Latest version of this plugin.
     */
    private transient final HPI latestOffered;

    // private List<ICoord> _classifier;
    // private boolean _supportMultiClassifier;
    public static PluginUpdateCenterEntry create(Plugin plugin) {
        TreeMap<PluginClassifier, HPI> artifacts = plugin.getArtifacts();
        if (artifacts.size() == 1) {
            for (Map.Entry<PluginClassifier, HPI> e : artifacts.entrySet()) {
                if (PluginClassifier.isNoneClassifier(e.getKey())) {
                    return new NoneClassifierPluginUpdateCenterEntry(plugin, e.getValue());
                }
            }
        }

        if (artifacts.size() > 1) {
            List<ICoord> coords = Lists.newArrayList();
            ICoord coord = null;
            HPI latest = null;
            for (Map.Entry<PluginClassifier, HPI> e : artifacts.entrySet()) {
                latest = e.getValue();
                coord = new DefaultCoord(e.getValue());
                coords.add(coord);
            }
            return new MultiClassifierPluginUpdateCenterEntry(plugin, coords, latest);
        }


        throw new IllegalStateException("illegal artifacts,size:" + artifacts.size());

    }

    private static class NoneClassifierPluginUpdateCenterEntry extends PluginUpdateCenterEntry implements ICoord {

        private transient final HPI hpi;

        public NoneClassifierPluginUpdateCenterEntry(Plugin plugin, HPI hpi) {
            super(plugin.getArtifactId(), hpi);
            this.hpi = hpi;
        }

        @JSONField(name = "url")
        public URL getDownloadUrl() throws MalformedURLException {
            return hpi.getDownloadUrl();
        }

        @Override
        public String getSha1() throws IOException {
            return hpi.getMetadata().sha1;
        }

        @Override
        public String getSha256() throws IOException {
            return hpi.getMetadata().sha256;
        }

        @Override
        public long getSize() throws IOException {
            return hpi.getMetadata().size;
        }

        @Override
        public String getGav() {
            return hpi.getGavId();
        }

    }

    private static class MultiClassifierPluginUpdateCenterEntry extends PluginUpdateCenterEntry {

        private List<ICoord> coords;

        public MultiClassifierPluginUpdateCenterEntry(Plugin plugin, List<ICoord> coords, HPI latest) {
            super(plugin.getArtifactId(), latest);
            this.coords = coords;
        }

        @JSONField(name = "classifier")
        public List<ICoord> getCoords() {
            return this.coords;
        }
    }

    /**
     * Previous version of this plugin.
     */
//    @CheckForNull
//    private transient final HPI previousOffered;
//    private PluginUpdateCenterEntry(String artifactId, HPI latestOffered, HPI previousOffered) {
//        this.artifactId = artifactId;
//        this.latestOffered = latestOffered;
////        this.previousOffered = previousOffered;
//    }
    public PluginUpdateCenterEntry(String artifactId, HPI latestOffered) {
        this.artifactId = artifactId;//plugin.getArtifactId();
//        HPI latest = null;
//
//        Iterator<HPI> it = plugin.getArtifacts().values().iterator();
//
//        boolean supportMultiClassifier = false;
//        Optional<PluginClassifier> classifier = null;
//        while (it.hasNext()) {
//            HPI h = it.next();
//            try {
//                h.getManifest();
//            } catch (IOException e) {
//                LOGGER.log(Level.WARNING, "Failed to resolve " + h + ". Dropping this version.", e);
//                continue;
//            }
//            latest = h;
//            classifier = h.getClassifier();
//            if (classifier.isPresent()) {
//                supportMultiClassifier = true;
//            }
//            if (supportMultiClassifier) {
//                //       _classifier.add(classifier.get());
//            }
        //  }
        // this._supportMultiClassifier = supportMultiClassifier;
        this.latestOffered = latestOffered;
//        while (previous == null && it.hasNext()) {
//            HPI h = it.next();
//            try {
//                h.getManifest();
//            } catch (IOException e) {
//                LOGGER.log(Level.WARNING, "Failed to resolve " + h + ". Dropping this version.", e);
//                continue;
//            }
//            previous = h;
//        }


        // this.previousOffered = previous == latest ? null : previous;
    }

//    public PluginUpdateCenterEntry(HPI hpi) {
//        this(hpi.artifact.getArtifactName(), hpi, null);
//    }


    /**
     * "classifier":[
     * "hudi_${hudi.version};spark_${spark2.version};hive_${hive.version};hadoop_${hadoop-version}"
     * ],
     *
     * @return
     */
//    @JSONField(name = "classifier")
//    public List<ICoord> getClassifier() {
//        return this._classifier;
//    }

//    public void addClassifier(String val) {
//        if (_classifier == null) {
//            this._classifier = Lists.newArrayList();
//        }
//        _classifier.add(val);
//    }

    /**
     * Historical name for the plugin documentation URL field.
     * <p>
     * Now always links to plugins.jenkins.io, which in turn uses
     * {@link io.jenkins.update_center.json.PluginDocumentationUrlsRoot} to determine where the documentation is
     * actually located.
     *
     * @return a URL
     */
    @JSONField
    public String getWiki() {
        return "https://plugins.jenkins.io/" + artifactId;
    }

    String getPluginUrl() throws IOException {
        return latestOffered.getPluginUrl();
    }


    @JSONField(name = "title")
    public String getName() throws IOException {
        return latestOffered.getName();
    }

    public String getVersion() {
        return latestOffered.version;
    }


    interface ICoord {
        //  public String getClassifier();

        @JSONField(name = "url")
        public URL getDownloadUrl() throws MalformedURLException;

        public String getSha1() throws IOException;

        public String getSha256() throws IOException;

        public long getSize() throws IOException;

        public String getGav();
    }

    private static class DefaultCoord implements ICoord {
        private final HPI hpi;
        private final PluginClassifier classifier;

        public DefaultCoord(HPI hpi) {
            this.hpi = hpi;
            Optional<PluginClassifier> c = hpi.getClassifier();
            if (!c.isPresent()) {
                throw new IllegalStateException("hpi:" + hpi.toString() + "the classifier must be present");
            }
            this.classifier = c.get();
        }

        @JSONField(name = "classifier")
        public String getClassifier() {
            return classifier.getClassifier();
        }

        @Override
        public URL getDownloadUrl() throws MalformedURLException {
            return hpi.getDownloadUrl();
        }

        @Override
        public String getSha1() throws IOException {
            return hpi.getMetadata().sha1;
        }

        @Override
        public String getSha256() throws IOException {
            return hpi.getMetadata().sha256;
        }

        @Override
        public long getSize() throws IOException {
            return hpi.getMetadata().size;
        }

        @Override
        public String getGav() {
            return hpi.getGavId();
        }
    }

    // public String getPreviousVersion() {
    //    return previousOffered == null ? null : previousOffered.version;
    // }

    public String getScm() throws IOException {
        return latestOffered.getScmUrl();
    }

    public List<IssueTrackerSource.IssueTracker> getIssueTrackers() {
        return IssueTrackerSource.getInstance().getIssueTrackers(artifactId);
    }

    public String getRequiredCore() throws IOException {
        return latestOffered.getRequiredJenkinsVersion();
    }

    public String getCompatibleSinceVersion() throws IOException {
        return latestOffered.getCompatibleSinceVersion();
    }

    public String getMinimumJavaVersion() throws IOException {
        final JavaSpecificationVersion minimumJavaVersion = latestOffered.getMinimumJavaVersion();
        return minimumJavaVersion == null ? null : minimumJavaVersion.toString();
    }

    public long getBuildDate() throws IOException {
        return latestOffered.getTimestamp();
    }

    public List<String> getLabels() throws IOException {
        return latestOffered.getLabels();
    }

//    public String getDefaultBranch() throws IOException {
//        return latestOffered.getDefaultBranch();
//    }

    public List<HPI.Dependency> getDependencies() throws IOException {
        return latestOffered.getDependencies();
    }

    public Map<String, List<String>> getExtendPoints() {
        return latestOffered.getExtendpoints();
    }

//    public String getSha1() throws IOException {
//        return latestOffered.getMetadata().sha1;
//    }

//    public String getSha256() throws IOException {
//        return latestOffered.getMetadata().sha256;
//    }
//
//    public long getSize() throws IOException {
//        return latestOffered.getMetadata().size;
//    }
//
//    public String getGav() {
//        return latestOffered.getGavId();
//    }

    public List<MaintainersSource.Maintainer> getDevelopers() {
        return MaintainersSource.getInstance().getMaintainers(this.latestOffered.artifact);
    }

    public String getExcerpt() throws IOException {
        return latestOffered.getDescription();
    }

    public String getReleaseTimestamp() throws IOException {
        return TIMESTAMP_FORMATTER.format(latestOffered.getTimestamp());
    }

//    public String getPreviousTimestamp() throws IOException {
//        return previousOffered == null ? null : TIMESTAMP_FORMATTER.format(previousOffered.getTimestamp());
//    }

    public int getPopularity() throws IOException {
        //return Popularities.getInstance().getPopularity(artifactId);
        return 1;
    }

    public String getLatest() {
        final LatestPluginVersions instance = LatestPluginVersions.getInstance();
        final VersionNumber latestPublishedVersion = instance.getLatestVersion(artifactId);
        if (latestPublishedVersion == null || latestPublishedVersion.equals(latestOffered.getVersion())) {
            // only include latest version information if the currently published version isn't the latest
            return null;
        }
        return latestPublishedVersion.toString();
    }

    private static final SimpleDateFormat TIMESTAMP_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);

    private static final Logger LOGGER = Logger.getLogger(PluginUpdateCenterEntry.class.getName());
}
