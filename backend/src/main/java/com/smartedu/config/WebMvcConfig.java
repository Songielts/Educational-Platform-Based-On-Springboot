package com.smartedu.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${upload.path}")
    private String uploadPath;

    @Value("${upload.image-path:images/}")
    private String imagePath;

    @PostConstruct
    public void init() {
        System.out.println("配置的上传路径: " + uploadPath);
    }

    /**
     * 配置静态资源映射
     * 将 /uploads/** 映射到本地文件系统
     * 新增：将 /images/** 映射到上传目录下的 images/ 子目录
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 获取绝对路径
        Path path = Paths.get(uploadPath);
        String absolutePath = path.toAbsolutePath().toString();

        // 确保路径以 / 或 \ 结尾
        if (!absolutePath.endsWith(File.separator)) {
            absolutePath += File.separator;
        }

        // Windows路径需要特殊处理
        String resourceLocation;
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("windows")) {
            // Windows: file:///D:/path/to/uploads/
            resourceLocation = "file:///" + absolutePath.replace("\\", "/");
        } else {
            // Linux/Mac: file:/path/to/uploads/
            resourceLocation = "file:" + absolutePath;
        }

        // 原有映射：/uploads/** → uploads/ 目录
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(resourceLocation);

        // === 关键修复：/images/** → uploads/images/ 目录 ===
        // 数据库存的 cover_image = /images/course-math.jpg
        // 实际文件存放在 D:/.../uploads/images/course-math.jpg
        String imageResourceLocation;
        String imageAbsolutePath = absolutePath + imagePath;
        if (!imageAbsolutePath.endsWith(File.separator)) {
            imageAbsolutePath += File.separator;
        }
        if (osName.contains("windows")) {
            imageResourceLocation = "file:///" + imageAbsolutePath.replace("\\", "/");
        } else {
            imageResourceLocation = "file:" + imageAbsolutePath;
        }

        registry.addResourceHandler("/images/**")
                .addResourceLocations(imageResourceLocation)
                .setCachePeriod(3600); // 缓存1小时

        System.out.println("静态资源映射: /uploads/** -> " + resourceLocation);
        System.out.println("静态资源映射: /images/** -> " + imageResourceLocation);
        System.out.println("操作系统: " + osName);
    }

    /**
     * 跨域配置
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}