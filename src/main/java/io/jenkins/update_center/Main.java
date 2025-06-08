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

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Browser.NewPageOptions;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.qlangetch.tis.AbstractTISRepository;
import com.qlangetch.tis.ProfilesReader;
import com.qlangetch.tis.TISEndsDocsGenerator;
import com.qlangetch.tis.TISEndsDocsGenerator.CptConsumer;
import com.qlangetch.tis.impl.TISAliyunOSSRepositoryImpl;
import com.qlangetch.tis.impl.TISLocalPluginContextArtifactCoordinates;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.async.message.client.consumer.impl.MQListenerFactory;
import com.qlangtech.tis.datax.impl.DataxReader;
import com.qlangtech.tis.datax.impl.DataxWriter;
import com.qlangtech.tis.extension.Describable;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.PluginFormProperties;
import com.qlangtech.tis.extension.PluginManager;
import com.qlangtech.tis.extension.PluginWrapper;
import com.qlangtech.tis.extension.impl.PropertyType;
import com.qlangtech.tis.extension.model.UpdateCenter;
import com.qlangtech.tis.extension.model.UpdateCenterResource;
import com.qlangtech.tis.extension.util.PluginExtraProps;
import com.qlangtech.tis.extension.util.VersionNumber;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.plugin.IDataXEndTypeGetter;
import com.qlangtech.tis.plugin.IEndTypeGetter;
import com.qlangtech.tis.plugin.IEndTypeGetter.EndType;
import com.qlangtech.tis.plugin.IEndTypeGetter.EndTypeCategory;
import com.qlangtech.tis.plugin.IdentityName;
import com.qlangtech.tis.plugin.PluginCategory;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.plugin.incr.TISSinkFactory;
import com.qlangtech.tis.trigger.util.JsonUtil;
import com.qlangtech.tis.util.HeteroEnum;
import com.qlangtech.tis.util.Memoizer;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jenkins.lib.support_log_formatter.SupportLogFormatter;
import io.jenkins.update_center.args4j.LevelOptionHandler;
import io.jenkins.update_center.filters.JavaVersionPluginFilter;
import io.jenkins.update_center.json.PlatformPluginsRoot;
import io.jenkins.update_center.json.PluginDocumentationUrlsRoot;
import io.jenkins.update_center.json.PluginVersionsRoot;
import io.jenkins.update_center.json.RecentReleasesRoot;
import io.jenkins.update_center.json.ReleaseHistoryRoot;
import io.jenkins.update_center.json.TieredUpdateSitesGenerator;
import io.jenkins.update_center.json.UpdateCenterRoot;
import io.jenkins.update_center.util.JavaSpecificationVersion;
import io.jenkins.update_center.wrappers.AllowedArtifactsListMavenRepository;
import io.jenkins.update_center.wrappers.AlphaBetaOnlyRepository;
import io.jenkins.update_center.wrappers.FilteringRepository;
import io.jenkins.update_center.wrappers.StableWarMavenRepository;
import io.jenkins.update_center.wrappers.TruncatedMavenRepository;
import io.jenkins.update_center.wrappers.VersionCappedMavenRepository;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.kohsuke.args4j.ClassParser;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import javax.annotation.CheckForNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.qlangtech.tis.util.HeteroEnum.DATASOURCE;
import static com.qlangtech.tis.util.HeteroEnum.DATAX_READER;
import static com.qlangtech.tis.util.HeteroEnum.DATAX_WRITER;
import static com.qlangtech.tis.util.HeteroEnum.MQ;

/**
 * java -classpath ./lib/*:./update-center2.jar -Dplugin_dir_root=/tmp/release/tis-plugin -Dtis.plugin.release.version=3.4.0  io.jenkins.update_center.Main --www-dir=./dist
 */
public class Main {

    private static final String COMMUNITY_VIP_ICON = ":closed_lock_with_key:";

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

    @Option(name = "--generate-end-component-screenshot")
    public boolean generateEndComponents = false;


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

    static {
        // com.qlangtech.tis.extension.util.impl.DefaultGroovyShellFactory.setInConsoleModule();
    }

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
        // 只加载特定
        PluginManager.targetClassifierFilter = ProfilesReader.readeProfile();
        MavenRepository repo = createRepository();
        initializeLatestPluginVersions(skipLatestPluginRelease);

        if (tierListFile != null) {
            new TieredUpdateSitesGenerator().withRepository(repo).write(tierListFile, prettyPrint);
            return;
        }

        metadataWriter.writeMetadataFiles(repo, www);

        final String signedUpdateCenterJson
                = new UpdateCenterRoot(repo, new File(Main.resourcesDir, WARNINGS_JSON_FILENAME))
                .encodeWithSignature(signer, prettyPrint);

        final File updateCenterJson = new File(www, UPDATE_CENTER_JSON_FILENAME);
        writeToFile(updateCenterPostCallJson(signedUpdateCenterJson), updateCenterJson);
        writeToFile(signedUpdateCenterJson, new File(www, UPDATE_CENTER_ACTUAL_JSON_FILENAME));
        writeToFile(updateCenterPostMessageHtml(signedUpdateCenterJson), new File(www, UPDATE_CENTER_JSON_HTML_FILENAME));

        if (!skipUpdateCenter) {
            /*******************************************
             * deploy to remote OSS repository
             *******************************************/
            String ossPath = AbstractTISRepository.PLUGIN_RELEASE_VERSION + UpdateCenterResource.KEY_UPDATE_SITE + "/" + UpdateCenterResource.KEY_DEFAULT_JSON;
            TISAliyunOSSRepositoryImpl.getOSSClient().writeFile(ossPath, updateCenterJson);
        }


        MarkContentBuilder pluginList = new MarkContentBuilder();

