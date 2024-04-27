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
     * Similarity is assumed to be assymetric.
     * Insert similarities with put.
     */
    public SimilarityMatrix() {
        this.similarityIsSymmetric = false;
    }

    /**
     * Create empty SimilarityMatrix.
     * Insert similarities with put.
     */
    public SimilarityMatrix(boolean similarityIsSymmetric) {
        this.similarityIsSymmetric = similarityIsSymmetric;
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
     * Construct similarity matrix between one user and a set other users.
     */
    public SimilarityMatrix(Similarity similarity, int user_id, Set<Integer> other_user_ids, boolean similarityIsSymmetric) {
        this.similarityIsSymmetric = similarityIsSymmetric;
        for (int other_user_id : other_user_ids) {
            put(similarity, user_id, other_user_id);
        }
    }

    /**
     * Similarity scores are assumed to be symmetric if not specified. 
     */
    public SimilarityMatrix(Similarity similarity, int user_id, Set<Integer> other_user_ids) {
        this(similarity, user_id, other_user_ids, true);
    }

    /**
     * Insert/update similarity score sim(A,B). 
     */
    public void put(Similarity similarity, int user_id_A, int user_id_B) {
        double sim = similarity.sim(user_id_A, user_id_B);
        if (!similarityIsSymmetric || user_id_A <= user_id_B) {
            if (userToUserSimilarity.get(user_id_A) == null) {
                userToUserSimilarity.put(user_id_A, new HashMap<>());
            }
            userToUserSimilarity.get(user_id_A).put(user_id_B, sim);
        } else {
            if (userToUserSimilarity.get(user_id_B) == null) {
                userToUserSimilarity.put(user_id_B, new HashMap<>());
            }
            userToUserSimilarity.get(user_id_B).put(user_id_A, sim);
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
        var ratingMatrix = new RatingMatrix();

        /* Insert 3 users, each having rated 4 books out of 6 books, into the matrix. */
        ratingMatrix.put(0, 0, 3);
        ratingMatrix.put(0, 1, 5);
        ratingMatrix.put(0, 3, 4);
        ratingMatrix.put(0, 4, 1);

        ratingMatrix.put(1, 0, 4);
        ratingMatrix.put(1, 2, 1);
        ratingMatrix.put(1, 3, 2);
        ratingMatrix.put(1, 5, 4);

        ratingMatrix.put(2, 1, 3);
        ratingMatrix.put(2, 2, 2);
        ratingMatrix.put(2, 3, 5);
        ratingMatrix.put(2, 4, 2);

        /* Compute cosine similarities. */
        var cosineSimilarity = new CosineSimilarity(ratingMatrix);

        /* Store precomputed similarities from ratingMatrix to a SimilarityMatrix. */
        //var similarityMatrix = new SimilarityMatrix(cosineSimilarity, ratingMatrix.getUserIds());
        //var similarityMatrix = new SimilarityMatrix();
        var similarityMatrix = new SimilarityMatrix(cosineSimilarity, 0, ratingMatrix.getUserIds());
        for (int i = 1; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                similarityMatrix.put(cosineSimilarity, i, j);
            }
        }

        /* Similarities should be equal. */
        var ratingSim00 = cosineSimilarity.sim(0,0);
        var ratingSim01 = cosineSimilarity.sim(0,1);
        var ratingSim02 = cosineSimilarity.sim(0,2);
        var ratingSim11 = cosineSimilarity.sim(1,1);
        var ratingSim12 = cosineSimilarity.sim(1,2);
        var ratingSim22 = cosineSimilarity.sim(2,2);

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
