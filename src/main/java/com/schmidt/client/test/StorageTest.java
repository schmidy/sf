package com.schmidt.client.test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import org.apache.log4j.Logger;

public class StorageTest extends Thread {
	
	private static final double MEG = (Math.pow(1024, 2));
	private static final String RECORD = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJCKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()-\n";
	
	private int fileSize;
	private int chunkSize;
	private String clientName;
	private PrintWriter out;
	
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
		
		this.clientName = clientName;
		this.chunkSize = chunkSize;
		out = printWriter;
		
		this.fileSize = fileSize;
	}
	
	/**
	 * Run method to start writing records setup in constructor.
	 */
	public void run() {

		int count = 0;
		File testFile;
		try {
			while (true) {
				testFile = new File("testfile_" + clientName + "." + count);
				writeBuffered(testFile);
				Thread.sleep(500);
				count += 1;
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			
		}

	}

	/**
	 * 
	 * 
	 * @param records
	 * @param bufSize
	 * @throws IOException
	 */
	private void writeBuffered(File testFile) throws IOException {

		Float writeTime = new Float(0);
		log.debug("Writing file: "+ testFile.getName() + " file size: " + fileSize + " chunk size: " + chunkSize);
		
		FileWriter writer = new FileWriter(testFile);
		int chunkWritten = 0;
		
		// Start writing chunks of data to test file
	    try {
	    	while (chunkWritten < fileSize) {
				writeTime = write(createDataSize(chunkSize/RECORD.length()), writer);
				out.println(clientName + " chunksize: " + chunkSize +
						" time to write: " + writeTime + " secs");
				chunkWritten += chunkSize;
	    	}
	    } finally {
	    	// If the last data records was smaller than the chunkSize, write out the last data
	    	if (chunkWritten > fileSize) {
				writeTime = write(createDataSize((chunkSize - fileSize)), writer);
				out.println(clientName + " last chunksize: " + chunkSize + " time to write: " +  
						writeTime + " secs");
	    	}
	    	writer.close();
	        // comment this out if you want to inspect the files afterward
	    	testFile.delete();
	    }
	    
	}

	/**
	 * Write records to file.  Return the amount of time taken to write file.
	 * @param records
	 * @param writer
	 * @return
	 * @throws IOException
	 */
	private static Float write(String record, Writer writer) throws IOException {
		
	    long start = System.currentTimeMillis();
	    
	    writer.write(record);
	    
	    writer.flush();
	    
	    long end = System.currentTimeMillis();
	    
	    return Float.valueOf((end - start) / 1000f);
	}
	
	/**
	 * Quickly create a String of msgSize and return
	 * @param file
	 */
	private static String createDataSize(int msgSize) {
		
		StringBuilder sb = new StringBuilder(msgSize);
		for (int i = 0; i < msgSize; i++) {
			sb.append(RECORD);
		}
		return sb.toString();
	}

}
