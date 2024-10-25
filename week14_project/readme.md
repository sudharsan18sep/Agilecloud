<link rel='stylesheet' href='web/swiss.css'/>

# Lab session 4: Aggregation Pipelines and Replication/Scalability with MongoDB

In this week’s lab, you’ll combine **MongoDB aggregation pipelines** with **replication** and **scalability** in a hands-on session using **MongoDB Atlas**. We’ll guide you step-by-step through exercises that teach you how to process and analyze large datasets efficiently while ensuring high availability and fault tolerance through replication.

## Part 1: Aggregation Pipelines

### Setting Up the Project

1. **Download the project** from Blackboard.
2. **Unzip the project folder** and import it into **Eclipse** using `File > Import > Existing Gradle Project`. Ensure that the Gradle build completes successfully.
3. Create a new project as you did for the previous lab. If you already have created it reuse it and skip this step.
   * Create a new project with name `CO7217` without adding any member. 
   * Then the main page will give you the option to `Create a Cluster`, do so. Choose name `Cluster0` (default name) and select the free tier and `Create  Deployment`. Create the cluster and wait until it is active.
   * In the left menu, under `Security > Network access`, click on `Add IP ADDRESS` and select `ALLOW ACCESS FROM ANYWHERE`. Then `Confirm` the settings.
   * In the left menu, under `Security > Database access`, add a new user and select the role `Read and write to any database`. Use this user credentials for the connection string.
   * In the left menu, under `Database > Cluster`, click on `Browse Collections` and `Load Sample Dataset` (this may take 10 minutes, use this time to read the worksheet and plan your reflection submission at the end of the session in a local file). This is likely to take a few minutes, use this time to read the rest of the worksheet and the lecture notes and start thinking about how to solve the exercises. We will work with the database **sample_mflix** and the collection **movies**.
4. **Ensure MongoDB connection**: Configure your MongoDB Atlas connection in `src/main/resources/mongodb.properties` with your MongoDB Atlas username, password, and database name.
   - Example configuration:
     ```properties
     USN=yourUsername
     PWD=yourPassword
     DB=yourDatabase
     ```
5. The exercises can be found in `src/main/groovy/Part1_exercises.groovy`

### Exercise 1: Basic Aggregation Pipeline

In this exercise, you’ll build a simple aggregation pipeline to process data from the **movies** collection. The aggregation pipeline performs a sequence of operations, known as **stages**, which MongoDB executes in order.

- In `src/main/groovy/Part1_exercises.groovy`, define a pipeline that filters, groups, and sorts the movie data.

   ```groovy
   def pipeline_1 = [
      match(gte("year", 2000)),
      project(new Document("genres", 1) // Projecting genres and imdb.rating fields
         .append("imdbRating", "\$imdb.rating")
      ),
      group('\$genres', avg('avgRating', '\$imdbRating')), // Group by genres and calculate avgRating
      sort(descending('avgRating')) // Sort by avgRating
   ]
   
   def result = db.getCollection('movies').aggregate(pipeline_1).into([])
   result.each { println it }
   ```

- **Output**: The aggregation pipeline will filter movies released after 2000, group them by genre, and calculate the average IMDb rating for each genre.
- **Expected Result**: A list of genres, each with an average rating sorted from highest to lowest.


### Exercise 2: Advanced Aggregation – Multiple Stages

Extend the pipeline to:
1. **Project only necessary fields** like `genres` and `avgRating`.
2. **Limit the results** to the top 3 genres.

1. Add a `$project` stage:
   ```groovy
   def pipeline_2 = [
      match(gte("year", 2000)),
      group('\$genres', avg('avgRating', '\$imdb.rating')), // Grouping by genres
      project(fields(include("genres", "avgRating"))), // Project only genres and avgRating
      sort(descending('avgRating')), // Sort by avgRating
      limit(3) // Limit to top 3
   ]
   
   def result = db.getCollection('movies').aggregate(pipeline_2).into([])
   result.each { println it }
   ```

### **Exercise 3: Filtering on Multiple Fields**
**Goal:** Filter movies released after 2010 with an IMDb rating of 8 or higher. Group by the movie's directors, calculate the total number of movies per director, and the average IMDb rating.

**New Operator:**
- **`$and`**: Filters with multiple conditions.
- **`$group`**: Grouping documents by a specific field, useful for aggregation.

**Steps:**
1. Add a `$match` stage to filter movies released after 2010 with an IMDb rating greater than or equal to 8.
2. Group the results by directors.
3. Calculate the total number of movies and the average IMDb rating per director.
4. Sort by the number of movies.

### **Exercise 4: Aggregation with Array Fields**
**Goal:** Filter movies where at least one of the genres is "Action." Unwind the genres array and group the movies by genre to calculate the total number of movies per genre and the average IMDb rating.

**New Operator:**
- **`$unwind`**: Splits array fields into individual elements.

**Steps:**
1. Filter movies with the genre "Action."
2. Use `$unwind` to process each genre individually.
3. Group by genres and calculate the total number of movies per genre and the average IMDb rating.

### **Exercise 5: Field Selection and Transformation**
**Goal:** Use the `$project` stage to display the movie titles, years of release, and the number of words in each movie title.

**New Operator:**
- **`$project`**: Used for field selection and transformation, helpful for modifying documents.

**Steps:**
1. Use `$project` to select the `title` and `year` fields.
2. Use the `$size` and `$split` operators within `$project` to calculate the number of words in each title.

### **Exercise 6: Using `$merge` for Writing Results to Another Collection**
**Goal:** Group movies by decade of release, calculate the average IMDb rating for each decade, and store the results in a new collection.

