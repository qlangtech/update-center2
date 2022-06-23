package com.qlangetch.tis;

import com.qlangtech.tis.maven.plugins.tpi.PluginClassifier;
import io.jenkins.update_center.ArtifactCoordinates;

import java.util.Date;
import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-07-04 21:58
 **/
public class TISArtifactCoordinates extends ArtifactCoordinates {
    public final long contentSize;
    public final Date lastModified;

    public TISArtifactCoordinates(String groupId, String artifactId, String version
            , String packaging, long contentSize, Date lastModified, Optional<PluginClassifier> classifier) {
        super(groupId, artifactId, version, packaging, classifier, false);
        this.contentSize = contentSize;
        this.lastModified = lastModified;
    }
}