        Map<String, List<PluginExtendsionImpl>> extendPoints = Maps.newHashMap();
        List<PluginExtendsionImpl> extendImpls = null;
        Collection<Plugin> artifacts = repo.listJenkinsPlugins();
        HPI latest = null;
        String excerpt = null;
        boolean isCommunityVIP;
        AtomicInteger communityVIPcountCount = new AtomicInteger();
        AtomicInteger communityEditionCount = new AtomicInteger();
        for (Plugin plugin : artifacts) {

            latest = plugin.getLatest();
            isCommunityVIP = latest.isCommunityVIP();
            if (isCommunityVIP) {
                communityVIPcountCount.incrementAndGet();
            } else {
                communityEditionCount.incrementAndGet();
            }
            pluginList.append("## ").append(isCommunityVIP ? COMMUNITY_VIP_ICON : StringUtils.EMPTY).append(latest.artifact.getArtifactName() + AbstractTISRepository.TIS_PACKAGE_EXTENSION).append("\n\n");

            if (!latest.isCommunityVIP()) {
                pluginList.append("* **下载地址：** ").append(String.valueOf(latest.getDownloadUrl())).append("\n\n");
            }


            excerpt = latest.getDescription();
            if (StringUtils.isNotBlank(excerpt)) {
                pluginList.append("* **介绍：** \n\n");
                appendRichMdContent(pluginList, 1, excerpt);
            }

            pluginList.append("* **扩展列表：** \n\n");
            for (Map.Entry<String, List<String>> e : latest.getExtendpoints().entrySet()) {
                extendImpls = extendPoints.get(e.getKey());
                pluginList.append("\t* [").append(e.getKey()).append("](./plugins#").append(getExtendPointHtmlAnchor(e.getKey())).append(")\n\n");
                if (extendImpls == null) {
                    extendImpls = Lists.newArrayList();
                    extendPoints.put(e.getKey(), extendImpls);
                }
                extendImpls.addAll(e.getValue().stream().map((impl) -> {
                    PluginExtendsionImpl eimpl = new PluginExtendsionImpl(impl, plugin.getLatest());
                    pluginList.append("\t\t * ");
                    eimpl.appendExtendImplMDSsript(pluginList);
                    pluginList.append("\n\n");
                    return eimpl;
                }).collect(Collectors.toList()));
            }
        }

        // tpis.mdx
        FileUtils.write(new File(www, PLUGIN_TPIS_MARK_DOWN_FILENAME)
                , (new MarkdownBuilder("header-tpis.txt", (headerContent) -> {
                    // {{communityEditionCount}}，**社区协作版插件包数量**：{{communityCollaborationEditionCount}}
                    headerContent = StringUtils.replace(headerContent, "{{communityEditionCount}}", String.valueOf(communityEditionCount.get()));
                    headerContent = StringUtils.replace(headerContent
                            , "{{communityCollaborationEditionCount}}", String.valueOf(communityVIPcountCount.get()));
                    return headerContent;
                }, pluginList.getContent())).build(), TisUTF8.get());

        StringBuffer extendsList = new StringBuffer();
        Descriptor descriptor = null;

        for (Map.Entry<String, List<PluginExtendsionImpl>> e : extendPoints.entrySet()) {
            System.out.println(e.getKey());
            extendsList.append("## ").append(e.getKey()).append("\n\n");

            for (PluginExtendsionImpl extendImpl : e.getValue()) {
                boolean vip = extendImpl.isCommunityVIP();
                descriptor = extendImpl.getDesc();
                if (descriptor != null) {

                    extendsList.append("### ").append(vip ? COMMUNITY_VIP_ICON : StringUtils.EMPTY).append(descriptor.clazz.getName()).append("\n\n");
                    extendsList.append("* **显示名:** ").append(descriptor.getDisplayName()).append(" \n\n");
                    extendsList.append("* **全路径名:** [").append(extendImpl.extendImpl).append("](").append(extendImpl.getExtendImplURL()).append(") \n\n");
                    if (descriptor instanceof IEndTypeGetter) {
                        IEndTypeGetter endType = (IEndTypeGetter) descriptor;
                        IEndTypeGetter.PluginVender vender = endType.getVender();
                        extendsList.append("* **提供者:** [").append(vender.getName()).append("](").append(vender.getUrl()).append(") \n\n");
                    }

                } else {
                    extendsList.append("### ").append(vip ? COMMUNITY_VIP_ICON : StringUtils.EMPTY).append(extendImpl.extendImpl).append("\n\n");
                }
                // 社区版(免费) or 社区协作
                extendsList.append("* **费用:** ").append(vip ? (COMMUNITY_VIP_ICON + " `社区协作") : ":smile: `社区版(免费)").append("`").append("\n\n");
                extendsList.append("* **插件包:** [").append(extendImpl.getArchiveFileName())
                        .append("](./tpis#").append(extendImpl.getArchiveFileNameHtmlAnchor()).append(")").append("\n\n");
//                md.append("* 费用:");
//                md.append("* 版本:");
//                md.append("* 包大小:");
//                md.append("* 打包时间:");
                if (descriptor == null) {
                    continue;
                }
                extendsList.append(buildFieldDescListByMD(descriptor));
            }
        }

        AtomicInteger extendPointCount = new AtomicInteger();
        AtomicInteger implCount = new AtomicInteger();
        for (Map.Entry<String, List<PluginExtendsionImpl>> e : extendPoints.entrySet()) {
            extendPointCount.incrementAndGet();
            implCount.addAndGet(e.getValue().size());
        }

        if (MapUtils.isEmpty(extendPoints)) {
            throw new IllegalStateException("extendPoints can not be null");
        }
//        Memoizer<IEndTypeGetter.EndType, EndTypePluginStore> endTypePluginDescs
//                = new Memoizer<IEndTypeGetter.EndType, EndTypePluginStore>() {
//            @Override
//            public EndTypePluginStore compute(IEndTypeGetter.EndType key) {
//                return new EndTypePluginStore();
//            }
//        };

        AllEndTypePluginProcess allEndTypePluginProcess = new AllEndTypePluginProcess(extendPoints);

        final MarkdownBuilder tabView = new MarkdownBuilder(
                "header-source-sink.txt", (h) -> h
                , allEndTypePluginProcess.drawEndTypePluginTableView()
                , Optional.of("footer-source-sink.txt"));

        final File pluginCategory = allEndTypePluginProcess.processCategoryPlugin();
        if (!skipUpdateCenter) {
            String ossPath = AbstractTISRepository.PLUGIN_RELEASE_VERSION + UpdateCenterResource.KEY_UPDATE_SITE + "/" + UpdateCenter.PLUGIN_CATEGORIES_FILENAME;
            TISAliyunOSSRepositoryImpl.getOSSClient().writeFile(ossPath, pluginCategory);
        }

