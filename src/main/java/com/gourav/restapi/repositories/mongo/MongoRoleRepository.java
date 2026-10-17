package com.gourav.restapi.repositories.mongo;

import com.gourav.restapi.models.ERole;
import com.gourav.restapi.models.Role;
import com.gourav.restapi.repositories.RoleRepository;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Optional;
import org.bson.Document;

/** MongoDB implementation of {@link RoleRepository}. */
@ApplicationScoped
public class MongoRoleRepository implements RoleRepository {

  private final MongoDatabase database;

  private MongoCollection<Document> roles;

  @Inject
  public MongoRoleRepository(MongoDatabase database) {
    this.database = database;
  }

  @PostConstruct
  void init() {
    this.roles = database.getCollection("roles");
  }

  @Override
  public Optional<Role> findByName(ERole name) {
    Document document = roles.find(Filters.eq("name", name.name())).first();
    return Optional.ofNullable(document).map(this::toRole);
  }

  private Role toRole(Document document) {
    Role role = new Role(ERole.valueOf(document.getString("name")));
    role.setId(MongoSupport.fromId(document.get("_id")));
    return role;
  }
}
