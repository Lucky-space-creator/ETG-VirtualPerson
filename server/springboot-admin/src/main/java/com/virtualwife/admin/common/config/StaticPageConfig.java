package com.virtualwife.admin.common.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 静态页面路由
 */
@Controller
public class StaticPageConfig {

    /**
     * 游客端首页（数字人导游界面）
     * 访问: http://localhost:8080/api/admin/tourist
     */
    @GetMapping("/tourist")
    public String touristPage() {
        return "forward:/index-tourist.html";
    }
}
