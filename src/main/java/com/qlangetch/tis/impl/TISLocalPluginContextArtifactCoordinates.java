package com.qlangetch.tis.impl;

import com.qlangetch.tis.TISArtifactCoordinates;
import com.qlangtech.tis.maven.plugins.tpi.PluginClassifier;

import java.io.File;
import java.util.Date;
import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-01-04 11:56
 **/
public class TISLocalPluginContextArtifactCoordinates extends TISArtifactCoordinates {
    // final PluginWrapper plugin;
    private final File tpi;

    public TISLocalPluginContextArtifactCoordinates(
            File tpi, String groupId, String artifactId
            , String version, String packaging, long contentSize, Date lastModified, Optional<PluginClassifier> classifier) {
        super(groupId, artifactId, version, packaging, contentSize, lastModified, classifier);
        this.tpi = tpi;
    }

    public File getArchiveFile() {
        return this.tpi;
    }
}
