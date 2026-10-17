package com.gourav.restapi.utils;

import com.gourav.restapi.config.AppProperties;
import com.gourav.restapi.models.Pets;
import com.gourav.restapi.repositories.PetsRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Seeds the pets collection with sample data when the {@code local} profile is active.
 *
 * <p>The source ran this from a {@code @Profile("local") @EventListener(ApplicationReadyEvent.class)}
 * bean. Here the profile check is explicit and the trigger is an explicit call from
 * {@code RestApiApplication} once the HTTP server is up, which is the same moment in the lifecycle.
 */
@ApplicationScoped
public class DbSeeder {

    private static final Logger log = LoggerFactory.getLogger(DbSeeder.class);

    private final PetsRepository petsRepository;
    private final AppProperties properties;

    @Inject
    public DbSeeder(PetsRepository petsRepository, AppProperties properties) {
        this.petsRepository = petsRepository;
        this.properties = properties;
    }

    public void savePets() {
        if (!isLocalProfile()) {
            return;
        }
        if (petsRepository.count() > 0) {
            log.info("DbSeeder: pets collection already has data — skipping seed.");
            return;
        }
        log.info("DbSeeder: seeding pets collection with sample data...");
        petsRepository.save(Pets.builder().name("Spike").species("Dog").breed("Bulldog")
                .age(3).color("Brown").adoptionStatus("AVAILABLE").build());
        petsRepository.save(Pets.builder().name("Tom").species("Cat").breed("Regular")
                .age(2).color("Grey").adoptionStatus("AVAILABLE").build());
        petsRepository.save(Pets.builder().name("Jerry").species("Mouse").breed("Special")
                .age(1).color("Brown").adoptionStatus("ADOPTED").build());
        log.info("DbSeeder: seeded {} pets.", petsRepository.count());
    }

    private boolean isLocalProfile() {
        String profiles = properties.get("app.profiles.active", "");
        for (String profile : profiles.split(",")) {
            if ("local".equals(profile.trim())) {
                return true;
            }
        }
        return false;
    }
}
