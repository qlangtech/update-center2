package com.qlangetch.tis;

import com.google.common.collect.Lists;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Page.NavigateOptions;
import com.microsoft.playwright.Page.WaitForSelectorOptions;
import com.microsoft.playwright.options.BoundingBox;
import com.microsoft.playwright.options.ScreenshotAnimations;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.options.WaitUntilState;
import com.qlangtech.tis.config.ParamsConfig;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.manage.common.Option;
import com.qlangtech.tis.plugin.IDataXEndTypeGetter;
import com.qlangtech.tis.plugin.IEndTypeGetter;
import com.qlangtech.tis.plugin.IEndTypeGetter.EndType;
import com.qlangtech.tis.plugin.ds.DataSourceFactory;
import com.qlangtech.tis.plugin.incr.TISSinkFactory;
import com.qlangtech.tis.util.HeteroEnum;
import io.jenkins.update_center.Main.EndTypePluginStore;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2025-03-07 09:48
 **/
public class TISEndsDocsGenerator {
    private static final Logger logger = LoggerFactory.getLogger(TISEndsDocsGenerator.class);


    public static void buildEndTypeImages(
            Page page, File endDir, EndType endType, EndTypePluginStore pluginStore, BiConsumer<HeteroEnum, Descriptor> cptConsumer) {
//        try (Playwright playwright = Playwright.create()) {
//            Browser browser = playwright.chromium().launch();
//            Page page = browser.newPage();
        //  page.navigate("http://192.168.28.201:8080/");


        List<Pair<Option, Runnable>> paramsAndImageBuilder = Lists.newArrayList();

        boolean hasAddDataSource = false;
        aa:
        for (Pair<IEndTypeGetter, Descriptor> pair : pluginStore.miscPlugins) {

            List<Descriptor<DataSourceFactory>> dsDescriptors = HeteroEnum.DATASOURCE.descriptors();
            for (Descriptor<DataSourceFactory> dsDesc : dsDescriptors) {
                if (!hasAddDataSource && pair.getValue() == dsDesc) {
                    paramsAndImageBuilder.add(
                            Pair.of(
                                    new Option(HeteroEnum.DATASOURCE.getIdentity(), pair.getValue().getDisplayName())
                                    , () -> buildPluginDivImage(pair.getValue(), HeteroEnum.DATASOURCE, endDir, page)));
                    // 确保 datasource 只被添加一次，如mysql的dataSource mysql5，和mysql8 只会被添加一次
                    cptConsumer.accept(HeteroEnum.DATASOURCE, pair.getValue());
                    hasAddDataSource = true;
                    continue aa;
                }
            }

            List<Descriptor<ParamsConfig>> paramDescriptors = HeteroEnum.PARAMS_CONFIG.descriptors();
            for (Descriptor<ParamsConfig> paramDesc : paramDescriptors) {
                if (pair.getValue() == paramDesc) {
                    paramsAndImageBuilder.add(
                            Pair.of(
                                    new Option(HeteroEnum.PARAMS_CONFIG.getIdentity(), pair.getValue().getDisplayName())
                                    , () -> buildPluginDivImage(pair.getValue(), HeteroEnum.PARAMS_CONFIG, endDir, page)));
                    cptConsumer.accept(HeteroEnum.PARAMS_CONFIG, pair.getValue());
                    continue aa;
                }
            }

        }

        //Option param = null;
        for (Pair<IDataXEndTypeGetter, Descriptor> pair : pluginStore.dataXReaders) {
            paramsAndImageBuilder.add(
                    Pair.of(new Option(HeteroEnum.DATAX_READER.getIdentity(), pair.getValue().getDisplayName())
                            , () -> buildPluginDivImage(pair.getValue(), HeteroEnum.DATAX_READER, endDir, page)));
            cptConsumer.accept(HeteroEnum.DATAX_READER, pair.getValue());
            break;
        }

        for (Pair<IDataXEndTypeGetter, Descriptor> pair : pluginStore.dataXWriters) {
            paramsAndImageBuilder.add(
                    Pair.of(new Option(HeteroEnum.DATAX_WRITER.getIdentity(), pair.getValue().getDisplayName()),
                            () -> buildPluginDivImage(pair.getValue(), HeteroEnum.DATAX_WRITER, endDir, page)));
            cptConsumer.accept(HeteroEnum.DATAX_WRITER, pair.getValue());
            break;
        }

        for (Pair<IEndTypeGetter, Descriptor> pair : pluginStore.incrSources) {
            paramsAndImageBuilder.add(
                    Pair.of(
                            new Option(HeteroEnum.MQ.getIdentity(), pair.getValue().getDisplayName()),
                            () -> buildPluginDivImage(pair.getValue(), HeteroEnum.MQ, endDir, page)));
            cptConsumer.accept(HeteroEnum.MQ, pair.getValue());
            break;
        }

        for (Pair<IEndTypeGetter, Descriptor> pair : pluginStore.incrSinks) {
            paramsAndImageBuilder.add(
                    Pair.of(
                            new Option(TISSinkFactory.sinkFactory.getIdentity(), pair.getValue().getDisplayName())
                            , () -> buildPluginDivImage(pair.getValue(), TISSinkFactory.sinkFactory, endDir, page)));
            cptConsumer.accept(TISSinkFactory.sinkFactory, pair.getValue());
            break;
        }



        final String navUrl = "http://localhost:4200/base/cpt-list?"
                + paramsAndImageBuilder.stream()
                .map((pair) -> pair.getKey())
                .map((param) -> param.getName() + "=" + param.getValue()).collect(Collectors.joining("&"));
        logger.info(navUrl);
        page.navigate(navUrl, new NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));

