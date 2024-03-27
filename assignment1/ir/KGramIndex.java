/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Dmytro Kalpakchi, 2018
 */

package ir;

import java.io.*;
import java.util.*;
import java.nio.charset.StandardCharsets;


public class KGramIndex {

    /** Mapping from term ids to actual term strings */
    HashMap<Integer,String> id2term = new HashMap<Integer,String>();

    /** Mapping from term strings to term ids */
    HashMap<String,Integer> term2id = new HashMap<String,Integer>();

    /** Index from k-grams to list of term ids that contain the k-gram */
    HashMap<String,List<KGramPostingsEntry>> index = new HashMap<String,List<KGramPostingsEntry>>();

    /** The ID of the last processed term */
    int lastTermID = -1;

    /** Number of symbols to form a K-gram */
    int K = 3;

    public KGramIndex(int k) {
        K = k;
        if (k <= 0) {
            System.err.println("The K-gram index can't be constructed for a negative K value");
            System.exit(1);
        }
    }

    /** Generate the ID for an unknown term */
    private int generateTermID() {
        return ++lastTermID;
    }

    public int getK() {
        return K;
    }


    /**
     *  Get intersection of two postings lists
     */
    private List<KGramPostingsEntry> intersect(List<KGramPostingsEntry> p1, List<KGramPostingsEntry> p2) {
        ArrayList<KGramPostingsEntry> answer = new ArrayList<>();
        int point1 = 0;
        int point2 = 0;
        if (p1 == null | p2 == null) return answer;
        while(point1 != p1.size() && point2 != p2.size()){
            if(p1.get(point1).tokenID == p2.get(point2).tokenID){
                answer.add(new KGramPostingsEntry(p1.get(point1).tokenID));
                point1++;
                point2++;
            } else if (p1.get(point1).tokenID < p2.get(point2).tokenID) point1++;
            else point2++;
        }
        return answer;
    }


    /** Inserts all k-grams from a token into the index. */
    public void insert( String token ) {
        if(term2id.containsKey(token)) return;
        int k = getK();

        // insert term and generated id in maps
        generateTermID();
        term2id.put(token,lastTermID);
        id2term.put(lastTermID,token);

        int termId = getIDByTerm(token);
        // add start and end k-gram
        String newTok = "^" + token + "$";
        HashSet<String> kgs = new HashSet<>();
        for (int i = 0; i <= newTok.length() - k; i++) {
                String kg = newTok.substring(i,i+k); // k-character substring i.e k-gram
                if(!kgs.contains(kg)){
                    kgs.add(kg);
                    insertKg(kg,termId);
                }
        }
    }

    public void insertKg(String kg, int termId){
        KGramPostingsEntry kgp = new KGramPostingsEntry(termId);
        if (!index.containsKey(kg)) {
            ArrayList<KGramPostingsEntry> kList = new ArrayList<>();
            kList.add(kgp);
            index.put(kg,kList);
        } else {
            index.get(kg).add(kgp);
        }
    }

    /** Get postings for the given k-gram */
    public List<KGramPostingsEntry> getPostings(String kgram) {
        return index.getOrDefault(kgram, null);
    }

    public List<KGramPostingsEntry> getIntersection(String[] kgs){
        List<KGramPostingsEntry> answer = new ArrayList<>();
        answer = index.get(kgs[0]);
        for (int i = 1; i < kgs.length; i++) {
            answer = intersect(answer,index.get(kgs[i]));
        }
        //System.out.println("get intersection size : "+answer.size());
        return answer;
    }

    /** Get id of a term */
    public Integer getIDByTerm(String term) {
        return term2id.get(term);
    }

    /** Get a term by the given id */
    public String getTermByID(Integer id) {
        return id2term.get(id);
    }

    private static HashMap<String,String> decodeArgs( String[] args ) {
        HashMap<String,String> decodedArgs = new HashMap<String,String>();
        int i=0, j=0;
        while ( i < args.length ) {
            if ( "-p".equals( args[i] )) {
                i++;
                if ( i < args.length ) {
                    decodedArgs.put("patterns_file", args[i++]);
                }
            } else if ( "-f".equals( args[i] )) {
                i++;
                if ( i < args.length ) {
                    decodedArgs.put("file", args[i++]);
                }
            } else if ( "-k".equals( args[i] )) {
                i++;
                if ( i < args.length ) {
                    decodedArgs.put("k", args[i++]);
                }
            } else if ( "-kg".equals( args[i] )) {
                i++;
                if ( i < args.length ) {
                    decodedArgs.put("kgram", args[i++]);
                }
            } else {
                System.err.println( "Unknown option: " + args[i] );
                break;
            }
        }
        return decodedArgs;
    }

    public static void main(String[] arguments) throws FileNotFoundException, IOException {
        HashMap<String,String> args = decodeArgs(arguments);

        int k = Integer.parseInt(args.getOrDefault("k", "3"));
        KGramIndex kgIndex = new KGramIndex(k);

        File f = new File(args.get("file"));
        Reader reader = new InputStreamReader( new FileInputStream(f), StandardCharsets.UTF_8 );
        Tokenizer tok = new Tokenizer( reader, true, false, true, args.get("patterns_file") );
        while ( tok.hasMoreTokens() ) {
            String token = tok.nextToken();
            kgIndex.insert(token);
        }
      //  String[] in = {"th","he"};
       // kgIndex.getIntersection(in);
        String[] kgrams = args.get("kgram").split(" ");
        List<KGramPostingsEntry> postings = null;
        for (String kgram : kgrams) {
            if (kgram.length() != k) {
                System.err.println("Cannot search k-gram index: " + kgram.length() + "-gram provided instead of " + k + "-gram");
                System.exit(1);
            }

            if (postings == null) {
                postings = kgIndex.getPostings(kgram);
            } else {
                postings = kgIndex.intersect(postings, kgIndex.getPostings(kgram));
            }
        }
        if (postings == null) {
            System.err.println("Found 0 posting(s)");
        } else {
            int resNum = postings.size();
            System.err.println("Found " + resNum + " posting(s)");
            if (resNum > 10) {
                System.err.println("The first 10 of them are:");
                resNum = 10;
            }
            for (int i = 0; i < resNum; i++) {
                System.err.println(kgIndex.getTermByID(postings.get(i).tokenID));
            }
        }
    }
}
