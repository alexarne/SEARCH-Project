package searcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLContext;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchPhraseQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
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

    /*private Map<Integer, Double> getBoostedScores(List<Hit<Book>> hits, UserProfile user) {
        Map<Integer, Double> boosts = new HashMap<>();
        List<UserProfile> similarUsers = user.getSimilarUsers();*/

        /* Test user similarity. */
        /*if (!user.getRatings().isEmpty()) {
            List<Book> books = hits.stream().map(hit -> hit.source()).toList();
            similarUsers = UserProfile.getSimilarUsers(books, 10); // Compare ratings to 10 randomly generated users.
            List<Double> simScores = user.getSimilarityScores(similarUsers);

            for (Hit<Book> hit : hits) {
                int bookId = hit.source().getId();
                boosts.put(bookId, hit.score());

                for (int i = 0; i < similarUsers.size(); ++i) {
                    boosts.put(bookId, boosts.get(bookId) + 1e2*simScores.get(i)*similarUsers.get(i).getRating(bookId));
                }
            }
            return boosts;
        }

        for (Hit<Book> hit : hits) {
            int bookId = hit.source().getId();
            boosts.put(bookId, hit.score());
            for (UserProfile sim : similarUsers) {
                boosts.put(bookId, boosts.get(bookId) + sim.getRating(bookId));
            }
        }
        return boosts;
    }*/

    /*public List<Book> searchBooks(String queryTerms, QueryType queryType, UserProfile user) throws IOException {
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
        SearchResponse<Book> response = esClient.search(s -> s
                        .index(indexName)
                        .query(q -> q
                                .bool(b -> b
                                        .should(byTitle)
                                        .should(byAbstract)
                                        .should(byTitlePhrase)
                                        .should(byAbstractPhrase)
                                ))
                        .size(SEARCH_LIMIT),
                Book.class);
        List<Hit<Book>> hits = response.hits().hits();
        List<Book> results = new ArrayList<>();
        for (Hit<Book> hit : hits) {
            results.add(hit.source());
        }
        if (queryType == QueryType.USER_QUERY) {
            Map<Integer, Double> boostedScores = getBoostedScores(hits, user);
            results.sort(new Comparator<Book>() {
                @Override
                public int compare(Book b1, Book b2) {
                    return (int)Math.signum(boostedScores.get(b2.getId()) - boostedScores.get(b1.getId()));
                }
            });
        }
        return results;
    }*/

    public List<Book> searchBooks(String queryTerms, QueryType queryType, DisplayType displayType, UserProfile user, RatingMatrix ratingMatrix, Similarity similarity) throws IOException {
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
        SearchResponse<Book> response = esClient.search(s -> s
                        .index(indexName)
                        .query(q -> q
                                .bool(b -> b
                                        .should(byTitle)
                                        .should(byAbstract)
                                        .should(byTitlePhrase)
                                        .should(byAbstractPhrase)
                                ))
                        .size(SEARCH_LIMIT),
                Book.class);
        List<Hit<Book>> hits = response.hits().hits();
        List<Book> results = new ArrayList<>();
        for (Hit<Book> hit : hits) {
            if (displayType == DisplayType.SHOW_READ_BOOKS || (user.getRating(hit.source().getId()) == 0))  results.add(hit.source());
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
            //List<UserProfile> similarUsers = similarUserIds.stream().map(book_id -> new UserProfile(book_id)).toList();
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
            int bookId = hit.source().getId();
            boosts.put(bookId, hit.score());

            for (int i = 0; i < similarUsers.size(); ++i) {
                boosts.put(bookId, boosts.get(bookId) + boostFunction(simScores.get(i), ratingMatrix.getRating(similarUsers.get(i), bookId)));
            }
        }
        return boosts;
    }

    private double boostFunction(double simScore, double rating) {
        double boost = 1e6*simScore*(rating-3);
        return boost;
    }
}
