package io.jenkins.update_center;

import com.qlangtech.tis.extension.impl.IOUtils;

import java.util.Objects;
import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-09-11 08:26
 **/
public class MarkdownBuilder {
    private final String headerPath;
    private final StringBuffer bodyContent;
    private final Optional<String> footerPath;

    public MarkdownBuilder(String headerPath, StringBuffer bodyContent) {
        this(headerPath, bodyContent, Optional.empty());
    }

    public MarkdownBuilder(String headerPath, StringBuffer bodyContent, Optional<String> footerPath) {
        this.headerPath = headerPath;
        this.footerPath = footerPath;
        this.bodyContent = Objects.requireNonNull(bodyContent, "bodyContent can not be null");
    }

    public StringBuffer build() {
        StringBuffer buffer = new StringBuffer(IOUtils.loadResourceFromClasspath(MarkdownBuilder.class, this.headerPath));
        buffer.append("\n\n");
        buffer.append(this.bodyContent);
        if (footerPath.isPresent()) {
            buffer.append("\n\n");
            buffer.append(IOUtils.loadResourceFromClasspath(MarkdownBuilder.class, this.footerPath.get()));
        }
        return buffer;
    }
}
