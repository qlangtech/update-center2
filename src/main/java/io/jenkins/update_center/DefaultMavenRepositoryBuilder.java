package io.jenkins.update_center;

import com.qlangetch.tis.impl.TISLocalFileRepositoryImpl;

public class DefaultMavenRepositoryBuilder {

//    private static String ARTIFACTORY_API_USERNAME = Environment.getString("ARTIFACTORY_USERNAME");
//    private static String ARTIFACTORY_API_PASSWORD = Environment.getString("ARTIFACTORY_PASSWORD");

    private DefaultMavenRepositoryBuilder() {

    }

    private static BaseMavenRepository instance;

    public static BaseMavenRepository getInstance() {
        if (instance == null) {
            // if (ARTIFACTORY_API_PASSWORD != null && ARTIFACTORY_API_USERNAME != null) {
            // instance = new TISAliyunOSSRepositoryImpl(); //new ArtifactoryRepositoryImpl(ARTIFACTORY_API_USERNAME, ARTIFACTORY_API_PASSWORD);
            instance = new TISLocalFileRepositoryImpl();
//            } else {
//                throw new IllegalStateException("ARTIFACTORY_USERNAME and ARTIFACTORY_PASSWORD need to be set");
//            }
        }
        return instance;
    }
}
