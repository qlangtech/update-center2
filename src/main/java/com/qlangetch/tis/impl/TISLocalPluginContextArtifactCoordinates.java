package com.qlangetch.tis.impl;

import com.qlangetch.tis.TISArtifactCoordinates;
import com.qlangtech.tis.extension.PluginWrapper;

import java.io.File;
import java.util.Date;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-01-04 11:56
 **/
public class TISLocalPluginContextArtifactCoordinates extends TISArtifactCoordinates {
    final PluginWrapper plugin;

    public TISLocalPluginContextArtifactCoordinates(
            PluginWrapper plugin, String groupId, String artifactId, String version, String packaging, long contentSize, Date lastModified) {
        super(groupId, artifactId, version, packaging, contentSize, lastModified, plugin.getClassifier());
        this.plugin = plugin;
    }

    public File getArchiveFile() {
        return this.plugin.getArchive();
    }
}
