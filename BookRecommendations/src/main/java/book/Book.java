package book;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Book {

    String title;
    String abstr;
    String author;
    double rating;
    int numRatings;
    int numReviews;

    int myRating;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Book(@JsonProperty("title") String title, @JsonProperty("abstr") String abstr, @JsonProperty("author") String author, @JsonProperty("rating") double rating, @JsonProperty("numRatings") int numRatings, @JsonProperty("numReviews") int numReviews) {
        this.title = title;
        this.abstr = abstr;
        this.author = author;
        this.rating = rating;
        this.numRatings = numRatings;
        this.numReviews = numReviews;
        this.myRating = -1;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getAbstr() {
        return abstr;
    }

    public double getRating() {
        return rating;
    }

    public int getMyScore() {
        return myRating;
    }

    public void setMyScore(int myScore) {
        this.myRating = myScore;
    }

    @Override
    public String toString() {
        return this.title + " by " + this.author + " (" + rating + " stars)";
    }
}
