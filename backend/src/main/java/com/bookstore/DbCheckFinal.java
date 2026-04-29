import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class DbCheckFinal {
    public static void main(String[] args) {
        String uri = "mongodb+srv://chandanak1009_db_user:LeafyBooks2026@cluster0.k00qwf5.mongodb.net/bookstore?retryWrites=true&w=majority&authSource=admin&appName=Cluster0";
        try (MongoClient mongoClient = MongoClients.create(uri)) {
            MongoDatabase database = mongoClient.getDatabase("bookstore");
            MongoCollection<Document> books = database.getCollection("books");
            System.out.println("Total books in DB: " + books.countDocuments());
            for (Document doc : books.find()) {
                System.out.println("Book: " + doc.get("title") + " Seller: " + doc.get("sellerId"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
