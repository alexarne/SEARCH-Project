import java.util.HashMap;
import java.util.Map;
import java.util.function.DoubleFunction;


/**
 * Matrix where rows and columns are represented by user id's.
 * Entries are similarity scores between each user computed
 * from a RatingMatrix object.
 */
public class SimilarityMatrix {
    /* Store similarity scores. */
    private Map<Integer,Map<Integer,Double>> userToUserSimilarity = new HashMap<>();

    /** 
     * Precompute similarities with specified metric and store in RAM. 
     */
    SimilarityMatrix(RatingMatrix ratingMatrix, DoubleFunction<Double> metric) {
        /* Iterate over every user in rating matrix. */
        for (var user_id_A : ratingMatrix.getUserIds()) {
            userToUserSimilarity.put(user_id_A, new HashMap<>());
            for (var user_id_B : ratingMatrix.getUserIds()) {
                /* Check if similarity has been computed already. */
                double sim;
                if (userToUserSimilarity.get(user_id_B) == null || user_id_A == user_id_B) {
                    sim = ratingMatrix.sim(user_id_A, user_id_B, metric);
                } else {
                    sim = userToUserSimilarity.get(user_id_B).get(user_id_A);
                }
                userToUserSimilarity.get(user_id_A).put(user_id_B, sim);
            }
        }
    }

    /**
     * Default behaviour is using euclidean metric.
     */
    SimilarityMatrix(RatingMatrix ratingMatrix) {
        this(ratingMatrix, rating -> Math.pow(rating, 2));
    }

    /**
     * Get similarity of user, user pair or null if does not exist.
     */
    public Double sim(int user_id_A, int user_id_B) {
        var similarities = userToUserSimilarity.get(user_id_A);
        if (similarities == null) {
            return null;
        }
        return similarities.get(user_id_B);
    }


    /**
     * Test that similaritites between RatingMatrix and 
     * SimilarityMatrix are consistent.
     */
    public static void main(String[] args) {
        var ratingMatrix = new RatingMatrix();

        /* Insert 3 users, each having rated 4 books out of 6 books, into the matrix. */
        ratingMatrix.insert(0, 0, 3);
        ratingMatrix.insert(0, 1, 5);
        ratingMatrix.insert(0, 3, 4);
        ratingMatrix.insert(0, 4, 1);

        ratingMatrix.insert(1, 0, 4);
        ratingMatrix.insert(1, 2, 1);
        ratingMatrix.insert(1, 3, 2);
        ratingMatrix.insert(1, 5, 4);

        ratingMatrix.insert(2, 1, 3);
        ratingMatrix.insert(2, 2, 2);
        ratingMatrix.insert(2, 3, 5);
        ratingMatrix.insert(2, 4, 2);

        var similarityMatrix = new SimilarityMatrix(ratingMatrix);

        /* Sim should be equal to expectedSim. */
        var ratingSim00 = ratingMatrix.sim(0,0);
        var ratingSim01 = ratingMatrix.sim(0,1);
        var ratingSim02 = ratingMatrix.sim(0,2);
        var ratingSim11 = ratingMatrix.sim(1,1);
        var ratingSim12 = ratingMatrix.sim(1,2);
        var ratingSim22 = ratingMatrix.sim(2,2);

        var similaritySim00 = similarityMatrix.sim(0,0);
        var similaritySim01 = similarityMatrix.sim(0,1);
        var similaritySim02 = similarityMatrix.sim(0,2);
        var similaritySim11 = similarityMatrix.sim(1,1);
        var similaritySim12 = similarityMatrix.sim(1,2);
        var similaritySim22 = similarityMatrix.sim(2,2);

        System.out.println(ratingSim00 == (double) similaritySim00);
        System.out.println(ratingSim01 == (double) similaritySim01);
        System.out.println(ratingSim02 == (double) similaritySim02);
        System.out.println(ratingSim11 == (double) similaritySim11);
        System.out.println(ratingSim12 == (double) similaritySim12);
        System.out.println(ratingSim22 == (double) similaritySim22);
    }
}
