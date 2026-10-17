package com.gourav.restapi.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gourav.restapi.config.JacksonConfig;
import com.gourav.restapi.controllers.payload.request.CreatePetRequest;
import com.gourav.restapi.controllers.payload.response.PagedResponse;
import com.gourav.restapi.controllers.payload.response.PetResponse;
import com.gourav.restapi.server.HttpServer;
import com.gourav.restapi.services.PetsService;
import com.gourav.restapi.support.RawHttp;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Drives {@link PetsController} over HTTP with the service mocked out.
 *
 * <p>The pipeline is assembled without the authentication, authorisation and CORS filters, which is
 * the same slice the previous test drove.</p>
 */
class PetsControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private PetsService petsService;
    private HttpServer server;
    private int port;

    @BeforeEach
    void startServer() throws Exception {
        petsService = mock(PetsService.class);
        ResourceConfig application = new ResourceConfig()
                .register(new PetsController(petsService))
                .register(JacksonConfig.class)
                .register(GlobalExceptionHandler.ValidationExceptionMapper.class)
                .register(GlobalExceptionHandler.ApiStatusExceptionMapper.class)
                .register(GlobalExceptionHandler.RoleNotFoundExceptionMapper.class)
                .register(GlobalExceptionHandler.CatchAllExceptionMapper.class);
        application.property(ServerProperties.OUTBOUND_CONTENT_LENGTH_BUFFER, 0);
        application.property(ServerProperties.WADL_FEATURE_DISABLE, true);

        server = new HttpServer(0, application);
        server.start();
        port = server.getPort();
    }

    @AfterEach
    void stopServer() throws Exception {
        server.stop();
    }

    private JsonNode json(RawHttp.Response response) throws Exception {
        return objectMapper.readTree(response.body());
    }

    @Test
    void getAllPets() throws Exception {
        given(petsService.getPets(0, 10, "name", null, null))
                .willReturn(PagedResponse.<PetResponse>builder()
                        .content(List.of(PetResponse.builder().id("pet-1").name("Liam").species("cat").breed("tabby").build()))
                        .page(0).size(10).totalElements(1).totalPages(1).last(true)
                        .build());

        RawHttp.Response response = RawHttp.get(port, "/pets/");

        assertEquals(200, response.status());
        JsonNode body = json(response);
        assertEquals("pet-1", body.at("/content/0/id").asText());
        assertEquals("Liam", body.at("/content/0/name").asText());
        assertEquals("tabby", body.at("/content/0/breed").asText());
        assertEquals("cat", body.at("/content/0/species").asText());
    }

    @Test
    void getPetById() throws Exception {
        given(petsService.getPetById("pet-1"))
                .willReturn(PetResponse.builder().id("pet-1").name("Liam").species("cat").breed("tabby").build());

        RawHttp.Response response = RawHttp.get(port, "/pets/pet-1");

        assertEquals(200, response.status());
        JsonNode body = json(response);
        assertEquals("pet-1", body.get("id").asText());
        assertEquals("Liam", body.get("name").asText());
        assertEquals("tabby", body.get("breed").asText());
        assertEquals("cat", body.get("species").asText());
    }

    @Test
    void createPetReturnsCreatedPet() throws Exception {
        CreatePetRequest request = new CreatePetRequest();
        request.setName("Liam");
        request.setSpecies("cat");
        request.setBreed("tabby");

        given(petsService.createPet(any(CreatePetRequest.class)))
                .willReturn(PetResponse.builder().id("pet-1").name("Liam").species("cat").breed("tabby").build());

        RawHttp.Response response =
                RawHttp.json(port, "POST", "/pets/", objectMapper.writeValueAsString(request));

        assertEquals(201, response.status());
        JsonNode body = json(response);
        assertEquals("pet-1", body.get("id").asText());
        assertEquals("Liam", body.get("name").asText());
    }

    @Test
    void modifyPetByIdReturnsUpdatedPet() throws Exception {
        CreatePetRequest request = new CreatePetRequest();
        request.setName("Luna");
        request.setSpecies("cat");
        request.setBreed("siamese");

        given(petsService.updatePet(eq("pet-1"), any()))
                .willReturn(PetResponse.builder().id("pet-1").name("Luna").species("cat").breed("siamese").build());

        RawHttp.Response response =
                RawHttp.json(port, "PUT", "/pets/pet-1", objectMapper.writeValueAsString(request));

        assertEquals(200, response.status());
        JsonNode body = json(response);
        assertEquals("Luna", body.get("name").asText());
        assertEquals("siamese", body.get("breed").asText());
    }

    @Test
    void deletePetReturnsNoContent() {
        RawHttp.Response response = RawHttp.request(port, "DELETE", "/pets/pet-1", java.util.Map.of(), null);

        assertEquals(204, response.status());
        verify(petsService).deletePet("pet-1");
    }

    @Test
    void createPetRejectsInvalidBody() throws Exception {
        RawHttp.Response response = RawHttp.json(port, "POST", "/pets/", "{}");

        assertEquals(400, response.status());
        JsonNode fieldErrors = json(response).get("fieldErrors");
        assertFalse(fieldErrors.path("name").isMissingNode());
        assertFalse(fieldErrors.path("species").isMissingNode());
        assertFalse(fieldErrors.path("breed").isMissingNode());
    }
}
