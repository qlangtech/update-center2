package com.qlangetch.tis;

import com.qlangtech.tis.maven.plugins.tpi.PluginClassifier;
import org.apache.commons.collections.MapUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 从 plugins root项目中读取 profile信息
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2024-10-16 11:57
 **/
public class ProfilesReader {

    public static PluginClassifier readeProfile() {

        File mvnHome = new File("/opt/data/mvn_repository");
        File pomFilePath = new File(mvnHome, "com/qlangtech/tis/plugins/tis-plugin-parent/pom.xml");
        if (!pomFilePath.exists()) {
            throw new IllegalStateException("pomFilePath can not be exist:" + pomFilePath);
        }
        final String targetProfile = "default-emr";
        Map<String, String> targetProfileProperties = null;
        try (FileReader reader = new FileReader(pomFilePath)) {
            MavenXpp3Reader mavenReader = new MavenXpp3Reader();
            Model model = mavenReader.read(reader);

            List<Profile> profiles = model.getProfiles();
            for (Profile profile : profiles) {
                if (!targetProfile.equalsIgnoreCase(profile.getId())) {
                    continue;
                }
                targetProfileProperties = new HashMap<>();
                for (Map.Entry entry : profile.getProperties().entrySet()) {
                    targetProfileProperties.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
                }
//                System.out.println("Profile ID: " + profile.getId());
//                System.out.println("Activation: " + profile.getActivation());
//                System.out.println("Properties: " + profile.getProperties());
//                // 可以继续打印其他信息
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (MapUtils.isEmpty(targetProfileProperties)) {
            throw new IllegalStateException("targetProfile:" + targetProfile + " pomfile:" + pomFilePath.getAbsolutePath() + " relevant ");
        }
        return new PluginClassifier(targetProfileProperties);
    }

}
