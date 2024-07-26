package com.cl.centralapi.model;

import com.cl.centralapi.enums.Status;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.util.List;

@Entity
public class Collection {

    // Base properties
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference
    private User user;

    @OneToMany(mappedBy = "collection", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Image> images;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    // Non-nullable properties
    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private Integer internalSize;

    @Column(nullable = false)
    private Integer bedrooms;

    @Column(nullable = false)
    private Integer bathrooms;

    // Nullable properties
    private Boolean aircon;
    private Boolean heating;
    private Integer parking;
    private Double externalSize;
    private Integer levels;
    private Boolean pool;

    @ElementCollection
    private List<String> extraFeatures;

    // Constructors
    public Collection() {}

    public Collection(User user, List<Image> images, Status status, String address, Double price, String description,
                      Integer internalSize, Integer bedrooms, Integer bathrooms, Boolean aircon, Boolean heating, Integer parking,
                      Double externalSize, Integer levels, Boolean pool, List<String> extraFeatures) {
        this.user = user;
        this.images = images;
        this.status = status != null? status : Status.PENDING;
        this.address = address;
        this.price = price;
        this.description = description;
        this.internalSize = internalSize;
        this.bedrooms = bedrooms;
        this.bathrooms = bathrooms;
        this.aircon = aircon;
        this.heating = heating;
        this.parking = parking;
        this.externalSize = externalSize;
        this.levels = levels;
        this.pool = pool;
        this.extraFeatures = extraFeatures;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<Image> getImages() {
        return images;
    }

    public void setImages(List<Image> images) {
        this.images = images;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getInternalSize() {
        return internalSize;
    }

    public void setInternalSize(Integer internalSize) {
        this.internalSize = internalSize;
    }

    public Integer getBedrooms() {
        return bedrooms;
    }

    public void setBedrooms(Integer bedrooms) {
        this.bedrooms = bedrooms;
    }

    public Integer getBathrooms() {
        return bathrooms;
    }

    public void setBathrooms(Integer bathrooms) {
        this.bathrooms = bathrooms;
    }

    public Boolean getAircon() {
        return aircon;
    }

    public void setAircon(Boolean aircon) {
        this.aircon = aircon;
    }

    public Boolean getHeating() {
        return heating;
    }

    public void setHeating(Boolean heating) {
        this.heating = heating;
    }

    public Integer getParking() {
        return parking;
    }

    public void setParking(Integer parking) {
        this.parking = parking;
    }

    public Double getExternalSize() {
        return externalSize;
    }

    public void setExternalSize(Double externalSize) {
        this.externalSize = externalSize;
    }

    public Integer getLevels() {
        return levels;
    }

    public void setLevels(Integer levels) {
        this.levels = levels;
    }

    public Boolean getPool() {
        return pool;
    }

    public void setPool(Boolean pool) {
        this.pool = pool;
    }

    public List<String> getExtraFeatures() {
        return extraFeatures;
    }

    public void setExtraFeatures(List<String> extraFeatures) {
        this.extraFeatures = extraFeatures;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
