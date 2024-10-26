package exercises

import org.bson.Document
import com.mongodb.client.MongoClients
import static com.mongodb.client.model.Filters.*

// Load credentials from src/main/resources/mongodb.properties
def properties = new Properties()
def propertiesFile = new File('src/main/resources/mongodb.properties')
propertiesFile.withInputStream {
    properties.load(it)
}

// connection string to connect to the primary node to make write operations
String connectionString = "mongodb+srv://${properties.USN}:${properties.PWD}@cluster0.${properties.SERVER}.mongodb.net/${properties.DB}?retryWrites=true&w=majority&appName=${properties.APP_NAME}";

def mongoClient = MongoClients.create(connectionString)
def db = mongoClient.getDatabase(properties.DB)
def airbnb_collection = db.getCollection("listingsAndReviews")

println airbnb_collection.find().first()
