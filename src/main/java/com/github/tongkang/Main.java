package com.github.tongkang;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main {
    public static void main(String[] args) throws IOException {

        //首先创建一个池子
        //待处理的链接池
        List<String> linkPool = new ArrayList<>();
        //已经处理的链接池，用Set好判断是否存在这个池子里面
        Set<String> processedLinks = new HashSet<>();
        linkPool.add("https://sina.cn");

        //先写一个死循环，然后再去优化代码
        while (true) {
            if (linkPool.isEmpty()) {
                break;
            }
            //ArrayList从尾部删除更有效率
            String link = linkPool.remove(linkPool.size() - 1);

            if (processedLinks.contains(link)) {
                continue;
            }

            if (isInterestingLink(link)) {

                Document doc = httpGetAndParseHtml(link);

                //获取所有的链接
                doc.select("a").stream().map(aTag -> aTag.attr("href")).forEach(linkPool::add);

                storeIntoDatabaseIfNewsPage(doc);

                processedLinks.add(link);

            } else {
                //这不是我们感兴趣的，不需要处理
            }
        }


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
        return (isNewPage(link) || isIndexPage(link)) && isNotLoginPage(link);
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
}
