package com.gourav.restapi.repositories.mongo;

import com.gourav.restapi.config.AppProperties;
import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

/**
 * Opens the single application-wide MongoDB connection from {@code mongodb.uri} and exposes the
 * database named in that connection string.
 */
@ApplicationScoped
public class MongoClientProducer {

    private final AppProperties properties;

    @Inject
    public MongoClientProducer(AppProperties properties) {
        this.properties = properties;
    }

    @Produces
    @ApplicationScoped
    public MongoClient mongoClient() {
        return MongoClients.create(connectionString());
    }

    @Produces
    @ApplicationScoped
    public MongoDatabase mongoDatabase(MongoClient mongoClient) {
        String database = connectionString().getDatabase();
        return mongoClient.getDatabase(database == null ? "rest_tutorial" : database);
    }

    public void close(@Disposes MongoClient mongoClient) {
        mongoClient.close();
    }

    private ConnectionString connectionString() {
        return new ConnectionString(properties.get("mongodb.uri"));
    }
}
