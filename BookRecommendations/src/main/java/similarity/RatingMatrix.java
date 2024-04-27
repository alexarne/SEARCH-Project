package similarity;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
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
    private Map<Integer,SortedMap<Integer,Double>> userToBookScore = new HashMap<>();
    private Map<Integer,Set<Integer>> bookToUsers = new HashMap<>();


    /**
     * Insert/update rating for user, book pair into the matrix.
     */
    public void put(int user_id, int book_id, double rating) {
        var bookScores = userToBookScore.get(user_id);
        if (bookScores == null) {
            bookScores = new TreeMap<>();
            userToBookScore.put(user_id, bookScores);
        }
        bookScores.put(book_id, rating);

        var bookToUserSet = bookToUsers.get(book_id);
        if (bookToUserSet == null) {
            bookToUserSet = new HashSet<>();
            bookToUsers.put(book_id, bookToUserSet);
        }
        bookToUserSet.add(user_id);
    } 

    /**
     * Get rating of user book pair.
     */
    public double getRating(int user_id, int book_id) {
        return userToBookScore.get(user_id).getOrDefault(book_id, 0.0);
    }

    /**
     * Return set of all user id's.
     */
    public Set<Integer> getUserIds() {
        return userToBookScore.keySet();
    }

    /**
     * Return set of all book id's.
     */
    public Set<Integer> getBookIds() {
        return bookToUsers.keySet();
    }

    /**
     * Get set of users who has rated a book.
     * Null if no one in matrix has rated book.
     */
    public Set<Integer> getUsersFromBook(int book_id) {
        return bookToUsers.get(book_id);
    }

    /**
     * Get set of books who a user has rated.
     */
    public Set<Integer> getBooksFromUser(int user_id) {
        return userToBookScore.get(user_id).keySet();
    }

    /**
     * Get ratings from user.
     */
    public Collection<Double> getRatingsFromUser(int user_id) {
        return userToBookScore.get(user_id).values();
    }

    /**
     * Get set of book/rating key/value pair from user.
     */
    public Set<Entry<Integer,Double>> getEntrySetFromUser(int user_id) {
        return userToBookScore.get(user_id).entrySet();
    }


    /**
     * Test class.
     */
    public static void main(String[] args) {
        var matrix = new RatingMatrix();

        /* Insert 3 users, each having rated 4 books out of 6 books, into the matrix. */
        matrix.put(0, 0, 3 - 3);
        matrix.put(0, 1, 5 - 3);
        matrix.put(0, 3, 4 - 3);
        matrix.put(0, 4, 1 - 3);

        matrix.put(1, 0, 4 - 3);
        matrix.put(1, 2, 1 - 3);
        matrix.put(1, 3, 2 - 3);
        matrix.put(1, 5, 4 - 3);

        matrix.put(2, 1, 3 - 3);
        matrix.put(2, 2, 2 - 3);
        matrix.put(2, 3, 5 - 3);
        matrix.put(2, 4, 2 - 3);

        var users = matrix.getUserIds();
        for (int user_id = 0; user_id < 3; ++user_id) {
            System.out.println(users.contains(user_id));
        }

        var books = matrix.getBookIds();
        for (int book_id = 0; book_id < 5; ++book_id) {
            System.out.println(books.contains(book_id));
        }

        var usersFromBook4 = matrix.getUsersFromBook(4);
        System.out.println(usersFromBook4.contains(0));
        System.out.println(!usersFromBook4.contains(1));
        System.out.println(usersFromBook4.contains(2));

        var booksFromUser0 = matrix.getBooksFromUser(0);
        System.out.println(booksFromUser0.contains(0));
        System.out.println(booksFromUser0.contains(1));
        System.out.println(!booksFromUser0.contains(2));
        System.out.println(booksFromUser0.contains(3));
        System.out.println(booksFromUser0.contains(4));
        System.out.println(!booksFromUser0.contains(5));
    }
}
