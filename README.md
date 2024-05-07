# Book Recommendation Search Engine

## About the project
This is a book recommendation search engine based on book and user data scraped from Goodreads. It uses ElasticSearch to index and search for books by keywords. It also supports user profiles and augmenting search results based on the preferences of similar users.

### Requirements
- JDK 21
- Maven 2
- Python 3
- [ElasticSearch](https://github.com/elastic/elasticsearch)
- [Goodreads](https://www.goodreads.com) account

### Installation

1. Clone the repo and enter the project folder.
```sh
   git clone git@github.com:alexarne/SEARCH-Project.git
   cd BookRecommendations
   ```

2. Configure environment variables.

Create a `.env` file with the following lines.
```shell
ES_FINGERPRINT=
ES_PASSWORD=
ES_INDEX=
COOKIES_UBID_MAIN=
COOKIES_AT_MAIN=
```
`ES_FINGERPRINT` and `ES_PASSWORD` should correspond to the HTTP CA certificate SHA-256 fingerprint and elastic user password respectively from the output of the ElasticSearch set-up. `ES_INDEX` should be the name of the ElasticSearch index to store the Goodreads data. `COOKIES_UBID_MAIN` and `COOKIES_AT_MAIN` should correspond to the Goodreads account cookies "ubid-main" and "at-main", respectively (these should be extracted from a current login-session in an actual browser).  

3. Run ElasticSearch on `localhost` port `9200`.

4. Scrape the Goodreads data.

Use `pip` or `conda` to install the requirements `src/main/python/requirements.txt`.

Run the scraper.
```
python3 src/main/python/indexer.py
```

5. Run the search engine.

```
mvn clean compile exec:java
```

### Usage

To run a keyword search, write query terms in the search bar and click enter. Click on a result to display its abstract.

To get a customized user search, use the stars in the search results to enter ratings. Alternatively, select one of the test profiles under the _Users_ menu.

To toggle between customized search and neutral keyword search, use the _Search options_ menu.

To display/hide books already read by the user, use the _Display my books?_ menu.
