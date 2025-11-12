package com.gradproject.taskmanager.modules.release.dto;

import com.gradproject.taskmanager.modules.release.domain.enums.ReleaseStatus;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;


public record UpdateReleaseRequest(
    @Size(max = 255, message = "Release name must not exceed 255 characters")
    String name,

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    String description,

    @Size(max = 100, message = "Version must not exceed 100 characters")
    String version,

    LocalDate releaseDate,

    ReleaseStatus status
) {}
