package com.cl.centralapi.model;

import com.cl.centralapi.enums.Status;
import com.cl.centralapi.enums.ImageTags;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import software.amazon.awssdk.services.s3.endpoints.internal.Value;

import java.time.ZonedDateTime;

@Entity
public class Image {

    // Base properties
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String imageUrl;

    @Column(nullable = false)
    private ZonedDateTime uploadTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImageTags imageTag;

    @Column(nullable = false)
    private int instanceNumber;

    @Column(nullable = false, unique = true)
    private String imageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status imageStatus;

    @Column(length = 500)
    private String rejectionReason;

    @Column(length = 500)
    private String description;

    @Column(length = 500)
    private String descriptionSummary;

    @ManyToOne
    @JoinColumn(name = "collection_id", nullable = false)
    @JsonBackReference
    private Collection collection;

    // Constructors
    public Image() {}

    public Image(String imageUrl, ZonedDateTime uploadTime, ImageTags imageTag, int instanceNumber, String imageId, Status imageStatus, String rejectionReason, Collection collection) {
        this.imageUrl = imageUrl;
        this.uploadTime = uploadTime;
        this.imageTag = imageTag;
        this.instanceNumber = instanceNumber;
        this.imageId = imageId;
        this.imageStatus = imageStatus != null ? imageStatus : Status.PENDING;
        this.rejectionReason = rejectionReason;
        this.collection = collection;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public ZonedDateTime getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(ZonedDateTime uploadTime) {
        this.uploadTime = uploadTime;
    }

    public ImageTags getImageTag() {
        return imageTag;
    }

    public void setImageTag(ImageTags imageTag) {
        this.imageTag = imageTag;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public Status getImageStatus() {
        return imageStatus;
    }

    public void setImageStatus(Status imageStatus) {
        this.imageStatus = imageStatus;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Collection getCollection() {
        return collection;
    }

    public void setCollection(Collection collection) {
        this.collection = collection;
    }

    public int getInstanceNumber() { return instanceNumber; }

    public void setInstanceNumber(int instanceNumber) { this.instanceNumber = instanceNumber; }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescriptionSummary() {
        return descriptionSummary;
    }

    public void setDescriptionSummary(String descriptionSummary) {
        this.descriptionSummary = descriptionSummary;
    }

}
