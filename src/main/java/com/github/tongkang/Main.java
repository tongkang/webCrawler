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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main {

    private static final String USERNAME = "root";
    private static final String PASSWORD = "root";

    @SuppressFBWarnings("DMI_CONSTANT_DB_PASSWORD")
    public static void main(String[] args) throws IOException, SQLException {
        Connection connection = DriverManager.getConnection("jdbc:h2:file:D:\\Java_practice\\webCrawler\\news", USERNAME, PASSWORD);


        while (true) {
            //从数据库加载即将处理的链接代码
            List<String> linkPool = loadUrlsFromDatabase(connection, "select link from LINKS_TO_BE_PROCESSED");

            //从数据库中已经处理的链接代码
            Set<String> processedLinks = new HashSet<>(loadUrlsFromDatabase(connection, "select link from LINKS_ALREADY_PROCESSED"));


            if (linkPool.isEmpty()) {
                break;
            }
            //ArrayList从尾部删除更有效率
            //从待处理的池子中拿取一个链接处理
            //处理完后，从池子中（包括数据库）删除
            String link = linkPool.remove(linkPool.size() - 1);
            insertLinkIntoDatabase(connection, link, "delete from LINKS_TO_BE_PROCESSED where LINK= ?");

            //询问数据库，当前链接是不是已经被处理过了？
            if (isLinkProcessed(connection, link)) {
                continue;
            }

            if (isInterestingLink(link)) {

                Document doc = httpGetAndParseHtml(link);

                //获取所有的链接
                parseUrlsFromPageAndStoreIntoDatabase(connection, linkPool, doc);

                storeIntoDatabaseIfNewsPage(doc);

                //加入数据库
                insertLinkIntoDatabase(connection, link, "insert into LINKS_ALREADY_PROCESSED (link) values ?");


            } else {
                //这不是我们感兴趣的，不需要处理
            }
        }


    }

    private static void parseUrlsFromPageAndStoreIntoDatabase(Connection connection, List<String> linkPool, Document doc) throws SQLException {
        for (Element aTag : doc.select("a")) {
            String href = aTag.attr("href");
            linkPool.add(href);
            insertLinkIntoDatabase(connection, href, "insert into LINKS_TO_BE_PROCESSED (link) values ?");
        }
    }

    private static boolean isLinkProcessed(Connection connection, String link) throws SQLException {
        ResultSet resultSet = null;
        try (PreparedStatement statement = connection.prepareStatement("select LINK from LINKS_ALREADY_PROCESSED where LINK= ?")) {
            statement.setString(1, link);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                return true;
            }
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
        }
        return false;
    }

    private static void insertLinkIntoDatabase(Connection connection, String link, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, link);
            statement.executeUpdate();
        }
    }

    private static List<String> loadUrlsFromDatabase(Connection connection, String sql) throws SQLException {
        List<String> results = new ArrayList<>();
        ResultSet resultSet = null;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                results.add(resultSet.getString(1));
            }
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
        }
        return results;
    }

    private static void storeIntoDatabaseIfNewsPage(Document doc) {
        //假如这是一个新闻的详情页面，就存入数据库，否则，就什么都不做
        ArrayList<Element> articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articleTag : articleTags) {
                String titile = articleTags.get(0).child(0).text();
                System.out.println(titile);
            }
        }
    }

    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE")
    private static Document httpGetAndParseHtml(String link) throws IOException {
        //这是我们感兴趣的，我们需要处理它
        CloseableHttpClient httpclient = HttpClients.createDefault();

        System.out.println(link);
        if (link.startsWith("//")) {
            link = "https:" + link;
            System.out.println(link);
        }
        HttpGet httpGet = new HttpGet(link);
        httpGet.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.90 Safari/537.36");

        try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {

            System.out.println(response1.getStatusLine());
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
        return !link.contains("games.sina.cn") && !link.contains("lives.sina.cn");
    }

}
