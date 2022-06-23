package io.jenkins.update_center.wrappers;

import com.qlangtech.tis.maven.plugins.tpi.PluginClassifier;
import io.jenkins.update_center.HPI;
import io.jenkins.update_center.Plugin;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;

/**
 * Filter down to alpha/beta releases of plugins (or the negation of it.)
 *
 * @author Kohsuke Kawaguchi
 */
public class AlphaBetaOnlyRepository extends MavenRepositoryWrapper {

    /**
     * If true, negate the logic and only find non-alpha/beta releases.
     */
    private boolean negative;

    public AlphaBetaOnlyRepository(boolean negative) {
        this.negative = negative;
    }

    @Override
    public Collection<Plugin> listJenkinsPlugins() throws IOException {
        Collection<Plugin> r = base.listJenkinsPlugins();
        for (Iterator<Plugin> jtr = r.iterator(); jtr.hasNext(); ) {
            Plugin h = jtr.next();

            for (Iterator<Entry<PluginClassifier, HPI>> itr = h.getArtifacts().entrySet().iterator(); itr.hasNext(); ) {
                Entry<PluginClassifier, HPI> e = itr.next();
                if (e.getValue().isAlphaOrBeta() ^ negative)
                    continue;
                itr.remove();
            }

            if (h.getArtifacts().isEmpty())
                jtr.remove();
        }

        return r;
    }
}
