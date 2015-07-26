package org.example;

import org.apache.log4j.Logger;

/**
 * Created by Митек on 23.07.2015.
 */
public class WebImageResizeJob implements Runnable {

    private final static Logger logger = Logger.getLogger(WebImageResizeJob.class);

    private WebImage webImage;

    @Override
    public void run() {
        try {
            webImage.downloadBufferedImage();
            webImage.makeThumbnail();
        } catch (Exception e) {
            logger.error("Failed to download and/or resize the image " + webImage.getAbsUrl(), e);
        }
    }

    public WebImageResizeJob(WebImage webImage) {
        this.webImage = webImage;
    }
}
