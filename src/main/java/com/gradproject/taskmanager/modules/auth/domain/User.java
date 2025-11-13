package com.gradproject.taskmanager.modules.auth.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Setter
@Getter
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private boolean locked = false;

    @Column(name = "first_name", length = 50)
    private String firstName;

    @Column(name = "last_name", length = 50)
    private String lastName;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "job_title", length = 100)
    private String jobTitle;

    @Column(name = "department", length = 100)
    private String department;

    @Column(length = 20)
    private String phone;

    @Column(length = 50)
    private String timezone = "UTC";

    @Column(length = 10)
    private String language = "en";

    @Column(name = "date_format", length = 20)
    private String dateFormat = "MM/DD/YYYY";

    @Column(name = "time_format", length = 10)
    private String timeFormat = "12h";

    @Column(columnDefinition = "TEXT")
    private String bio;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @Column(name = "ts_created", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "ts_updated")
    private LocalDateTime updatedAt;

    public User() {}

    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

}
