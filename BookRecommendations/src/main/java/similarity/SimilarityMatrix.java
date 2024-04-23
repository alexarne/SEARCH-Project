package similarity;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Author: Erik Lidbjörk.
 * Date 2024.
 * 
 * Matrix where rows and columns are represented by user id's.
 * Entries are similarity scores between each user from a 
 * Set of users computed from a Similarity.
 */
public class SimilarityMatrix implements Similarity {
    /* Store similarity scores. */
    private Map<Integer,Map<Integer,Double>> userToUserSimilarity = new HashMap<>();
    private final boolean similarityIsSymmetric;

    /**
     * Create empty SimilarityMatrix.
     * Insert similarities with insert.
     * Similarity is assumed to be assymetric.
     */
    public SimilarityMatrix() {
        this.similarityIsSymmetric = false;
    }

    /** 
     * Precompute similarities between all users in user_ids and store in RAM. 
     * If similarity scores are symmetric, that is sim(A,B) = sim(B,A) for all A and B, 
     * only compute and store one of the computations above.
     */
    public SimilarityMatrix(Similarity similarity, Set<Integer> user_ids, boolean similarityIsSymmetric) {
        /* Iterate over every user in rating matrix. */
        this.similarityIsSymmetric = similarityIsSymmetric;
        for (var user_id_A : user_ids) {
            userToUserSimilarity.put(user_id_A, new HashMap<>());
            for (var user_id_B : user_ids) {
                if (!similarityIsSymmetric || user_id_A <= user_id_B) {
                    double sim = similarity.sim(user_id_A, user_id_B);
                    userToUserSimilarity.get(user_id_A).put(user_id_B, sim);
                } 
            }
        }
    }

    /**
     * Similarity scores are assumed to be symmetric if not specified. 
     */
    public SimilarityMatrix(Similarity similarity, Set<Integer> user_ids) {
        this(similarity, user_ids, true);
    }

    /**
     * Insert similarity scores sim(A,B) where A is user_id and B are a set user_ids.
     */
    public void insert(Similarity similarity, int user_id, Set<Integer> other_user_ids) {
        if (userToUserSimilarity.get(user_id) == null) {
            userToUserSimilarity.put(user_id, new HashMap<>());
        }
        for (var other_user_id : other_user_ids) {
            double sim = similarity.sim(user_id, other_user_id);
            if (!similarityIsSymmetric || user_id <= other_user_id) {
                userToUserSimilarity.get(user_id).put(other_user_id, sim);
            } else {
                if (userToUserSimilarity.get(other_user_id) == null) {
                    userToUserSimilarity.put(other_user_id, new HashMap<>());
                }
                userToUserSimilarity.get(other_user_id).put(user_id, sim);
            }
        }
    }

    /**
     * Get similarity score between two users.
     */
    @Override
    public double sim(int user_id_A, int user_id_B) {
        if (!similarityIsSymmetric || user_id_A <= user_id_B) {
            return userToUserSimilarity.get(user_id_A).get(user_id_B);
        } else {
            return userToUserSimilarity.get(user_id_B).get(user_id_A);
        }
    }


    /**
     * Test that similaritites between RatingMatrix and 
     * SimilarityMatrix are consistent.
     */
    public static void main(String[] args) {
        var ratingMatrix = new RatingMatrixCosineSimilarity();

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

        /* Store precomputed similarities from ratingMatrix to a SimilarityMatrix. */
        var similarityMatrix = new SimilarityMatrix(ratingMatrix, ratingMatrix.getUserIds());

        /* Sim similarities should be equal. */
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

        System.out.println(ratingSim00 == similaritySim00);
        System.out.println(ratingSim01 == similaritySim01);
        System.out.println(ratingSim02 == similaritySim02);
        System.out.println(ratingSim11 == similaritySim11);
        System.out.println(ratingSim12 == similaritySim12);
        System.out.println(ratingSim22 == similaritySim22);
    }
}
