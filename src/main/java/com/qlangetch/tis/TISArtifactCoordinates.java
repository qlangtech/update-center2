package com.qlangetch.tis;

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

    public final Optional<String> classifier;

    public TISArtifactCoordinates(String groupId, String artifactId, String version
            , String packaging, long contentSize, Date lastModified, Optional<String> classifier) {
        super(groupId, artifactId, version, packaging);
        this.contentSize = contentSize;
        this.lastModified = lastModified;
        this.classifier = classifier;
    }
}
