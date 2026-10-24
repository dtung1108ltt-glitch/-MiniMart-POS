package com.minimart.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI minimartOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("MiniMart POS — Hệ thống Quản lý Bán hàng & Siêu thị Mini API")
                .version("2.0.0")
                .description("REST API cho tiểu luận Lập trình Hướng đối tượng (Nhóm 8): Quản lý sản phẩm, Bán hàng POS, Hóa đơn, Thống kê doanh thu và Tích hợp API ngoài.")
                .contact(new Contact().name("Nhóm 8 - OOP")));
    }
}
