package com.github.tongkang;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class Crawler {


    private CrawlerDao dao = new JdbcCrawlerDao();

    public void run() throws SQLException, IOException {

        String link;

        //从数据库中加载下一个链接，如果能加载到，就进行循环
        while ((link = dao.getNextLinkThenDelete()) != null) {

            //询问数据库，当前链接是不是已经被处理过了？
            if (dao.isLinkProcessed(link)) {
                continue;
            }

            if (isInterestingLink(link)) {
                System.out.println(link);
                Document doc = httpGetAndParseHtml(link);

                //获取所有的链接
                parseUrlsFromPageAndStoreIntoDatabase(doc);

                storeIntoDatabaseIfNewsPage(doc, link);

                //加入数据库
                dao.updateDatabase(link, "insert into LINKS_ALREADY_PROCESSED (link) values ?");


            }
        }
    }

    @SuppressFBWarnings("DMI_CONSTANT_DB_PASSWORD")
    public void main(String[] args) throws IOException, SQLException {
        new Crawler().run();


    }


    private void parseUrlsFromPageAndStoreIntoDatabase(Document doc) throws SQLException {
        for (Element aTag : doc.select("a")) {
            String href = aTag.attr("href");
            if (href.startsWith("//")) {
                href = "https:" + href;
            }
            if (!href.toLowerCase().startsWith("javascript")) {
                dao.updateDatabase(href, "insert into LINKS_TO_BE_PROCESSED (link) values ?");
            }
        }
    }


    private void storeIntoDatabaseIfNewsPage(Document doc, String link) throws SQLException {
        //假如这是一个新闻的详情页面，就存入数据库，否则，就什么都不做
        ArrayList<Element> articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articleTag : articleTags) {
                String title = articleTags.get(0).child(0).text();
                String content = articleTag.select("p").stream().map(Element::text).collect(Collectors.joining("\n"));
                dao.insertNewsIntoDatabase(link, title, content);
            }
        }
    }

    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE")
    private static Document httpGetAndParseHtml(String link) throws IOException {
        //这是我们感兴趣的，我们需要处理它
        CloseableHttpClient httpclient = HttpClients.createDefault();


        HttpGet httpGet = new HttpGet(link);
        httpGet.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.90 Safari/537.36");

        try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
            HttpEntity entity1 = response1.getEntity();
            String html = EntityUtils.toString(entity1);

            return Jsoup.parse(html);
        }

    }

    private static boolean isInterestingLink(String link) {
        return (isNewPage(link) || isIndexPage(link)) && isNotLoginPage(link) && isNotGamePage(link);
    }

    private static boolean isIndexPage(String link) {
        return "https://sina.cn".equals(link);
    }

    private static boolean isNewPage(String link) {
        return link.contains("sina.cn");
    }

    private static boolean isNotLoginPage(String link) {
        return !link.contains("passport.sina.cn");
    }

    private static boolean isNotGamePage(String link) {
        return !link.contains("games.sina.cn") &&
                !link.contains("lives.sina.cn") &&
                !link.contains("weather1.sina.cn") &&
                !link.contains("k.sina.cn");
    }

}
