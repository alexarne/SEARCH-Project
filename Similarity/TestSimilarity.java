import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

interface Entry<S> {
    int user_id();
    int book_id();
    S score();
}

class SimilarityMatrix<S> {
    class InternalEntry {
        int book_id;
        S score;
        InternalEntry(int book_id, S score) {
            this.book_id = book_id;
            this.score = score;
        }
    }

    Map<Integer,List<Integer>> userToBooks = new HashMap<>();
    Map<Integer,S> bookToScores = new HashMap<>();

    public void insertEntry(Entry<S> e) {

    } 
}

/*
class Entry {
    int book_id;
    int user_id;
    int rating;
    Entry(int book_id, int user_id, int rating) {
        this.book_id = book_id;
        this.user_id = user_id;
        this.user_id = rating;
    }
} */

interface Similarity<S> {
    public S sim(int A, int B);
    public void scaleRating(int rating);
}

public class TestSimilarity {
    public static void main(String[] args) {

        /* Test data. */
        List<Entry> data = new ArrayList<>();

        /* 4 books, 10 users. */
        data.add(new Entry(1,1,1)); 
        data.add(new Entry(1,4,3)); 
        data.add(new Entry(1,7,2)); 
        data.add(new Entry(1,8,5)); 

        data.add(new Entry(2,2,1)); 
        data.add(new Entry(2,3,1)); 
        data.add(new Entry(2,5,3)); 
        data.add(new Entry(2,7,5)); 
        data.add(new Entry(2,8,2)); 
        data.add(new Entry(2,9,4)); 

        data.add(new Entry(3,1,4)); 
        data.add(new Entry(3,3,5)); 
        data.add(new Entry(3,4,2)); 
        data.add(new Entry(3,6,1)); 
        data.add(new Entry(3,10,2)); 

        data.add(new Entry(4,2,2)); 
        data.add(new Entry(4,5,2)); 
        data.add(new Entry(4,6,2)); 
        data.add(new Entry(4,9,2)); 
        data.add(new Entry(4,10,2)); 
    }
}