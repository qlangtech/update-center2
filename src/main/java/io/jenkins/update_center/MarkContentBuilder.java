package io.jenkins.update_center;

import org.apache.commons.lang.StringUtils;

/**
 * 需要确保每个li的最后不能有三个回车符号，不然<ol>下的<li>重新设置序号（从1开始）
 */
class MarkContentBuilder {
    private final StringBuffer extendsList = new StringBuffer();
    int returnCharNumber;

    public StringBuffer getContent() {
        processTailReturnChar();
        return this.extendsList;
    }

    public MarkContentBuilder append(String content) {
        if (!"\t".equals(content) && StringUtils.isEmpty(content)) {
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
