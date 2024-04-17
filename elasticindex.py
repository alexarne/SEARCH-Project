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
  basic_auth=("elastic", ELASTIC_PASSWORD)
)

# Create index
client.indices.create(index="books")