        if (this.generateEndComponents) {
            // 生成各个数据端的图片
            allEndTypePluginProcess.generateEndComponents();
        }


        FileUtils.write(new File(www, PLUGIN_TABVIEW_MARK_DOWN_FILENAME), tabView.build(), TisUTF8.get());
        // plugins.mdx
        FileUtils.write(new File(www, PLUGIN_DESC_MARK_DOWN_FILENAME)
                , (new MarkdownBuilder("header-plugins.txt", (headerContent) -> {

                    // {{extnedPointCount}}，**实现插件**：{{extnedPointImplCount}}
                    headerContent = StringUtils.replace(headerContent, "{{extnedPointCount}}", String.valueOf(extendPointCount.get()));
                    headerContent = StringUtils.replace(headerContent, "{{extnedPointImplCount}}", String.valueOf(implCount.get()));

                    return headerContent;
                }, extendsList)).build(), TisUTF8.get());

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

    private StringBuffer buildFieldDescListByMD(Descriptor descriptor) throws IOException {
        return buildFieldDescListByMD(false, null, descriptor);
    }

    /**
     * @param appendPluginFieldsElement 是否在配置项列表外嵌套一个<PluginFields/>标签，用于美化列表的数字
     * @param descriptor
     * @return
     * @throws IOException
     */
    private StringBuffer buildFieldDescListByMD(boolean appendPluginFieldsElement, IdentityName descId, Descriptor descriptor) throws IOException {
        MarkContentBuilder extendsList = new MarkContentBuilder();
        PluginFormProperties pluginFormPropertyTypes = descriptor.getPluginFormPropertyTypes();

        final List<Map.Entry<String, PropertyType>> props
                = pluginFormPropertyTypes.getSortedUseableProperties();//.accept(new PluginFormProperties.IVisitor() {
        PropertyType ptype = null;
        PluginExtraProps.Props extraProps = null;
        if (CollectionUtils.isNotEmpty(props)) {
            extendsList.append("* **配置项说明:** ").appendReturn(2);
//                    md.append("|  配置项    | 类型    | 必须     | 说明    |").append("\n\n");
//                    md.append("|  :-----   | :-----  | :-----  | :-----  |").append("\n\n");
            if (appendPluginFieldsElement) {
                extendsList.append("<PluginFields>").appendReturn(2);
            }
            int index = 1;
            for (Map.Entry<String, PropertyType> prop : props) {
                ptype = prop.getValue();
                extraProps = ptype.extraProp;

                extendsList.appendLiBlock(index++);//.append(". ");
                if (extraProps == null) {
                    extendsList.append(prop.getKey()).appendReturn();
                } else {
                    extendsList.append(StringUtils.defaultString(extraProps.getLable(), prop.getKey())).appendReturn(2);
                }

                extendsList.append("\t* **类型:** ").append(descFieldType(ptype.formField)).appendReturn();
                extendsList.append("\t* **必须:** ").append(descRequire(ptype.formField)).appendReturn();

                if (extraProps == null) {
                    continue;
                }
                Object dftVal = extraProps.getDftVal();
                extendsList.append("\t* **默认值:** ")
                        .append((dftVal == null) ? "无" : String.valueOf(dftVal)).appendReturn();

                extendsList.append("\t* **说明:** ");

                if (StringUtils.isEmpty(extraProps.getAsynHelp())) {
                    // extendsList.append().append("\n\n");
                    processLine(extendsList, 2, StringUtils.defaultString(extraProps.getHelpContent(), "无"));
                    extendsList.appendReturn();
                } else {
                    extendsList.appendReturn(2);
                    appendRichMdContent(extendsList, 2, extraProps.getAsynHelp());
                    extendsList.appendReturn();
                }
                LineIterator lineIt = null;
                if (appendPluginFieldsElement && ptype.isDescribable()) {
                    List<? extends Descriptor> applicableDescriptors = ptype.getApplicableDescriptors();
                    extendsList.append("\t* **可选项说明:** 可选")
                            .append(applicableDescriptors.stream().map((desc) -> "`" + desc.getDisplayName() + "`").collect(Collectors.joining(",")))
                            .append("以下是详细说明：").appendReturn();

                    for (Descriptor propDesc : applicableDescriptors) {
                        extendsList.append("\t\t* ").append(propDesc.getDisplayName()).appendReturn(2);

                        if (propDesc.getPropertyFields().size() > 0) {
                            extendsList.append("\t\t\t<Figure img={require('./"
                                    + TISEndsDocsGenerator.createPluginDescriblePropFieldImageName(descId, ptype, propDesc) + "')}/>\n\n");


                            try (StringReader propMDDoc
                                         = new StringReader(String.valueOf(
                                    buildFieldDescListByMD(false, descId, propDesc)))) {
                                lineIt = IOUtils.lineIterator(propMDDoc);
                                while (lineIt.hasNext()) {
                                    extendsList.append("\t\t\t" + lineIt.nextLine()).appendReturn();
                                }
                            }
                        }
                    }

                }
            }
            if (appendPluginFieldsElement) {
                extendsList.append("\n</PluginFields>");
            }
        }
        return extendsList.getContent();
    }

    /**
     * 需要确保每个li的最后不能有三个回车符号，不然<ol>下的<li>重新设置序号（从1开始）
     */
    private static class MarkContentBuilder {
        private final StringBuffer extendsList = new StringBuffer();
        int returnCharNumber;

        public StringBuffer getContent() {
            processTailReturnChar();
            return this.extendsList;
        }

        public MarkContentBuilder append(String content) {
            if (!"\t".equals(content) && StringUtils.isBlank(content)) {
                return this;
            }
            int contentLastReturnNumber = 0;
            int idx = content.length() - 1;
            for (; idx >= 0; idx--) {
                if ('\n' != content.charAt(idx)) {
                    break;
                } else {
                    contentLastReturnNumber++;
                }
            }
            if (returnCharNumber > 0) {
                while (returnCharNumber-- > 0) {
                    this.extendsList.append("\n");
                }
            }
            this.extendsList.append(StringUtils.substring(content, 0, idx + 1));
            returnCharNumber = contentLastReturnNumber;
            return this;
        }

