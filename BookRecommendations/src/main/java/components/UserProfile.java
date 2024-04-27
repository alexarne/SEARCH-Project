package components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import similarity.CosineSimilarity;
import similarity.RatingMatrix;

public class UserProfile {

    private final Map<Integer, Integer> ratings = new HashMap<>();
    private final int user_id;

    public UserProfile() {
        user_id = 0; // Default id as 0 since goodreads start counting at id = 1.
    }

    public UserProfile(int user_id) {
        this.user_id = user_id;
    }

    public Map<Integer, Integer> getRatings() {
        return ratings;
    }

    public int getRating(Book book) {
        return getRating(book.getId());
    }

    public int getRating(int bookId) {
        return ratings.getOrDefault(bookId, 0);
    }

    public void removeRating(int bookId) {
        ratings.remove(bookId);
    }

    public void removeRating(Book book) {
       removeRating(book.getId());
    }

    public void setRating(int bookId, int rating) {
        ratings.put(bookId,rating);
    }

    public void setRating(Book book, int rating) {
        setRating(book.getId(),rating);
    }

    public void resetRatings() {
        ratings.clear();
    }

    public int getId() {
        return user_id;
    }

    public void loadRatings(RatingMatrix ratingMatrix) {
        for (Map.Entry<Integer, Double> entry : ratingMatrix.getEntrySetFromUser(user_id)) {
            setRating(entry.getKey(), (int)Math.round(entry.getValue()));
        }
    }

    public List<UserProfile> getSimilarUsers() {
        List<UserProfile> test = new ArrayList<>();
        UserProfile user1 = new UserProfile();
        user1.setRating(916,2);
        user1.setRating(780,-2);
        test.add(user1);
        UserProfile user2 = new UserProfile();
        user2.setRating(916,2);
        user2.setRating(780,-2);
        test.add(user2);
        return test;
    }

    /**
     * Test method.
     * Return a List of length numUsers containing UserProfiles 
     * having randomly rated each book in books between 1-5.
     */
    public static List<UserProfile> getSimilarUsers(List<Book> books, int numUsers) {
        List<UserProfile> similarUsers = new ArrayList<>();
        Random rng = new Random();

        /* Let numUsers rate each book randomly. */
        for (int i = 0; i < numUsers; ++i) {
            var u = new UserProfile(i + 1); // Set ids from 1 to numUsers.
            for (var book : books) {
                int rating = rng.nextInt(5) + 1;
                u.setRating(book, rating);
            }
            similarUsers.add(u);
        }
        return similarUsers;
    }

    /** 
     * Test method.
     * Returns similarity scores between user and each similarUser.
     */
    public List<Double> getSimilarityScores(List<UserProfile> similarUsers) {
        var m = new RatingMatrix();

        /* Calculate similarities. */
        for (var entry : this.getRatings().entrySet()) {
            m.put(user_id, entry.getKey(), entry.getValue());
        }
        List<Double> simScores = new ArrayList<>();
        for (var similarUser : similarUsers) {
            for (var entry : similarUser.getRatings().entrySet()) {
                m.put(similarUser.user_id, entry.getKey(), entry.getValue());
            }
        }

        var sim = new CosineSimilarity(m);
        for (var similarUser : similarUsers) {
            simScores.add(sim.sim(user_id,similarUser.user_id));
        }

        return simScores;
    }
}
