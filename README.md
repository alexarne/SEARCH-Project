# Book Recommendation Search Engine

## About the project

## Getting started

### Requirements
- JDK 21
- Maven 2
- Python 3
- [ElasticSearch](https://github.com/elastic/elasticsearch)
- [Goodreads](https://www.goodreads.com) account

### Installation

1. Clone the repo and enter the project folder
```sh
   git clone git@github.com:alexarne/SEARCH-Project.git
   cd BookRecommendations
   ```

2. Configure environment variables

Create a `.env` file with the lines
```shell
ES_FINGERPRINT=
ES_PASSWORD=
ES_INDEX=
COOKIES_UBID_MAIN=
COOKIES_AT_MAIN=
```
`ES_FINGERPRINT` and `ES_PASSWORD` should correspond to the HTTP CA certificate SHA-256 fingerprint and elastic user password respectively from the output of the ElasticSearch set-up. `ES_INDEX` should be the name of the ElasticSearch index to store the Goodreads data. `COOKIES_UBID_MAIN` and `COOKIES_AT_MAIN` should correspond to the Goodreads account cookies.  
3. Run ElasticSearch on `localhost` port `9200`
5. Scrape the Goodreads data

Use `pip` or `conda` to install the requirements `src/main/python/requirements.txt`.

Run the scraper.
```
python3 src/main/python/indexer.py
```

5. Run the search engine

```mvn clean compile exec:java```

### Usage
...

## Contributors