package org.example;

import org.example.util.Base64;
import org.example.util.ImageUtils;
import org.example.util.StringUtils;
import org.example.util.WebUtils;
import org.imgscalr.Scalr;
import org.jsoup.nodes.Element;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;

/**
 * Created by Митек on 22.07.2015.
 */
public class WebImage {
    public static final int thumbnailSize = 200;

    private String originalTag;

    private String absUrl;

    private byte[] srcBytes;

    private BufferedImage bufferedImage;

    private BufferedImage thumbnail;

    private int originalWidth;

    private int originalHeight;

    public String getOriginalTag() {
        return originalTag;
    }

    public void setOriginalTag(String originalTag) {
        this.originalTag = originalTag;
    }

    /**
     * Загружаем по урлу из absUrl в массив байт
     * @deprecated использовать downloadBufferedImage
     */
    @Deprecated
    public void downloadSrc() throws IOException {
        URL url = new URL(this.getAbsUrl());
        InputStream in = url.openStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        for (int b; (b = in.read()) != -1;) {
            out.write(b);
        }

        out.close();
        in.close();

        this.setSrcBytes(out.toByteArray());
    }

    /**
     * загружаем избражение по урлу absUrl в BufferedImage
     * @throws IOException
     */
    public void downloadBufferedImage() throws IOException {
        URL url = new URL(this.getAbsUrl());
        this.setBufferedImage(ImageIO.read(url));
    }

    public static boolean isFitToThumbnail(BufferedImage bufferedImage) {
        return  bufferedImage.getWidth() <= thumbnailSize && bufferedImage.getHeight() <= thumbnailSize;
    }

    /**
     * создаем thumbnail из BufferedImage, загруженного ранее
     */
    public void makeThumbnail() {
        if (bufferedImage != null && !isFitToThumbnail(bufferedImage)) {
            this.thumbnail = Scalr.resize(bufferedImage, thumbnailSize);
        } else if (bufferedImage != null) {
            this.thumbnail = ImageUtils.cloneBufferedImage(bufferedImage);
        }
    }

    /**
     * создаем класс-враппер на основе переданного элемента страницы
     * @param element - org.jsoup.nodes.Element
     */
    public WebImage(Element element) {
        this.setOriginalTag(element.toString());
        this.setAbsUrl(element.absUrl("src"));
    }

    public String getAbsUrl() {
        return absUrl;
    }

    public void setAbsUrl(String absUrl) {
        this.absUrl = absUrl;
    }

    public byte[] getSrcBytes() {
        return srcBytes;
    }

    public void setSrcBytes(byte[] srcBytes) {
        this.srcBytes = srcBytes;
    }

    public int getOriginalHeight() {
        return originalHeight;
    }

    public void setOriginalHeight(int originalHeight) {
        this.originalHeight = originalHeight;
    }

    public int getOriginalWidth() {
        return originalWidth;
    }

    public void setOriginalWidth(int originalWidth) {
        this.originalWidth = originalWidth;
    }

    public BufferedImage getBufferedImage() {
        return bufferedImage;
    }

    public void setBufferedImage(BufferedImage bufferedImage) {
        this.bufferedImage = bufferedImage;
    }

    public BufferedImage getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(BufferedImage thumbnail) {
        this.thumbnail = thumbnail;
    }

    /**
     * получаем img тег с закодированным thumbnail'ом внутри
     * а также со ссылкой на оригинальное изображение
     * @return
     * @throws IOException
     */
    public String getThumbnailTag() throws IOException {
        String contentType = WebUtils.getContentTypeFromUrl(this.getAbsUrl());

        if (thumbnail == null || contentType == null || !contentType.startsWith("image")) {
            return "";
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        String format = contentType.substring(6).toUpperCase(); //ex. GIF from image/gif

        ImageIO.write(thumbnail, format, out);
        byte[] bytes = out.toByteArray();

        String base64bytes = Base64.encodeToString(bytes, Base64.DEFAULT); //TODO: use Base64OutputStream instead?
        String src = "data:" + contentType + ";base64," + base64bytes;

        return "<img src=" + StringUtils.quoteString(src) + ">";
    }
}