        public MarkContentBuilder appendReturn() {
            return this.appendReturn(1);
        }

        public MarkContentBuilder appendReturn(int count) {
            returnCharNumber += count;
            return this;
        }

        public void appendLiBlock(int orderNum) {
            processTailReturnChar();
            extendsList.append(orderNum).append(". ");
            returnCharNumber = 0;
        }

        private void processTailReturnChar() {
            if (returnCharNumber > 0) {
                returnCharNumber = Math.min(2, returnCharNumber);
                while (returnCharNumber-- > 0) {
                    this.extendsList.append("\n");
                }
            }
        }
    }

    /**
     * @param validateMsg
     * @param batchs
     * @param incrs
     */
    private void validateBatchIncrEndMatch(StringBuffer validateMsg
            , List<Pair<IDataXEndTypeGetter, Descriptor>> batchs, List<Pair<IEndTypeGetter, Descriptor>> incrs) {
        for (Pair<IDataXEndTypeGetter, Descriptor> p : batchs) {
            if (p.getLeft().isSupportIncr() ^ CollectionUtils.isNotEmpty(incrs)) {
                validateMsg.append(p.getRight().clazz.getName())
                        .append(",supportIncr:").append(p.getLeft().isSupportIncr())
                        .append(",incr end size:" + incrs.size()).append("\n");
            }
        }
    }

    private void addToEndTypeStore(Memoizer<IEndTypeGetter.EndType
            , EndTypePluginStore> endTypePluginDescs
            , Descriptor pluginDesc
            , PluginDescConsumer consumer) {

        IEndTypeGetter endTypeGetter = (IEndTypeGetter) pluginDesc;

        Pair<IEndTypeGetter, Descriptor> typedDesc = Pair.of(endTypeGetter, pluginDesc);

        consumer.accept(endTypePluginDescs.get(Objects.requireNonNull(endTypeGetter.getEndType())), typedDesc);

    }


    interface PluginDescConsumer {
        void accept(EndTypePluginStore pluginStore, Pair<IEndTypeGetter, Descriptor> typedDesc);
    }


    private static final String CHECK_ICON = "<i className={clsx('tis-check')}></i>";

    final static String[] colors = new String[]{"tomato", "orange", "dodgerblue", "MediumSeaGreen", "Gray", "SlateBlue", "Violet", "LightGray"};

//    private Memoizer<IEndTypeGetter.PluginVender, String> venderColor = new Memoizer<IEndTypeGetter.PluginVender, String>() {
//        int index = 0;
//
//        @Override
//        public String compute(IEndTypeGetter.PluginVender key) {
//            return colors[index++];
//        }
//    };


    private void drawPluginCell(StringBuffer tabView, List<Pair<IEndTypeGetter, Descriptor>> plugins) {
        tabView.append("<td>");

        if (CollectionUtils.isNotEmpty(plugins)) {
            tabView.append(CHECK_ICON);
        }
        final int[] index = new int[]{1};

        for (Pair<IEndTypeGetter, Descriptor> p : plugins) {
            buildPluginLink(tabView, p.getLeft().getVender(), (s) -> {
                PluginExtendsionImpl eimpl = null;
                eimpl = new PluginExtendsionImpl(p.getRight().clazz.getName(), null);
                eimpl.appendExtendImplMDSsript(String.valueOf(index[0]++), true, s);
            });
        }
        tabView.append("</td>");
    }

    private void buildPluginLink(StringBuffer script, IEndTypeGetter.PluginVender vender, Consumer<StringBuffer> titleAppender) {
        script.append("<i className='plugin-link ").append(vender.getTokenId()).append("-color'>");
        titleAppender.accept(script);
        script.append("</i>");
    }


    static class PluginClass {
        private final PluginClassAndDescClassPair pluginDescClassPair;
        private final String gav;

        public PluginClass(PluginClassAndDescClassPair pluginDescClassPair, String gav) {
            this.pluginDescClassPair = pluginDescClassPair;
            this.gav = gav;
        }

        public String getClazz() {
            return this.pluginDescClassPair.getPluginParentClazz().getName();
        }

        public String getDescClass() {
            Class<? extends Descriptor> descClazz = this.pluginDescClassPair.getDescParentPlugin();
            if (descClazz == null) {
                return null;
            }
            return descClazz.getName();
        }

        public String getGav() {
            return gav;
        }
    }

    /**
     * 扩展类的父类和Descriptor类的 对应
     */
    static class PluginClassAndDescClassPair {
        private final Class<? extends Describable> pluginParentClazz;
        private final Class<? extends Descriptor> descPlugin;

        boolean describleMatched = false;

        public PluginClassAndDescClassPair(Class<? extends Describable> pluginParentClazz, Class<? extends Descriptor> descPlugin) {
            this.pluginParentClazz = pluginParentClazz;
            this.descPlugin = descPlugin;
        }

        public Class<? extends Describable> getPluginParentClazz() {
            return this.pluginParentClazz;
        }

        public Class<? extends Descriptor> getDescParentPlugin() {
            return this.descPlugin;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PluginClassAndDescClassPair that = (PluginClassAndDescClassPair) o;
            return com.google.common.base.Objects.equal(pluginParentClazz, that.pluginParentClazz) &&
                    com.google.common.base.Objects.equal(descPlugin, that.descPlugin);
        }

        @Override
        public int hashCode() {
            return com.google.common.base.Objects.hashCode(pluginParentClazz, descPlugin);
        }
    }


    private class AllEndTypePluginProcess {
        public final Class<DataxReader> extendPointDataXReader = DataxReader.class;
        public final Class<DataxWriter> extendPointDataXWriter = DataxWriter.class;
        public final Class<TISSinkFactory> extnedPointIncrSink = TISSinkFactory.class;
        public final Class<MQListenerFactory> extendPointIncrSources = MQListenerFactory.class;
        boolean hasInitEndTypePluginDescs;
        Memoizer<IEndTypeGetter.EndType, EndTypePluginStore> dataEndTypePluginDescs
                = new Memoizer<IEndTypeGetter.EndType, EndTypePluginStore>() {
            @Override
            public EndTypePluginStore compute(IEndTypeGetter.EndType key) {
                return new EndTypePluginStore();
            }
        };
        final Set<EndType> assistTypes = (EndType.getAssistTypes());
        final Set<EndType> transformerTypes = (EndType.getTransformerTypes());


