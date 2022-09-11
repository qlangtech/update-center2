package io.jenkins.update_center;

import com.qlangtech.tis.extension.impl.IOUtils;

import java.util.Objects;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-09-11 08:26
 **/
public class MarkdownBuilder {
    private final String headerPath;
    private final StringBuffer bodyContent;

    public MarkdownBuilder(String headerPath, StringBuffer bodyContent) {
        this.headerPath = headerPath;
        this.bodyContent = Objects.requireNonNull(bodyContent, "bodyContent can not be null");
    }

    public StringBuffer build() {
        StringBuffer buffer = new StringBuffer(IOUtils.loadResourceFromClasspath(MarkdownBuilder.class, this.headerPath));
        buffer.append("\n\n");
        buffer.append(this.bodyContent);
        return buffer;
    }
}
