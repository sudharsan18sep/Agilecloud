package exercises

import org.bson.Document

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import static com.mongodb.client.model.Accumulators.*

import static com.mongodb.client.model.Aggregates.*
import static com.mongodb.client.model.Filters.*
import static com.mongodb.client.model.Sorts.*
import static com.mongodb.client.model.Projections.*

import com.mongodb.client.model.Filters

import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.BucketAutoOptions
import com.mongodb.client.model.Facet
import com.mongodb.client.model.UnwindOptions

import com.mongodb.client.MongoClients


// Load credentials from src/main/resources/mongodb.properties
def properties = new Properties()
def propertiesFile = new File('src/main/resources/mongodb.properties')
propertiesFile.withInputStream {
    properties.load(it)
}

def uploadCollection(collection, src) {
	def parser = new JsonSlurper()
	def file = new File(src)
	def doc = parser.parse(file) // parse JSON file
	
	int i = 0
	doc.each{
		println ""
		println "object ${i++}: $it"
		document = new Document();
		document.putAll(it)
		collection.insertOne(document)
	}
}

def printResult(col, pipeline) {
	def result = col.aggregate(pipeline).into([])
	println("----------------------")
	result.each { println it }
	
	println("----------------------")
}


// Connection string to connect to the primary node to make write operations
String connectionString = "mongodb+srv://${properties.USN}:${properties.PWD}@cluster0.${properties.SERVER}.mongodb.net/${properties.DB}?retryWrites=true&w=majority&appName=${properties.APP_NAME}";

def mongoClient = MongoClients.create(connectionString)
def db = mongoClient.getDatabase(properties.DB)
def airbnb_collection = db.getCollection("listingsAndReviews")

//  UPLOAD country_gni json as a collection to MongoDB Atlas
//def country_gni_collection = db.getCollection("country_gni")
// uploadCollection(country_gni_collection, 'src/main/resources/country_gni.json')




//--------------------------------------------- DATA PROJECTION -----------------------------------------------------------

// SELECT listingsAndReviews.country, listingsAndReviews.price, country_gni.gni_2019
// FROM (
//	SELECT address->>'country' AS country, *
//	FROM listingsAndReviews
//	) AS listingsAndReviews
//	INNER JOIN country_gni
//	ON listingsAndReviews.country = country_gni.country;


def data_projection_pipeline = [
	lookup( // join listingsAndReviews and country_gni on country field
		"country_gni", // collection to join with
		"address.country", // local field in listingsAndReviews
		"country", // country field from country_gni collection
		"gni_info" // output for the matched documents
	),
	unwind( // filter docs greater than 40
		"\$gni_info"
	),
	project(new Document([
		name: '$name',
        country: '$address.country',
		price: '$price',
		gni_2019: '$gni_info.gni_2019'
    ]))
]
printResult(airbnb_collection, data_projection_pipeline)


//--------------------------------------------- DATA FILTERING -----------------------------------------------------------

// SELECT listingsAndReviews.country, listingsAndReviews.price, country_gni.gni_2019
// FROM (
//	 SELECT address->>'country' AS country,
//   price,
//   review_scores->'review_scores'->>'review_scores_rating' AS review_scores_rating
//	 FROM listingsAndReviews
// ) AS listingsAndReviews
// INNER JOIN country_gni
// ON listingsAndReviews.country = country_gni.country
// WHERE listingsAndReviews.review_scores_rating::int > 50;

def data_filtering_pipeline = [
	lookup( // join listingsAndReviews and country_gni on country field
		"country_gni", // collection to join with
		"address.country", // local field in listingsAndReviews
		"country", // country field from country_gni collection
		"gni_info" // output collection for the matched documents
	),
	unwind( // expand gni_info to access gni data
		"\$gni_info", new UnwindOptions().preserveNullAndEmptyArrays(true)
	),
	project(new Document([ // 1st stage of projection: extract country, price, review_score_rating, gni_2019
		country: '$address.country',
		price: '$price',
		review_scores_rating: '$review_scores.review_scores_rating',
		gni_2019: '$gni_info.gni_2019'
	])),
	match( // filter docs greater than 50
		gte("review_scores_rating", 50)
	),
	project(new Document([
		_id: 0, // exclude id
        country: '$country',
		price: '$price',
		gni_2019: '$gni_2019'
    ])),
]
printResult(airbnb_collection, data_filtering_pipeline)


//--------------------------------------------- DATA COMBINATION -----------------------------------------------------------

// SELECT listingsAndReviews.country, avg(listingsAndReviews.price) as average_price, country_gni.gni_2019
// FROM (
//     SELECT address->>'country' AS country, 
//   price,
//   review_scores->'review_scores'->>'review_scores_rating' AS review_scores_rating
//     FROM listingsAndReviews
// ) AS listingsAndReviews
// INNER JOIN country_gni
// ON listingsAndReviews.country = country_gni.country
// WHERE listingsAndReviews.review_scores_rating::int > 50
// GROUP BY listingsAndReviews.country, country_gni.gni_2019
// ORDER BY country_gni.gni_2019 asc;

def data_combination_pipeline = [
	lookup( // join listingsAndReviews and country_gni on country field
		"country_gni", // collection to join with
		"address.country", // local field in listingsAndReviews
		"country", // country field from country_gni collection
		"gni_info" // output collection for the matched documents
	),
	unwind( // expand gni_info to access gni data
		"\$gni_info", new UnwindOptions().preserveNullAndEmptyArrays(true)
	),
	project(new Document([ // 1st stage of projection: extract country, price, review_score_rating, gni_2019
		country: '$address.country',
		price: '$price',
		review_scores_rating: '$review_scores.review_scores_rating',
		gni_2019: '$gni_info.gni_2019'
	])),
	match( // filter docs greater than 50
		gte("review_scores_rating", 50)
	),
	group(
		new Document([ // Group by both country and gni_2019
			country: '$country',
			gni_2019: '$gni_2019'
		]),
		avg("average_price", '$price') // Calculate average price per group
		),
	sort(ascending("gni_2019")),
]
printResult(airbnb_collection, data_combination_pipeline)


