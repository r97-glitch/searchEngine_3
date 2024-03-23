/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, KTH, 2018
 */  

package ir;

import java.io.*;
import java.util.*;
import java.nio.charset.*;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.math.BigInteger;


/*
 *   Implements an inverted index as a hashtable on disk.
 *   
 *   Both the words (the dictionary) and the data (the postings list) are
 *   stored in RandomAccessFiles that permit fast (almost constant-time)
 *   disk seeks. 
 *
 *   When words are read and indexed, they are first put in an ordinary,
 *   main-memory HashMap. When all words are read, the index is committed
 *   to disk.
 */
public class PersistentHashedIndex implements Index {

    /** The directory where the persistent index files are stored. */
    public static final String INDEXDIR = "./index";

    /** The dictionary file name */
    public static final String DICTIONARY_FNAME = "dictionary";

    /** The data file name */
    public static final String DATA_FNAME = "data";

    /** The terms file name */
    public static final String TERMS_FNAME = "terms";

    /** The doc info file name */
    public static final String DOCINFO_FNAME = "docInfo";

    /** The dictionary hash table on disk can fit this many entries. */
    public static final long TABLESIZE = 611953L;

    /** The dictionary hash table is stored in this file. */
    RandomAccessFile dictionaryFile;

    /** The data (the PostingsLists) are stored in this file. */
    RandomAccessFile dataFile;

    /** Pointer to the first free memory cell in the data file. */
    long free = 0L;

    /** The cache as a main-memory hash map. */
    HashMap<String,PostingsList> index = new HashMap<String,PostingsList>();



    // ===================================================================

    /**
     *   A helper class representing one entry in the dictionary hashtable.
     */ 
    public class Entry {
        public long startAddress;
        public int size;
        public String checkSum;

        public Entry() {}

        public Entry( long start, int size, String check){
            this.startAddress = start;
            this.size = size;
            this.checkSum = check;
        }
    }


