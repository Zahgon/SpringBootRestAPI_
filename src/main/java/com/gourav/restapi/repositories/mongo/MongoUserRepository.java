package com.gourav.restapi.repositories.mongo;

import com.gourav.restapi.models.ERole;
import com.gourav.restapi.models.Role;
import com.gourav.restapi.models.User;
import com.gourav.restapi.repositories.UserRepository;
import com.mongodb.DBRef;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.bson.Document;

/**
 * MongoDB implementation of {@link UserRepository}.
 *
 * <p>Roles are stored as MongoDB DBRef values pointing at the {@code roles} collection, which is
 * the layout the collection already has; the references are resolved eagerly on read.
 */
@ApplicationScoped
public class MongoUserRepository implements UserRepository {

  private static final String ROLES_COLLECTION = "roles";

  private final MongoDatabase database;

  private MongoCollection<Document> users;
  private MongoCollection<Document> roles;

  @Inject
  public MongoUserRepository(MongoDatabase database) {
    this.database = database;
  }

  @PostConstruct
  void init() {
    this.users = database.getCollection("users");
    this.roles = database.getCollection(ROLES_COLLECTION);
  }

  @Override
  public Optional<User> findByUsername(String username) {
    Document document = users.find(Filters.eq("username", username)).first();
    return Optional.ofNullable(document).map(this::toUser);
  }

  @Override
  public Boolean existsByUsername(String username) {
    return users.countDocuments(Filters.eq("username", username)) > 0;
  }

  @Override
  public Boolean existsByEmail(String email) {
    return users.countDocuments(Filters.eq("email", email)) > 0;
  }

  @Override
  public User save(User user) {
    Document document = toDocument(user);
    if (user.getId() == null) {
      users.insertOne(document);
      user.setId(MongoSupport.fromId(document.get("_id")));
    } else {
      users.replaceOne(Filters.eq("_id", MongoSupport.toId(user.getId())), document);
    }
    return user;
  }

  private User toUser(Document document) {
    User user = new User();
    user.setId(MongoSupport.fromId(document.get("_id")));
    user.setUsername(document.getString("username"));
    user.setEmail(document.getString("email"));
    user.setPassword(document.getString("password"));
    user.setRoles(resolveRoles(document.getList("roles", Object.class)));
    return user;
  }

  private Set<Role> resolveRoles(List<Object> references) {
    Set<Role> resolved = new HashSet<>();
    if (references == null) {
      return resolved;
    }
    for (Object reference : references) {
      Object roleId = reference instanceof DBRef ref ? ref.getId() : reference;
      Document document = roles.find(Filters.eq("_id", roleId)).first();
      if (document != null) {
        Role role = new Role(ERole.valueOf(document.getString("name")));
        role.setId(MongoSupport.fromId(document.get("_id")));
        resolved.add(role);
      }
    }
    return resolved;
  }

  private Document toDocument(User user) {
    List<DBRef> references = new ArrayList<>();
    for (Role role : user.getRoles()) {
      references.add(new DBRef(ROLES_COLLECTION, MongoSupport.toId(role.getId())));
    }
    Document document =
        new Document()
            .append("username", user.getUsername())
            .append("email", user.getEmail())
            .append("password", user.getPassword())
            .append("roles", references);
    if (user.getId() != null) {
      document.append("_id", MongoSupport.toId(user.getId()));
    }
    return document;
  }
}
