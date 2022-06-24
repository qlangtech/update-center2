/*
 * The MIT License
 *
 * Copyright (c) 2004-2020, Sun Microsystems, Inc. and other contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.jenkins.update_center;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.qlangetch.tis.AbstractTISRepository;
import com.qlangetch.tis.impl.TISAliyunOSSRepositoryImpl;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.PluginFormProperties;
import com.qlangtech.tis.extension.impl.PropertyType;
import com.qlangtech.tis.extension.impl.RootFormProperties;
import com.qlangtech.tis.extension.model.UpdateCenter;
import com.qlangtech.tis.extension.util.PluginExtraProps;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.Validator;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.util.VersionNumber;
import io.jenkins.lib.support_log_formatter.SupportLogFormatter;
import io.jenkins.update_center.args4j.LevelOptionHandler;
import io.jenkins.update_center.filters.JavaVersionPluginFilter;
import io.jenkins.update_center.json.*;
import io.jenkins.update_center.util.JavaSpecificationVersion;
import io.jenkins.update_center.wrappers.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.args4j.ClassParser;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import javax.annotation.CheckForNull;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * java -classpath ./lib/*:./update-center2.jar -Dplugin_dir_root=/tmp/release/tis-plugin -Dtis.plugin.release.version=3.4.0  io.jenkins.update_center.Main --www-dir=./dist
 */
public class Main {
    /* Control meta-execution options */
    @Option(name = "--arguments-file", usage = "Specify invocation arguments in a file, with each line being a separate update site build. This argument cannot be re-set via arguments-file.")
    @SuppressFBWarnings
    @CheckForNull
    public static File argumentsFile;

    @Option(name = "--resources-dir", usage = "Specify the path to the resources directory containing warnings.json, artifact-ignores.properties, etc. This argument cannot be re-set via arguments-file.")
    @SuppressFBWarnings
    @NonNull
    public static File resourcesDir = new File("resources"); // Default value for tests -- TODO find a better way to set a value for tests

    @Option(name = "--log-level", usage = "A java.util.logging.Level name. Use CONFIG, FINE, FINER, or FINEST to log more output.", handler = LevelOptionHandler.class)
    @SuppressFBWarnings
    @CheckForNull
    public static Level level = Level.INFO;


    /* Configure repository source */
    @Option(name = "--limit-plugin-core-dependency", usage = "Cap the core dependency and only include plugins that are compatible with this core (or older)")
    @CheckForNull
    public String capPlugin;

    @Option(name = "--limit-core-release", usage = "Cap the version number of Jenkins core offered. Not generally useful.")
    @CheckForNull
    public String capCore; // TODO remove

    @Option(name = "--only-stable-core", usage = "Limit core releases to stable (LTS) releases (those with three component version numbers)")
    public boolean stableCore;

    @Option(name = "--only-experimental", usage = "Only include experimental alpha/beta releases")
    public boolean onlyExperimental; // TODO would it make more sense to generate this as the experimental update site?

    @Option(name = "--with-experimental", usage = "Include experimental alpha/beta releases")
    public boolean includeExperimental;

    @Option(name = "--java-version", usage = "Target Java version for the update center. Plugins will be excluded if their minimum Java version does not match. If not set, required Java version will be ignored")
    @CheckForNull
    public String javaVersion;

    @Option(name = "--max-plugins", usage = "For testing purposes: Limit the number of plugins included to the specified number.")
    @CheckForNull
    public Integer maxPlugins;

    @Option(name = "--allowed-artifacts-file", usage = "For testing purposes: A Java properties file whose keys are artifactIds and values are space separated lists of versions to allow, or '*' to allow all")
    @CheckForNull
    public File allowedArtifactsListFile;


    /* Configure what kinds of output to generate */
    @Option(name = "--dynamic-tier-list-file", usage = "Generate tier list JSON file at the specified path. If this option is set, we skip generating all other output.")
    @CheckForNull
    public File tierListFile;

    @Option(name = "--www-dir", usage = "Generate simple output files, JSON(ish) and others, into this directory")
    @CheckForNull
    public File www;

