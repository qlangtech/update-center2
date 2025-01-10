package io.jenkins.update_center;

import com.qlangtech.tis.extension.impl.IOUtils;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-09-11 08:26
 **/
public class MarkdownBuilder {
    private final String headerPath;
    private final StringBuffer bodyContent;
    private final Optional<String> footerPath;
    private final Function<String, String> headerDecorator;

    public MarkdownBuilder(String headerPath, Function<String, String> headerDecorator, StringBuffer bodyContent) {
        this(headerPath, headerDecorator, bodyContent, Optional.empty());
    }

    public MarkdownBuilder(String headerPath, Function<String, String> headerDecorator, StringBuffer bodyContent, Optional<String> footerPath) {
        this.headerPath = headerPath;
        this.headerDecorator = headerDecorator;
        this.footerPath = footerPath;
        this.bodyContent = Objects.requireNonNull(bodyContent, "bodyContent can not be null");
    }

    public StringBuffer build() {
        StringBuffer buffer = new StringBuffer(headerDecorator.apply(IOUtils.loadResourceFromClasspath(MarkdownBuilder.class, this.headerPath)));
        buffer.append("\n\n");
        buffer.append(this.bodyContent);
        if (footerPath.isPresent()) {
            buffer.append("\n\n");
            buffer.append(IOUtils.loadResourceFromClasspath(MarkdownBuilder.class, this.footerPath.get()));
        }
        return buffer;
    }
}
