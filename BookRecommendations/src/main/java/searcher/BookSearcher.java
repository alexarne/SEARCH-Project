package searcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.SSLContext;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchPhraseQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.TransportUtils;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import book.Book;

public class BookSearcher {

    ElasticsearchClient esClient;
    String indexName;

    public BookSearcher(String host, int port, String fingerprint, String password, String indexName) {
        this.esClient = getClient(host, port, fingerprint, password);
    }

    private ElasticsearchClient getClient(String host, int port, String fingerprint, String password) {
        SSLContext sslContext = TransportUtils.sslContextFromCaFingerprint(fingerprint);
        BasicCredentialsProvider credsProv = new BasicCredentialsProvider();
        credsProv.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("elastic", password));
        RestClient restClient = RestClient
                .builder(new HttpHost(host, port, "https"))
                .setHttpClientConfigCallback(hc -> hc
                        .setSSLContext(sslContext)
                        .setDefaultCredentialsProvider(credsProv)
                )
                .build();
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }

    public List<Book> searchBooks(String queryTerms) throws IOException {
        Query byTitle = MatchQuery.of(m -> m
                .field("title")
                .query(queryTerms)
        )._toQuery();
        Query byAbstract = MatchQuery.of(m -> m
                .field("abstr")
                .query(queryTerms)
        )._toQuery();
        Query byTitlePhrase = MatchPhraseQuery.of(m -> m
                .field("title")
                .query(queryTerms)
        )._toQuery();
        Query byAbstractPhrase = MatchPhraseQuery.of(m -> m
                .field("abstr")
                .query(queryTerms)
        )._toQuery();
        Query byRating = RangeQuery.of(r -> r
                .field("rating")
                .gte(JsonData.of(4))
        )._toQuery();
        SearchResponse<Book> response = esClient.search(s -> s
                        .index("books")
                        .query(q -> q
                                .bool(b -> b
                                        .should(byTitle)
                                        .must(byAbstract)
                                        .should(byTitlePhrase)
                                        .should(byAbstractPhrase)
                                        .should(byRating)
                                ))
                        .size(20),
                Book.class);
        List<Book> results = new ArrayList<>();
        for (Hit<Book> hit : response.hits().hits()) {
            results.add(hit.source());
        }
        return results;
    }
}
