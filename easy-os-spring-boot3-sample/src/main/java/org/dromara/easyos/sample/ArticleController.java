package org.dromara.easyos.sample;

import org.dromara.easyos.biz.PageInfo;
import org.dromara.easyos.conditions.LambdaQueryWrapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/articles")
public class ArticleController {

    private final ArticleMapper articleMapper;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public ArticleController(ArticleMapper articleMapper) {
        this.articleMapper = articleMapper;
    }

    /** 新增 */
    @PostMapping
    public Map<String, Object> create(@RequestBody Article article) {
        int rows = articleMapper.insert(article);
        return result(rows > 0, "created", article);
    }

    /** 按 id 修改 */
    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable("id") String id, @RequestBody Article article) {
        article.setId(id);
        int rows = articleMapper.updateById(article);
        return result(rows > 0, "updated", article);
    }

    /** 按 id 删除 */
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable("id") String id) {
        int rows = articleMapper.deleteById(id);
        return result(rows > 0, "deleted", null);
    }

    /**
     * 自定义 SQL 示例：按 status 查询（@OsSelect）
     */
    @GetMapping("/by-status")
    public List<Article> listByStatus(@RequestParam("status") Integer status) {
        return articleMapper.listByStatus(status);
    }

    /**
     * 分页查询（放在 /{id} 之前，避免被路径变量抢匹配）
     */
    @GetMapping("/page")
    public PageInfo<Article> page(
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "status", required = false) Integer status) {
        if (pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize < 1) {
            pageSize = 10;
        }
        return articleMapper.page(buildWrapper(keyword, title, status), pageNum, pageSize);
    }

    /** 点查：按 id 查询 */
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable("id") String id) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result(false, "not found", null));
        }
        return ResponseEntity.ok(article);
    }

    /**
     * 列表查询（可选条件）
     * <p>keyword 走全文 match；title 走 like；status 精确匹配。
     */
    @GetMapping
    public List<Article> list(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "status", required = false) Integer status) {
        return articleMapper.selectList(buildWrapper(keyword, title, status));
    }

    private LambdaQueryWrapper<Article> buildWrapper(String keyword, String title, Integer status) {
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.match(Article::getTitle, keyword);
        }
        if (StringUtils.hasText(title)) {
            wrapper.like(Article::getTitle, title);
        }
        if (status != null) {
            wrapper.eq(Article::getStatus, status);
        }
        wrapper.orderByDesc(Article::getStarNum);
        wrapper.orderByScoreDesc();
        return wrapper;
    }

    private Map<String, Object> result(boolean success, String message, Object data) {
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("success", success);
        body.put("message", message);
        if (data != null) {
            body.put("data", data);
        }
        return body;
    }
}
