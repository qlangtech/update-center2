package com.qlangetch.tis.impl;

import com.qlangetch.tis.AbstractTISRepository;
import com.qlangetch.tis.TISArtifactCoordinates;
import com.qlangtech.tis.extension.impl.PluginManifest;
import com.qlangtech.tis.maven.plugins.tpi.PluginClassifier;
import org.apache.commons.io.FileUtils;

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

    private TISLocalPluginContextArtifactCoordinates(
            File tpi, String groupId, String artifactId
            , String version, String packaging, long contentSize, Date lastModified, Optional<PluginClassifier> classifier) {
        super(groupId, artifactId, version, packaging, contentSize, lastModified, classifier);
        this.tpi = tpi;
    }


    public static TISLocalPluginContextArtifactCoordinates create(PluginManifest manifest, File tpi) {

        if (manifest == null) {
            throw new IllegalStateException("tpi file:" + tpi.getAbsolutePath() + " must exist");
        }

        final String shortName = manifest.computeShortName(tpi != null ? tpi.getName() : null);
        long tpiSize = tpi != null ? FileUtils.sizeOf(tpi) : 0;

        try {
            Optional<PluginClassifier> classifier = manifest.parseClassifier();
            TISLocalPluginContextArtifactCoordinates coord = new TISLocalPluginContextArtifactCoordinates(tpi, manifest.getGroupId()
                    , shortName, manifest.getVersionOf()
                    , AbstractTISRepository.TIS_PACKAGING_TPI
                    , tpiSize, new Date(manifest.getLastModfiyTime()), classifier);
//            plugins.put(coord.getGav(), coord);
            return coord;
        } catch (Exception e) {
            throw new IllegalStateException(shortName, e);
        }
    }

    public File getArchiveFile() {
        return this.tpi;
    }
}
