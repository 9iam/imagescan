package org.example.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Митек on 22.07.2015.
 */
public class WebUtils
{
    /**
     * получаем conenet-type содержимого по урлу
     * делая HEAD запрос
     */
    public static String getContentTypeFromUrl(String urlname) throws IOException {
        URL url = new URL(urlname);
        HttpURLConnection connection = (HttpURLConnection)  url.openConnection();
        try {
            connection.setRequestMethod("HEAD");
            connection.connect();
            return connection.getContentType();
        }
        finally {
            connection.disconnect();
        }
    }
}
