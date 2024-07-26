package com.cl.centralapi.model;

import com.cl.centralapi.enums.Status;
import com.cl.centralapi.enums.ImageTags;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

@Entity
public class Image {

    // Base properties
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String url;

    @ManyToOne
    @JoinColumn(name = "collection_id", nullable = false)
    @JsonBackReference
    private Collection collection;

    // Non-nullable properties
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImageTags tag;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    // Nullable properties
    private String customTag;
    private String description;

    // Constructors
    public Image() {}

    public Image(String url, Collection collection, ImageTags tag, Status status, String customTag, String description) {
        this.url = url;
        this.collection = collection;
        this.tag = tag;
        this.status = status != null? status : Status.PENDING;
        this.customTag = customTag;
        this.description = description;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Collection getCollection() {
        return collection;
    }

    public void setCollection(Collection collection) {
        this.collection = collection;
    }

    public ImageTags getTag() {
        return tag;
    }

    public void setTag(ImageTags tag) {
        this.tag = tag;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getCustomTag() {
        return customTag;
    }

    public void setCustomTag(String customTag) {
        this.customTag = customTag;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
