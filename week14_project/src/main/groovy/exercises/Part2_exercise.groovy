package exercises

import org.bson.Document

import com.mongodb.client.MongoClients
import static com.mongodb.client.model.Filters.*
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

// Load credentials from src/main/resources/mongodb.properties
def properties = new Properties()
def propertiesFile = new File('src/main/resources/mongodb.properties')
propertiesFile.withInputStream {
    properties.load(it)
}

def uploadCollection(collection, src) {
	def parser = new JsonSlurper()
	// parse JSON file
	def file = new File(src)
	def doc = parser.parse(file)
	
	int i = 0
	doc.each{
		println ""
		println "object ${i++}: $it"
		document = new Document();
		document.putAll(it)
		collection.insertOne(document)
	}
}

// connection string to connect to the primary node to make write operations
String connectionString = "mongodb+srv://${properties.USN}:${properties.PWD}@cluster0.${properties.SERVER}.mongodb.net/${properties.DB}?retryWrites=true&w=majority&appName=${properties.APP_NAME}";

def mongoClient = MongoClients.create(connectionString)
def db = mongoClient.getDatabase(properties.DB)
def airbnb_collection = db.getCollection("listingsAndReviews")
def country_gni_collection = db.getCollection("country_gni")

// upload method for upload collection to mongodb atlas
//uploadCollection(country_gni_collection, 'src/main/resources/country_gni.json')


