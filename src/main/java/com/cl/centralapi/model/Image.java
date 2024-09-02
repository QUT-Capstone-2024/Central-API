package com.cl.centralapi.model;

import com.cl.centralapi.enums.Status;
import com.cl.centralapi.enums.ImageTags;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

import java.time.ZonedDateTime;

@Entity
public class Image {

    // Base properties
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String imageUrl; // Matches "imageUrl" in JSON

    @Column(nullable = false)
    private ZonedDateTime uploadTime; // Matches "uploadTime" in JSON

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImageTags imageTag; // Matches "imageTag" in JSON

    @Column(nullable = false, unique = true)
    private String imageId; // Matches "imageId" in JSON

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status imageStatus; // Matches "imageStatus" in JSON

    @Column(length = 500) // Adjust length as needed
    private String rejectionReason; // Matches "rejectionReason" in JSON

    @ManyToOne
    @JoinColumn(name = "collection_id", nullable = false)
    @JsonBackReference
    private Collection collection; // Relationship to Collection

    // Constructors
    public Image() {}

    public Image(String imageUrl, ZonedDateTime uploadTime, ImageTags imageTag, String imageId, Status imageStatus, String rejectionReason, Collection collection) {
        this.imageUrl = imageUrl;
        this.uploadTime = uploadTime;
        this.imageTag = imageTag;
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
}