    @Option(name = "--skip-update-center", usage = "Skip generation of update center files (mostly useful during development)")
    public boolean skipUpdateCenter;

    @Option(name = "--skip-latest-plugin-release", usage = "Do not include information about the latest existing plugin release (if an older release is being offered)")
    public boolean skipLatestPluginRelease;

    @Option(name = "--generate-release-history", usage = "Generate release history")
    public boolean generateReleaseHistory;

    @Option(name = "--generate-plugin-versions", usage = "Generate plugin versions")
    public boolean generatePluginVersions;

    @Option(name = "--generate-plugin-documentation-urls", usage = "Generate plugin documentation URL mapping (for plugins.jenkins.io)")
    public boolean generatePluginDocumentationUrls;

    @Option(name = "--generate-recent-releases", usage = "Generate recent releases file (as input to targeted rsync etc.)")
    public boolean generateRecentReleases;

    @Option(name = "--generate-platform-plugins", usage = "Generate platform-plugins.json (to override wizard suggestions)")
    public boolean generatePlatformPlugins;


    /* Configure options modifying output */
    @Option(name = "--pretty-json", usage = "Pretty-print JSON files")
    public boolean prettyPrint;

    @Option(name = "--id", usage = "Uniquely identifies this update center. We recommend you use a dot-separated name like \"com.sun.wts.jenkins\". This value is not exposed to users, but instead internally used by Jenkins.")
    @CheckForNull
    public String id;

    @Option(name = "--connection-check-url", usage = "Specify an URL of the 'always up' server for performing connection check.")
    @CheckForNull
    public String connectionCheckUrl;


    /* These fields are other objects configurable with command-line options */
    private Signer signer = new Signer();
    private MetadataWriter metadataWriter = new MetadataWriter();
    private DirectoryTreeBuilder directoryTreeBuilder = new DirectoryTreeBuilder();


    public static void main(String[] args) throws Exception {
        if (!System.getProperty("file.encoding").equals("UTF-8")) {
            System.err.println("This tool must be launched with -Dfile.encoding=UTF-8");
            System.exit(1);
        }

        final Logger rootLogger = Logger.getLogger("");
        rootLogger.setLevel(Level.INFO);
        for (Handler h : rootLogger.getHandlers()) {
            if (h instanceof ConsoleHandler) {
                h.setFormatter(new SupportLogFormatter());
            }
            h.setLevel(Level.ALL);
        }

        System.exit(new Main().run(args));
    }

    public int run(String[] args) throws Exception {
        CmdLineParser p = new CmdLineParser(this);
        new ClassParser().parse(signer, p);
        new ClassParser().parse(metadataWriter, p);
        new ClassParser().parse(directoryTreeBuilder, p);
        try {
            p.parseArgument(args);

            if (argumentsFile == null) {
                run();
            } else {
                List<String> invocations = IOUtils.readLines(Files.newBufferedReader(argumentsFile.toPath(), StandardCharsets.UTF_8));
                int executions = 0;
                for (String line : invocations) {
                    if (!line.trim().startsWith("#") && !line.trim().isEmpty()) { // TODO more flexible comments support, e.g. end-of-line

                        LOGGER.log(Level.INFO, "Running with args: " + line);
                        // TODO combine args array and this list
                        String[] invocationArgs = line.trim().split(" +");

                        resetArguments(this, signer, metadataWriter, directoryTreeBuilder);

                        p.parseArgument(invocationArgs);
                        run();
                        executions++;
                    }
                }
                LOGGER.log(Level.INFO, "Finished " + executions + " executions found in parameters file " + argumentsFile);
            }

            return 0;
        } catch (CmdLineException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
            p.printUsage(System.err);
            return 1;
        }
    }

