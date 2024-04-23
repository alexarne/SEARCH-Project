/**
 * Author: Erik Lidbjörk.
 * Date 2024.
 * 
 * Get a similarity score between two users
 * A and B based on their user_ids.
 */
public interface Similarity {
    public double sim(int user_id_A, int user_id_B);
}   
