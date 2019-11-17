package com.github.tongkang;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;


import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;


public class MyBatisCrawlerDao implements CrawlerDao {

    private SqlSessionFactory sqlSessionFactory;

    public MyBatisCrawlerDao() {
        try {
            String resource = "db/mybatis/config.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public String getNextLinkThenDelete() throws SQLException {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            String url = session.selectOne("com.github.tongkang.MyMapper.selectAvailableLink");
            if (url != null) {
                session.delete("com.github.tongkang.MyMapper.deleteLink", url);
            }
            return url;
        }
    }

    @Override
    public void updateDatabase(String link, String sql) throws SQLException {

    }

    @Override
    public void insertNewsIntoDatabase(String url, String title, String content) throws SQLException {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.insert("com.github.tongkang.MyMapper.insertNews", new News(url, title, content));
        }

    }

    @Override
    public boolean isLinkProcessed(String link) throws SQLException {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            int count = session.selectOne("com.github.tongkang.MyMapper.countLink", link);
            return count != 0;
        }
    }

    @Override
    public void insertProcessedLink(String link) {
        Map<String, Object> param = new HashMap<>();
        param.put("tableName", "links_already_processed");
        param.put("link", link);
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.insert("com.github.tongkang.MyMapper.inserLink", param);
        }
    }

    @Override
    public void insertLinkToBeProcessed(String link) {
        Map<String, Object> param = new HashMap<>();
        param.put("tableName", "links_already_processed");
        param.put("link", link);
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.insert("com.github.tongkang.MyMapper.inserLink", param);
        }
    }
}
