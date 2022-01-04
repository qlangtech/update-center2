package com.qlangetch.tis.impl;

import com.google.common.collect.Maps;
import com.qlangetch.tis.AbstractTISRepository;
import com.qlangetch.tis.TISArtifactCoordinates;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.extension.PluginWrapper;
import io.jenkins.update_center.ArtifactCoordinates;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
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
            throw new IllegalStateException("plugin dir:" + pluginDir.getAbsolutePath() + " can not be exist");
        }

//        Iterator<File> tpiFiles
//                = FileUtils.iterateFiles(pluginDir, new String[]{AbstractTISRepository.TIS_PACKAGING_TPI}, false);
        TISArtifactCoordinates coord = null;
        for (PluginWrapper plugin : TIS.get().getPluginManager().getPlugins()) {
            coord = new TISLocalPluginContextArtifactCoordinates(plugin, plugin.getGroupId(), plugin.getShortName(), plugin.getVersion()
                    , AbstractTISRepository.TIS_PACKAGING_TPI, FileUtils.sizeOf(plugin.getArchive()), new Date(plugin.getLastModfiyTime()));
            plugins.put(coord.getGav(), coord);
        }


        return plugins;
    }

    @Override
    protected File getFile(ArtifactCoordinates artifact, String url) throws IOException {
        TISLocalPluginContextArtifactCoordinates coord = (TISLocalPluginContextArtifactCoordinates) artifact;
        return coord.getArchiveFile();
    }
}
