package components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserProfile {

    private final Map<Integer, Integer> ratings = new HashMap<>();

    public UserProfile() {
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
}
