/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.io.Serializable;

public class PostingsEntry implements Comparable<PostingsEntry>, Serializable {

    public int docID;
    public ArrayList<Integer> offsets;
    public double score = 0;

    /**
     *  PostingsEntries are compared by their score (only relevant
     *  in ranked retrieval).
     *
     *  The comparison is defined so that entries will be put in 
     *  descending order.
     */
    public int compareTo( PostingsEntry other ) {
       return Double.compare( other.score, score );
    }

    public double calculateScore(double idf, int docLength, double weight, double termWeight){
                this.score = (weight * termWeight * idf * offsets.size())/docLength;
                return this.score;
    }
    public double incrementScore(double newScore){
        this.score += newScore;
        return this.score;
    }

    public PostingsEntry(int docID){
        this.docID = docID;
        this.offsets = new ArrayList<>();
        this.score = 0;
    }
    public PostingsEntry(int docID, ArrayList<Integer> list){
        this.docID = docID;
        this.offsets = list;
        this.score = 0;
    }

    public void addOffset(int offset){
        offsets.add(offset);
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("Document ID: ").append(docID).append(", Offsets: ");
        sb.append(offsets.toString());
        return sb.toString();
    }

    public static PostingsEntry fromString(String str) {
        String[] parts = str.split(", Offsets: ");
        int documentId = Integer.parseInt(parts[0].substring("Document ID: ".length()));
        String offsetsString = parts[1].substring(1, parts[1].length() - 1);
        String[] offsetsArray = offsetsString.split(", ");
        ArrayList<Integer> offsets = new ArrayList<>();
        for (String offset : offsetsArray) {
            offsets.add(Integer.parseInt(offset));
        }
        return new PostingsEntry(documentId, offsets);
    }
}

