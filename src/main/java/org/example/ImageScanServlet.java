package org.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.example.util.StringUtils.quoteString;

public class ImageScanServlet extends HttpServlet
{
    private Set<WebImage> imagesCache;

    private static String getBackLink(String text) {
        return "<a href=\"/task.html\">" + text + "</a>";
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);

        PrintWriter out = response.getWriter();

        out.println("<!DOCTYPE html>");
        out.println("<html>");

        out.println("<head>");
        out.println("<LINK REL=\"StyleSheet\" HREF=\"default.css\" TYPE=\"text/css\">");
        out.println("</head>");

        out.println("<body>");
        out.println("<h2>" + getBackLink("ImageScan")+ "</h2>");
        try {
            URL url = new URL(request.getParameter("url"));

            Document doc = Jsoup.connect(url.toString()).get();
            Elements images = doc.select("img");

            out.println("showing images from " + request.getParameter("url"));

            //out.println("<p>Images from url:</p>");

            imagesCache = new HashSet<WebImage>();
            for (Element imageElement : images) {
                imagesCache.add(new WebImage(imageElement));
            }

            for (WebImage webImage : imagesCache) {
                webImage.downloadBufferedImage();
                webImage.makeThumbnail();
            }

            /*for (WebImage webImage : imagesCache) {
                out.println(webImage.getOriginalTag());
                out.println("<br/>");
            }*/

            //out.println("<p>Thumbnails:</p>");
            out.println("<div id=\"thumbnails\">");
            for (WebImage webImage : imagesCache) {
                out.println("<div class=\"thumb\">");
                out.println("<a href=" + quoteString(webImage.getAbsUrl()) + ">");
                out.println(webImage.getThumbnailTag());
                out.println("</a>");
                out.println("</div>");
            }
            out.println("</div>");
        }
        catch (MalformedURLException e) {
            out.println("Invalid url");
        }
        finally {
            out.println("<div id=\"footer\">");
            /*out.println("<br/><br/>");
            out.println("session=" + request.getSession(true).getId());*/
            out.println("<br/><br/>");
            out.println(getBackLink("back"));
            out.println("</div>");

            out.println("</body></html>");
            out.close();
        }
    }
}