        Memoizer<IEndTypeGetter.EndType, EndTypePluginStore> assistEndTypePluginDescs
                = new Memoizer<IEndTypeGetter.EndType, EndTypePluginStore>() {
            @Override
            public EndTypePluginStore compute(IEndTypeGetter.EndType key) {
                return new EndTypePluginStore();
            }
        };

        Memoizer<IEndTypeGetter.EndType, EndTypePluginStore> transformerEndTypePluginDescs
                = new Memoizer<IEndTypeGetter.EndType, EndTypePluginStore>() {
            @Override
            public EndTypePluginStore compute(IEndTypeGetter.EndType key) {
                return new EndTypePluginStore();
            }
        };

        final Map<String, List<PluginExtendsionImpl>> extendPoints;

        public AllEndTypePluginProcess(Map<String, List<PluginExtendsionImpl>> extendPoints) {
            this.extendPoints = extendPoints;
        }

        /**
         * 重新将所有的插件 归类，为自动生成的骨架代码的maven dependencies 提供依据
         *
         * @return
         */
        public final File processCategoryPlugin() {
            Map<PluginCategory, Set<PluginClassAndDescClassPair>> pluginCategories = Maps.newConcurrentMap();

            Map<IEndTypeGetter.EndType, EndTypePluginStore> snapshot = this.dataEndTypePluginDescs.snapshot();
            EndTypePluginStore endtypeStore = null;
            Set<Descriptor> processd = Sets.newHashSet();
            for (Map.Entry<IEndTypeGetter.EndType, EndTypePluginStore> entry : snapshot.entrySet()) {
                endtypeStore = entry.getValue();

                ((List<Pair<IDataXEndTypeGetter, Descriptor>>) CollectionUtils.union(endtypeStore.dataXReaders, endtypeStore.dataXWriters))
                        .forEach((p) -> {
                            //Sets.newHashSet(extendPointDataXReader, extendPointDataXWriter)
                            processd.add(p.getRight());
                            parseCategoryPlugin(PluginCategory.BATCH, pluginCategories, p.getRight());
                        });


                ((List<Pair<IEndTypeGetter, Descriptor>>) CollectionUtils.union(endtypeStore.incrSinks, endtypeStore.incrSources))
                        .forEach((p) -> {
                            // Sets.newHashSet(extnedPointIncrSink, extendPointIncrSources)
                            processd.add(p.getRight());
                            parseCategoryPlugin(PluginCategory.INCR, pluginCategories, p.getRight());
                        });
            }

            TIS.get().extensionLists.snapshot().forEach((key, entry) -> {
                entry.forEach((desc) -> {
                    if (processd.contains(desc)) {
                        return;
                    }
                    parseCategoryPlugin(PluginCategory.NONE, pluginCategories, (Descriptor) desc);
                });
            });


            try {
                JSONObject categoryContent = new JSONObject();
                PluginManager pluginManager = TIS.get().getPluginManager();
                for (Map.Entry<PluginCategory, Set<PluginClassAndDescClassPair>> entry : pluginCategories.entrySet()) {
                    List<PluginClass> pluginClas = entry.getValue().stream().map((pair) -> {
                        Class clazz = pair.getPluginParentClazz();
                        PluginWrapper pluginWrapper = pluginManager.whichPlugin(clazz);
                        return new PluginClass(pair, pluginWrapper != null
                                ? TISLocalPluginContextArtifactCoordinates.create(pluginWrapper.manifest, null).getGav()
                                : null);
                    }).collect(Collectors.toList());
                    categoryContent.put(entry.getKey().getToken(), pluginClas);
                }
                File pluginCategory = new File(www, UpdateCenter.PLUGIN_CATEGORIES_FILENAME);
                FileUtils.write(pluginCategory, JsonUtil.toString(categoryContent), TisUTF8.get());
                return pluginCategory;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }

        private void parseCategoryPlugin(PluginCategory category, Map<PluginCategory
                , Set<PluginClassAndDescClassPair>> pluginCategories, Descriptor desc) {
            Set<PluginClassAndDescClassPair> categoryPlugin = pluginCategories.computeIfAbsent(category, (key) -> Sets.newHashSet());


            List<PluginClassAndDescClassPair> superDescPairs = Lists.newArrayList();
            List<Class<?>> superDescClasses = ClassUtils.getAllSuperclasses(desc.getClass());
            for (Class<?> superDescClazz : superDescClasses) {
                if (superDescClazz == Descriptor.class) {
                    break;
                }
                superDescPairs.add(new PluginClassAndDescClassPair(
                        (Class<? extends Describable>) superDescClazz.getEnclosingClass(), (Class<? extends Descriptor>) superDescClazz));

            }
            //desc.getT()
            Class describleClazz = desc.clazz;

            List<Class<?>> superClasses = ClassUtils.getAllSuperclasses(describleClazz);
            for (Class<?> sper : superClasses) {
                if (sper == Object.class) {
                    break;
                }
                categoryPlugin.add(findPluginClassAndDescClassPair(superDescPairs, (Class<? extends Describable>) sper));
                if (sper.getSuperclass() == Object.class) {
                    break;
                }

//                if (categoryPluginClasses.contains(sper)) {
//                    break;
//                }
            }
        }

        private PluginClassAndDescClassPair findPluginClassAndDescClassPair(List<PluginClassAndDescClassPair> superDescPairs, Class<? extends Describable> sperDescribleClazz) {
            for (PluginClassAndDescClassPair superDescPair : superDescPairs) {
                if (superDescPair.getPluginParentClazz() == sperDescribleClazz) {
                    superDescPair.describleMatched = true;
                    return superDescPair;
                }
            }
            if (superDescPairs.size() < 1) {
                // 说明继承路径上没有中间 parent descriptor class 可用
                return new PluginClassAndDescClassPair(sperDescribleClazz, null);
            } else {
                Optional<PluginClassAndDescClassPair> first = superDescPairs.stream().filter((superPair) -> !superPair.describleMatched).findFirst();
                return new PluginClassAndDescClassPair(sperDescribleClazz, (first.isPresent() ? first.get().getDescParentPlugin() : null));
            }
        }

        private TreeSet<Map.Entry<IEndTypeGetter.EndType, EndTypePluginStore>>
        createCaseOrderSet(Memoizer<IEndTypeGetter.EndType, EndTypePluginStore> endTypePluginDescs) {
            final TreeSet<Map.Entry<IEndTypeGetter.EndType, EndTypePluginStore>> ends
                    = Sets.newTreeSet(new Comparator<Entry<EndType, EndTypePluginStore>>() {
                @Override
                public int compare(Entry<EndType, EndTypePluginStore> o1, Entry<EndType, EndTypePluginStore> o2) {
                    return String.CASE_INSENSITIVE_ORDER.compare(o1.getKey().name(), o2.getKey().name());
                }
            });
            ends.addAll(endTypePluginDescs.snapshot().entrySet());
            return ends;
        }

        /**
         * 使用playwight产出TIS 各端说明书
         */
        public void generateEndComponents() throws Exception {
            if (!this.hasInitEndTypePluginDescs) {
                this.drawEndTypePluginTableView();
            }
            File endsDocRoot = new File(www, "ends");
            File endDoc = null;
            File _category_ = null;
            File endDir = null;
            // EndTypePluginStore endTypePluginStore = null;
            MarkdownBuilder tabView = null;
            // StringBuffer bodyContent = null;

            try (Playwright playwright = Playwright.create()) {
                Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                        .setHeadless(false));
                // BrowserContext context = browser.newContext();
                Page page = browser.newPage(new NewPageOptions().setViewportSize(1680, 2000));

                final TreeSet<Map.Entry<IEndTypeGetter.EndType, EndTypePluginStore>> dataEnds
                        = createCaseOrderSet(dataEndTypePluginDescs);

                buildEndTypeDocAndShutcutImage(dataEnds, EndTypeCategory.Data, endsDocRoot, page);


                final TreeSet<Map.Entry<IEndTypeGetter.EndType, EndTypePluginStore>> assistEnds
                        = createCaseOrderSet(assistEndTypePluginDescs);

                buildEndTypeDocAndShutcutImage(assistEnds, EndTypeCategory.Assist, endsDocRoot, page);

                final TreeSet<Map.Entry<IEndTypeGetter.EndType, EndTypePluginStore>> transformerEnds = createCaseOrderSet(transformerEndTypePluginDescs);

                buildEndTypeDocAndShutcutImage(transformerEnds, EndTypeCategory.Transformer, endsDocRoot, page);


                browser.close();
            }
        }

