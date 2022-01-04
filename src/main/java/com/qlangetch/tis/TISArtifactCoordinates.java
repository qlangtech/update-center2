package com.qlangetch.tis;

import io.jenkins.update_center.ArtifactCoordinates;

import java.util.Date;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-07-04 21:58
 **/
public class TISArtifactCoordinates extends ArtifactCoordinates {
    public final long contentSize;
    public final Date lastModified;

    public TISArtifactCoordinates(String groupId, String artifactId, String version
            , String packaging, long contentSize, Date lastModified) {
        super(groupId, artifactId, version, packaging);
        this.contentSize = contentSize;
        this.lastModified = lastModified;
    }
}
