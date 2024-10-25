package exercises

import org.bson.Document
import com.mongodb.client.MongoClients
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.mongodb.client.model.Accumulators
import static com.mongodb.client.model.Projections.*

// Load credentials from src/main/resources/mongodb.properties
def properties = new Properties()
def propertiesFile = new File('src/main/resources/mongodb.properties')
propertiesFile.withInputStream {
    properties.load(it)
}

// TODO: Create connection to a secondary node using readPreference
def secondaryClient = MongoClients.create("mongodb+srv://${properties.USN}:${properties.PWD}@cluster0.${properties.SERVER}.mongodb.net/${properties.DB}?retryWrites=true&w=majority&readPreference=secondary")
// TODO: replace properties.DB with the name of your database
def db = secondaryClient.getDatabase(properties.DB)
def col = db.getCollection("movies")

// Define the aggregation pipeline to filter movies after 2000, group by genres, calculate avgRating, and sort results
def pipeline = [
    Aggregates.match(Filters.gte("year", 2000)), // Filter movies released after 2000
    Aggregates.project(new Document("genres", 1) // Project only genres and imdb.rating fields
        .append("imdbRating", "\$imdb.rating")
    ),
    Aggregates.group('\$genres', Accumulators.avg('avgRating', '\$imdbRating')), // Group by genres and calculate avgRating
    Aggregates.sort(Sorts.descending('avgRating')), // Sort by avgRating
    Aggregates.limit(3) // Limit to top 3 results
]

// Execute the aggregation pipeline on the secondary node
def result = col.aggregate(pipeline).into([])

println "Aggregated results from secondary node:"
result.each { println it }

// Simulate a failover by disabling access to the primary node in MongoDB Atlas, then rerun the pipeline
Thread.sleep(5000) // Short delay for manual failover testing

println "Re-running the aggregation pipeline after failover:"
def failoverResult = col.aggregate(pipeline).into([])
failoverResult.each { println it }
