import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.DoubleFunction;


/**
 * Matrix where rows are represented by user id's 
 * and columns book id's. Entries are (scale and shifted) 
 * rating of a book by a user.
 * 
 * Internal representation is based on maps since the 
 * matrix will be very sparse in practice.
 */
public class RatingMatrix {
    /* Matrix is internally represented as mappings from each user id
     * to an order mapping (by book ids ascendingly) to the rating. */
    private Map<Integer,SortedMap<Integer,Double>> userToBookScore = new HashMap<>();

    /**
     * Dot product between user A and B. 
     */
    private double dot(int user_id_A, int user_id_B) {
        var bookScoresA = userToBookScore.get(user_id_A);
        var bookScoresB = userToBookScore.get(user_id_B);
        
        /* Iterate over both users.*/
        var iterA = bookScoresA.entrySet().iterator();
        var iterB = bookScoresB.entrySet().iterator();

        double dotProduct = 0d;
        while (iterA.hasNext() && iterB.hasNext()) {
            var entryA = iterA.next();
            var entryB = iterB.next();

            if (entryA.getKey() == entryB.getKey()) {
                dotProduct += entryA.getValue() * entryB.getValue();
            }
        }        
        return dotProduct;
    }

    /**
     * Get length of vector with specified metric.
     */
    private double length(int user_id, DoubleFunction<Double> metric) {
        var bookScores = userToBookScore.get(user_id);
        double len = 0d; 
        for (double rating : bookScores.values()) {
            len += metric.apply(rating);
        }
        return len;
    }


    /**
     * Insert user, book pair into the matrix with scaleFactor = 1 and 
     * shiftTerm = -3.
     */
    public void insert(int user_id, int book_id, int rating) {
        insert(user_id, book_id, rating, 1d, -3d);
    }

    /**
     * Insert rating for user, book pair into the matrix with 
     * rating modified by: scaleFactor*rating + shiftTerm.
     */
    public void insert(int user_id, int book_id, int rating, double scaleFactor, double shiftTerm) {
        var bookScores = userToBookScore.get(user_id);
        if (bookScores == null) {
            bookScores = new TreeMap<>();
            userToBookScore.put(user_id, bookScores);
        }
        bookScores.put(book_id, scaleFactor*rating + shiftTerm);
    } 

    /**
     * Get rating of user, book pair or null if does not exist.
     */
    public Double getRating(int user_id, int book_id) {
        var bookScores = userToBookScore.get(user_id);
        if (bookScores == null) {
            return null;
        }
        return bookScores.get(user_id);
    }

    /**
     * Get cosine similarity between user A and B with specified metric.
     * Return positive/negative infinity if metric for user A or B is 0.
     */
    public double sim(int user_id_A, int user_id_B, DoubleFunction<Double> metric) {
        double dotProduct = dot(user_id_A, user_id_B);
        if (dotProduct == 0d) {
            return 0d;
        }

        double lenA = length(user_id_A, metric);
        if (lenA == 0d) {
            return Math.signum(dotProduct) * Double.POSITIVE_INFINITY;
        }

        double lenB = length(user_id_B, metric);
        if (lenB == 0d) {
            return Math.signum(dotProduct) * Double.POSITIVE_INFINITY;
        }

        return dotProduct / (lenA * lenB);
    }    

    /**
     * Default similarity is using euclidean metric.
     */
    public Double sim(int user_id_A, int user_id_B) {
        return simEuclidean(user_id_A, user_id_B);
    }

    /**
     * Get cosine similarity between user A and B with euclidean metric.
     */
    public double simEuclidean(int user_id_A, int user_id_B) {
        return sim(user_id_A, user_id_B, rating -> Math.pow(rating, 2));
    }

    /**
     * Get cosine similarity between user A and B with manhattan metric.
     */
    public double simManhattan(int user_id_A, int user_id_B) {
        return sim(user_id_A, user_id_B, rating -> Math.abs(rating));
    }

    /**
     * Return set of all user id's.
     */
    public Set<Integer> getUserIds() {
        return userToBookScore.keySet();
    }


    /**
     * Test class with mock data.
     */
    public static void main(String[] args) {
        var matrix = new RatingMatrix();

        /* Insert 3 users, each having rated 4 books out of 6 books, into the matrix. */
        matrix.insert(0, 0, 3);
        matrix.insert(0, 1, 5);
        matrix.insert(0, 3, 4);
        matrix.insert(0, 4, 1);

        matrix.insert(1, 0, 4);
        matrix.insert(1, 2, 1);
        matrix.insert(1, 3, 2);
        matrix.insert(1, 5, 4);

        matrix.insert(2, 1, 3);
        matrix.insert(2, 2, 2);
        matrix.insert(2, 3, 5);
        matrix.insert(2, 4, 2);

        /* Sim should be equal to expectedSim. */
        var sim01 = matrix.sim(0,1);
        var sim02 = matrix.sim(0,2);
        var sim12 = matrix.sim(1,2);

        double len0 = 0*0 + 2*2 + 1*1 + (-2)*(-2);
        double len1 = 1*1 + (-2)*(-2) + (-1)*(-1) + 1*1;
        double len2 = 0*0 + (-1)*(-1) + 2*2 + (-1)*(-1);

        double dot01 = 0*1 + 1*(-1);            // Three books in common.
        double dot02 = 2*0 + 1*2 + (-2)*(-1);   // Three books in common.
        double dot12 = (-2)*(-1) + (-1)*2;      // Two books in common.

        double expectedSim01 = dot01 / (len0 * len1);
        double expectedSim02 = dot02 / (len0 * len2);
        double expectedSim12 = dot12 / (len1 * len2);

        System.out.println(sim01 == expectedSim01);
        System.out.println(sim02 == expectedSim02);
        System.out.println(sim12 == expectedSim12);
    }
}
