/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.ArrayList;

public class PostingsList implements Comparable<PostingsList>{
    
    /** The postings list */
    public ArrayList<PostingsEntry> list = new ArrayList<PostingsEntry>();


    /** Number of postings in this list. */
    public int size() {
    return list.size();
    }

    /** Returns the ith posting. */
    public PostingsEntry get( int i ) {
    return list.get( i );
    }

    public void insertPosting(PostingsEntry pe){
        list.add(pe);
    }

    @Override
    public int compareTo(PostingsList o) {
        return list.size() - o.size();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (PostingsEntry entry : list) {
            sb.append(entry.toString()).append("\n");
        }
        return sb.toString();
    }

    public static PostingsList fromString(String str) {
        PostingsList postingsList = new PostingsList();
        String[] entryStrings = str.split("\n");
        for (String entryString : entryStrings) {
            PostingsEntry entry = PostingsEntry.fromString(entryString);
            postingsList.insertPosting(entry);
        }
        return postingsList;
    }
}

