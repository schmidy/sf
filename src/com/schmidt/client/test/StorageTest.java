package com.schmidt.client.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

public class StorageTest extends Thread {
	
	private static final double MEG = (Math.pow(1024, 2));
	private static final String RECORD = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJCKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()-\n";
	private static final int RECSIZE = RECORD.getBytes().length;
	
	private File testFile;
	private int chunkSize;
	private String clientName;
	private PrintWriter out;
	private List<String> records;
	
	final static Logger log = Logger.getLogger(StorageTest.class.getName());
	
	/**
	 * Setup for executing storage test.  
	 * Calculate the number of records needed to fill array used for writing chunks to the storage.
	 * Fill the array with records.
	 * 
	 * @param testFile
	 * @param fileSize
	 * @param testTime
	 */
	public StorageTest(String clientName, int fileSize, int chunkSize, int testTime, PrintWriter printWriter) {
		
		this.testFile = new File("testfile_" + clientName);
		this.clientName = clientName;
		this.chunkSize = chunkSize;
		out = printWriter;
		
		int numOfRecords = fileSize / RECORD.length();

	    records = new ArrayList<String>(numOfRecords);
	    int size = 0;
	    for (int i = 0; i < numOfRecords; i++) {
	        records.add(RECORD);
	        size += RECSIZE;
	    }
	    
	    if ( fileSize % RECORD.length() > 0 ) {
	    	records.add(RECORD.substring(0, fileSize % RECORD.length()));
	    	size += fileSize % RECORD.length();
	    }
	    
	    log.debug(records.size() + " 'records'");
	    log.debug(size / MEG + " MB");

	}
	
	/**
	 * Run method to start writing records setup in constructor.
	 */
	public void run() {

         try {

	        out.println(clientName + " time to write: " + writeBuffered(records, chunkSize) + " secs");
	        
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * 
	 * 
	 * @param records
	 * @param bufSize
	 * @throws IOException
	 */
	private Float writeBuffered(List<String> records, int bufSize) throws IOException {

		Float writeTime = new Float(0);
	    try {
	        FileWriter writer = new FileWriter(testFile);
	        BufferedWriter bufferedWriter = new BufferedWriter(writer, bufSize);

	        log.debug("Writing buffered (buffer size: " + bufSize + ")... ");
	        writeTime = write(records, bufferedWriter);
	        
	    } finally {
	        // comment this out if you want to inspect the files afterward
	    	testFile.delete();
	    }
	    
	    return writeTime;
	}

	/**
	 * Write records to file.  Return the amount of time taken to write file.
	 * @param records
	 * @param writer
	 * @return
	 * @throws IOException
	 */
	private static Float write(List<String> records, Writer writer) throws IOException {
		
	    long start = System.currentTimeMillis();
	    
	    for (String record: records) {
	        writer.write(record);
	    }
	    
	    writer.flush();
	    writer.close();
	    long end = System.currentTimeMillis();
	    
	    log.debug((end - start) / 1000f + " seconds");
	    return Float.valueOf((end - start) / 1000f);
	}
	
	
	public File getFile() {
		return testFile;
	}

	public void setFile(File file) {
		this.testFile = file;
	}

}
