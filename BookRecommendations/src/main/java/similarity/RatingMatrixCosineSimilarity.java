package similarity;

import java.util.function.DoubleFunction;

/**
 * Author: Erik Lidbjörk.
 * Date 2024.
 * 
 * RatingMatrix with the capability to 
 * compute cosine similarities between users.
 */
public class RatingMatrixCosineSimilarity extends RatingMatrix implements Similarity {
    /**
     * Dot product between user A and B. 
     */
    private double dot(int user_id_A, int user_id_B) {
        var bookScoresA = userToBookScore.get(user_id_A);
        var bookScoresB = userToBookScore.get(user_id_B);
        
        /* Iterate over both users.*/
        var iterA = bookScoresA.entrySet().iterator();
        var iterB = bookScoresB.entrySet().iterator();

        double dotProduct = 0d;
        var entryA = iterA.next();
        var entryB = iterB.next();
        /* Union search, linear scan. */
        while (true) {
            if (entryA.getKey() == entryB.getKey()) {
                dotProduct += entryA.getValue() * entryB.getValue();
            }
            if (!iterA.hasNext() && !iterB.hasNext()) {
                return dotProduct;
            }
            if (entryA.getKey() == entryB.getKey()) {
                entryA = iterA.next();
                entryB = iterB.next();
            } else if (entryA.getKey() < entryB.getKey()) {
                entryA = iterA.next();
            } else {
                entryB = iterB.next();
            }
        }   
    }

    /**
     * Get length of vector with specified metric.
     */
    private double length(int user_id, DoubleFunction<Double> metric) {
        var bookScores = userToBookScore.get(user_id);
        double len = 0d; 
        for (double rating : bookScores.values()) {
            len += metric.apply(rating);
        }
        return len;
    }


    /**
     * Get cosine similarity between user A and B with specified metric.
     * Return positive/negative infinity if metric for user A or B is 0.
     */
    public double sim(int user_id_A, int user_id_B, DoubleFunction<Double> metric) {
        double dotProduct = dot(user_id_A, user_id_B);
        if (dotProduct == 0d) {
            return 0d;
        }
        double lenA = length(user_id_A, metric);
        double lenB = length(user_id_B, metric);
        if (lenA == 0d && lenB != 0d) {
            return Math.signum(dotProduct) * Math.signum(lenB) * Double.POSITIVE_INFINITY;
        }
        if (lenA != 0d && lenB == 0d) {
            return Math.signum(dotProduct) * Math.signum(lenA) * Double.POSITIVE_INFINITY;
        }
        if (lenA == 0d && lenB == 0d) {
            return Math.signum(dotProduct) * Double.POSITIVE_INFINITY;
        }
        return dotProduct / (lenA * lenB);
    }    

    /**
     * Get cosine similarity between user A and B with euclidean metric.
     */
    public double simEuclidean(int user_id_A, int user_id_B) {
        return sim(user_id_A, user_id_B, rating -> Math.pow(rating, 2));
    }

    /**
     * Get cosine similarity between user A and B with manhattan metric.
     */
    public double simManhattan(int user_id_A, int user_id_B) {
        return sim(user_id_A, user_id_B, rating -> Math.abs(rating));
    }

    /**
     * Get "assymetric" cosine similarity between user A and B with specified metric.
     * Do not calcualte length of user A since it will be the same for all comparisons to other users.
     */
    public double simAssymetric(int user_id_A, int user_id_B, DoubleFunction<Double> metric) {
        double dotProduct = dot(user_id_A, user_id_B);
        if (dotProduct == 0d) {
            return 0d;
        }
        double lenB = length(user_id_B, metric);
        if (lenB == 0d) {
            return Math.signum(dotProduct) * Double.POSITIVE_INFINITY;
        }
        return dotProduct / lenB;
    }    

    /**
     * Get assymetric cosine similarity between user A and B with euclidean metric.
     */
    public double simAssymetricEuclidean(int user_id_A, int user_id_B) {
        return simAssymetric(user_id_A, user_id_B, rating -> Math.pow(rating, 2));
    }

    /**
     * Get assymetric cosine similarity between user A and B with manhattan metric.
     */
    public double simAssymetricManhattan(int user_id_A, int user_id_B) {
        return simAssymetric(user_id_A, user_id_B, rating -> Math.abs(rating));
    }


    /**
     * Default similarity score is symmetric with euclidean norm. 
     * Override this method to use something else for the 
     * Similarity interface.
     */
    @Override
    public double sim(int user_id_A, int user_id_B) {
        return simEuclidean(user_id_A, user_id_B);
    }


    /**
     * Test class with mock data.
     */
    public static void main(String[] args) {
        var matrix = new RatingMatrixCosineSimilarity();

        /* Insert 3 users, each having rated 4 books out of 6 books, into the matrix. */
        matrix.insert(0, 0, 3 - 3);
        matrix.insert(0, 1, 5 - 3);
        matrix.insert(0, 3, 4 - 3);
        matrix.insert(0, 4, 1 - 3);

        matrix.insert(1, 0, 4 - 3);
        matrix.insert(1, 2, 1 - 3);
        matrix.insert(1, 3, 2 - 3);
        matrix.insert(1, 5, 4 - 3);

        matrix.insert(2, 1, 3 - 3);
        matrix.insert(2, 2, 2 - 3);
        matrix.insert(2, 3, 5 - 3);
        matrix.insert(2, 4, 2 - 3);

        /* Sim should be equal to expectedSim. */
        var sim01 = matrix.simEuclidean(0,1);
        var sim02 = matrix.simEuclidean(0,2);
        var sim12 = matrix.simEuclidean(1,2);

        double len0 = 0*0 + 2*2 + 1*1 + (-2)*(-2);
        double len1 = 1*1 + (-2)*(-2) + (-1)*(-1) + 1*1;
        double len2 = 0*0 + (-1)*(-1) + 2*2 + (-1)*(-1);

        double dot01 = 0*1 + 1*(-1);            // Three books in common.
        double dot02 = 2*0 + 1*2 + (-2)*(-1);   // Three books in common.
        double dot12 = (-2)*(-1) + (-1)*2;      // Two books in common.

        double expectedSim01 = dot01 / (len0 * len1);
        double expectedSim02 = dot02 / (len0 * len2);
        double expectedSim12 = dot12 / (len1 * len2);

        System.out.println(sim01 == expectedSim01);
        System.out.println(sim02 == expectedSim02);
        System.out.println(sim12 == expectedSim12);
    }
    
}
