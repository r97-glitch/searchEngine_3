/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.*;

/**
 *  Searches an index for results of a query.
 */
public class Searcher {

    /** The index to be searched by this Searcher. */
    Index index;

    /** The k-gram index to be searched by this Searcher */
    KGramIndex kgIndex;
    
    /** Constructor */
    public Searcher( Index index, KGramIndex kgIndex ) {
        this.index = index;
        this.kgIndex = kgIndex;
    }

    public PostingsList intersectionSearch(PostingsList p1, PostingsList p2){
        PostingsList answer = new PostingsList();
        int point1 = 0;
        int point2 = 0;
        if (p1 == null | p2 == null) return answer;
        while(point1 != p1.size() && point2 != p2.size()){
            if(p1.get(point1).docID == p2.get(point2).docID){
                answer.insertPosting(new PostingsEntry(p1.get(point1).docID));
                point1++;
                point2++;
            } else if (p1.get(point1).docID < p2.get(point2).docID) point1++;
            else point2++;
        }
        return answer;
    }

    public PostingsList positionalSearch(PostingsList pl1, PostingsList pl2){
        PostingsList answer = new PostingsList();
        int p1 = 0;
        int p2 = 0;
        while(p1 != pl1.size() && p2 != pl2.size()){
            // matching document
            if(pl1.get(p1).docID == pl2.get(p2).docID){
                // positional lists and pointers
                int pp1 = 0;
                int pp2 = 0;
                ArrayList<Integer> pos1 = pl1.get(p1).offsets;
                ArrayList<Integer> pos2 = pl2.get(p2).offsets;
                ArrayList<Integer> ansPos = new ArrayList<>();
                while(pp1 != pos1.size() && pp2 != pos2.size() ){
                    // first pointer should have an offset one less than second pointer
                    if (pos1.get(pp1) >= pos2.get(pp2)) pp2++; // pp1 has larger/equal offset than pp2
                    else if (pos2.get(pp2) - pos1.get(pp1) == 1) { // match!
                        // add the last offset
                        ansPos.add(pos2.get(pp2));
                        pp1++;
                        pp2++;
                    }
                    else pp1++; // first pointer has offset less than second pointer by more than 1
                }
                if (ansPos.size() != 0) {
                    PostingsEntry ansPe = new PostingsEntry(pl1.get(p1).docID);
                    ansPe.offsets = ansPos;
                    answer.insertPosting(ansPe);
                }
                p1++;
                p2++;
            } else if (pl1.get(p1).docID < pl2.get(p2).docID) p1++;
            else p2++;
        }
        return answer;
    }

    public PostingsList rankedRetrieval(Query query){
        int i = 0;
        PostingsList answer = new PostingsList();
        // mapping docIds to their index in the answer list
        HashMap<Integer,Integer> indexMap = new HashMap<>();
        int numDocs = index.docNames.size();
      //  System.out.println("total docs : " + numDocs);
        for (Query.QueryTerm qt :query.queryterm ){

            PostingsList pl = index.getPostings(qt.term);
            //System.out.println("term before : "+qt.term);
            if(pl != null){
                double idf = Math.log((double) numDocs/ pl.size());
                for (PostingsEntry pe: pl.list){
                    int docLength = index.docLengths.get(pe.docID);
                    if (indexMap.containsKey(pe.docID)) {
                        int in = indexMap.get(pe.docID); // get index of existing doc in answer list
                        answer.get(in).incrementScore(pe.calculateScore(idf,docLength,1, qt.weight));
                    }else{
                        pe.calculateScore(idf,docLength,1, qt.weight);
                        answer.list.add(pe);
                        // put new doc in index map , increment index
                        indexMap.put(pe.docID,i);
                        i++;
                    }
                }
            }

        }
        Collections.sort(answer.list);
        return answer;
    }

    public PostingsList pagedRankSearch(Query query){
        PostingsList answer = new PostingsList();
        HashSet<Integer> set = new HashSet<>();
        for (Query.QueryTerm qt : query.queryterm){
            PostingsList pl = index.getPostings(qt.term);

            for (PostingsEntry pe: pl.list){
                if(!set.contains(pe.docID)){
                    set.add(pe.docID);
                    String docTitle = index.docNames.get(pe.docID);
                    pe.score = index.docRanks.get(docTitle);
                    answer.list.add(pe);
                }
            }
        }
        Collections.sort(answer.list);
        return answer;
    }

    public PostingsList combinationSearch(Query query, double idf_w, double rank_w){
        int i = 0;
        PostingsList answer = new PostingsList();
        // mapping docIds to their index in the answer list
        HashMap<Integer,Integer> indexMap = new HashMap<>();
        int numDocs = index.docNames.size();
        //  System.out.println("total docs : " + numDocs);
        for (Query.QueryTerm qt :query.queryterm ){

            PostingsList pl = index.getPostings(qt.term);

            double idf = Math.log((double) numDocs/ pl.size());
            for (PostingsEntry pe: pl.list){
                int docLength = index.docLengths.get(pe.docID);
                if (indexMap.containsKey(pe.docID)) {
                    int in = indexMap.get(pe.docID); // get index of existing doc in answer list
                    answer.get(in).incrementScore(pe.calculateScore(idf,docLength,idf_w,qt.weight));
                }else{
                    pe.calculateScore(idf,docLength,idf_w, qt.weight);
                    String docTitle = index.docNames.get(pe.docID);
                    double rank = index.docRanks.get(docTitle);
                    pe.incrementScore(rank * rank_w);

                    answer.list.add(pe);
                    // put new doc in index map , increment index
                    indexMap.put(pe.docID,i);
                    i++;
                }
            }
        }
        Collections.sort(answer.list);
        return answer;
    }



    /**
     *  Searches the index for postings matching the query.
     *  @return A postings list representing the result of the query.
     */
    public PostingsList search( Query query, QueryType queryType, RankingType rankingType, NormalizationType normType ) { 
        //
        //  REPLACE THE STATEMENT BELOW WITH YOUR CODE
        //
       switch (queryType) {
           case INTERSECTION_QUERY:
               if (query.queryterm.size() == 1) return index.getPostings(query.queryterm.get(0).term);
               else {
                   // priority queue for postings lists with smaller lists having higher priority
                   PriorityQueue<PostingsList> qpl = new PriorityQueue<>();
                   for (Query.QueryTerm qt: query.queryterm) {
                       if(index.getPostings(qt.term) == null) return null;
                       qpl.add(index.getPostings(qt.term));
                   }
                   PostingsList answer = qpl.poll();
                   while(!qpl.isEmpty()){
                       answer = intersectionSearch(answer,qpl.poll());
                       if (answer.size() == 0) return null;
                   }
                   return answer;
               }
           case PHRASE_QUERY:
               PostingsList answer = index.getPostings(query.queryterm.get(0).term);
               for (int i=1; i<query.queryterm.size();i++){
                   answer = positionalSearch(answer,index.getPostings(query.queryterm.get(i).term));
                   if (answer.size() == 0) return null;
               }
               return answer;
           case RANKED_QUERY:
                switch(rankingType){
                    case TF_IDF:
                        return rankedRetrieval(query);
                    case PAGERANK:
                        return pagedRankSearch(query);
                    case COMBINATION:
                        return combinationSearch(query,1,1);
                }
           default:
               return index.getPostings(query.queryterm.get(0).term);
       }
    }
}