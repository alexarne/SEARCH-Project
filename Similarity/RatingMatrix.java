import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;


/**
 * Author: Erik Lidbjörk.
 * Date 2024.
 * 
 * Matrix where rows are represented by user id's 
 * and columns book id's. Entries are 
 * rating of a book by a user.
 * 
 * Internal representation is based on maps since the 
 * matrix will be very sparse in practice.
 */
public class RatingMatrix {
    /* Matrix is internally represented as mappings from each user id
     * to an order mapping (by book ids ascendingly) to the rating. */
    protected Map<Integer,SortedMap<Integer,Double>> userToBookScore = new HashMap<>();


    /**
     * Insert rating for user, book pair into the matrix.
     */
    public void insert(int user_id, int book_id, double rating) {
        var bookScores = userToBookScore.get(user_id);
        if (bookScores == null) {
            bookScores = new TreeMap<>();
            userToBookScore.put(user_id, bookScores);
        }
        bookScores.put(book_id, rating);
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
     * Return set of all user id's.
     */
    public Set<Integer> getUserIds() {
        return userToBookScore.keySet();
    }
}
