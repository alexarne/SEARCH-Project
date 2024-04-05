from elasticsearch import Elasticsearch

# Read authentication details from file
# Expected format: {HTTP CA certificate SHA-256 fingerprint}\n{Password for the elastic user}\n{API Key encoded}
with open("elastic_auth.txt") as f:
    CERT_FINGERPRINT = f.readline().strip()
    ELASTIC_PASSWORD= f.readline().strip()
    API_KEY = f.readline().strip()

# Elasticsearch client connection
# Authorization either by API key or username/password
client = Elasticsearch(
  "https://localhost:9200",
  ssl_assert_fingerprint = CERT_FINGERPRINT,
  #api_key=API_KEY
  basic_auth=("elastic", ELASTIC_PASSWORD)
)

# Test connection
# print(client.info())

# Create index
# client.indices.create(index="goodreads_books")

# Index a sample document
client.index(index="goodreads_books",
             id=1,
             document={"title": "Pride and Prejudice",
                       "abstract": "Since its immediate success in 1813, Pride and Prejudice has remained one of the most popular novels in the English language. Jane Austen called this brilliant work 'her own darling child' and its vivacious heroine, Elizabeth Bennet, 'as delightful a creature as ever appeared in print.' The romantic clash between the opinionated Elizabeth and her proud beau, Mr. Darcy, is a splendid performance of civilized sparring. And Jane Austen's radiant wit sparkles as her characters dance a delicate quadrille of flirtation and intrigue, making this book the most superb comedy of manners of Regency England.",
                       "author": "Jane Austen",
                       "rating": 4.29,
                       "numratings": 4263751,
                       "numreviews": 114560,
                       })

# Search for sample document
results = client.search(index="goodreads_books",
              query={
                  "match": {
                      "title": "Pride"
                  }
              })
print(results)