    // ==================================================================

    
    /**
     *  Constructor. Opens the dictionary file and the data file.
     *  If these files don't exist, they will be created. 
     */
    public PersistentHashedIndex() {
        try {
            dictionaryFile = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME, "rw" );
            dataFile = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME, "rw" );
        } catch ( IOException e ) {
            e.printStackTrace();
        }

        try {
            readDocInfo();
        } catch ( FileNotFoundException e ) {
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    /**
     *  Writes data to the data file at a specified place.
     *
     *  @return The number of bytes written.
     */ 
    int writeData( String dataString, long ptr ) {
        try {
            dataFile.seek( ptr ); 
            byte[] data = dataString.getBytes();
            dataFile.write( data );
            return data.length;
        } catch ( IOException e ) {
            e.printStackTrace();
            return -1;
        }
    }


    /**
     *  Reads data from the data file
     */ 
    String readData( long ptr, int size ) {
        try {
            dataFile.seek( ptr );
            byte[] data = new byte[size];
            dataFile.readFully( data );
            return new String(data);
        } catch ( IOException e ) {
            e.printStackTrace();
            return null;
        }
    }


    // ==================================================================
    //
    //  Reading and writing to the dictionary file.

    /*
     *  Writes an entry to the dictionary hash table file. 
     *
     *  @param entry The key of this entry is assumed to have a fixed length
     *  @param ptr   The place in the dictionary file to store the entry
     */
    void writeEntry( Entry entry, long ptr ) {
        try{
            dictionaryFile.seek(ptr);
            dictionaryFile.writeLong(entry.startAddress);
            dictionaryFile.writeInt(entry.size);
            dictionaryFile.writeUTF(entry.checkSum);
        } catch (IOException e){
            System.out.println("IO EXCEPTION from writeEntry");
        }
    }

    private static boolean isEmpty(byte[] chunk) {
        // Check if the chunk contains all zeros
        for (byte b : chunk) {
            if (b != 0) {
                return false; // Chunk is not empty
            }
        }
        return true; // Chunk is empty
    }

    /**
     *  Reads an entry from the dictionary file.
     *
     *  @param ptr The place in the dictionary file where to start reading.
     */
    Entry readEntry( String term, long ptr ) {
        try{
            // read from offset and load into buffer
            dictionaryFile.seek(ptr);
            byte[] buffer = new byte[78];
            while(dictionaryFile.read(buffer) != -1){
                // read data from buffer and compare checksums
                ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
                long address = byteBuffer.getLong();
                int size = byteBuffer.getInt();
                byte[] padBytes = new byte[2];
                byte[] bytes = new byte[34];
                //remove 2 byte overhead from writeUTF() method
                byteBuffer.get(padBytes);
                // get byte array of checksum string
                byteBuffer.get(bytes);
                String str = new String(bytes);
                String checkSum = computeChecksum(term);
                if(str.equals(checkSum)) {
                    return new Entry(address, size, checkSum);
                }
            }
            return null;
        } catch (IOException e){
            return null;
        }
    }


    // ==================================================================

    /**
     *  Writes the document names and document lengths to file.
     *
     * @throws IOException  { exception_description }
     */
    private void writeDocInfo() throws IOException {
        FileOutputStream fout = new FileOutputStream( INDEXDIR + "/docInfo" );
        for ( Map.Entry<Integer,String> entry : docNames.entrySet() ) {
            Integer key = entry.getKey();
            String docInfoEntry = key + ";" + entry.getValue() + ";" + docLengths.get(key) + "\n";
            fout.write( docInfoEntry.getBytes() );
        }
        fout.close();
    }


    /**
     *  Reads the document names and document lengths from file, and
     *  put them in the appropriate data structures.
     *
     * @throws     IOException  { exception_description }
     */
    private void readDocInfo() throws IOException {
        File file = new File( INDEXDIR + "/docInfo" );
        FileReader freader = new FileReader(file);
        try ( BufferedReader br = new BufferedReader(freader) ) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(";");
                docNames.put( new Integer(data[0]), data[1] );
                docLengths.put( new Integer(data[0]), new Integer(data[2]) );
            }
        }
        freader.close();
    }


    /**
     *  Write the index to files.
     */
    public void writeIndex() {
        int collisions = 0;
        try {
            // Write the 'docNames' and 'docLengths' hash maps to a file
            writeDocInfo();

            // Write the dictionary and the postings list
            long dataFilePointer = 0;
            for (String term:
                    index.keySet()
                 ) {
                // get string rep of postings list and write it to data file
                String postingsListString = index.get(term).toString();
                int size = writeData(postingsListString, dataFilePointer);
                // compute checksum and create entry
                String check = computeChecksum(term);
                Entry entry = new Entry(dataFilePointer,size,check);

                long hash = multiplicativeHash(term);
                dictionaryFile.seek(hash);
                byte[] buffer = new byte[78];
                // if nothing is in the doc write data
                if(dictionaryFile.read(buffer) == -1) writeEntry(entry,hash);
                else{
                    // find the nearest empty entry block
                    while(dictionaryFile.read(buffer)!= -1){
                        // empty buffer, go back the size of the buffer and write entry
                        if (isEmpty(buffer)){
                            hash = dictionaryFile.getFilePointer() - 78L;
                            writeEntry(entry, hash);
                            break;
                        }else{
                            // non empty line which means a collision
                            collisions++;
                        }
                    }
                }
                dataFilePointer = dataFilePointer + size;
            }

        } catch ( IOException e ) {
            e.printStackTrace();
        }
        System.err.println( collisions + " collisions." );
    }

    public String computeChecksum(String str) {
        try{
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(str.getBytes(StandardCharsets.UTF_8));
            BigInteger number = new BigInteger(1, hash);

            // Convert message digest into hex value
            StringBuilder hexString = new StringBuilder(number.toString(16));

            // Pad with leading zeros
            while (hexString.length() < 64)
            {
                hexString.insert(0, '0');
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    public static long hash(String str) {
        int hash = 0;
        for (int i = 0; i < str.length(); i++) {
            hash = str.charAt(i) + ((hash << 5) - hash);
        }
        return Math.abs(hash%TABLESIZE);
    }

    public long multiplicativeHash(String word) {
        long hash = 0;
        long prime = 31; // A commonly used prime number
        for (int i = 0; i < word.length(); i++) {
            hash = (hash * prime + word.charAt(i)) % TABLESIZE;
        }
        return hash;
    }

    public long hash2(String str){
        return Math.abs(str.hashCode() % TABLESIZE);
    }


    // ==================================================================


    /**
     *  Returns the postings for a specific term, or null
     *  if the term is not in the index.
     */
    public PostingsList getPostings( String token ) {
       long hash = multiplicativeHash(token);
       Entry entry = readEntry(token, hash);
       if (entry == null) return null;
       String postingListString = readData(entry.startAddress, entry.size);
        return PostingsList.fromString(postingListString);
    }


    /**
     *  Inserts this token in the main-memory hashtable.
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
                pl.get(pl.size()-1).addOffset(offset);
            } else { // new doc new postings entry
                PostingsEntry pe = new PostingsEntry(docID);
                pe.addOffset(offset);
                index.get(token).insertPosting(pe);
            }
        }
    }


    /**
     *  Write index to file after indexing is done.
     */
    public void cleanup() {
        System.err.println( index.keySet().size() + " unique words" );
        System.err.print( "Writing index to disk..." );
        writeIndex();
        System.err.println( "done!" );
    }
}
