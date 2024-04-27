package components;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Book {

    private int id;
    private String title;
    private String abstr;
    private String author;
    private double rating;
    private int numRatings;
    private int numReviews;

    private String[] genres;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Book(@JsonProperty("id") int id, @JsonProperty("title") String title, @JsonProperty("abstr") String abstr, @JsonProperty("author") String author, @JsonProperty("rating") double rating, @JsonProperty("numRatings") int numRatings, @JsonProperty("numReviews") int numReviews, @JsonProperty("genres") String[] genres) {
        this.id = id;
        this.title = title;
        this.abstr = abstr;
        this.author = author;
        this.rating = rating;
        this.numRatings = numRatings;
        this.numReviews = numReviews;
        this.genres = genres;
    }

    public int getId() {
        return id;
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

    @Override
    public String toString() {
        return this.title + " by " + this.author + " (" + rating + " stars)";
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Book other) {
            return (other.getTitle().equals(this.getTitle()) &&
                    other.getAuthor().equals(this.getAuthor()));
        }
        return false;
    }
}
