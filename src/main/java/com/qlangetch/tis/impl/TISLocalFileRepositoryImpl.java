package com.qlangetch.tis.impl;

import com.google.common.collect.Maps;
import com.qlangetch.tis.AbstractTISRepository;
import com.qlangetch.tis.TISArtifactCoordinates;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.extension.PluginWrapper;
import io.jenkins.update_center.ArtifactCoordinates;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-01-04 10:16
 **/
public class TISLocalFileRepositoryImpl extends AbstractTISRepository {
    @Override
    protected Map<String, TISArtifactCoordinates> initialize() throws IOException {
        Map<String, TISArtifactCoordinates> plugins = Maps.newHashMap();

        File pluginDir = TIS.pluginDirRoot;
        if (!pluginDir.exists()) {
            throw new IllegalStateException("plugin dir:" + pluginDir.getAbsolutePath() + " must be exist");
        }
        Collection<File> tpis = FileUtils.listFiles(pluginDir, new String[]{AbstractTISRepository.TIS_PACKAGING_TPI}, false);
        if (tpis.size() < 1) {
            throw new IllegalStateException("plugin dir:" + pluginDir.getAbsolutePath() + " must contain plugins");
        }
        TISArtifactCoordinates coord = null;
        for (PluginWrapper plugin : TIS.get().getPluginManager().getPlugins()) {
            try {
                coord = new TISLocalPluginContextArtifactCoordinates(plugin, plugin.getGroupId(), plugin.getShortName(), plugin.getVersion()
                        , AbstractTISRepository.TIS_PACKAGING_TPI, FileUtils.sizeOf(plugin.getArchive()), new Date(plugin.getLastModfiyTime()));
                plugins.put(coord.getGav(), coord);
            } catch (Exception e) {
                throw new IllegalStateException(plugin.getShortName(), e);
            }
        }


        return plugins;
    }

//    @Override
//    protected void extraTpiZipEntry(ArtifactCoordinates artifact, String uri, String entryPath) throws IOException {
//
//    }

    @Override
    protected File getFile(ArtifactCoordinates artifact, String url) throws IOException {
        if (!StringUtils.equals(artifact.packaging, TIS_PACKAGING_TPI)) {
            // throw new IllegalStateException(artifact.toString() + " must be type of " + TIS_PACKAGING_TPI);
            File cacheFile = new File(cacheDirectory, url);
            return cacheFile;
        }

        if (!(artifact instanceof TISLocalPluginContextArtifactCoordinates)) {
            File pluginDir = TIS.pluginDirRoot;
            if (artifact.classifier.isPresent()) {
                return new File(pluginDir, artifact.classifier.get().getTPIPluginName(artifact.artifactId, TIS_PACKAGE_EXTENSION));
            }
            return new File(pluginDir, artifact.artifactId + TIS_PACKAGE_EXTENSION);
        }

        TISLocalPluginContextArtifactCoordinates coord = (TISLocalPluginContextArtifactCoordinates) artifact;
        return coord.getArchiveFile();
    }
}
