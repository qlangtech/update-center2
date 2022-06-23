package com.qlangetch.tis.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.*;
import com.google.common.collect.Maps;
import com.qlangetch.tis.AbstractTISRepository;
import com.qlangetch.tis.TISArtifactCoordinates;
import com.qlangtech.tis.extension.PluginManager;
import com.qlangtech.tis.maven.plugins.tpi.PluginClassifier;
import io.jenkins.update_center.ArtifactCoordinates;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-01-04 10:18
 **/
public class TISAliyunOSSRepositoryImpl extends AbstractTISRepository {
    private AliyunOSS ossClient;
    // private String ossBucketName;


    protected Map<String, TISArtifactCoordinates> initialize() throws IOException {
        if (initialized) {
            throw new IllegalStateException("re-initialized");
        }

        LOGGER.log(Level.INFO, "Initializing " + this.getClass().getName());

        this.ossClient = getOSSClient();
        Map<String, TISArtifactCoordinates> plugins = Maps.newHashMap();
        getCreatePluginMetas(plugins, getPluginParentPath());

        LOGGER.log(Level.INFO, "Initialized " + this.getClass().getName());
        return plugins;
    }


    private void getCreatePluginMetas(Map<String, TISArtifactCoordinates> pluginMeta, String pluginParentPath) {
        //Map<String, TISArtifactCoordinates> plugins;
        ListObjectsRequest request = new ListObjectsRequest();
        request.setBucketName(this.ossClient.ossBucketName);
        request.setPrefix(pluginParentPath);
        request.setMaxKeys(1000);
        ObjectListing objList = ossClient.listObjects(request);
        String filePath = null;
        pluginMeta = Maps.newHashMap();
        TISArtifactCoordinates coord = null;
        ObjectMetadata objectMetadata = null;
        for (OSSObjectSummary obj : objList.getObjectSummaries()) {
            filePath = obj.getKey();

            if (obj.getSize() > 0 && StringUtils.endsWith(filePath, TIS_PACKAGE_EXTENSION)) {
                // String groupId, String artifactId, String version, String packaging
                objectMetadata = ossClient.getObjectMetadata(ossClient.ossBucketName, filePath);
                Map<String, String> userMeta = objectMetadata.getUserMetadata();


                coord = new TISArtifactCoordinates(userMeta.get("groupId"), userMeta.get("artifactId"), userMeta.get("version")
                        , userMeta.get("packaging"), objectMetadata.getContentLength(), objectMetadata.getLastModified()
                        , Optional.ofNullable(new PluginClassifier(userMeta.get(PluginManager.PACAKGE_CLASSIFIER))));
                //  plugins.add(coord);
                pluginMeta.put(coord.getGav(), coord);
            } else {
                getCreatePluginMetas(pluginMeta, filePath);
            }
        }
        //return plugins;
    }

    public static AliyunOSS getOSSClient() {
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

        return new AliyunOSS(new OSSClientBuilder().build(endpoint, accessKeyId, secretKey), getProp(props, "bucketName"));
    }

    public static class AliyunOSS {
        private final OSS ossClient;
        private final String ossBucketName;

        public AliyunOSS(OSS ossClient, String ossBucketName) {
            if (ossClient == null) {
                throw new IllegalArgumentException("ossClient can not be null");
            }
            if (StringUtils.isEmpty(ossBucketName)) {
                throw new IllegalArgumentException("ossBucketName can not be null");
            }
            this.ossClient = ossClient;
            this.ossBucketName = ossBucketName;
        }

        public ObjectListing listObjects(ListObjectsRequest request) {
            return this.ossClient.listObjects(request);
        }

        public void getObject(GetObjectRequest getObjRequest, File cacheFile) {
            this.ossClient.getObject(getObjRequest, cacheFile);
        }

        public ObjectMetadata getObjectMetadata(String ossBucketName, String filePath) {
            return this.ossClient.getObjectMetadata(ossBucketName, filePath);
        }

        public PutObjectResult writeFile(String ossPath, File updateCenterJson) {
            return this.ossClient.putObject(this.ossBucketName, ossPath, updateCenterJson);
        }
    }

    @Override
    protected File getFile(ArtifactCoordinates artifact, final String url) throws IOException {
        ensureInitialized();
        // String urlBase64 = Base64.encodeBase64String(url.getBytes(StandardCharsets.UTF_8));
        File cacheFile = new File(cacheDirectory, url);
        if (cacheFile.exists()) {
            return cacheFile;
        }
        FileUtils.touch(cacheFile);
        if (StringUtils.equals(artifact.packaging, TIS_PACKAGING_TPI)) {
            GetObjectRequest getObjRequest = new GetObjectRequest(
                    this.ossClient.ossBucketName, getPluginParentPath() + "/" + artifact.artifactId + TIS_PACKAGE_EXTENSION);
            this.ossClient.getObject(getObjRequest, cacheFile);
        } else {
            throw new IllegalStateException("artifact has not match any get strategy:" + artifact.toString());
        }

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

    private static String getProp(Properties props, String key) {
        String value = props.getProperty(key);
        if (StringUtils.isEmpty(value)) {
            throw new IllegalStateException("key:" + key + " relevant value can not be null");
        }
        return value;
    }

    private static String getConfigTemplateContent() {
        try {
            return IOUtils.toString(AbstractTISRepository.class.getResourceAsStream("config.tpl"), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
