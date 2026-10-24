package com.minimart.service;

import com.minimart.domain.FoodProduct;
import com.minimart.domain.HouseholdProduct;
import com.minimart.domain.Product;
import com.minimart.exception.DemoReadonlyException;
import com.minimart.exception.DuplicateProductException;
import com.minimart.exception.ProductNotFoundException;
import com.minimart.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lớp nghiệp vụ quản lý danh sách sản phẩm (thêm, sửa, xóa, tìm kiếm, lọc).
 * Vận dụng tính Đa hình (List<Product> hỗn hợp) và biểu thức Lambda với Stream API.
 */
@Service
public class ProductManager {

    public static final int LOW_STOCK_THRESHOLD = 10;
    public static final int EXPIRING_SOON_DAYS = 7;

    private final ProductRepository productRepository;
    private final boolean demoReadonly;

    public ProductManager(
            ProductRepository productRepository,
            @Value("${app.demo-readonly:false}") boolean demoReadonly) {
        this.productRepository = productRepository;
        this.demoReadonly = demoReadonly;
    }

    @Transactional
    public Product addProduct(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Thông tin sản phẩm không được để trống");
        }
        String id = product.getProductId().trim();
        if (productRepository.existsById(id)) {
            throw new DuplicateProductException(id);
        }
        return productRepository.save(product);
    }

    @Transactional
    public Product updateProduct(
            String productId,
            String name,
            BigDecimal price,
            int quantity,
            String category,
            LocalDate expiryDate,
            String brand) {
        Product existing = getProductById(productId);
        existing.setName(name);
        existing.setPrice(price);
        existing.setQuantity(quantity);

        if (existing instanceof FoodProduct foodProduct) {
            foodProduct.setExpiryDate(expiryDate);
        } else if (existing instanceof HouseholdProduct householdProduct) {
            householdProduct.setBrand(brand);
        }
        return productRepository.save(existing);
    }

    @Transactional
    public void deleteProduct(String productId) {
        if (demoReadonly) {
            throw new DemoReadonlyException("Chế độ demo chỉ đọc (DEMO_READONLY) đang bật, không cho phép xóa sản phẩm");
        }
        Product existing = getProductById(productId);
        try {
            productRepository.delete(existing);
            productRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("Không thể xóa sản phẩm '" + productId + "' vì đã phát sinh trong hóa đơn bán hàng");
        }
    }

    @Transactional(readOnly = true)
    public Product getProductById(String productId) {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("Mã sản phẩm không được để trống");
        }
        return productRepository.findById(productId.trim())
            .orElseThrow(() -> new ProductNotFoundException(productId.trim()));
    }

    /**
     * Tìm kiếm theo từ khóa trên mã hoặc tên sản phẩm bằng biểu thức Lambda (khớp docx mục 3.5).
     */
    @Transactional(readOnly = true)
    public List<Product> searchByKeyword(String keyword) {
        List<Product> allProducts = productRepository.findAll();
        if (keyword == null || keyword.isBlank()) {
            return allProducts;
        }
        String lowerKeyword = keyword.trim().toLowerCase(Locale.ROOT);
        return allProducts.stream()
            .filter(p -> p.getName().toLowerCase(Locale.ROOT).contains(lowerKeyword)
                || p.getProductId().toLowerCase(Locale.ROOT).contains(lowerKeyword))
            .toList();
    }

    @Transactional(readOnly = true)
    public Page<Product> findProducts(
            String q,
            String category,
            Boolean lowStock,
            Boolean expiringSoon,
            Pageable pageable) {
        List<Product> filtered = filterProducts(q, category, lowStock, expiringSoon, pageable.getSort());
        if (pageable.isUnpaged()) {
            return new PageImpl<>(filtered);
        }
        int start = (int) pageable.getOffset();
        if (start >= filtered.size()) {
            return new PageImpl<>(List.of(), pageable, filtered.size());
        }
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        return new PageImpl<>(filtered.subList(start, end), pageable, filtered.size());
    }

    @Transactional(readOnly = true)
    public List<Product> filterProducts(
            String q,
            String category,
            Boolean lowStock,
            Boolean expiringSoon,
            Sort sort) {
        String normalizedQ = (q == null || q.isBlank()) ? null : q.trim().toLowerCase(Locale.ROOT);
        String normalizedCat = normalizeCategoryFilter(category);

        Comparator<Product> comparator = buildComparator(sort);

        return productRepository.findAll().stream()
            .filter(p -> normalizedQ == null
                || p.getName().toLowerCase(Locale.ROOT).contains(normalizedQ)
                || p.getProductId().toLowerCase(Locale.ROOT).contains(normalizedQ))
            .filter(p -> normalizedCat == null
                || p.getCategoryCode().equalsIgnoreCase(normalizedCat)
                || p.getCategory().equalsIgnoreCase(normalizedCat))
            .filter(p -> !Boolean.TRUE.equals(lowStock) || p.getQuantity() <= LOW_STOCK_THRESHOLD)
            .filter(p -> !Boolean.TRUE.equals(expiringSoon)
                || (p instanceof FoodProduct fp && (fp.isExpired() || fp.isExpiringSoon(EXPIRING_SOON_DAYS))))
            .sorted(comparator)
            .toList();
    }

    public static Product createProductFromFields(
            String productId,
            String name,
            BigDecimal price,
            int quantity,
            String category,
            LocalDate expiryDate,
            String brand) {
        String code = normalizeCategoryFilter(category);
        if ("FOOD".equals(code)) {
            return new FoodProduct(productId, name, price, quantity, expiryDate);
        } else if ("HOUSEHOLD".equals(code)) {
            return new HouseholdProduct(productId, name, price, quantity, brand);
        }
        throw new IllegalArgumentException("Loại sản phẩm không hợp lệ (chỉ chấp nhận FOOD/Thực phẩm hoặc HOUSEHOLD/Đồ gia dụng)");
    }

    public static String normalizeCategoryFilter(String category) {
        if (category == null || category.isBlank() || "ALL".equalsIgnoreCase(category.trim())) {
            return null;
        }
        String c = category.trim();
        if ("FOOD".equalsIgnoreCase(c) || "Thực phẩm".equalsIgnoreCase(c) || "THUC_PHAM".equalsIgnoreCase(c)) {
            return "FOOD";
        }
        if ("HOUSEHOLD".equalsIgnoreCase(c) || "Đồ gia dụng".equalsIgnoreCase(c) || "DO_GIA_DUNG".equalsIgnoreCase(c)) {
            return "HOUSEHOLD";
        }
        return c;
    }

    private Comparator<Product> buildComparator(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return Comparator.comparing(Product::getProductId);
        }
        Comparator<Product> result = null;
        for (Sort.Order order : sort) {
            Comparator<Product> propComp = switch (order.getProperty()) {
                case "name" -> Comparator.comparing(p -> p.getName().toLowerCase(Locale.ROOT));
                case "price" -> Comparator.comparing(Product::getPrice);
                case "quantity" -> Comparator.comparingInt(Product::getQuantity);
                case "category" -> Comparator.comparing(Product::getCategory);
                default -> Comparator.comparing(Product::getProductId);
            };
            if (order.isDescending()) {
                propComp = propComp.reversed();
            }
            result = (result == null) ? propComp : result.thenComparing(propComp);
        }
        return result != null ? result : Comparator.comparing(Product::getProductId);
    }
}
