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

// Create connection to the primary node
// TODO: write the connection string to connect to the primary node to make write operations

// TODO: replace properties.DB with your database name
def db = mongoClient.getDatabase(properties.DB)
def col = db.getCollection("movies")

// Insert a new movie document into the 'movies' collection
def newMovie = [
    title: "Test Movie",
    year: 2023,
    genres: ["Action", "Sci-Fi"],
    imdb: [ rating: 8.5, votes: 10000 ] // Aligning with the correct structure
]
col.insertOne(new Document(newMovie))
println "Inserted new movie document into the primary node."

// Wait for a short period to ensure replication is complete
Thread.sleep(5000) // 5-second delay

// TODO: configure the connection string to connect to the secondary node and read the document
def secondaryClient = MongoClients.create("")
// TODO: replace properties.DB with your database name
def secondaryDb = secondaryClient.getDatabase(properties.DB)
def result = secondaryDb.getCollection('movies').find(eq('title', "Test Movie")).into([])

println "Reading from secondary node:"
result.each { println it }
