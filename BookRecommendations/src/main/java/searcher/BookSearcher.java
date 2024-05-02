/**
 * ElasticSearch-based searcher class for the engine
 */

package searcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.net.ssl.SSLContext;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchPhraseQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.TransportUtils;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import components.DisplayType;
import components.QueryType;
import components.UserProfile;
import similarity.RatingMatrix;
import similarity.Similarity;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import components.Book;

public class BookSearcher {

    final int SEARCH_LIMIT = 10000;

    ElasticsearchClient esClient;
    String indexName;


    public BookSearcher(String host, int port, String fingerprint, String password, String indexName) {
        this.esClient = getClient(host, port, fingerprint, password);
        this.indexName = indexName;
    }

    /**
     * Connect to the elasticsearch client
     */
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

    /**
     * Sends given query to elasticsearch client and processes results
     */
    public List<Book> searchBooks(String queryTerms, QueryType queryType, DisplayType displayType, UserProfile user, RatingMatrix ratingMatrix, Similarity similarity) throws IOException {
        Query byTitle = MatchQuery.of(m -> m
                .field("title")
                .query(queryTerms)
                .fuzziness("2")
                .boost(1.0F)
        )._toQuery();
        Query byTitleAnd = MatchQuery.of(m -> m
                .field("title")
                .query(queryTerms)
                .fuzziness("2")
                .boost(1.0F)
                .operator(Operator.And)
        )._toQuery();
        Query byTitlePhrase = MatchPhraseQuery.of(m -> m
                .field("title")
                .query(queryTerms)
                .boost(1.0F)
        )._toQuery();
        Query byAbstract = MatchQuery.of(m -> m
                .field("abstr")
                .query(queryTerms)
                .fuzziness("2")
                .boost(1.0F)
        )._toQuery();
        Query byAbstractAnd = MatchQuery.of(m -> m
                .field("abstr")
                .query(queryTerms)
                .fuzziness("2")
                .boost(1.0F)
                .operator(Operator.And)
        )._toQuery();
        Query byAbstractPhrase = MatchPhraseQuery.of(m -> m
                .field("abstr")
                .query(queryTerms)
                .boost(1.0F)
        )._toQuery();
        Query byAuthor = MatchQuery.of(m -> m
                .field("author")
                .query(queryTerms)
                .fuzziness("2")
                .boost(1.0F)
        )._toQuery();
        Query byAuthorAnd = MatchQuery.of(m -> m
                .field("author")
                .query(queryTerms)
                .fuzziness("2")
                .boost(1.0F)
                .operator(Operator.And)
        )._toQuery();
        Query byAuthorPhrase = MatchPhraseQuery.of(m -> m
                .field("author")
                .query(queryTerms)
                .boost(1.0F)
        )._toQuery();
        Query bySeries = MatchQuery.of(m -> m
                .field("series")
                .query(queryTerms)
                .fuzziness("2")
                .boost(1.0F)
        )._toQuery();
        Query bySeriesAnd = MatchQuery.of(m -> m
                .field("series")
                .query(queryTerms)
                .fuzziness("2")
                .boost(1.0F)
                .operator(Operator.And)
        )._toQuery();
        Query bySeriesPhrase = MatchPhraseQuery.of(m -> m
                .field("series")
                .query(queryTerms)
                .boost(1.0F)
        )._toQuery();
        Query byGenre = MatchQuery.of(m -> m
                .field("genres")
                .query(queryTerms)
                .fuzziness("2")
                .boost(1.0F)
        )._toQuery();
        Query byGenreAnd = MatchQuery.of(m -> m
                .field("genres")
                .query(queryTerms)
                .fuzziness("2")
                .boost(1.0F)
                .operator(Operator.And)
        )._toQuery();
        SearchResponse<Book> response = esClient.search(s -> s
                        .index(indexName)
                        .query(q -> q
                                .bool(b -> b
                                        .should(byTitle)
                                        .should(byTitleAnd)
                                        .should(byTitlePhrase)
                                        .should(byAbstract)
                                        .should(byAbstractAnd)
                                        .should(byAbstractPhrase)
                                        .should(byAuthor)
                                        .should(byAuthorAnd)
                                        .should(byAuthorPhrase)
                                        .should(bySeries)
                                        .should(bySeriesAnd)
                                        .should(bySeriesPhrase)
                                        .should(byGenre)
                                        .should(byGenreAnd)
                                ))
                        .size(SEARCH_LIMIT),
                Book.class);
        List<Hit<Book>> hits = response.hits().hits();
        List<Book> results = new ArrayList<>();
        for (Hit<Book> hit : hits) {
            if (displayType == DisplayType.SHOW_READ_BOOKS || (user.getRating(Objects.requireNonNull(hit.source()).getId()) == 0))  results.add(hit.source());
        }
        if (queryType == QueryType.USER_QUERY && !user.getRatings().isEmpty()) {
            Set<Integer> similarUserIds = new HashSet<>();
            for (var book : results) {
                int book_id = book.getId();
                var users = ratingMatrix.getUsersFromBook(book_id);
                if (users != null) {
                    similarUserIds.addAll(users); // Expand set.
                }
            }
            similarUserIds.remove(user.getId()); // Remove current user from similar users.
            List<Integer> similarUsers = new ArrayList<>(similarUserIds);

            Map<Integer, Double> boostedScores = getBoostedScores(hits, user, similarUsers, ratingMatrix, similarity);
            results.sort(new Comparator<Book>() {
                @Override
                public int compare(Book b1, Book b2) {
                    return (int)Math.signum(boostedScores.get(b2.getId()) - boostedScores.get(b1.getId()));
                }
            });
        }
        return results;
    }

    private Map<Integer, Double> getBoostedScores(List<Hit<Book>> hits, UserProfile user, List<Integer> similarUsers, RatingMatrix ratingMatrix, Similarity similarity) {
        Map<Integer, Double> boosts = new HashMap<>();

        List<Double> simScores = new ArrayList<>();
        for (var similarUser : similarUsers) {
            simScores.add(similarity.sim(user.getId(), similarUser));
        }

        for (Hit<Book> hit : hits) {
            if (hit.source() != null) {
                int bookId = hit.source().getId();
                boosts.put(bookId, hit.score());
                for (int i = 0; i < similarUsers.size(); ++i) {
                    boosts.put(bookId, boosts.get(bookId) + boostFunction(simScores.get(i), ratingMatrix.getRating(similarUsers.get(i), bookId)));
                }
            }
        }
        return boosts;
    }

    /**
     * Boosting function for given user similarity score and book rating
     */
    private double boostFunction(double simScore, double rating) {
        return 1e6*simScore*(rating-3);
    }
}
