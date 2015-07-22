package org.example.util;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;

/**
 * Created by Митек on 22.07.2015.
 */
public class ImageUtils {
    public static BufferedImage cloneBufferedImage(BufferedImage bi) {
        ColorModel cm = bi.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = bi.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

    /**
     * определяет формат изображения по расширению файла. FIXME НЕНАДЕЖНЫЙ СПОСОБ!
     * @deprecated use getContentTypeFromUrl from WebUtils
     */
    @Deprecated
    public static String getFormat(String imageName)
    {
        String s = imageName.toLowerCase();
        if (s.endsWith(".png")) {
            return "PNG";
        } else if (s.endsWith(".gif")) {
            return "GIF";
        } else if (s.endsWith(".tiff")) {
            return "TIFF";
        } else if (s.endsWith(".jpg")) {
            return "JPG";
        } else if (s.endsWith(".jpeg")) {
            return "JPEG";
        }

        return "UNKNOWN";
    }
}
