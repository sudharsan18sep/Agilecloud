package exercises

import static com.mongodb.client.model.Accumulators.*
import static com.mongodb.client.model.Aggregates.*
import static com.mongodb.client.model.Filters.*
import static com.mongodb.client.model.Sorts.*
import static com.mongodb.client.model.Projections.*

import org.bson.Document

import com.mongodb.client.MongoClients

import com.mongodb.client.model.Filters

import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.BucketAutoOptions
import com.mongodb.client.model.Facet



// Load credentials from src/main/resources/mongodb.properties
def properties = new Properties()
def propertiesFile = new File('src/main/resources/mongodb.properties')
propertiesFile.withInputStream {
    properties.load(it)
}

def printResult(exercise, col, pipeline) {
	def result = col.aggregate(pipeline).into([])
	println("----------------------")
	println("EXERCISE ${exercise}")
	result.each { println it }
}

// Create connection: replace <YOUR-CONNECTION-STRING> with the corresponding part of your connection string
def mongoClient = MongoClients.create("mongodb+srv://<YOUR-CONNECTION-STRING>?retryWrites=true&w=majority")
// replace properties.DB with the name of your DB
def db = mongoClient.getDatabase(properties.DB)
def col = db.getCollection("movies")



// Exercise 1: create aggregation pipeline to filter movies released after 2000
def pipeline_1 = [
    // Add stages for $match, $group, and $sort
]
printResult(1, col, pipeline_1)


// Exercise 2: extend the previous pipeline to add projection and limit stages
def pipeline_2 = [
    // Add stages for $match, $group, $project, $sort, and $limit
]
printResult(2, col, pipeline_2)


// Exercise 3: create pipeline to filter movies after 2010 with IMDb >= 8 and group by director
def pipeline_3 = [
    // Add stages for $match, $group (by director), $sum, and $avg
]
printResult(3, col, pipeline_3)


// Exercise 4: create pipeline to filter movies with "Action" genre, unwind genres, and group by genre
def pipeline_4 = [
    // Add stages for $match (filter by "Action"), $unwind, $group (by genre)
]
printResult(4, col, pipeline_4)


// Exercise 5: Using $project for Field Selection and Transformation
def pipeline_5 = [
    // Implement the $project pipeline here
]
printResult(5, col, pipeline_5)


// Exercise 6: Using $merge for Writing Results to Another Collection
def pipeline_6 = [
    // Implement the $merge pipeline here
]
printResult(6, col, pipeline_6)
