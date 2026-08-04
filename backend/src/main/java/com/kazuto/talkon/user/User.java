package com.kazuto.talkon.user;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name="display_name", nullable=false, length=50) private String displayName;
    @Column(nullable=false, unique=true, length=255) private String email;
    @Column(name="password_hash", nullable=false, length=100) private String passwordHash;
    @Column(name="created_at", nullable=false) private Instant createdAt;
    @Column(name="updated_at", nullable=false) private Instant updatedAt;
    protected User() {}
    public User(String displayName, String email, String passwordHash) {
        this.displayName=displayName; this.email=email; this.passwordHash=passwordHash;
        this.createdAt=Instant.now(); this.updatedAt=this.createdAt;
    }
    public Long getId(){return id;} public String getDisplayName(){return displayName;}
    public String getEmail(){return email;} public String getPasswordHash(){return passwordHash;}
}

