package com.example.invoiceapp.service;

import com.example.invoiceapp.dto.BrandDTO;
import com.example.invoiceapp.dto.CategoryDTO;
import com.example.invoiceapp.dto.ProductDTO;
import com.example.invoiceapp.dto.UnitDTO;
import com.example.invoiceapp.model.Brand;
import com.example.invoiceapp.model.Category;
import com.example.invoiceapp.model.Product;
import com.example.invoiceapp.model.Stock;
import com.example.invoiceapp.model.Unit;
import com.example.invoiceapp.repository.BrandRepository;
import com.example.invoiceapp.repository.CategoryRepository;
import com.example.invoiceapp.repository.ProductRepository;
import com.example.invoiceapp.repository.StockRepository;
import com.example.invoiceapp.repository.UnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepo;
    private final CategoryRepository categoryRepo;
    private final BrandRepository brandRepo;
    private final UnitRepository unitRepo;
    private final StockRepository stockRepo;

    public ProductService(ProductRepository productRepo,
                          CategoryRepository categoryRepo,
                          BrandRepository brandRepo,
                          UnitRepository unitRepo,
                          StockRepository stockRepo) {
        this.productRepo = productRepo;
        this.categoryRepo = categoryRepo;
        this.brandRepo = brandRepo;
        this.unitRepo = unitRepo;
        this.stockRepo = stockRepo;
    }

    public List<ProductDTO> listProducts(Long organizationId) {
        if (organizationId == null) return List.of();
        List<Product> products = productRepo.findByOrganizationIdAndDeletedFalseOrderByNameAsc(organizationId);

        Map<Long, String> categoryNames = categoryRepo.findByOrganizationIdAndDeletedFalseOrderByNameAsc(organizationId)
                .stream().collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));
        Map<Long, String> brandNames = brandRepo.findByOrganizationIdAndDeletedFalseOrderByNameAsc(organizationId)
                .stream().collect(Collectors.toMap(Brand::getId, Brand::getName, (a, b) -> a));
        Map<Long, String> unitNames = unitRepo.findByOrganizationIdAndDeletedFalseOrderByNameAsc(organizationId)
                .stream().collect(Collectors.toMap(Unit::getId, Unit::getName, (a, b) -> a));

        List<Stock> stocks = stockRepo.findByOrganizationId(organizationId);
        Map<Long, BigDecimal> stockMap = stocks.stream()
                .collect(Collectors.groupingBy(Stock::getProductId,
                        Collectors.reducing(BigDecimal.ZERO, Stock::getQuantity, BigDecimal::add)));

        return products.stream().map(p -> {
            ProductDTO dto = toDTO(p);
            if (p.getCategoryId() != null) dto.setCategoryName(categoryNames.get(p.getCategoryId()));
            if (p.getBrandId() != null) dto.setBrandName(brandNames.get(p.getBrandId()));
            if (p.getUnitId() != null) dto.setUnitName(unitNames.get(p.getUnitId()));
            dto.setCurrentStock(stockMap.getOrDefault(p.getId(), BigDecimal.ZERO));
            return dto;
        }).collect(Collectors.toList());
    }

    public Optional<Product> findById(Long id, Long organizationId) {
        if (id == null || organizationId == null) return Optional.empty();
        return productRepo.findByIdAndOrganizationId(id, organizationId)
                .filter(p -> !p.isDeleted());
    }

    public Product get(Long id, Long organizationId) {
        return findById(id, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + id));
    }

    @Transactional
    public Product saveProduct(Long organizationId, ProductDTO dto) {
        if (organizationId == null) throw new IllegalArgumentException("Organization ID is required");

        Product product;
        if (dto.getId() != null) {
            product = get(dto.getId(), organizationId);
        } else {
            product = new Product();
            product.setOrganizationId(organizationId);
            product.setCreatedAt(Instant.now());
        }

        // Validate SKU uniqueness within organization
        if (dto.getSku() != null && !dto.getSku().isBlank()) {
            String sku = dto.getSku().trim();
            Optional<Product> existing = productRepo.findByOrganizationIdAndSkuAndDeletedFalse(organizationId, sku);
            if (existing.isPresent() && (dto.getId() == null || !existing.get().getId().equals(dto.getId()))) {
                throw new IllegalArgumentException("A product with SKU '" + sku + "' already exists in this organization.");
            }
            product.setSku(sku);
        } else {
            product.setSku("SKU-" + System.currentTimeMillis() % 1000000);
        }

        product.setName(dto.getName() != null ? dto.getName().trim() : "Unnamed Product");
        product.setBarcode(dto.getBarcode());
        product.setDescription(dto.getDescription());
        product.setCategoryId(dto.getCategoryId());
        product.setBrandId(dto.getBrandId());
        product.setUnitId(dto.getUnitId());
        product.setPurchasePrice(dto.getPurchasePrice() != null ? dto.getPurchasePrice() : BigDecimal.ZERO);
        product.setSellingPrice(dto.getSellingPrice() != null ? dto.getSellingPrice() : BigDecimal.ZERO);
        product.setTaxRate(dto.getTaxRate() != null ? dto.getTaxRate() : BigDecimal.ZERO);
        product.setMinimumStock(dto.getMinimumStock() != null ? dto.getMinimumStock() : BigDecimal.ZERO);
        product.setActive(dto.isActive());
        product.setUpdatedAt(Instant.now());

        return productRepo.save(product);
    }

    @Transactional
    public void deleteProduct(Long id, Long organizationId) {
        Product product = get(id, organizationId);
        product.markDeleted();
        productRepo.save(product);
    }

    // Categories
    public List<CategoryDTO> listCategories(Long organizationId) {
        return categoryRepo.findByOrganizationIdAndDeletedFalseOrderByNameAsc(organizationId).stream()
                .map(c -> new CategoryDTO(c.getId(), c.getPublicId(), c.getName(), c.getDescription()))
                .collect(Collectors.toList());
    }

    @Transactional
    public Category saveCategory(Long organizationId, String name, String description) {
        Category c = new Category();
        c.setOrganizationId(organizationId);
        c.setName(name.trim());
        c.setDescription(description);
        c.setCreatedAt(Instant.now());
        c.setUpdatedAt(Instant.now());
        return categoryRepo.save(c);
    }

    // Brands
    public List<BrandDTO> listBrands(Long organizationId) {
        return brandRepo.findByOrganizationIdAndDeletedFalseOrderByNameAsc(organizationId).stream()
                .map(b -> new BrandDTO(b.getId(), b.getPublicId(), b.getName()))
                .collect(Collectors.toList());
    }

    @Transactional
    public Brand saveBrand(Long organizationId, String name) {
        Brand b = new Brand();
        b.setOrganizationId(organizationId);
        b.setName(name.trim());
        b.setCreatedAt(Instant.now());
        b.setUpdatedAt(Instant.now());
        return brandRepo.save(b);
    }

    // Units
    public List<UnitDTO> listUnits(Long organizationId) {
        return unitRepo.findByOrganizationIdAndDeletedFalseOrderByNameAsc(organizationId).stream()
                .map(u -> new UnitDTO(u.getId(), u.getPublicId(), u.getName(), u.getAbbreviation()))
                .collect(Collectors.toList());
    }

    @Transactional
    public Unit saveUnit(Long organizationId, String name, String abbreviation) {
        Unit u = new Unit();
        u.setOrganizationId(organizationId);
        u.setName(name.trim());
        u.setAbbreviation(abbreviation != null ? abbreviation.trim() : "");
        u.setCreatedAt(Instant.now());
        u.setUpdatedAt(Instant.now());
        return unitRepo.save(u);
    }

    public ProductDTO toDTO(Product p) {
        if (p == null) return null;
        ProductDTO dto = new ProductDTO();
        dto.setId(p.getId());
        dto.setPublicId(p.getPublicId());
        dto.setSku(p.getSku());
        dto.setBarcode(p.getBarcode());
        dto.setName(p.getName());
        dto.setDescription(p.getDescription());
        dto.setCategoryId(p.getCategoryId());
        dto.setBrandId(p.getBrandId());
        dto.setUnitId(p.getUnitId());
        dto.setPurchasePrice(p.getPurchasePrice());
        dto.setSellingPrice(p.getSellingPrice());
        dto.setTaxRate(p.getTaxRate());
        dto.setMinimumStock(p.getMinimumStock());
        dto.setActive(p.isActive());
        return dto;
    }
}
