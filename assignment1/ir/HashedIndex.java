/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  


package ir;

import java.util.HashMap;
import java.util.Iterator;


/**
 *   Implements an inverted index as a Hashtable from words to PostingsLists.
 */
public class HashedIndex implements Index {


    /** The index as a hashtable. */
    private HashMap<String,PostingsList> index = new HashMap<String,PostingsList>();


    /**
     *  Inserts this token in the hashtable.
     */
    public void insert( String token, int docID, int offset ) {
        // if this is the first time encountering the word
        // we create a new postings list + entry and add it to the index
            if (!index.containsKey(token)){
                PostingsEntry pe = new PostingsEntry(docID);
                pe.addOffset(offset);
                PostingsList pl = new PostingsList();
                pl.insertPosting(pe);
                index.put(token,pl);
            } else {
                // not the first time encountering the word
                PostingsList pl = index.get(token);
                // if we're still in the same doc then simply add the position/offset in the list
                if(pl.get(pl.size()-1).docID == docID){
                    index.get(token).get(pl.size()-1).addOffset(offset);
                } else { // new doc new postings entry
                    PostingsEntry pe = new PostingsEntry(docID);
                    pe.addOffset(offset);
                    index.get(token).insertPosting(pe);
                }
            }
    }


    /**
     *  Returns the postings for a specific term, or null
     *  if the term is not in the index.
     */
    public PostingsList getPostings( String token ) {
        return index.getOrDefault(token, null);
    }


    /**
     *  No need for cleanup in a HashedIndex.
     */
    public void cleanup() {
    }
}
