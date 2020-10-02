# webCrawler
这是一个网络爬虫并用ES数据分析实现

#### Flyway初始化数据库
1. 初始化命令：`mvn flyway:migrate'
2. 如果又脏数据的话：`mvn flyway:clean flyway:migrate'

#### Docker 启动MySQL
'docker run --name mysql -e MYSQL_ROOT_PASSWORD=root -p 3306:3306 -d mysql'

启动测试数据库
'docker run -d -p 3307:3306 --name=testmysql -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=wxshop  mysql'

#### Docker安装Elashticsearch
'docker run -d -v D:\Java_practice\webCrawler\esdata:/usr/share/elasticsearch/data --name elasticsearch -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" elasticsearch:7.4.2'
