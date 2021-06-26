package com.qlangetch.tis;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.*;
import com.google.common.collect.Sets;
import io.jenkins.update_center.ArtifactCoordinates;
import io.jenkins.update_center.BaseMavenRepository;
import io.jenkins.update_center.MavenArtifact;
import io.jenkins.update_center.util.Environment;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.zip.ZipEntry;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-11 15:09
 **/
public class TISRepositoryImpl extends BaseMavenRepository {
    private boolean initialized = false;
    public static final String TIS_PACKAGING_TPI = "tpi";
    public static final String TIS_PACKAGE_EXTENSION = "." + TIS_PACKAGING_TPI;
    public static final String PLUGIN_RELEASE_VERSION = System.getProperty("tis.plugin.release.version", "2.3.0");

    private File cacheDirectory = new File(Environment.getString("ARTIFACTORY_CACHEDIR", "caches/artifactory"));
    private Set<ArtifactCoordinates> plugins;
    private OSS ossClient;
    private String ossBucketName;


    private void ensureInitialized() throws IOException {
        if (!initialized) {
            initialize();
            initialized = true;
        }
    }

    private void initialize() throws IOException {
        if (initialized) {
            throw new IllegalStateException("re-initialized");
        }
        LOGGER.log(Level.INFO, "Initializing " + this.getClass().getName());

        File cfgFile = new File(System.getProperty("user.home"), "aliyun-oss/config.properties");
        if (!cfgFile.exists()) {
            throw new IllegalStateException("oss config file is not exist:" + cfgFile.getAbsoluteFile() + "\n config.properties template is \n"
                    + getConfigTemplateContent());
        }
        Properties props = new Properties();
        try {
            try (InputStream reader = FileUtils.openInputStream(cfgFile)) {
                props.load(reader);
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        String endpoint = getProp(props, "endpoint");
        String accessKeyId = getProp(props, "accessKey");
        String secretKey = getProp(props, "secretKey");
        this.ossBucketName = getProp(props, "bucketName");

        this.ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, secretKey);

        ListObjectsRequest request = new ListObjectsRequest();
        request.setBucketName(ossBucketName);
        request.setPrefix(getPluginParentPath());
        request.setMaxKeys(1000);
        ObjectListing objList = ossClient.listObjects(request);
        String filePath = null;
        plugins = Sets.newHashSet();
        ArtifactCoordinates coord = null;
        ObjectMetadata objectMetadata = null;
        for (OSSObjectSummary obj : objList.getObjectSummaries()) {
            filePath = obj.getKey();
            if (obj.getSize() > 0 && StringUtils.endsWith(filePath, TIS_PACKAGE_EXTENSION)) {
                // String groupId, String artifactId, String version, String packaging
                objectMetadata = ossClient.getObjectMetadata(ossBucketName, filePath);
                Map<String, String> userMeta = objectMetadata.getUserMetadata();
                coord = new ArtifactCoordinates(userMeta.get("groupId"), userMeta.get("artifactId"), userMeta.get("version"), userMeta.get("packaging"));
                plugins.add(coord);
            }
        }

        LOGGER.log(Level.INFO, "Initialized " + this.getClass().getName());
    }

    private String getPluginParentPath() {
        return PLUGIN_RELEASE_VERSION + "/tis-plugin";
    }


    private String getConfigTemplateContent() {
        try {
            return IOUtils.toString(TISRepositoryImpl.class.getResourceAsStream("config.tpl"), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getProp(Properties props, String key) {
        String value = props.getProperty(key);
        if (StringUtils.isEmpty(value)) {
            throw new IllegalStateException("key:" + key + " relevant value can not be null");
        }
        return value;
    }

    @Override
    protected Set<ArtifactCoordinates> listAllJenkinsWars(String groupId) throws IOException {
        ArtifactCoordinates c = new ArtifactCoordinates("com.qlagntech.tis", "jenkins-war", "1.0.0", "war");
        return Sets.newHashSet(c);
    }

    @Override
    public Collection<ArtifactCoordinates> listAllPlugins() throws IOException {
        ensureInitialized();
        return this.plugins;
    }

    @Override
    public ArtifactMetadata getMetadata(MavenArtifact artifact) throws IOException {
        ArtifactMetadata ameta = new ArtifactMetadata();
        ameta.sha1 = "";
        ameta.sha256 = "";
        ameta.size = 1;
        return ameta;
    }

    @Override
    public Manifest getManifest(MavenArtifact artifact) throws IOException {
        File artifactFile = resolve(artifact.artifact);
        try {
            try (JarFile jarFile = new JarFile(artifactFile)) {
                return jarFile.getManifest();
            }
        } catch (Exception e) {
            throw new RuntimeException(artifactFile.getAbsolutePath(), e);
        }
    }

    @Override
    public InputStream getZipFileEntry(MavenArtifact artifact, String path) throws IOException {
        return FileUtils.openInputStream(resolve(artifact.artifact));
    }

    @Override
    public File resolve(ArtifactCoordinates artifact) throws IOException {

        final String uri = getUri(artifact);
        if ("pom".equals(artifact.packaging)) {
            ArtifactCoordinates tpiCoord = new ArtifactCoordinates(artifact.groupId, artifact.artifactId, artifact.version, TIS_PACKAGING_TPI);
            File tpi = getFile(tpiCoord, getUri(tpiCoord));
            if (!tpi.exists()) {
                throw new IllegalStateException("tpi is not exist:" + tpi.getAbsolutePath());
            }
            try (JarFile j = new JarFile(tpi)) {
                ZipEntry pom = j.getEntry("META-INF/maven/" + tpiCoord.groupId + "/" + tpiCoord.artifactId + "/pom.xml");

                try (OutputStream pomOut = FileUtils.openOutputStream(new File(cacheDirectory, uri));
                     InputStream in = j.getInputStream(pom)) {
                    IOUtils.copy(in, pomOut);
                }
            }

        }

        return getFile(artifact, uri);
    }


    private File getFile(ArtifactCoordinates artifact, final String url) throws IOException {
        ensureInitialized();
        // String urlBase64 = Base64.encodeBase64String(url.getBytes(StandardCharsets.UTF_8));
        File cacheFile = new File(cacheDirectory, url);
        if (cacheFile.exists()) {
            return cacheFile;
        }
        FileUtils.touch(cacheFile);
        GetObjectRequest getObjRequest = new GetObjectRequest(
                this.ossBucketName, getPluginParentPath() + "/" + artifact.artifactId + TIS_PACKAGE_EXTENSION);
        this.ossClient.getObject(getObjRequest, cacheFile);


        return cacheFile;

//        if (!cacheFile.exists()) {
//            // High log level, but during regular operation this will indicate when an artifact is newly picked up, so useful to know.
//            LOGGER.log(Level.INFO, "Downloading : " + url + " (not found in cache)");
//
//            final File parentFile = cacheFile.getParentFile();
//            if (!parentFile.mkdirs() && !parentFile.isDirectory()) {
//                throw new IllegalStateException("Failed to create non-existing directory " + parentFile);
//            }
//            try {
//                OkHttpClient.Builder builder = new OkHttpClient.Builder();
//                OkHttpClient client = builder.build();
//                Request request = new Request.Builder().url(url).get().build();
//                final Response response = client.newCall(request).execute();
//                if (response.isSuccessful()) {
//                    try (final ResponseBody body = response.body()) {
//                        Objects.requireNonNull(body); // always non-null according to Javadoc
//                        try (InputStream inputStream = body.byteStream();
//                             ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                             FileOutputStream fos = new FileOutputStream(cacheFile);
//                             TeeOutputStream tos = new TeeOutputStream(fos, baos)) {
//
//                            IOUtils.copy(inputStream, tos);
//                            if (baos.size() <= CACHE_ENTRY_MAX_LENGTH) {
//                                final String value = baos.toString("UTF-8");
//                                LOGGER.log(Level.FINE, () -> "Caching in memory: " + url + " with content: " + value);
//                                this.cache.put(url, value);
//                            }
//                        }
//                    }
//                } else {
//                    LOGGER.log(Level.INFO, "Received HTTP error response: " + response.code() + " for URL: " + url);
//                    if (!cacheFile.mkdir()) {
//                        LOGGER.log(Level.WARNING, "Failed to create cache 'not found' directory" + cacheFile);
//                    }
//                }
//            } catch (RuntimeException e) {
//                throw new IOException(e);
//            }
//        } else {
//            if (cacheFile.isDirectory()) {
//                // indicator that this is a cached error
//                this.cache.put(url, null);
//                throw new IOException("Failed to retrieve content of " + url + " (cached)");
//            } else {
//                // read from cached file
//                if (cacheFile.length() <= CACHE_ENTRY_MAX_LENGTH) {
//                    this.cache.put(url, FileUtils.readFileToString(cacheFile, StandardCharsets.UTF_8));
//                }
//            }
//        }
        // return cacheFile;
    }

    private String getUri(ArtifactCoordinates a) {
        String basename = a.artifactId + "-" + a.version;
        String filename = filename = basename + "." + a.packaging;
        return a.groupId.replace(".", "/") + "/" + a.artifactId + "/" + a.version + "/" + filename;
    }

    // private static final File LOCAL_REPO = new File(new File(System.getProperty("user.home")), ".m2/repository");
}
