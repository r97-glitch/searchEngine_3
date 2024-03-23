/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.*;
import java.nio.charset.*;
import java.io.*;


/**
 *  A class for representing a query as a list of words, each of which has
 *  an associated weight.
 */
public class Query {

    /**
     *  Help class to represent one query term, with its associated weight. 
     */
    class QueryTerm {
        String term;
        double weight;
        QueryTerm( String t, double w ) {
            term = t;
            weight = w;
        }
    }

    /** 
     *  Representation of the query as a list of terms with associated weights.
     *  In assignments 1 and 2, the weight of each term will always be 1.
     */
    public ArrayList<QueryTerm> queryterm = new ArrayList<QueryTerm>();

    /**  
     *  Relevance feedback constant alpha (= weight of original query terms). 
     *  Should be between 0 and 1.
     *  (only used in assignment 3).
     */
    double alpha = 0.2;

    /**  
     *  Relevance feedback constant beta (= weight of query terms obtained by
     *  feedback from the user). 
     *  (only used in assignment 3).
     */
    double beta = 1 - alpha;
    
    
    /**
     *  Creates a new empty Query 
     */
    public Query() {
    }
    
    
    /**
     *  Creates a new Query from a string of words
     */
    public Query( String queryString  ) {
        StringTokenizer tok = new StringTokenizer( queryString );
        while ( tok.hasMoreTokens() ) {
            queryterm.add( new QueryTerm(tok.nextToken(), 1.0) );
        }    
    }
    
    
    /**
     *  Returns the number of terms
     */
    public int size() {
        return queryterm.size();
    }
    
    
    /**
     *  Returns the Manhattan query length
     */
    public double length() {
        double len = 0;
        for ( QueryTerm t : queryterm ) {
            len += t.weight; 
        }
        return len;
    }
    
    
    /**
     *  Returns a copy of the Query
     */
    public Query copy() {
        Query queryCopy = new Query();
        for ( QueryTerm t : queryterm ) {
            queryCopy.queryterm.add( new QueryTerm(t.term, t.weight) );
        }
        return queryCopy;
    }
    
    
    /**
     *  Expands the Query using Relevance Feedback
     *
     *  @param results The results of the previous query.
     *  @param docIsRelevant A boolean array representing which query results the user deemed relevant.
     *  @param engine The search engine object
     */
    public void relevanceFeedback( PostingsList results, boolean[] docIsRelevant, Engine engine ) {

        int relevantDocs = 0;
        int glIndex = 0;
        // re-weight query terms and add them to map
        HashMap<String,Integer> terms = new HashMap<>();
        for (int i = 0; i < queryterm.size(); i++) {
            QueryTerm qt = queryterm.get(i);
            terms.put(qt.term,glIndex);
            glIndex++;
            qt.weight *= alpha ;
        }

        /*for (QueryTerm qt : queryterm){
            System.out.println("query term : "  + qt.term+ " weight: " + qt.weight);
        }*/
        for (int i = 0; i < docIsRelevant.length; i++) {
            if (docIsRelevant[i]) relevantDocs++;
        }
        System.out.println("relevantDocs : "+ relevantDocs);
        double relv_w = beta / relevantDocs;
        System.out.println("relevant weight : "+ relv_w);
        for (int i = 0; i < docIsRelevant.length; i++) {
            if (docIsRelevant[i]) {
                PostingsEntry pe = results.list.get(i);
                // get relevant doc title, read its tokens and add to query
                String file = engine.index.docNames.get(pe.docID);
                System.out.println("file title : "+ file);
                int tempCounter =0;
                try {
                    Reader reader = new InputStreamReader( new FileInputStream(file), StandardCharsets.UTF_8 );
                    Tokenizer tok = new Tokenizer( reader, true, false, true, "C:\\Users\\Rabi\\OneDrive\\Desktop\\assignment1\\patterns.txt" );
                    while ( tok.hasMoreTokens() ) {
                        String token = tok.nextToken();
                        //  new term
                        if(!terms.containsKey(token)){
                            // new query term added to the query
                            QueryTerm nqt = new QueryTerm(token,(double) relv_w );
                            queryterm.add(nqt);
                            terms.put(token,glIndex);
                            glIndex++;
                        } else {
                            tempCounter++;
                            // re-occuring term, increment weight
                            if(tempCounter < 17) System.out.println("token : "+ token);
                            int in = terms.get(token);
                            if(tempCounter < 17) System.out.println("supposedly the token : " + queryterm.get(in).term+ " its weight : "+queryterm.get(in).weight);
                            queryterm.get(in).weight= (double) queryterm.get(in).weight + relv_w;

                            if(tempCounter < 17) System.out.println("new weight : "+ queryterm.get(in).weight);

                        }
                    }

                    reader.close();
                } catch ( IOException e ) {
                    System.err.println( "Warning: IOException during indexing." );
                }
            }
        }
    }
}