    private void resetArguments(Object... optionHolders) {
        for (Object o : optionHolders) {
            for (Field field : o.getClass().getFields()) {
                if (field.getAnnotation(Option.class) != null && !Modifier.isStatic(field.getModifiers())) {
                    if (Object.class.isAssignableFrom(field.getType())) {
                        try {
                            field.set(o, null);
                        } catch (IllegalAccessException e) {
                            LOGGER.log(Level.WARNING, "Failed to reset argument", e);
                        }
                    } else if (boolean.class.isAssignableFrom(field.getType())) {
                        try {
                            field.set(o, false);
                        } catch (IllegalAccessException e) {
                            LOGGER.log(Level.WARNING, "Failed to reset boolean option", e);
                        }
                    }
                }
            }
        }
    }

    public void run() throws Exception {

        if (level != null) {
            PACKAGE_LOGGER.setLevel(level);
        }

        MavenRepository repo = createRepository();
        initializeLatestPluginVersions(skipLatestPluginRelease);

        if (tierListFile != null) {
            new TieredUpdateSitesGenerator().withRepository(repo).write(tierListFile, prettyPrint);
            return;
        }

        metadataWriter.writeMetadataFiles(repo, www);

        if (!skipUpdateCenter) {
            final String signedUpdateCenterJson
                    = new UpdateCenterRoot(repo, new File(Main.resourcesDir, WARNINGS_JSON_FILENAME))
                    .encodeWithSignature(signer, prettyPrint);

            File updateCenterJson = new File(www, UPDATE_CENTER_JSON_FILENAME);

            writeToFile(updateCenterPostCallJson(signedUpdateCenterJson), updateCenterJson);
            /*******************************************
             * deploy to remote OSS repository
             *******************************************/
            String ossPath = AbstractTISRepository.PLUGIN_RELEASE_VERSION + UpdateCenter.KEY_UPDATE_SITE + "/" + UpdateCenter.KEY_DEFAULT_JSON;
            TISAliyunOSSRepositoryImpl.getOSSClient().writeFile(ossPath, updateCenterJson);

            writeToFile(signedUpdateCenterJson, new File(www, UPDATE_CENTER_ACTUAL_JSON_FILENAME));
            writeToFile(updateCenterPostMessageHtml(signedUpdateCenterJson), new File(www, UPDATE_CENTER_JSON_HTML_FILENAME));
        }

        StringBuffer pluginList = new StringBuffer();

        Map<String, List<PluginExtendsionImpl>> extendPoints = Maps.newHashMap();
        List<PluginExtendsionImpl> extendImpls = null;
        Collection<Plugin> artifacts = repo.listJenkinsPlugins();
        HPI latest = null;
        String excerpt = null;
        for (Plugin plugin : artifacts) {

            latest = plugin.getLatest();
            pluginList.append("## 插件名：").append(latest.getArchiveFileName()).append("\n\n");
            pluginList.append("* **下载地址：** ").append(latest.getDownloadUrl()).append("\n\n");
            excerpt = latest.getDescription();
            if (StringUtils.isNotBlank(excerpt)) {
                pluginList.append("* **介绍：** \n\n");//.append(latest.getDescription()).append("\n\n");
                appendRichMdContent(pluginList, 1, excerpt);
            }


            //latest.getScmUrl();
            pluginList.append("* **扩展列表：** \n\n");
            for (Map.Entry<String, List<String>> e : latest.getExtendpoints().entrySet()) {
                extendImpls = extendPoints.get(e.getKey());
                pluginList.append("\t* [").append(e.getKey()).append("]({{< relref \"./plugins/#扩展点").append(getExtendPointHtmlAnchor(e.getKey())).append("\">}})\n\n");
                if (extendImpls == null) {
                    extendImpls = Lists.newArrayList();
                    extendPoints.put(e.getKey(), extendImpls);
                }
                extendImpls.addAll(e.getValue().stream().map((impl) -> {
                    PluginExtendsionImpl eimpl = new PluginExtendsionImpl(impl, plugin.getLatest());
                    pluginList.append("\t\t * [").append(impl).append("]({{< relref \"./plugins/#").append(eimpl.getHtmlAnchor()).append("\">}})\n\n");
                    return eimpl;
                }).collect(Collectors.toList()));
            }
        }


        FileUtils.write(new File(www, PLUGIN_TPIS_MARK_DOWN_FILENAME), pluginList.toString(), TisUTF8.get());

        StringBuffer extendsList = new StringBuffer();
        Descriptor descriptor = null;
        PluginFormProperties pluginFormPropertyTypes = null;
        for (Map.Entry<String, List<PluginExtendsionImpl>> e : extendPoints.entrySet()) {
            System.out.println(e.getKey());
            extendsList.append("## 扩展点:").append(e.getKey()).append("\n\n");

            for (PluginExtendsionImpl extendImpl : e.getValue()) {

                descriptor = TIS.get().getDescriptor(extendImpl.extendImpl);
                if (descriptor != null) {
                    extendsList.append("### ").append(descriptor.clazz.getSimpleName()).append("\n\n");
                    extendsList.append("* **显示名:** ").append(descriptor.getDisplayName()).append(" \n\n");
                    extendsList.append("* **全路径名:** [").append(extendImpl.extendImpl).append("](").append(extendImpl.getExtendImplURL()).append(") \n\n");
                } else {
                    extendsList.append("### ").append(extendImpl.extendImpl).append("\n\n");
                }
                extendsList.append("* **费用:** `社区版(免费)`").append("\n\n");
                extendsList.append("* **插件包:** [").append(extendImpl.getArchiveFileName())
                        .append("]({{< relref \"./tpis/#插件名").append(extendImpl.getArchiveFileNameHtmlAnchor()).append("\">}})").append("\n\n");
//                md.append("* 费用:");
//                md.append("* 版本:");
//                md.append("* 包大小:");
//                md.append("* 打包时间:");


                if (descriptor == null) {
                    continue;
                }
                pluginFormPropertyTypes = descriptor.getPluginFormPropertyTypes();

                final Set<Map.Entry<String, PropertyType>> props
                        = pluginFormPropertyTypes.accept(new PluginFormProperties.IVisitor() {
                    @Override
                    public Set<Map.Entry<String, PropertyType>> visit(RootFormProperties props) {
                        return props.getKVTuples();
                    }
                });
                PropertyType ptype = null;
                PluginExtraProps.Props extraProps = null;
                if (CollectionUtils.isNotEmpty(props)) {
                    extendsList.append("* **参数说明:** ").append("\n\n");
//                    md.append("|  配置项    | 类型    | 必须     | 说明    |").append("\n\n");
//                    md.append("|  :-----   | :-----  | :-----  | :-----  |").append("\n\n");
                    int index = 1;
                    for (Map.Entry<String, PropertyType> prop : props) {
                        ptype = prop.getValue();
                        extraProps = ptype.extraProp;

                        extendsList.append(index++).append(". ");
                        if (extraProps == null) {
                            extendsList.append(prop.getKey()).append("\n\n");
                        } else {
                            extendsList.append(StringUtils.defaultString(extraProps.getLable(), prop.getKey())).append("\n\n");
                        }

                        extendsList.append("\t* **类型:** ").append(descFieldType(ptype.formField)).append("\n\n");
                        extendsList.append("\t* **必须:** ").append(descRequire(ptype.formField)).append("\n\n");

                        if (extraProps == null) {
                            continue;
                        }

                        extendsList.append("\t* **说明:** ");

                        if (StringUtils.isEmpty(extraProps.getAsynHelp())) {
                            extendsList.append(StringUtils.defaultString(extraProps.getHelpContent(), "无")).append("\n");
                        } else {
                            extendsList.append("\n");
                            appendRichMdContent(extendsList, 2, extraProps.getAsynHelp());
                        }

                        Object dftVal = extraProps.getDftVal();

                        extendsList.append("\t* **默认值:** ")
                                .append((dftVal == null) ? "无" : dftVal).append("\n\n");

                    }
                }
            }
        }

        FileUtils.write(new File(www, PLUGIN_DESC_MARK_DOWN_FILENAME), extendsList.toString(), TisUTF8.get());

        if (generatePluginDocumentationUrls) {
            new PluginDocumentationUrlsRoot(repo).write(new File(www, PLUGIN_DOCUMENTATION_URLS_JSON_FILENAME), prettyPrint);
        }

        if (generatePluginVersions) {
            new PluginVersionsRoot("1", repo).writeWithSignature(new File(www, PLUGIN_VERSIONS_JSON_FILENAME), signer, prettyPrint);
        }

        if (generateReleaseHistory) {
            new ReleaseHistoryRoot(repo).write(new File(www, RELEASE_HISTORY_JSON_FILENAME), prettyPrint);
        }

        if (generateRecentReleases) {
            new RecentReleasesRoot(repo).write(new File(www, RECENT_RELEASES_JSON_FILENAME), prettyPrint);
        }

        if (generatePlatformPlugins) {
            new PlatformPluginsRoot(new File(Main.resourcesDir, PLATFORM_PLUGINS_RESOURCE_FILENAME)).writeWithSignature(new File(www, PLATFORM_PLUGINS_JSON_FILENAME), signer, prettyPrint);
        }

        directoryTreeBuilder.build(repo);
    }

