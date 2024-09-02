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

    @Column(nullable = false)
    private String propertyDescription;

    @Column(nullable = false)
    private String propertyAddress;

    private List<String> imageUrls;

    @Column(nullable = false)
    private String collectionId;

    @Column(nullable = false)
    private Integer propertySize;

    @Column(nullable = false)
    private Long propertyOwnerId;

    @Column(nullable = false)
    private Integer bedrooms;

    @Column(nullable = false)
    private Integer bathrooms;

    private Integer parkingSpaces;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status approvalStatus = Status.PENDING;

    @Column(nullable = false)
    private String propertyType;

    // Constructors
    public Collection() {}

    public Collection(Long id, String propertyDescription, String propertyAddress, List imageUrls, String collectionId,
                      Integer propertySize, Long propertyOwnerId, Integer bedrooms, Integer bathrooms,
                      Integer parkingSpaces, Status approvalStatus, String propertyType) {
        this.id = id;
        this.propertyDescription = propertyDescription;
        this.propertyAddress = propertyAddress;
        this.imageUrls = imageUrls;
        this.collectionId = collectionId;
        this.propertySize = propertySize;
        this.propertyOwnerId = propertyOwnerId;
        this.bedrooms = bedrooms;
        this.bathrooms = bathrooms;
        this.parkingSpaces = parkingSpaces;
        this.approvalStatus = approvalStatus != null ? approvalStatus : Status.PENDING;
        this.propertyType = propertyType;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPropertyDescription() {
        return propertyDescription;
    }

    public void setPropertyDescription(String propertyDescription) {
        this.propertyDescription = propertyDescription;
    }

    public String getPropertyAddress() {
        return propertyAddress;
    }

    public void setPropertyAddress(String propertyAddress) {
        this.propertyAddress = propertyAddress;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List imageUrls) {
        this.imageUrls = imageUrls;
    }

    public String getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(String collectionId) {
        this.collectionId = collectionId;
    }

    public Integer getPropertySize() {
        return propertySize;
    }

    public void setPropertySize(Integer propertySize) {
        this.propertySize = propertySize;
    }

    public Long getPropertyOwnerId() {
        return propertyOwnerId;
    }

    public void setPropertyOwnerId(Long propertyOwnerId) {
        this.propertyOwnerId = propertyOwnerId;
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

    public Integer getParkingSpaces() {
        return parkingSpaces;
    }

    public void setParkingSpaces(Integer parkingSpaces) {
        this.parkingSpaces = parkingSpaces;
    }

    public Status getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(Status approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public String getPropertyType() {
        return propertyType;
    }

    public void setPropertyType(String propertyType) {
        this.propertyType = propertyType;
    }
}
