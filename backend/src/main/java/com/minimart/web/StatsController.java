package com.minimart.web;

import com.minimart.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@Tag(name = "Statistics", description = "Thống kê doanh thu (chỉ tính hóa đơn COMPLETED)")
public class StatsController {

    private final StatisticsService statisticsService;

    public StatsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Tổng hợp doanh thu, số hóa đơn, trung bình đơn và tổng giảm giá")
    public StatisticsService.StatsSummary getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return statisticsService.getSummary(from, to);
    }

    @GetMapping("/daily")
    @Operation(summary = "Doanh thu theo từng ngày trong khoảng thời gian")
    public List<StatisticsService.DailyRevenuePoint> getDaily(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return statisticsService.getDailyRevenue(from, to);
    }

    @GetMapping("/by-category")
    @Operation(summary = "Cơ cấu doanh thu theo loại sản phẩm")
    public List<StatisticsService.CategoryRevenuePoint> getByCategory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return statisticsService.getRevenueByCategory(from, to);
    }

    @GetMapping("/top-products")
    @Operation(summary = "Danh sách sản phẩm bán chạy nhất")
    public List<StatisticsService.TopProductPoint> getTopProducts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "5") int limit) {
        return statisticsService.getTopProducts(from, to, limit);
    }
}