    private String getExtendPointHtmlAnchor(String key) {
        return StringUtils.remove(key, ".");
    }

    private void appendRichMdContent(StringBuffer extendsList, int indent, String content) throws IOException {
        try (StringReader mdReader = new StringReader(content)) {
            for (String line : IOUtils.readLines(mdReader)) {
                for (int i = 0; i < indent; i++) {
                    extendsList.append("\t");
                }
                extendsList.append(line).append("\n");
            }
        }
    }

    private static class PluginExtendsionImpl {
        private final String extendImpl;
        private final HPI hpi;

        public PluginExtendsionImpl(String extendImpl, HPI hpi) {
            this.extendImpl = extendImpl;
            this.hpi = hpi;
        }

        public String getExtendImplURL() {
            try {
                final String scmUrl = hpi.getScmUrl();
                boolean endWithSlash = StringUtils.endsWith(scmUrl, "/");
                return scmUrl + (endWithSlash ? StringUtils.EMPTY : "/") + "src/main/java/" + StringUtils.replace(extendImpl, ".", "/") + ".java";
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public String getHtmlAnchor() {
            return StringUtils.substringAfterLast(extendImpl, ".");
        }

        public String getArchiveFileName() {
            return hpi.getArchiveFileName();
        }

        public String getArchiveFileNameHtmlAnchor() {
            return StringUtils.removeAll(hpi.getArchiveFileName(), "\\.|-");
        }
    }

    private String descRequire(FormField formField) {
        for (Validator v : formField.validate()) {
            if (v == Validator.require) {
                return "是";
            }
        }

        return "否";
    }

    private String updateCenterPostCallJson(String updateCenterJson) {
        return updateCenterJson;
        // return "updateCenter.post(" + EOL + updateCenterJson + EOL + ");";
    }

    private String descFieldType(FormField formField) {
        switch (formField.type()) {
            case DATE:
                return "日期";
            case PASSWORD:
                return "密码";
            case TEXTAREA:
                return "富文本";
            case INPUTTEXT:
                return "单行文本";
            case INT_NUMBER:
                return "整型数字";
            case SELECTABLE:
            case ENUM:
                return "单选";
            case MULTI_SELECTABLE:
                return "多选";
            case FILE:
                return "文件";
            default:
                throw new IllegalStateException("invalid type:" + formField.type());
        }
    }

    private String updateCenterPostMessageHtml(String updateCenterJson) {
        // needs the DOCTYPE to make JSON.stringify work on IE8
        return "\uFEFF<!DOCTYPE html><html><head><meta http-equiv='Content-Type' content='text/html;charset=UTF-8' /></head><body><script>window.onload = function () { window.parent.postMessage(JSON.stringify(" + EOL + updateCenterJson + EOL + "),'*'); };</script></body></html>";
    }

    private static void writeToFile(String string, final File file) throws IOException {
        File parentFile = file.getParentFile();
        if (parentFile != null && !parentFile.isDirectory() && !parentFile.mkdirs()) {
            throw new IOException("Failed to create parent directory " + parentFile);
        }
        PrintWriter rhpw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
        rhpw.print(string);
        rhpw.close();
    }

    private void initializeLatestPluginVersions(boolean skip) throws IOException {
        if (skip) {
            LatestPluginVersions.initializeEmpty();
            return;
        }
        MavenRepository repo = DefaultMavenRepositoryBuilder.getInstance();
        if (allowedArtifactsListFile != null) {
            final Properties properties = new Properties();
            try (FileInputStream fis = new FileInputStream(allowedArtifactsListFile)) {
                properties.load(fis);
            }
            repo = new AllowedArtifactsListMavenRepository(properties).withBaseRepository(repo);
        }
        if (maxPlugins != null) {
            repo = new TruncatedMavenRepository(maxPlugins).withBaseRepository(repo);
        }
        if (onlyExperimental) {
            repo = new AlphaBetaOnlyRepository(false).withBaseRepository(repo);
        }
        if (!includeExperimental) {
            repo = new AlphaBetaOnlyRepository(true).withBaseRepository(repo);
        }
        LatestPluginVersions.initialize(repo);
    }

    private MavenRepository createRepository() throws Exception {

        MavenRepository repo = DefaultMavenRepositoryBuilder.getInstance();
        if (allowedArtifactsListFile != null) {
            final Properties properties = new Properties();
            try (FileInputStream fis = new FileInputStream(allowedArtifactsListFile)) {
                properties.load(fis);
            }
            repo = new AllowedArtifactsListMavenRepository(properties).withBaseRepository(repo);
        }
        if (maxPlugins != null) {
            repo = new TruncatedMavenRepository(maxPlugins).withBaseRepository(repo);
        }
        if (onlyExperimental) {
            repo = new AlphaBetaOnlyRepository(false).withBaseRepository(repo);
        }
        if (!includeExperimental) {
            repo = new AlphaBetaOnlyRepository(true).withBaseRepository(repo);
        }
        if (stableCore) {
            repo = new StableWarMavenRepository().withBaseRepository(repo);
        }
        if (capCore != null || capPlugin != null) {
            VersionNumber vp = capPlugin == null ? null : new VersionNumber(capPlugin);
            VersionNumber vc = capCore == null ? null : new VersionNumber(capCore);
            repo = new VersionCappedMavenRepository(vp, vc).withBaseRepository(repo);
        }
        if (javaVersion != null) {
            repo = new FilteringRepository().withPluginFilter(new JavaVersionPluginFilter(new JavaSpecificationVersion(this.javaVersion))).withBaseRepository(repo);
        }
        return repo;
    }

    private static final String WARNINGS_JSON_FILENAME = "warnings.json";
    private static final String UPDATE_CENTER_JSON_FILENAME = "update-center.json";
    private static final String UPDATE_CENTER_ACTUAL_JSON_FILENAME = "update-center.actual.json";
    private static final String UPDATE_CENTER_JSON_HTML_FILENAME = "update-center.json.html";
    private static final String PLUGIN_DOCUMENTATION_URLS_JSON_FILENAME = "plugin-documentation-urls.json";
    private static final String PLUGIN_DESC_MARK_DOWN_FILENAME = "plugins.md";
    private static final String PLUGIN_TPIS_MARK_DOWN_FILENAME = "tpis.md";
    private static final String PLUGIN_VERSIONS_JSON_FILENAME = "plugin-versions.json";
    private static final String RELEASE_HISTORY_JSON_FILENAME = "release-history.json";
    private static final String RECENT_RELEASES_JSON_FILENAME = "recent-releases.json";
    private static final String PLATFORM_PLUGINS_JSON_FILENAME = "platform-plugins.json";
    private static final String PLATFORM_PLUGINS_RESOURCE_FILENAME = "platform-plugins.json";
    private static final String EOL = System.getProperty("line.separator");

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    private static final Logger PACKAGE_LOGGER = Logger.getLogger(Main.class.getPackage().getName());
}
