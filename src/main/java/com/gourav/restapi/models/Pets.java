package com.gourav.restapi.models;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Document stored in the {@code pets} collection.
 *
 * <p>{@code createdAt} is stamped once when the document is first written and {@code updatedAt}
 * on every write; see {@code repositories.mongo.MongoPetsRepository}.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pets {

    private String id;

    @NotBlank
    private String name;

    @NotBlank
    private String species;

    @NotBlank
    private String breed;

    private Integer age;

    private String color;

    @Builder.Default
    private String adoptionStatus = "AVAILABLE";

    private Instant createdAt;

    private Instant updatedAt;
}
