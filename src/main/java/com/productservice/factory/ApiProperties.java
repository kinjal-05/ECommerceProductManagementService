package com.productservice.factory;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Binds all values from api-paths.yml into strongly-typed fields.
 *
 * File location: src/main/resources/api-paths.yml
 *
 * As new controllers are added (e.g. ProductController),
 * add a new nested static class here and a new block in api-paths.yml.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "api")
@PropertySource(
        value = "classpath:api-paths.yml",
        factory = YamlPropertySourceFactory.class
)
public class ApiProperties {

    private Category category = new Category();
    private Product product = new Product();
    private ProductImage productImage = new ProductImage();

    @Getter
    @Setter
    public static class Category {
        private String base;
        private String create;
        private String getAll;
        private String getById;
        private String updateById;
        private String deleteById;
        private String search;
    }

    @Getter
    @Setter
    public static class Product {
        private String base;
        private String create;
        private String getAll;
        private String getById;
        private String updateById;
        private String deleteById;
        private String search;
    }

    @Getter
    @Setter
    public static class ProductImage {
        private String base;
        private String upload;
        private String update;
        private String delete;
    }
}