package org.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.example.util.StringUtils.quoteString;

public class ImageScanServlet extends HttpServlet
{
    // количество изначально загружаемых эскизов
    private static final int INITIAL_NUMBER_OF_THUMBNAILS = 4;

    // количество эскизов, подгружаемых по клику на more
    private static final int MORE_NUMBER_OF_THUMBNAILS = 4;

    // один пул потоков для всех пользователей
    private final int corePoolSize = 4;
    private final int maximumPoolSize = 8;
    int keepAliveTime = 5000;
    private final ExecutorService executorService = new ThreadPoolExecutor(corePoolSize, maximumPoolSize,
            keepAliveTime, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>());

    /**
     * ссылка на первый экран приложения с текстом labelText
     * @return
     */
    private static String getBackLink(String labelText) {
        return "<a href=\"/task.jsp\">" + labelText + "</p>";
    }

    /**
     * выводим в out эскизы из imagesCache начиная с fromIndex
     * @return количество выведенных эскизов
     * @throws IOException
     */
    private int printThumbnails(Set<WebImage> imagesCache, PrintWriter out, int fromIndex) throws IOException {
        int printedCount = 0;
        int elementIndex = 0;
        out.println("<div id=\"thumbnails\">");
        for (WebImage webImage : imagesCache) {
            synchronized (webImage) {
                boolean ignore = elementIndex < fromIndex || webImage.getThumbnail() == null;
                elementIndex++;
                if (!ignore) {
                    out.println("<div class=\"thumb\">");
                    out.println("<a href=" + quoteString(webImage.getAbsUrl()) + ">");
                    out.println(webImage.getThumbnailTag());
                    out.println("</a>");
                    out.println("</div>");
                    printedCount++;
                }
            }
        }
        out.println("</div>");
        return printedCount;
    }

    /**
     * печетает все тамбнейлы из imagesCache, которые не null, в out
     * @throws IOException
     */
    private int printThumbnails(Set<WebImage> imagesCache, PrintWriter out) throws IOException {
        return printThumbnails(imagesCache, out, 0);
    }

    private LinkedHashSet<WebImage> getImagesCache(HttpServletRequest request) {
        return (LinkedHashSet<WebImage>) request.getSession().getAttribute("imagesCache");
    }

    private void setImagesCache(HttpServletRequest request, Set<WebImage> imagesCache) {
        request.getSession().setAttribute("imagesCache", imagesCache);
    }

    /**
     * подгружает недогруженные ранее тамбнейлы и пишет в out
     * @throws IOException
     */
    private void doGetMore(HttpServletRequest request, PrintWriter out) throws IOException {
        LinkedHashSet<WebImage> imagesCache = getImagesCache(request);

        if (imagesCache != null) {
            String fromIndex = request.getParameter("fromIndex");

            int printedCount = 0,
                processedAlready = fromIndex != null ? Integer.valueOf(fromIndex) : 0;

            if (processedAlready < imagesCache.size()) {
                printedCount = printThumbnails(imagesCache, out, processedAlready);

                if (printedCount + processedAlready < imagesCache.size()) {
                    startProcessMoreThumbnails(imagesCache);
                    out.println("<div style=\"clear:both\">");
                    out.println("<a class=\"more\" href=\"imagescan?more=true\">more</a>");
                    out.println("</div>");
                }
            }
        }
    }

    /**
     * отдает на клиент что-то, стартует джобу по конвертации следующих избражений
     * @throws IOException
     */
    private void doGetInitial(HttpServletRequest request, PrintWriter out) throws IOException {
        URL url = new URL(request.getParameter("url"));

        Document doc = Jsoup.connect(url.toString()).get();
        Elements images = doc.select("img");

        out.println(request.getParameter("url") + " " + getBackLink("&#128281;"));

        // заполняем кэш изображений ссылками из хтмл-а
        // TODO: проверять, если кэш уже заполнен всей нужной информацией, то отдавать ее сразу всю
        Set<WebImage> imagesCache = new LinkedHashSet<WebImage>();
        setImagesCache(request, imagesCache);
        for (Element imageElement : images) {
            imagesCache.add(new WebImage(imageElement));
        }

        // обрабатываем INITIAL_NUMBER_OF_THUMBNAILS изборажения, остальные кладем в пул
        // обработка это: скачать, изменить размер
        int processed = 0;
        for (WebImage webImage : imagesCache) {
            webImage.downloadBufferedImage();
            webImage.makeThumbnail();
            processed++;
            if (processed >= INITIAL_NUMBER_OF_THUMBNAILS) {
                break;
            }
        }

        // начинаем обработку следующих изображений
        startProcessMoreThumbnails(imagesCache);

        // выводим на экран, что уже готово
        processed = this.printThumbnails(imagesCache, out);

        // если нужно, выводим ссылку "подгрузить еще"
        if (processed < imagesCache.size()) {
            out.println("<div style=\"clear:both\">");
            out.println("<a class=\"more\" href=\"imagescan?more=true\">more</a>");
            out.println("</div>");
        }
    }

    /**
     * запускаем на обработку следующие MORE_NUMBER_OF_THUMBNAILS избражений из интернета
     * @param imagesCache
     */
    private void startProcessMoreThumbnails(Set<WebImage> imagesCache) {
        int count = 0;
        synchronized (executorService) {
            for (WebImage webImage : imagesCache) {
                if (webImage.getThumbnail() == null) {
                    executorService.execute(new WebImageResizeJob(webImage));
                    count++;
                }
                if (count >= MORE_NUMBER_OF_THUMBNAILS) {
                    break;
                }
            }
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);

        PrintWriter out = response.getWriter();

        if (request.getParameter("more") != null && request.getParameter("more").equals("ajax")) {
            doGetMore(request, out);
            return;
        }

        out.println("<!DOCTYPE html>");
        out.println("<html>");

        out.println("<head>");
        out.println("<TITLE>ImageScan</TITLE>");
        out.println("<LINK REL=\"StyleSheet\" HREF=\"default.css\" TYPE=\"text/css\">");
        out.println("<script language=\"javascript\" src=\"https://code.jquery.com/jquery-2.1.4.min.js\"></script>");
        out.println("<script language=\"javascript\" src=\"script.js\"></script>");
        out.println("</head>");

        out.println("<body>");
        //out.println("<h2>ImageScan</h2>");
        try {
            if (request.getParameter("more") != null) {
                doGetMore(request, out);
            } else {
                doGetInitial(request, out);
            }
        }
        catch (MalformedURLException e) {
            out.println("Invalid url");
        }
        finally {
            out.println("<div id=\"footer\"><br/><br/>");
            out.println(getBackLink("back"));
            out.println("</div>");

            out.println("</body></html>");
            out.close();
        }
    }
}