        Locator locator = page.locator(".advance-opts");
        for (Locator elmt : locator.all()) {
            elmt.waitFor();
            elmt.click();
        }


        paramsAndImageBuilder.forEach((builder) -> {
            builder.getValue().run();
        });


        // }
    }

    public static void drawFieldIndexNumber(File imageFile, List<BoundingBox> fieldsBox, Descriptor descriptor) {
        // String inputPath = "input.png";
        String outputPath = "output.png";
        int number = 1; // 要显示的数字
        int positionX = 50;  // 圆圈中心坐标X（从右侧计算）
        int positionY = 50;  // 圆圈中心坐标Y（从顶部计算）
        Color circleColor = new Color(195, 42, 4);// Color.getColor(); //new Color(255, 0, 0); // 圆圈颜色（红色）
        Color textColor = new Color(253, 202, 15); // 文字颜色
        try {
            // 1. 加载原始图片
            BufferedImage image = ImageIO.read(imageFile);
            int width = image.getWidth();
            int height = image.getHeight();

//        PluginFormProperties pluginFormPropertyTypes = descriptor.getPluginFormPropertyTypes();
//        List<Entry<String, PropertyType>> kvTuples = pluginFormPropertyTypes.getSortedUseableProperties();
            // 2. 创建绘图上下文
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 3. 设置字体并计算文本尺寸
            Font font = new Font("Arial", Font.BOLD, 15);
            g.setFont(font);


            FontMetrics metrics = g.getFontMetrics();

            for (BoundingBox box : fieldsBox) {
                String text = String.valueOf(number++);
                positionX = width - 40; //(int) box.x;

                // 4. 计算圆圈尺寸（自动适应数字宽度）
                int textWidth = metrics.stringWidth(text);
                int textHeight = metrics.getHeight();
                int padding = 3; // 内边距
                int diameter = Math.max(textWidth, textHeight) + padding * 2;
                int radius = diameter / 2;
                positionY = (int) box.y + radius/*微调，稍微往下一点*/;
                // 5. 计算位置（右上角坐标转换为中心坐标）
                int circleX = width - positionX - radius; // 右侧起始位置
                int circleY = positionY - radius;         // 顶部起始位置

                // 6. 绘制红色实心圆圈
                g.setColor(circleColor);
                g.fillOval(circleX, circleY, diameter, diameter);

                // 7. 绘制白色边框（可选）
                g.setColor(new Color(255, 255, 255, 200));
                g.setStroke(new BasicStroke(3));
                g.drawOval(circleX, circleY, diameter, diameter);

                // 8. 居中绘制数字
                int textX = circleX + (diameter - textWidth) / 2;
                int textY = circleY + ((diameter + metrics.getAscent()) / 2);
                g.setColor(textColor);
                g.drawString(text, textX, textY);
            }


            // 9. 释放资源并保存
            g.dispose();
            //  ImageIO.write(image, "PNG", new File(imageFile.getParentFile(), outputPath));

            ImageIO.write(image, "PNG", imageFile);
            System.out.println("带圈数字生成成功！");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void buildPluginDivImage(Descriptor descriptor, HeteroEnum hetero, File endDir, Page page) {
        // 定位目标 div
        ElementHandle divElement
                = page.waitForSelector("div#" + hetero.getIdentity()
                , new WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));
        File imageFile = null;
        if (divElement != null) {
            BoundingBox box = null;
            BoundingBox fieldBox = null;
            List<BoundingBox> fieldsBox = Lists.newArrayList();
            try {
                // 获取元素的边界框
                divElement.scrollIntoViewIfNeeded();
                box = divElement.boundingBox();
                if (box != null) {

                    // divElement.querySelector("xpath=//item-prop-val/child::nz-form-item");

                    Locator fieldsLocator = page.locator("//div[@id='" + hetero.getIdentity() + "']//item-prop-val[not(ancestor::nz-form-item)]/child::nz-form-item");
                    for (Locator elmt : fieldsLocator.all()) {
                        fieldBox = elmt.boundingBox();
                        fieldBox.x -= box.x;
                        fieldBox.y -= box.y;
                        fieldsBox.add(fieldBox);
                    }


                    // 截取该区域
                    imageFile = new File(endDir, hetero.getIdentity() + ".png");
                    page.screenshot(new Page.ScreenshotOptions()
                            .setPath(imageFile.toPath())
                            .setClip(box.x, box.y, box.width, box.height)
                            .setAnimations(ScreenshotAnimations.DISABLED));

                    drawFieldIndexNumber(imageFile, fieldsBox, descriptor);
                }
            } catch (Exception e) {
                if (box != null) {
                    throw new RuntimeException("Clipped area:x="
                            + box.x + ",y=" + box.y + ",width:" + box.width + ",height:" + box.height, e);
                } else {
                    throw new RuntimeException(e);
                }
            }
        } else {
            throw new IllegalStateException("div " + hetero.getIdentity()
                    + ",endDir:" + endDir.getAbsolutePath() + " can not found relevant element");
        }
    }

}
