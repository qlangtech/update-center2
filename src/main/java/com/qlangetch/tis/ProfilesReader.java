package com.qlangetch.tis;

import com.google.common.collect.Maps;
import com.qlangtech.tis.maven.plugins.tpi.PluginClassifier;

import java.util.Map;

/**
 * 从 plugins root项目中读取 profile信息
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2024-10-16 11:57
 **/
public class ProfilesReader {

    public static PluginClassifier readeProfile() {

//        File mvnHome = new File("/opt/data/mvn_repository");
//        File pomFilePath = new File(mvnHome, "com/qlangtech/tis/plugins/tis-plugin-parent/4.0.1/tis-plugin-parent-4.0.1.pom");
//        if (!pomFilePath.exists()) {
//            throw new IllegalStateException("pomFilePath can not be exist:" + pomFilePath);
//        }
//        final String targetProfile = "default-emr";
//        Map<String, String> targetProfileProperties = null;
//        try (FileReader reader = new FileReader(pomFilePath)) {
//            MavenXpp3Reader mavenReader = new MavenXpp3Reader();
//            Model model = mavenReader.read(reader);
//
//            List<Profile> profiles = model.getProfiles();
//            for (Profile profile : profiles) {
//                if (!targetProfile.equalsIgnoreCase(profile.getId())) {
//                    continue;
//                }
//                targetProfileProperties = new HashMap<>();
//                for (Map.Entry entry : profile.getProperties().entrySet()) {
//                    targetProfileProperties.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
//                }
//
//            }
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
        Map<String, String> targetProfileProperties = Maps.newHashMap();
//        if (MapUtils.isEmpty(targetProfileProperties)) {
//            throw new IllegalStateException("targetProfile:" + targetProfile + " pomfile:" + pomFilePath.getAbsolutePath() + " relevant ");
//        }
        targetProfileProperties.put("hudi", "0.14.1");
        targetProfileProperties.put("spark", "2.4.4");
        targetProfileProperties.put("hive", "2.3.1");
        targetProfileProperties.put("hadoop", "2.7.3");
        return new PluginClassifier(targetProfileProperties);
    }

}
