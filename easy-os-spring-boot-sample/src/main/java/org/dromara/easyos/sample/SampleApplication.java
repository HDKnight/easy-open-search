package org.dromara.easyos.sample;

import org.dromara.easyos.conditions.LambdaQueryWrapper;
import org.dromara.easyos.spring.boot.MapperScan;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.Map;

@SpringBootApplication
@MapperScan("org.dromara.easyos.sample")
public class SampleApplication {
    public static void main(String[] args) {
        SpringApplication.run(SampleApplication.class, args);
    }

    @Bean
    CommandLineRunner demo(ArticleMapper articleMapper) {
        return args -> {
            // Example only — requires a running OpenSearch with SQL plugin.
            // articleMapper.createIndex();
            // Article article = new Article();
            // article.setId("1");
            // article.setTitle("传统功夫");
            // article.setStatus(1);
            // article.setStarNum(10);
            // articleMapper.insert(article);

            LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
            wrapper.match(Article::getTitle, "功夫")
                    .eq(Article::getStatus, 1)
                    .orderByScoreDesc();
            // List<Article> list = articleMapper.selectList(wrapper);

            LambdaQueryWrapper<Article> agg = new LambdaQueryWrapper<>();
            agg.select(Article::getTitle)
                    .selectCount("cnt")
                    .selectSum(Article::getStarNum, "sumStar")
                    .eq(Article::getStatus, 1)
                    .groupBy(Article::getTitle);
            // List<Map<String, Object>> maps = articleMapper.selectMaps(agg);
        };
    }
}
