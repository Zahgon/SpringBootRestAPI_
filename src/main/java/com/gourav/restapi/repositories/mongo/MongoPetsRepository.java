package com.gourav.restapi.repositories.mongo;

import com.gourav.restapi.models.Pets;
import com.gourav.restapi.repositories.PetsRepository;
import com.gourav.restapi.repositories.paging.Page;
import com.gourav.restapi.repositories.paging.PageImpl;
import com.gourav.restapi.repositories.paging.Pageable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class MongoPetsRepository implements PetsRepository {

    private static final String COLLECTION = "pets";

    private final MongoDatabase database;
    private MongoCollection<Document> pets;

    @Inject
    public MongoPetsRepository(MongoDatabase database) {
        this.database = database;
    }

    @PostConstruct
    void init() {
        this.pets = database.getCollection(COLLECTION);
    }

    @Override
    public Page<Pets> findAll(Pageable pageable) {
        return page(new Document(), pageable);
    }

    @Override
    public Page<Pets> findBySpeciesContainingIgnoreCase(String species, Pageable pageable) {
        return page(MongoSupport.containsIgnoreCase("species", species), pageable);
    }

    @Override
    public Page<Pets> findByAdoptionStatus(String adoptionStatus, Pageable pageable) {
        return page(Filters.eq("adoptionStatus", adoptionStatus), pageable);
    }

    @Override
    public Page<Pets> findBySpeciesContainingIgnoreCaseAndAdoptionStatus(String species, String adoptionStatus,
                                                                        Pageable pageable) {
        Bson filter = Filters.and(
                MongoSupport.containsIgnoreCase("species", species),
                Filters.eq("adoptionStatus", adoptionStatus));
        return page(filter, pageable);
    }

    @Override
    public Optional<Pets> findById(String id) {
        Document document = pets.find(Filters.eq("_id", MongoSupport.toId(id))).first();
        return Optional.ofNullable(document).map(MongoPetsRepository::toPet);
    }

    @Override
    public Pets save(Pets pet) {
        Instant now = Instant.now();
        if (pet.getCreatedAt() == null) {
            pet.setCreatedAt(now);
        }
        pet.setUpdatedAt(now);

        Document document = toDocument(pet);
        if (pet.getId() == null) {
            pets.insertOne(document);
            pet.setId(MongoSupport.fromId(document.get("_id")));
        } else {
            pets.replaceOne(Filters.eq("_id", MongoSupport.toId(pet.getId())), document);
        }
        return pet;
    }

    @Override
    public void delete(Pets pet) {
        pets.deleteOne(Filters.eq("_id", MongoSupport.toId(pet.getId())));
    }

    @Override
    public long count() {
        return pets.countDocuments();
    }

    private Page<Pets> page(Bson filter, Pageable pageable) {
        long total = pets.countDocuments(filter);
        List<Pets> content = new ArrayList<>();
        pets.find(filter)
                .sort(MongoSupport.sort(pageable))
                .skip((int) pageable.getOffset())
                .limit(pageable.getPageSize())
                .forEach(document -> content.add(toPet(document)));
        return new PageImpl<>(content, pageable, total);
    }

    private static Pets toPet(Document document) {
        return Pets.builder()
                .id(MongoSupport.fromId(document.get("_id")))
                .name(document.getString("name"))
                .species(document.getString("species"))
                .breed(document.getString("breed"))
                .age(MongoSupport.integer(document, "age"))
                .color(document.getString("color"))
                .adoptionStatus(document.getString("adoptionStatus"))
                .createdAt(MongoSupport.instant(document, "createdAt"))
                .updatedAt(MongoSupport.instant(document, "updatedAt"))
                .build();
    }

    private static Document toDocument(Pets pet) {
        Document document = new Document();
        if (pet.getId() != null) {
            document.put("_id", MongoSupport.toId(pet.getId()));
        }
        document.put("name", pet.getName());
        document.put("species", pet.getSpecies());
        document.put("breed", pet.getBreed());
        document.put("age", pet.getAge());
        document.put("color", pet.getColor());
        document.put("adoptionStatus", pet.getAdoptionStatus());
        document.put("createdAt", MongoSupport.date(pet.getCreatedAt()));
        document.put("updatedAt", MongoSupport.date(pet.getUpdatedAt()));
        return document;
    }
}
