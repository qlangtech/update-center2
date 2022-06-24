package com.qlangetch.tis;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.qlangtech.tis.extension.PluginManager;
import com.qlangtech.tis.maven.plugins.tpi.PluginClassifier;
import io.jenkins.update_center.ArtifactCoordinates;
import io.jenkins.update_center.BaseMavenRepository;
import io.jenkins.update_center.MavenArtifact;
import io.jenkins.update_center.util.Environment;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-11 15:09
 **/
public abstract class AbstractTISRepository extends BaseMavenRepository {
    protected boolean initialized = false;
    public static final String TIS_PACKAGING_TPI = PluginClassifier.PACAKGE_TPI_EXTENSION_NAME;
    public static final String TIS_PACKAGING_JAR = "jar";
    public static final String TIS_PACKAGE_EXTENSION = PluginManager.PACAKGE_TPI_EXTENSION;// "." + TIS_PACKAGING_TPI;
    public static final String PLUGIN_RELEASE_VERSION = System.getProperty("tis.plugin.release.version");

    protected File cacheDirectory = new File(Environment.getString("ARTIFACTORY_CACHEDIR", "caches/artifactory"));
    private Map<String, TISArtifactCoordinates> plugins;


    protected void ensureInitialized() throws IOException {
        if (!initialized) {
            this.plugins = initialize();
            initialized = true;
        }
    }

    protected abstract Map<String, TISArtifactCoordinates> initialize() throws IOException;


    protected String getPluginParentPath() {
        return PLUGIN_RELEASE_VERSION + "/tis-plugin";
    }


    @Override
    protected Set<ArtifactCoordinates> listAllJenkinsWars(String groupId) throws IOException {
        ArtifactCoordinates c = ArtifactCoordinates.create("com.qlangtech.tis", "jenkins-war", "1.0.0", "war");
        return Sets.newHashSet(c);
    }

    @Override
    public Collection<ArtifactCoordinates> listAllPlugins() throws IOException {
        ensureInitialized();
        return this.plugins.values().stream().map((cood) -> cood).collect(Collectors.toList());
    }

    @Override
    public ArtifactMetadata getMetadata(MavenArtifact artifact) throws IOException {
        this.ensureInitialized();
        ArtifactMetadata ameta = new ArtifactMetadata();
        TISArtifactCoordinates coord = plugins.get(artifact.artifact.getGav());
        if (coord != null) {
            ameta.timestamp = coord.lastModified.getTime();
            ameta.size = coord.contentSize;
        }


        ;// artifact.getTimestamp();
        ameta.sha1 = "";
        ameta.sha256 = "";
        //  = artifact.resolve().length();
        return ameta;
    }

    private final Map<String, Manifest> manifestCache = Maps.newHashMap();

    @Override
    public Manifest getManifest(MavenArtifact artifact) throws IOException {

        Manifest manifest = manifestCache.get(artifact.getGavId());
        if (manifest == null) {
            File artifactFile = resolve(artifact.artifact);
            try {
                try (JarFile jarFile = new JarFile(artifactFile)) {
                    manifest = jarFile.getManifest();
                    manifestCache.put(artifact.getGavId(), manifest);
                }
            } catch (Exception e) {
                throw new RuntimeException(artifactFile.getAbsolutePath(), e);
            }

        }
        return manifest;
    }

    @Override
    public InputStream getZipFileEntry(MavenArtifact artifact, String path) throws IOException {

        ZipFile file = new ZipFile(resolve(artifact.artifact));
        ZipEntry entry = file.getEntry(path);
        if (entry == null) {
            return null;
        }
        return file.getInputStream(entry);
    }

    @Override
    public File resolve(ArtifactCoordinates artifact) throws IOException {

        final String uri = getUri(artifact);
        if ("pom".equals(artifact.packaging)) {
            extraTpiZipEntry(artifact, uri, "META-INF/maven/" + artifact.groupId + "/" + artifact.artifactId + "/pom.xml");
        } else if (TIS_PACKAGING_JAR.equals(artifact.packaging)) {
            extraTpiZipEntry(artifact, uri, "WEB-INF/lib/" + artifact.artifactId + "." + TIS_PACKAGING_JAR);
        }

        return getFile(artifact, uri);
    }

    // protected abstract void extraTpiZipEntry(ArtifactCoordinates artifact, String uri, String entryPath) throws IOException;


    protected final void extraTpiZipEntry(ArtifactCoordinates artifact, String uri, String entryPath) throws IOException {
        ArtifactCoordinates tpiCoord = new ArtifactCoordinates(
                artifact.groupId, artifact.artifactId, artifact.version, TIS_PACKAGING_TPI, artifact.classifier, artifact.findParent);
        File tpi = getFile(tpiCoord, getUri(tpiCoord));
        if (!tpi.exists()) {
            if (tpiCoord.findParent) {
                // 如果是查找父亲组件的没有找到，则退出
                return;
            }
            throw new IllegalStateException("tpi is not exist:" + tpi.getAbsolutePath());
        }
        try (JarFile j = new JarFile(tpi)) {
            ZipEntry entry = j.getEntry(entryPath);

            try (OutputStream pomOut = FileUtils.openOutputStream(new File(cacheDirectory, uri));
                 InputStream in = j.getInputStream(entry)) {
                IOUtils.copy(in, pomOut);
            }
        }
    }


    protected abstract File getFile(ArtifactCoordinates artifact, final String url) throws IOException;


    protected String getUri(ArtifactCoordinates a) {
        String basename = a.artifactId + "-" + a.version;
        String filename = basename + "." + a.packaging;
        return a.groupId.replace(".", "/") + "/" + a.artifactId + "/" + a.version + "/" + filename;
    }

    // private static final File LOCAL_REPO = new File(new File(System.getProperty("user.home")), ".m2/repository");
}
