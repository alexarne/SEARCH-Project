package components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import similarity.CosineSimilarity;
import similarity.RatingMatrix;

public class UserProfile {

    private final Map<Integer, Integer> ratings = new HashMap<>();
    private final int user_id;

    public UserProfile() {
        user_id = 0;
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

    /**
     * Load ratings from rating matrix into user profile (by user id)
     */
    public void loadRatings(RatingMatrix ratingMatrix) {
        for (Map.Entry<Integer, Double> entry : ratingMatrix.getEntrySetFromUser(user_id)) {
            setRating(entry.getKey(), (int)Math.round(entry.getValue()));
        }
    }

    /** 
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
