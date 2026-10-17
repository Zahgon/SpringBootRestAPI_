package com.gourav.restapi.controllers.payload.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** The wire order of a pet, stated rather than left to field declaration order. */
@JsonPropertyOrder({"id", "name", "species", "breed", "age", "color", "adoptionStatus", "createdAt", "updatedAt"})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetResponse {
    private String id;
    private String name;
    private String species;
    private String breed;
    private Integer age;
    private String color;
    private String adoptionStatus;
    private Instant createdAt;
    private Instant updatedAt;
}
