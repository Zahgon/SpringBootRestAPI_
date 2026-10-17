package com.gourav.restapi.services;

import com.gourav.restapi.controllers.payload.request.CreatePetRequest;
import com.gourav.restapi.controllers.payload.request.UpdatePetRequest;
import com.gourav.restapi.controllers.payload.response.PagedResponse;
import com.gourav.restapi.controllers.payload.response.PetResponse;
import com.gourav.restapi.exceptions.ApiStatusException;
import com.gourav.restapi.models.Pets;
import com.gourav.restapi.repositories.PetsRepository;
import com.gourav.restapi.repositories.paging.PageImpl;
import com.gourav.restapi.repositories.paging.PageRequest;
import com.gourav.restapi.repositories.paging.Pageable;
import com.gourav.restapi.repositories.paging.Sort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PetsServiceTest {

    @Mock
    private PetsRepository petsRepository;

    @InjectMocks
    private PetsService petsService;

    private Pets testPet;
    private CreatePetRequest createRequest;
    private UpdatePetRequest updateRequest;

    @BeforeEach
    void setUp() {
        testPet = Pets.builder().id("pet-123").name("Liam").species("cat").breed("tabby").build();
        
        createRequest = new CreatePetRequest();
        createRequest.setName("Liam");
        createRequest.setSpecies("cat");
        createRequest.setBreed("tabby");

        updateRequest = new UpdatePetRequest();
        updateRequest.setName("Luna");
        updateRequest.setSpecies("dog");
        updateRequest.setBreed("golden retriever");
    }

    private static Pageable firstPage() {
        return PageRequest.of(0, 10, Sort.by("name").ascending());
    }

    @Test
    void getAllPets_ReturnsAllPets() {
        // Arrange
        List<Pets> petsList = List.of(testPet);
        given(petsRepository.findAll(any(Pageable.class)))
                .willReturn(new PageImpl<>(petsList, firstPage(), petsList.size()));

        // Act
        PagedResponse<PetResponse> result = petsService.getPets(0, 10, "name", null, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Liam", result.getContent().get(0).getName());
        assertEquals("pet-123", result.getContent().get(0).getId());
        verify(petsRepository).findAll(any(Pageable.class));
    }

    @Test
    void getAllPets_ReturnsEmptyList_WhenNoProjects() {
        // Arrange
        given(petsRepository.findAll(any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(), firstPage(), 0));

        // Act
        PagedResponse<PetResponse> result = petsService.getPets(0, 10, "name", null, null);

        // Assert
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void getPetById_ReturnsPet_WhenFound() {
        // Arrange
        given(petsRepository.findById("pet-123")).willReturn(Optional.of(testPet));

        // Act
        PetResponse result = petsService.getPetById("pet-123");

        // Assert
        assertNotNull(result);
        assertEquals("pet-123", result.getId());
        assertEquals("Liam", result.getName());
        assertEquals("cat", result.getSpecies());
        assertEquals("tabby", result.getBreed());
    }

    @Test
    void getPetById_ThrowsNotFoundException_WhenPetNotFound() {
        // Arrange
        given(petsRepository.findById("missing-id")).willReturn(Optional.empty());

        // Act & Assert
        assertThrows(ApiStatusException.class, () -> petsService.getPetById("missing-id"));
    }

    @Test
    void createPet_ReturnsPetResponse() {
        // Arrange
        Pets savedPet = Pets.builder().id("pet-456").name("Liam").species("cat").breed("tabby").build();
        given(petsRepository.save(any(Pets.class))).willReturn(savedPet);

        // Act
        PetResponse result = petsService.createPet(createRequest);

        // Assert
        assertNotNull(result);
        assertEquals("pet-456", result.getId());
        assertEquals("Liam", result.getName());
        verify(petsRepository).save(any(Pets.class));
    }

    @Test
    void updatePet_UpdatesAndReturnsPet() {
        // Arrange
        Pets existingPet = Pets.builder().id("pet-123").name("Liam").species("cat").breed("tabby").build();
        Pets updatedPet = Pets.builder().id("pet-123").name("Luna").species("dog").breed("golden retriever").build();
        
        given(petsRepository.findById("pet-123")).willReturn(Optional.of(existingPet));
        given(petsRepository.save(any(Pets.class))).willReturn(updatedPet);

        // Act
        PetResponse result = petsService.updatePet("pet-123", updateRequest);

        // Assert
        assertNotNull(result);
        assertEquals("pet-123", result.getId());
        assertEquals("Luna", result.getName());
        assertEquals("dog", result.getSpecies());
        assertEquals("golden retriever", result.getBreed());
        verify(petsRepository).save(any(Pets.class));
    }

    @Test
    void updatePet_ThrowsNotFoundException_WhenPetNotFound() {
        // Arrange
        given(petsRepository.findById("missing-id")).willReturn(Optional.empty());

        // Act & Assert
        assertThrows(ApiStatusException.class, 
                     () -> petsService.updatePet("missing-id", updateRequest));
    }

    @Test
    void deletePet_CallsRepositoryDelete() {
        // Arrange
        given(petsRepository.findById("pet-123")).willReturn(Optional.of(testPet));

        // Act
        petsService.deletePet("pet-123");

        // Assert
        verify(petsRepository).delete(testPet);
    }

    @Test
    void deletePet_ThrowsNotFoundException_WhenPetNotFound() {
        // Arrange
        given(petsRepository.findById("missing-id")).willReturn(Optional.empty());

        // Act & Assert
        assertThrows(ApiStatusException.class, () -> petsService.deletePet("missing-id"));
    }

    @Test
    void createPet_MapsRequestCorrectly() {
        // Arrange
        Pets savedPet = Pets.builder().id("pet-789").name("Max").species("dog").breed("labrador").build();
        given(petsRepository.save(any(Pets.class))).willReturn(savedPet);

        CreatePetRequest request = new CreatePetRequest();
        request.setName("Max");
        request.setSpecies("dog");
        request.setBreed("labrador");

        // Act
        PetResponse result = petsService.createPet(request);

        // Assert
        assertEquals("Max", result.getName());
        assertEquals("dog", result.getSpecies());
        assertEquals("labrador", result.getBreed());
    }

    @Test
    void getPets_FiltersBySpecies_WhenOnlySpeciesGiven() {
        // Arrange
        List<Pets> pets = List.of(testPet);
        given(petsRepository.findBySpeciesContainingIgnoreCase(eq("cat"), any(Pageable.class)))
                .willReturn(new PageImpl<>(pets, firstPage(), pets.size()));

        // Act
        PagedResponse<PetResponse> result = petsService.getPets(0, 10, "name", "cat", null);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals("cat", result.getContent().get(0).getSpecies());
        verify(petsRepository).findBySpeciesContainingIgnoreCase(eq("cat"), any(Pageable.class));
    }

    @Test
    void getPets_FiltersByAdoptionStatus_WhenOnlyStatusGiven() {
        // Arrange
        List<Pets> pets = List.of(testPet);
        given(petsRepository.findByAdoptionStatus(eq("ADOPTED"), any(Pageable.class)))
                .willReturn(new PageImpl<>(pets, firstPage(), pets.size()));

        // Act
        PagedResponse<PetResponse> result = petsService.getPets(0, 10, "name", "  ", "ADOPTED");

        // Assert
        assertEquals(1, result.getContent().size());
        verify(petsRepository).findByAdoptionStatus(eq("ADOPTED"), any(Pageable.class));
    }

    @Test
    void getPets_FiltersByBoth_WhenSpeciesAndStatusGiven() {
        // Arrange
        List<Pets> pets = List.of(testPet);
        given(petsRepository.findBySpeciesContainingIgnoreCaseAndAdoptionStatus(
                        eq("cat"), eq("AVAILABLE"), any(Pageable.class)))
                .willReturn(new PageImpl<>(pets, firstPage(), pets.size()));

        // Act
        PagedResponse<PetResponse> result = petsService.getPets(0, 10, "name", "cat", "AVAILABLE");

        // Assert
        assertEquals(1, result.getContent().size());
        verify(petsRepository)
                .findBySpeciesContainingIgnoreCaseAndAdoptionStatus(eq("cat"), eq("AVAILABLE"), any(Pageable.class));
    }

    @Test
    void getPets_ClampsPageSize_ToOneHundred() {
        // Arrange
        given(petsRepository.findAll(any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(), firstPage(), 0));

        // Act
        petsService.getPets(0, 500, "name", null, null);

        // Assert
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(petsRepository).findAll(pageable.capture());
        assertEquals(100, pageable.getValue().getPageSize());
    }

    @Test
    void updatePet_AppliesOptionalFields_WhenPresent() {
        // Arrange
        updateRequest.setAge(7);
        updateRequest.setColor("brindle");
        updateRequest.setAdoptionStatus("ADOPTED");
        given(petsRepository.findById("pet-123")).willReturn(Optional.of(testPet));
        given(petsRepository.save(any(Pets.class))).willAnswer(invocation -> invocation.getArgument(0));

        // Act
        PetResponse result = petsService.updatePet("pet-123", updateRequest);

        // Assert
        assertEquals(7, result.getAge());
        assertEquals("brindle", result.getColor());
        assertEquals("ADOPTED", result.getAdoptionStatus());
    }

    @Test
    void createPet_KeepsExplicitAdoptionStatus() {
        // Arrange
        createRequest.setAdoptionStatus("ADOPTED");
        given(petsRepository.save(any(Pets.class))).willAnswer(invocation -> invocation.getArgument(0));

        // Act
        PetResponse result = petsService.createPet(createRequest);

        // Assert
        assertEquals("ADOPTED", result.getAdoptionStatus());
    }
}
