package com.gourav.restapi.controllers;

import com.gourav.restapi.controllers.payload.request.CreatePetRequest;
import com.gourav.restapi.controllers.payload.request.UpdatePetRequest;
import com.gourav.restapi.controllers.payload.response.PagedResponse;
import com.gourav.restapi.controllers.payload.response.PetResponse;
import com.gourav.restapi.services.PetsService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/pets")
@Produces(MediaType.APPLICATION_JSON)
public class PetsController {
	private final PetsService petsService;

	public PetsController(PetsService petsService) {
		this.petsService = petsService;
	}

	/**
	 * Returns a paginated, filterable list of pets.
	 *
	 * <p>Query parameters:
	 * <ul>
	 *   <li>{@code page}           — page number, 0-indexed (default: 0)</li>
	 *   <li>{@code size}           — items per page, max 100 (default: 10)</li>
	 *   <li>{@code sortBy}         — field to sort by (default: name)</li>
	 *   <li>{@code species}        — optional partial species filter (case-insensitive)</li>
	 *   <li>{@code adoptionStatus} — optional exact status filter (AVAILABLE, ADOPTED, PENDING)</li>
	 * </ul>
	 *
	 * <p>Examples:
	 * <pre>
	 *   GET /pets/?page=0&amp;size=10
	 *   GET /pets/?species=dog
	 *   GET /pets/?adoptionStatus=AVAILABLE&amp;page=1&amp;size=5
	 *   GET /pets/?species=cat&amp;adoptionStatus=AVAILABLE
	 * </pre>
	 */
	@GET
	@Path("/")
	@RolesAllowed({"ROLE_USER", "ROLE_MODERATOR", "ROLE_ADMIN"})
	public PagedResponse<PetResponse> getAllPets(
			@QueryParam("page") @DefaultValue("0") int page,
			@QueryParam("size") @DefaultValue("10") int size,
			@QueryParam("sortBy") @DefaultValue("name") String sortBy,
			@QueryParam("species") String species,
			@QueryParam("adoptionStatus") String adoptionStatus) {
		return petsService.getPets(page, size, sortBy, species, adoptionStatus);
	}

	@GET
	@Path("/{id}")
	@RolesAllowed({"ROLE_USER", "ROLE_MODERATOR", "ROLE_ADMIN"})
	public PetResponse getPetById(@PathParam("id") String id) {
		return petsService.getPetById(id);
	}

	@PUT
	@Path("/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@RolesAllowed({"ROLE_MODERATOR", "ROLE_ADMIN"})
	public PetResponse modifyPetById(@PathParam("id") String id,
									  @Valid UpdatePetRequest request) {
		return petsService.updatePet(id, request);
	}

	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@RolesAllowed({"ROLE_USER", "ROLE_MODERATOR", "ROLE_ADMIN"})
	public Response createPet(@Valid CreatePetRequest request) {
		return Response.status(Response.Status.CREATED).entity(petsService.createPet(request)).build();
	}

	@DELETE
	@Path("/{id}")
	@RolesAllowed("ROLE_ADMIN")
	public Response deletePet(@PathParam("id") String id) {
		petsService.deletePet(id);
		return Response.noContent().build();
	}
}
