package com.bookstore.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.bson.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Standalone Production Migration Script for Duplicate User Account Resolution.
 * Must be executed explicitly as a separate operation — NOT on startup or during unit tests.
 */
public class ProductionUserMigrationScript {

    private static final String DEFAULT_URI = "mongodb+srv://chandanak1009_db_user:LeafyBooks2026@cluster0.k00qwf5.mongodb.net/bookstore?retryWrites=true&w=majority&authSource=admin&appName=Cluster0";

    public static void main(String[] args) {
        String mongoUri = args.length > 0 ? args[0] : System.getenv().getOrDefault("SPRING_DATA_MONGODB_URI", DEFAULT_URI);
        System.out.println(">>> Starting Production User Migration on MongoDB URI: " + mongoUri.replaceAll(":([^@]+)@", ":****@"));

        try (MongoClient client = MongoClients.create(mongoUri)) {
            MongoDatabase db = client.getDatabase("bookstore");
            MongoCollection<Document> usersColl = db.getCollection("users");
            MongoCollection<Document> backupColl = db.getCollection("users_backup");

            // 1. Find all users with username "Chandana"
            List<Document> duplicateUsers = usersColl.find(new Document("username", "Chandana")).into(new ArrayList<>());
            System.out.println(">>> Found " + duplicateUsers.size() + " accounts with username 'Chandana'");

            if (duplicateUsers.isEmpty()) {
                System.out.println(">>> No duplicate username 'Chandana' found. Verifying indexes...");
            } else {
                for (Document u : duplicateUsers) {
                    System.out.println("  User ID: " + u.get("_id") + ", Email: " + u.get("email") + ", Roles: " + u.get("roles"));
                }

                Document primaryAcc = usersColl.find(new Document("_id", "69fa1fcfad154648cb0e19a2")).first();
                Document duplicateAcc = usersColl.find(new Document("_id", "69fa1fd2ad154648cb0e19a3")).first();

                if (duplicateAcc != null) {
                    System.out.println(">>> Target duplicate account 69fa1fd2ad154648cb0e19a3 identified.");

                    // 2. Check replacement username and email availability
                    String newUsername = "Chandana_2";
                    String newEmail = "chandana_2@leafybooks.com";

                    long existingNewUsernameCount = usersColl.countDocuments(new Document("username", newUsername));
                    long existingNewEmailCount = usersColl.countDocuments(new Document("email", newEmail));

                    if (existingNewUsernameCount > 0 || existingNewEmailCount > 0) {
                        System.err.println(">>> ERROR: Replacement username or email already in use!");
                        return;
                    }

                    // 3. Backup document to users_backup collection before modification
                    Document backupCopy = new Document(duplicateAcc);
                    backupCopy.put("_backedUpAt", Instant.now().toString());
                    backupColl.insertOne(backupCopy);
                    System.out.println(">>> Backup of duplicate account saved to 'users_backup' collection.");

                    // 4. Safely update username and email on duplicate account
                    Document updateOp = new Document("$set", new Document("username", newUsername).append("email", newEmail));
                    usersColl.updateOne(new Document("_id", "69fa1fd2ad154648cb0e19a3"), updateOp);
                    System.out.println(">>> Successfully updated account 69fa1fd2ad154648cb0e19a3 to username='" + newUsername + "', email='" + newEmail + "'");

                    // 5. Verify updated document
                    Document updatedDoc = usersColl.find(new Document("_id", "69fa1fd2ad154648cb0e19a3")).first();
                    System.out.println(">>> Verified updated document: " + updatedDoc.toJson());
                } else {
                    System.out.println(">>> Duplicate account 69fa1fd2ad154648cb0e19a3 not found (may already be resolved).");
                }
            }

            // 6. Ensure unique indexes on username and email
            System.out.println(">>> Creating/verifying unique index on users.username...");
            usersColl.createIndex(Indexes.ascending("username"), new IndexOptions().unique(true));
            System.out.println(">>> Creating/verifying unique index on users.email...");
            usersColl.createIndex(Indexes.ascending("email"), new IndexOptions().unique(true));

            System.out.println(">>> Production User Migration Completed Successfully! All unique indexes verified.");

        } catch (Exception e) {
            System.err.println(">>> Production Migration Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