        private void buildEndTypeDocAndShutcutImage(
                TreeSet<Entry<EndType, EndTypePluginStore>> dataEnds, EndTypeCategory endCategory, File endsDocRoot, Page page) throws IOException {
            int position = 1;
            File endDir;
            File _category_;
            File endDoc;
            MarkdownBuilder tabView;
            for (Entry<EndType, EndTypePluginStore> entry : dataEnds) {

                EndType endType = entry.getKey();
                EndTypePluginStore pluginStore = entry.getValue();
                if (entry.getKey() != EndType.AutoGen) {
                    //  continue;
                }

                endDir = new File(endsDocRoot, StringUtils.lowerCase(endType.category.name()) + "/" + entry.getKey().name());
                endDoc = new File(endDir, "index.mdx");
                _category_ = new File(endDir, "_category_.json");

                FileUtils.write(_category_, "{\n" +
                        "  \"label\": \"" + entry.getKey().name() + "\",\n" +
                        "  \"position\": " + (position++) + "\n" +
                        "}");


                final StringBuffer bodyContent = new StringBuffer();


                TISEndsDocsGenerator.buildEndTypeImages(page, endDir, endType, endCategory, entry.getValue()
                        , new CptConsumer() {
                            @Override
                            public void accept(HeteroEnum hetero, IdentityName descId, Descriptor desc) {
                                if (endCategory == EndTypeCategory.Assist || endCategory == EndTypeCategory.Transformer) {
                                    if (!assistTypes.contains(endType) && !transformerTypes.contains(endType)) {
                                        throw new IllegalStateException("endType:" + endType + " must contain in assistTypes or transformerTypes");
                                    }
                                    bodyContent.append("## ").append(desc.getDisplayName()).append("\n\n");
                                    bodyContent.append("<Figure img={require('./" + (desc.clazz.getSimpleName()) + ".png')}/>\n\n");
                                } else if (hetero.getExtensionPoint() == DATAX_READER.getExtensionPoint()) {
                                    bodyContent.append("## 批量读\n\n");
                                    bodyContent.append("<Figure img={require('./dataxReader.png')}/>\n\n");
                                } else if (hetero.getExtensionPoint() == DATAX_WRITER.getExtensionPoint()) {
                                    bodyContent.append("## 批量写\n\n");
                                    bodyContent.append("<Figure img={require('./dataxWriter.png')}/>\n\n");
                                } else if (hetero.getExtensionPoint() == MQ.getExtensionPoint()) {
                                    bodyContent.append("## 实时读\n\n");
                                    bodyContent.append("<Figure img={require('./mq.png')}/>\n\n");
                                } else if (hetero.getExtensionPoint() == TISSinkFactory.sinkFactory.getExtensionPoint()) {
                                    bodyContent.append("## 实时写\n\n");
                                    bodyContent.append("<Figure img={require('./sinkFactory.png')}/>\n\n");
                                } else if (hetero.getExtensionPoint() == DATASOURCE.getExtensionPoint()) {
                                    bodyContent.append("## 数据源配置\n\n");
                                    bodyContent.append("<Figure img={require('./datasource.png')}/>\n\n");
                                } else if (HeteroEnum.PARAMS_CONFIG.getExtensionPoint() == hetero.getExtensionPoint()) {
                                    bodyContent.append("## 数据端配置\n\n");
                                    bodyContent.append("<Figure img={require('./params-cfg.png')}/>\n\n");
                                } else {
                                    throw new IllegalStateException("illegal type:" + hetero.getExtensionPoint());
                                }

                                try {
                                    bodyContent.append(Main.this.buildFieldDescListByMD(true, descId, desc)).append("\n\n");
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        });

                tabView = new MarkdownBuilder(
                        "plugin-component-header.txt"
                        , (h) -> {
                    return StringUtils.replace(h, "${title}", endType.name());
                }
                        , bodyContent
                        , Optional.of("plugin-component-footer.txt"));


                FileUtils.write(endDoc, tabView.build(), TisUTF8.get(), false);
            }
        }

        /**
         * 插件实现视图
         */
        private StringBuffer drawEndTypePluginTableView() {
            try {
                final Set<EndType> dataEnds = EndType.getDataEnds();
                // final Set<EndType> assistTypes = Sets.newHashSet(EndType.getAssistTypes());
                /**
                 * 生成reader/writer(batch/incr) 一览视图
                 */
                Descriptor pluginDesc = null;
                // IEndTypeGetter endTypeGetter = null;
                for (Map.Entry<String, List<PluginExtendsionImpl>> e : extendPoints.entrySet()) {
                    for (PluginExtendsionImpl impl : e.getValue()) {
                        pluginDesc = impl.getDesc();
                        if (pluginDesc == null) {
                            continue;
                        }

                        if (extendPointDataXReader.isAssignableFrom(pluginDesc.clazz)) {
                            addToEndTypeStore(dataEndTypePluginDescs, pluginDesc
                                    , (store, typedDesc) -> store.dataXReaders.add(Pair.of((IDataXEndTypeGetter) typedDesc.getLeft(), typedDesc.getRight())));
                            continue;
                        }

                        if (extendPointDataXWriter.isAssignableFrom(pluginDesc.clazz)) {
                            addToEndTypeStore(dataEndTypePluginDescs, pluginDesc
                                    , (store, typedDesc) -> store.dataXWriters.add(Pair.of((IDataXEndTypeGetter) typedDesc.getLeft(), typedDesc.getRight())));
                            continue;
                        }

                        if (extnedPointIncrSink.isAssignableFrom(pluginDesc.clazz)) {
                            addToEndTypeStore(dataEndTypePluginDescs, pluginDesc, (store, typedDesc) -> store.incrSinks.add(typedDesc));
                            continue;
                        }

                        if (extendPointIncrSources.isAssignableFrom(pluginDesc.clazz)) {
                            addToEndTypeStore(dataEndTypePluginDescs, pluginDesc, (store, typedDesc) -> store.incrSources.add(typedDesc));
                            continue;
                        }

                        if (pluginDesc instanceof IEndTypeGetter) {
                            // 把plugin添加到杂项插件存储里去，改属性用于playwight写插件图流程
                            EndType endType = ((IEndTypeGetter) pluginDesc).getEndType();
                            if (dataEnds.contains(endType)) {
                                addToEndTypeStore(dataEndTypePluginDescs, pluginDesc, (store, typedDesc) -> {
                                    store.miscPlugins.add(typedDesc);
                                });
                            }

                            if (assistTypes.contains(endType)) {
                                addToEndTypeStore(assistEndTypePluginDescs, pluginDesc, (store, typedDesc) -> {
                                    store.miscPlugins.add(typedDesc);
                                });
                            }

                            if (transformerTypes.contains(endType)) {
                                addToEndTypeStore(transformerEndTypePluginDescs, pluginDesc, (store, typedDesc) -> {
                                    store.miscPlugins.add(typedDesc);
                                });
                            }
                        }
                    }
                }

                StringBuffer validateMsg = new StringBuffer();
                for (Map.Entry<IEndTypeGetter.EndType, EndTypePluginStore> entry
                        : dataEndTypePluginDescs.snapshot().entrySet()) {

                    EndTypePluginStore store = entry.getValue();
                    validateBatchIncrEndMatch(validateMsg, store.dataXReaders, store.incrSources);
                    validateBatchIncrEndMatch(validateMsg, store.dataXWriters, store.incrSinks);
                }
                if (!AbstractTISRepository.isSNAPSHOTVersion() && validateMsg.length() > 0) {
                    throw new RuntimeException("\n" + validateMsg.toString());
                }

                IEndTypeGetter.EndType endType = null;
                EndTypePluginStore store = null;
                StringBuffer tabView = new StringBuffer();
                tabView.append("<table style={{width: '100%', display: 'table'}}  border='1'>\n");
                tabView.append("<thead>");
                tabView.append("<tr><th rowspan='2'>类型</th><th colspan='2'>批量(DataX)</th><th colspan='2'>实时</th></tr>\n");
                tabView.append("<tr><th width='20%'>读</th><th width='20%'>写</th><th width='20%'>Source</th><th width='20%'>Sink</th></tr>\n");
                tabView.append("</thead>");
                tabView.append("<tbody>\n");
                for (Map.Entry<IEndTypeGetter.EndType, EndTypePluginStore> entry
                        : dataEndTypePluginDescs.snapshot().entrySet()) {
                    endType = entry.getKey();
                    store = entry.getValue();
                    tabView.append("<tr>\n");
                    tabView.append("<td class='endtype-name").append("'>").append(endType.name()).append("</td>");
                    drawPluginCell(tabView, store.convertDataXReaders());
                    drawPluginCell(tabView, store.convertDataXWriters());
                    drawPluginCell(tabView, store.incrSources);
                    drawPluginCell(tabView, store.incrSinks);

                    tabView.append("</tr>\n");
                }
                tabView.append("</tbody>");
                tabView.append("\n</table>");

                StringBuffer script = new StringBuffer("<p><strong>Provider:</strong> ");

                for (IEndTypeGetter.PluginVender vender : IEndTypeGetter.PluginVender.values()) {
                    buildPluginLink(script, vender, (buffer) -> {
                        buffer.append("<a target='_blank' href='").append(vender.getUrl()).append("'>").append(vender.getName()).append("</a>");
                    });
                }
                script.append("</p>");
                return script.append("\n\n").append(tabView);
            } finally {
                this.hasInitEndTypePluginDescs = true;
            }
        }

    }

    public static class EndTypePluginStore {


        public List<Pair<IDataXEndTypeGetter, Descriptor>> dataXReaders = Lists.newArrayList();
        public List<Pair<IDataXEndTypeGetter, Descriptor>> dataXWriters = Lists.newArrayList();
        public List<Pair<IEndTypeGetter, Descriptor>> incrSources = Lists.newArrayList();
        public List<Pair<IEndTypeGetter, Descriptor>> incrSinks = Lists.newArrayList();


        /**
         * 杂项插件，例如：DataSource，ElasticEndpoint
         */
        public List<Pair<IEndTypeGetter, Descriptor>> miscPlugins = Lists.newArrayList();

        List<Pair<IEndTypeGetter, Descriptor>> convertDataXReaders() {
            return this.convert(this.dataXReaders);
        }


        List<Pair<IEndTypeGetter, Descriptor>> convertDataXWriters() {
            return this.convert(this.dataXWriters);
        }


        private List<Pair<IEndTypeGetter, Descriptor>> convert(
                List<Pair<IDataXEndTypeGetter, Descriptor>> dataxComponents) {
            return dataxComponents.stream()
                    .map((pair) ->
                            Pair.of((IEndTypeGetter) pair.getLeft(), pair.getRight()))
                    .collect(Collectors.toList());
        }


//        public final boolean isChecked() {
//            return CollectionUtils.isNotEmpty(dataXReaders)
//                    || CollectionUtils.isNotEmpty(dataXWriters)
//                    || CollectionUtils.isNotEmpty(incrSources)
//                    || CollectionUtils.isNotEmpty(incrSinks);
//        }
    }

    private String getExtendPointHtmlAnchor(String key) {
        return escapeHtmlAnchor(key);
    }

    private static String escapeHtmlAnchor(String key) {
        return StringUtils.lowerCase(StringUtils.remove(key, "."));
    }

    private static Pattern MARKDOWN_LINK = Pattern.compile("\\[(.+)\\]\\((.+)\\)");

    private void appendRichMdContent(MarkContentBuilder extendsList, int indent, String content) throws IOException {
        try (StringReader mdReader = new StringReader(StringUtils.trim(content))) {
            for (String line : IOUtils.readLines(mdReader)) {
                for (int i = 0; i < indent; i++) {
                    extendsList.append("\t");
                }
                // 需要将文档中 有如下链接 [xxx](/base/ddd) 需要转化成[xxx](.) 这样文档在编译时候才不会出错
                Matcher match = MARKDOWN_LINK.matcher(line);
                String link = null;
                if (match.find()) {
                    link = match.group(2);
                    if (!StringUtils.startsWith(link, "http")) {
                        link = ".";
                    }
                    line = match.replaceAll("[$1](" + link + ")");
                }
                extendsList.append(line).appendReturn();
                //  processLine(extendsList, indent, line);
            }
        }
    }

    private static void processLine(MarkContentBuilder extendsList, int indent, String line) {
        int lineNum = 0;
        for (String l : StringUtils.split(line, "\n")) {
            for (int i = 0; i < indent; i++) {
                extendsList.append("\t");
            }
            extendsList.append(l).appendReturn();
        }
    }

    private static class PluginExtendsionImpl {
        private final String extendImpl;
        private final HPI hpi;

        private Descriptor descriptor;

        public PluginExtendsionImpl(String extendImpl, HPI hpi) {
            this.extendImpl = extendImpl;
            this.hpi = hpi;

        }

        public final boolean isCommunityVIP() {
            return hpi.isCommunityVIP();
        }

        public String getExtendImplURL() {
            try {
                final String scmUrl = hpi.getScmUrl();
                boolean endWithSlash = StringUtils.endsWith(scmUrl, "/");
                return scmUrl + (endWithSlash ? StringUtils.EMPTY : "/") + "src/main/java/"
                        + StringUtils.replace(extendImpl, ".", "/") + ".java";
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public Descriptor getDesc() {
            if (this.descriptor == null) {
                this.descriptor = TIS.get().getDescriptor(this.extendImpl);
            }
            return descriptor;
        }


        public String getHtmlAnchor() {
            return escapeHtmlAnchor(extendImpl);
        }

        public void appendExtendImplMDSsript(String linkTitle, boolean html, StringBuffer md) {

            if (html) {
                md.append("<Link to={plugins.metadata.permalink+'").append("#").append(this.getHtmlAnchor()).append("'}>").append(linkTitle).append("</Link>");
            } else {
                md.append("[").append(linkTitle).append("](./plugins#").append(this.getHtmlAnchor()).append(")");
            }
        }

        public void appendExtendImplMDSsript(MarkContentBuilder md) {
            appendExtendImplMDSsript(extendImpl, false, md.getContent());
        }


        public String getArchiveFileName() {
            return hpi.getArchiveFileName();
        }

        public String getArchiveFileNameHtmlAnchor() {
            // return StringUtils.removeAll(hpi.getArchiveFileName(), "\\.|-");
            // return StringUtils.removeAll(hpi.getArchiveFileName(), "\\.");
            return escapeHtmlAnchor(hpi.getArchiveFileName());
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
            case DateTime:
            case DATE:
                return "日期";
            case PASSWORD:
                return "密码";
            case TEXTAREA:
                return "富文本";
            case JDBCColumn:
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
            case DECIMAL_NUMBER:
                return "浮点数字";
            case DURATION_OF_HOUR:
            case DURATION_OF_MINUTE:
            case DURATION_OF_SECOND:
                return "时间跨度";
            case MEMORY_SIZE_OF_BYTE:
            case MEMORY_SIZE_OF_KIBI:
            case MEMORY_SIZE_OF_MEGA:
                return "字节规格容量";
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
    private static final String PLUGIN_DESC_MARK_DOWN_FILENAME = "plugins.mdx";
    private static final String PLUGIN_TABVIEW_MARK_DOWN_FILENAME = "source-sink-table.js";
    private static final String PLUGIN_TPIS_MARK_DOWN_FILENAME = "tpis.mdx";
    private static final String PLUGIN_VERSIONS_JSON_FILENAME = "plugin-versions.json";
    private static final String RELEASE_HISTORY_JSON_FILENAME = "release-history.json";
    private static final String RECENT_RELEASES_JSON_FILENAME = "recent-releases.json";
    private static final String PLATFORM_PLUGINS_JSON_FILENAME = "platform-plugins.json";
    private static final String PLATFORM_PLUGINS_RESOURCE_FILENAME = "platform-plugins.json";
    private static final String EOL = System.getProperty("line.separator");

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    private static final Logger PACKAGE_LOGGER = Logger.getLogger(Main.class.getPackage().getName());
}
