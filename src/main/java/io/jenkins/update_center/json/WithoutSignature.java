package io.jenkins.update_center.json;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.qlangtech.tis.manage.common.TisUTF8;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class WithoutSignature {

    public void write(File file, boolean pretty) throws IOException {
        write(file, this, pretty);
    }

    public static void write(File file, Object obj, boolean pretty) throws IOException {
        final File parent = file.getParentFile();
        if (!parent.mkdirs() && !parent.isDirectory()) {
            throw new IOException("Failed to create " + parent);
        }
        if (pretty) {
            FileUtils.write(file, JSON.toJSONString(obj, SerializerFeature.DisableCircularReferenceDetect,
                    SerializerFeature.PrettyFormat), TisUTF8.get());
        } else {
            FileUtils.write(file, JSON.toJSONString(obj, SerializerFeature.DisableCircularReferenceDetect),
                    TisUTF8.get());
        }
    }


}
