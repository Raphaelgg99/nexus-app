package com.nexusapp.back_end.folder.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tb_mockup")
@Getter
@Setter
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String image;

    @Column(name = "type", nullable = false, length = 80)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "sku", length = 80)
    private String sku;

    @Column(name = "available", nullable = false)
    private Boolean available;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    @ManyToOne(optional = false)
    @JoinColumn(name = "folder_id", nullable = false)
    private Folder folder;

    protected Product() {
    }

    public Product(
            String image,
            String name,
            String description,
            String sku,
            Boolean available,
            Integer stockQuantity,
            Folder folder
    ) {
        this.image = image;
        this.name = name;
        this.description = description;
        this.sku = sku;
        this.available = available != null && available;
        this.stockQuantity = stockQuantity != null ? stockQuantity : 0;
        this.folder = folder;
    }

    public boolean isAvailable() {
        return Boolean.TRUE.equals(available);
    }

    public void setAvailable(Boolean available) {
        this.available = available != null && available;
    }

    public int getStockQuantity() {
        return stockQuantity != null ? stockQuantity : 0;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity != null ? stockQuantity : 0;
    }
}
