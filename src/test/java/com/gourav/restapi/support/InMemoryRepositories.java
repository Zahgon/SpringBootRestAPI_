package com.gourav.restapi.support;

import com.gourav.restapi.models.ERole;
import com.gourav.restapi.models.Pets;
import com.gourav.restapi.models.Role;
import com.gourav.restapi.models.User;
import com.gourav.restapi.repositories.PetsRepository;
import com.gourav.restapi.repositories.RoleRepository;
import com.gourav.restapi.repositories.UserRepository;
import com.gourav.restapi.repositories.paging.Page;
import com.gourav.restapi.repositories.paging.PageImpl;
import com.gourav.restapi.repositories.paging.Pageable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/** Repository stand-ins so the HTTP surface can be exercised without a database. */
public final class InMemoryRepositories {

	private InMemoryRepositories() {
	}

	/** Pets kept in insertion order, paged and sorted the way the Mongo-backed repository is. */
	public static final class PetStore implements PetsRepository {

		private final Map<String, Pets> store = new LinkedHashMap<>();
		private final AtomicInteger sequence = new AtomicInteger();

		@Override
		public Page<Pets> findAll(Pageable pageable) {
			return page(pet -> true, pageable);
		}

		@Override
		public Page<Pets> findBySpeciesContainingIgnoreCase(String species, Pageable pageable) {
			return page(pet -> contains(pet.getSpecies(), species), pageable);
		}

		@Override
		public Page<Pets> findByAdoptionStatus(String adoptionStatus, Pageable pageable) {
			return page(pet -> adoptionStatus.equals(pet.getAdoptionStatus()), pageable);
		}

		@Override
		public Page<Pets> findBySpeciesContainingIgnoreCaseAndAdoptionStatus(
				String species, String adoptionStatus, Pageable pageable) {
			return page(
					pet -> contains(pet.getSpecies(), species) && adoptionStatus.equals(pet.getAdoptionStatus()),
					pageable);
		}

		@Override
		public Optional<Pets> findById(String id) {
			return Optional.ofNullable(store.get(id));
		}

		@Override
		public Pets save(Pets pets) {
			if (pets.getId() == null) {
				pets.setId("pet-" + sequence.incrementAndGet());
			}
			if (pets.getCreatedAt() == null) {
				pets.setCreatedAt(Instant.now());
			}
			pets.setUpdatedAt(Instant.now());
			store.put(pets.getId(), pets);
			return pets;
		}

		@Override
		public void delete(Pets pets) {
			store.remove(pets.getId());
		}

		@Override
		public long count() {
			return store.size();
		}

		private Page<Pets> page(Predicate<Pets> filter, Pageable pageable) {
			List<Pets> matched = new ArrayList<>(store.values().stream().filter(filter).toList());
			matched.sort(Comparator.comparing(pet -> String.valueOf(property(pet, pageable))));
			int from = (int) Math.min(pageable.getOffset(), matched.size());
			int to = Math.min(from + pageable.getPageSize(), matched.size());
			return new PageImpl<>(matched.subList(from, to), pageable, matched.size());
		}

		private static Object property(Pets pet, Pageable pageable) {
			return switch (pageable.getSort().getProperty()) {
				case "name" -> pet.getName();
				case "species" -> pet.getSpecies();
				case "breed" -> pet.getBreed();
				default -> "";
			};
		}

		private static boolean contains(String value, String fragment) {
			return value != null && value.toLowerCase(Locale.ROOT).contains(fragment.toLowerCase(Locale.ROOT));
		}
	}

	/** Users addressed by username, as the Mongo-backed repository does. */
	public static final class UserStore implements UserRepository {

		private final Map<String, User> store = new LinkedHashMap<>();
		private final AtomicInteger sequence = new AtomicInteger();

		@Override
		public Optional<User> findByUsername(String username) {
			return store.values().stream().filter(user -> user.getUsername().equals(username)).findFirst();
		}

		@Override
		public Boolean existsByUsername(String username) {
			return findByUsername(username).isPresent();
		}

		@Override
		public Boolean existsByEmail(String email) {
			return store.values().stream().anyMatch(user -> user.getEmail().equals(email));
		}

		@Override
		public User save(User user) {
			if (user.getId() == null) {
				user.setId("user-" + sequence.incrementAndGet());
			}
			store.put(user.getId(), user);
			return user;
		}
	}

	/** The three seeded roles. */
	public static final class RoleStore implements RoleRepository {

		private final Map<ERole, Role> store = new LinkedHashMap<>();

		public RoleStore() {
			for (ERole name : ERole.values()) {
				Role role = new Role(name);
				role.setId(name.name().toLowerCase(Locale.ROOT).replace("role_", "role-"));
				store.put(name, role);
			}
		}

		@Override
		public Optional<Role> findByName(ERole name) {
			return Optional.ofNullable(store.get(name));
		}
	}
}