**New Operator:**
- **`$merge`**: Used to write the results of an aggregation to another collection.

**Steps:**
1. Add a `$match` stage to filter movies released before the year 2000.
2. Group movies by their decade of release.
3. Calculate the average IMDb rating per decade.
4. Use `$merge` to write the results to a new collection.

To make the worksheet more thought-provoking while keeping the tasks manageable and aligned with the script-running exercises, you can add questions that encourage students to reflect on the replication, failover, and aggregation concepts. These can be designed to prompt critical thinking without adding much extra work. Here are some ideas:



### **Part 2: Setting Up Replication on MongoDB Atlas**

1. **Set Up a Replica Set**:
   - Your free-tier cluster will have **replication enabled by default**. A replica set in MongoDB consists of a **primary node** and **secondary nodes**.
   - Access your cluster and ensure that replication is working by checking the status in the **MongoDB Atlas dashboard** under the **Replica Set** section.

   **Reflection Question**:  
   - **Why is replication important for ensuring data availability and fault tolerance?**  
     Think about scenarios where a node might fail. How would replication prevent downtime or data loss?

2. **Test Replication**:
   - Use the following Groovy code to insert a document into the `movies` collection from the primary node:
     ```groovy
     def newMovie = [
         title: "Test Movie",
         year: 2023,
         genres: ["Action", "Sci-Fi"],
         imdb: [ rating: 8.5, votes: 10000 ] // Aligning with the correct structure
     ]
     db.getCollection('movies').insertOne(new Document(newMovie))
     ```

   - **Read from a secondary node** by setting a read preference:
     ```groovy
     def secondaryClient = MongoClients.create("<YOUR-CONNECTION-STRING>?readPreference=secondary")
     def db = secondaryClient.getDatabase('yourDatabase')
     def result = db.getCollection('movies').find().into([])
     result.each { println it }
     ```

   **Exercise**:
   - After inserting the document on the primary node and querying it from a secondary node:
     - **Reflection Question**:  
       - **What potential delays might occur when reading from a secondary node after writing to the primary?**  
         Consider how MongoDB handles replication lag and how it may affect read consistency. Would you expect immediate access to the inserted document from the secondary node?


### **Part 3: Combining Aggregation with Replication**

1. **Modify Aggregation Pipeline for Secondary Reads**:
   - Use the **readPreference** parameter to direct your aggregation queries to secondary nodes, which improves read scalability.
   - The JSON schema includes fields like `imdb.rating` instead of `imdbRating`, so ensure that the aggregation pipeline references the correct field names.

   ```groovy
   def secondaryClient = MongoClients.create("<YOUR-CONNECTION-STRING>?retryWrites=true&w=majority&readPreference=secondary")
   def db = secondaryClient.getDatabase(properties.DB)

   def pipeline = [
       match(gte("year", 2000)),
       project(new Document("genres", 1).append("imdbRating", "$imdb.rating")), // Project genres and imdb.rating
       group('\$genres', avg('avgRating', '\$imdbRating')), // Group by genres and calculate avgRating
       sort(Sorts.descending('avgRating')), // Sort by avgRating
       limit(3) // Limit to top 3 results
   ]
   
   def result = db.getCollection('movies').aggregate(pipeline).into([])
   result.each { println it }
   ```

   **Reflection Question**:  
   - **Why is it beneficial to direct read-heavy operations, like aggregation queries, to secondary nodes?**  
     Consider how distributing read operations might improve overall database performance and scalability, especially for high-traffic applications.

2. **Simulate Failover**:
   - In MongoDB Atlas, temporarily disable access to the **primary node** by pausing or restricting connections to the primary.
   - MongoDB Atlas will automatically promote one of the **secondary nodes** to primary, ensuring **high availability**.
   - Verify that your aggregation pipeline continues to run without interruptions when reading from the newly promoted primary or other secondary nodes.
   - Rerun the above aggregation query and confirm that it continues to fetch the results correctly.

   **Exercise**:
   - **Reflection Question**:  
     - **What happens when a secondary node is promoted to primary, and how does MongoDB ensure a seamless failover?**  
       Reflect on how MongoDB's automatic failover mechanism works. Would there be any interruption during a write operation if a primary node fails? How does MongoDB handle this?


### Optional Questions for Deeper Understanding
- **Consistency vs. Availability**:
   - MongoDB uses eventual consistency for replicated data. **How would this affect applications that require strong consistency?**
   - When would it be better to use `readPreference=primary` instead of `readPreference=secondary` for queries? Consider cases where strong consistency is crucial.

- **Replication Lag**:
   - **How does replication lag affect real-time applications?**  
     In cases where you’re reading from a secondary node that’s slightly behind the primary due to replication lag, how would you handle data that may be temporarily inconsistent?

- **Sharding and Replication**:
   - MongoDB can also distribute data across multiple shards in addition to replication. **How would sharding affect replication, and what additional complexities might arise when scaling out both sharding and replication in a distributed environment?**











## Documentation

* Official API documentation [MongoDb API](https://mongodb.github.io/mongo-java-driver/)
* [Projections](https://www.mongodb.com/docs/manual/tutorial/project-fields-from-query-results/): Documentation of the driver’s support for building projections
* [Sorts](https://www.mongodb.com/docs/drivers/java/sync/v5.2/fundamentals/builders/sort/): Documentation of the driver’s support for building sort criteria
* [Aggregation](https://www.mongodb.com/docs/manual/aggregation/): Documentation of the driver’s support for building aggregation pipelines

***
&copy; Artur Boronat, 2015-24
