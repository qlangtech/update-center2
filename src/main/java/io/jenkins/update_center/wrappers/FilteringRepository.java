package io.jenkins.update_center.wrappers;

import com.qlangtech.tis.maven.plugins.tpi.PluginClassifier;
import io.jenkins.update_center.HPI;
import io.jenkins.update_center.Plugin;
import io.jenkins.update_center.PluginFilter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;

public class FilteringRepository extends MavenRepositoryWrapper {

    /**
     * Adds a plugin filter.
     *
     * @param filter Filter to be added.
     */
    private void addPluginFilter(@Nonnull PluginFilter filter) {
        pluginFilters.add(filter);
    }

    private List<PluginFilter> pluginFilters = new ArrayList<>();

    @Override
    public Collection<Plugin> listJenkinsPlugins() throws IOException {
        Collection<Plugin> r = base.listJenkinsPlugins();
        for (Iterator<Plugin> jtr = r.iterator(); jtr.hasNext(); ) {
            Plugin h = jtr.next();

            for (Iterator<Map.Entry<PluginClassifier, HPI>> itr = h.getArtifacts().entrySet().iterator(); itr.hasNext(); ) {
                Map.Entry<PluginClassifier, HPI> e = itr.next();
                for (PluginFilter filter : pluginFilters) {
                    if (filter.shouldIgnore(e.getValue())) {
                        itr.remove();
                    }
                }
            }

            if (h.getArtifacts().isEmpty())
                jtr.remove();
        }

        return r;
    }

    public FilteringRepository withPluginFilter(PluginFilter pluginFilter) {
        addPluginFilter(pluginFilter);
        return this;
    }
}
