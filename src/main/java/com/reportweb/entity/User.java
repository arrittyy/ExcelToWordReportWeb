package com.reportweb.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "username", length = 256)
    private String userName;

    @Column(name = "normalized_username", length = 256)
    private String normalizedUserName;

    @Column(name = "email", length = 256)
    private String email;

    @Column(name = "normalized_email", length = 256)
    private String normalizedEmail;

    @Column(name = "email_confirmed")
    private Boolean emailConfirmed;

    @Column(name = "password_hash", length = 500)
    private String passwordHash;

    @Column(name = "security_stamp", length = 500)
    private String securityStamp;

    @Column(name = "concurrency_stamp", length = 500)
    private String concurrencyStamp;

    @Column(name = "phone_number", length = 50)
    private String phoneNumber;

    @Column(name = "phone_number_confirmed")
    private Boolean phoneNumberConfirmed;

    @Column(name = "two_factor_enabled")
    private Boolean twoFactorEnabled;

    @Column(name = "lockout_end")
    private LocalDateTime lockoutEnd;

    @Column(name = "lockout_enabled")
    private Boolean lockoutEnabled;

    @Column(name = "access_failed_count")
    private Integer accessFailedCount;

    // Custom fields from original User model
    @Column(name = "full_name", length = 200)
    private String fullName;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "role", length = 50)
    private String role = "USER"; // 默认角色为普通用户

    /** When non-null, this user is a sub-account (SUB_USER); parent user ID. */
    @Column(name = "parent_user_id", length = 255)
    private String parentUserId;

    // Navigation properties
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Project> projects = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Report> reports = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Image> images = new ArrayList<>();

}


