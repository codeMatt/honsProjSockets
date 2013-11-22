package com.heeere.android.dnssdtuto;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Date;
import android.os.Environment;
import android.widget.TextView;

public class SocketClient extends Thread {

	private File path;
	private String address;
	private String[] fileList;
	private int port;
	private TextView downloadText;
    private android.os.Handler handler;
    private String logText;
    private int endOfFileProtocol = 0;
    private final int BUFFER_SIZE = 16 * 1024;
	
	public SocketClient(String socketAddress, File filePath, String[] fileList, int socketPort, TextView text, int choice){
		path = filePath;
		address = socketAddress;
		this.fileList = fileList;
		port = socketPort;
		downloadText = text;
		handler = new android.os.Handler();
		endOfFileProtocol = choice;
	}

	// Checks if external storage is available for read and write
	public boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return true;
		}
		return false;
	}

	//convert an byte[] into a integer
	private int getIntFromByte(byte[] array){

		ByteBuffer buffer = ByteBuffer.allocate(array.length);
		buffer.put(array);
		buffer.flip();
		int value = buffer.getInt();		
		buffer.rewind();

		return value;
	}
	
	public void run(){
		if (endOfFileProtocol == 1){
			runFileSizeBlock();
		}
		else if (endOfFileProtocol == 2){
			runEndOfFileMarker();
		}
		else if (endOfFileProtocol == 0){
			System.out.println("choice not set!");
		}
	}	

	//first run method that tests file size block
	//Initialize the socket connection and file transfer
	public void runFileSizeBlock(){

		System.out.println("Getting files From:" + address);	

		int totalByteTransfer = 0;
		String date = new Date().toString();
		addToLog("New Session:Client - 3 - File Length block" + ": Buffer Size: " + BUFFER_SIZE + "-------------------------" + "\n"
				 + date);

		//Opening The socket & sending the file------------------------------------------------------
		//System.out.println("Starting file sending process");  		

		Socket connection = null;
		BufferedOutputStream oos = null;
		BufferedOutputStream bos = null;

		try {

			long sessionStart = System.currentTimeMillis();
			byte[] mybytearray = new byte [8 * 1024];							
			int filesLeft = fileList.length;
			int currentFile = 0;						
			connection = new Socket (address, port); 
			

			while(filesLeft > 0){
				System.out.println("new file download starting:");
				/*initialising messages to device 
				  what action to be taken,
				  the length of the filename and the filename*/				
				byte[] action = {1};
				byte[]fileName = fileList[currentFile].getBytes();
				String fileNameString = fileList[currentFile++];
				int filenameLength = fileName.length;							
				byte[] nameLength = ByteBuffer.allocate(4).putInt(filenameLength).array();

				System.out.println("sending message");
				//sending message to device
				long msgStart = System.currentTimeMillis();
				oos = new BufferedOutputStream(connection.getOutputStream());
				oos.write(action, 0 , 1);
				oos.write(nameLength,0,4);
				oos.write(fileName,0, filenameLength);
				
				//test the latency of the connection
				long pingStart = new Date().getTime();
				oos.write(longToBytes(pingStart), 0, 8);
				oos.flush();
				//long pingTestTime = new Date().getTime();
				long msgDuration = System.currentTimeMillis() - msgStart;
				
				//measure the amount of bytes sent
				addToLog("Sent Message" + "\nDuration: " + msgDuration + " ms"
										+ "\nSize: " + (1+4+filenameLength) + " bytes");
				
				byte[] pingData = new byte[8];
				
				//stream initialisation
				InputStream is = connection.getInputStream();
				//read ping data
				is.read(pingData, 0, 8);
				long ping = (new Date().getTime() - bytesToLong(pingData));
				
				File receivedFile;
				

				receivedFile = new File(path, "/received_" + fileNameString);				
				bos = new BufferedOutputStream(new FileOutputStream(receivedFile));

				//receive data from stream
				System.out.println("Connection latency RTT: " + ping + " ms");
				System.out.println("data reading started of " + fileNameString);
				int count;
				long cumulativeCount = 0;

				byte[] fileSizeByte= new byte[4];
				is.read(fileSizeByte, 0 , 4);
				//measure the reading of bytes
				totalByteTransfer+= 4;
				long fileSize = getIntFromByte(fileSizeByte);
				System.out.println("File being received size: " + fileSize);

				
				long fileStart= System.currentTimeMillis();
				
				//read in file (read in exact amount of bytes that the file contains)
				while( cumulativeCount < fileSize) {				
					count = is.read(mybytearray);
						if(count == -1){
							System.out.println("file sending ended on other side");
							break;
						}
					bos.write(mybytearray, 0, count);
					cumulativeCount+= count;
					notifyUserDownload("Downloaded: " + (int)((cumulativeCount*100)/fileSize) + "% of file "
							+ currentFile + " of " + fileList.length);	
				}
				long fileDuration = System.currentTimeMillis() - fileStart;
				addToLog("\nReceived file: " + fileNameString
						   + "\tDuration: " + fileDuration +" ms"
						   + "\tfile size: " + fileSize + " bytes"
						   + " latency: " + ping + " ms");
				notifyUserDownload("Download current file complete");
				//measure the reading of bytes
				totalByteTransfer+= cumulativeCount;
				
				bos.flush();
				bos.close();

				System.out.println("Data reading complete, total bytes read: " + cumulativeCount);

				//System.out.println("time taken = " + String.valueOf(end-start) + "ms");
				filesLeft--;
			}		
			
			//Sending message to device to end session
			byte[] action = {0};
			oos.write(action,0,1);
			oos.flush();
			
			long sessionComplete = System.currentTimeMillis() - sessionStart;
			addToLog("\nSession complete:\nSession length " + sessionComplete + "ms\n"
					  + "Session transfer amount: " + (totalByteTransfer+1) + "bytes" 
					  + "\n-------------------------------------------");
			writeLog("SocketClient 5mb - 1 Client");
			
			notifyUserDownload("download idle");


		} catch (FileNotFoundException e) {
			System.out.println("FileNotFound exception");
			e.printStackTrace();
		}catch (UnknownHostException e) {
			System.out.println("Unknown host exception");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("IOException!");
			e.printStackTrace();
		} catch (ArrayIndexOutOfBoundsException e){
			System.out.println("arrayoutofbounds exception!");
			e.printStackTrace();
		}

		//house cleaning of streams and socket
		try{							
			System.out.println("closing connections");							
			oos.close();
			bos.close();
			connection.close();
		}
		catch(IOException e){
			System.out.println("Error closing streams, IOException");
			e.printStackTrace();			
		} catch(NullPointerException npe){
			npe.printStackTrace();
		}
	}	
	
	//SECOND RUN METHOD TO TEST END OF FILE MARKER
	//Initialize the socket connection and file transfer
	public void runEndOfFileMarker(){

			int totalByteTransfer = 0;
			addToLog("New Session:Client - 1 - End of file marker, " + "Buffer size = 16384 bytes -------------------------" + "\n"
					 + new Date().toString());

			//Opening The socket & sending the file------------------------------------------------------
			//System.out.println("Starting file sending process");  		

			Socket connection = null;
			BufferedOutputStream oos = null;
			BufferedOutputStream bos = null;

			try {

				long sessionStart = System.currentTimeMillis();
				byte [] mybytearray;							
				int filesLeft = fileList.length;
				int currentFile = 0;						



				while(filesLeft > 0){

					connection = new Socket (address, port);
					notifyUserDownload("Downloading file:" + (currentFile+1) + " of " + (fileList.length));
					/*initialising messages to device 
					  what action to be taken,
					  the length of the filename and the filename*/
					byte[] action = {1};
					byte[]fileName = fileList[currentFile].getBytes();
					String fileNameString = fileList[currentFile++];
					int filenameLength = fileName.length;							
					byte[] nameLength = ByteBuffer.allocate(4).putInt(filenameLength).array();

					//sending message to device
					long msgStart = System.currentTimeMillis();
					oos = new BufferedOutputStream(connection.getOutputStream());
					oos.write(action, 0 , 1);
					oos.write(nameLength,0,4);
					oos.write(fileName,0, filenameLength);
					//test the latency of the connection
					long pingStart = new Date().getTime();
					oos.write(longToBytes(pingStart), 0, 8);
					oos.flush();
					long msgDuration = System.currentTimeMillis() - msgStart;

					//measure the amount of bytes sent
					addToLog("\nSent Message" + "\nDuration: " + msgDuration + " ms"
							+ "\nSize: " + (1+4+filenameLength) + " bytes");

					byte[] pingData = new byte[8];
					//stream initialisation
					InputStream is = connection.getInputStream();
					//read ping data
					is.read(pingData, 0, 8);
					long ping = (new Date().getTime() - bytesToLong(pingData));
					
					File receivedFile;	
					
					byte[] fileSizeByte= new byte[4];
					is.read(fileSizeByte, 0 , 4);
					
					//measure the reading of bytes
					totalByteTransfer+= 4;
					long fileSize = getIntFromByte(fileSizeByte);
					System.out.println("Filesize: " + fileSize);
					receivedFile = new File(path, "/received_" + fileNameString);				
					bos = new BufferedOutputStream(new FileOutputStream(receivedFile));

					//receive data from stream
					System.out.println("data reading started of " + fileNameString);
					int count;
					long cumulativeCount = 0;

					//measure the reading of bytes
					totalByteTransfer+= 4;
					mybytearray = new byte [1024];

					long fileStart= System.currentTimeMillis();

					//read in file (read in exact amount of bytes that the file contains)
					while( (count = is.read(mybytearray)) > 0) {				

						bos.write(mybytearray, 0, count);
						cumulativeCount+= count;
						notifyUserDownload("Downloaded: " + (int)((cumulativeCount*100)/fileSize) + "% of file "
																	+ currentFile + " of " + fileList.length);	
					}
					long fileDuration = System.currentTimeMillis() - fileStart;
					addToLog("\nReceived file: " + fileNameString
							   + "\tDuration: " + fileDuration +" ms"
							   + "\tfile size: " + fileSize + " bytes"
							   + " latency: " + ping + " ms");
					//notifyUserDownload("Download file complete");
					//measure the reading of bytes
					totalByteTransfer+= cumulativeCount;

					is.close();
					bos.flush();
					bos.close();

					System.out.println("Data reading complete, total bytes read: " + cumulativeCount);

					//System.out.println("time taken = " + String.valueOf(end-start) + "ms");
					filesLeft--;



					notifyUserDownload("download idle");
					
					//house cleaning of streams and socket
					try{							
						System.out.println("closing connections");
						is.close();
						oos.close();
						bos.close();
						connection.close();
					}
					catch(IOException e){
						System.out.println("Error closing streams, IOException");
						e.printStackTrace();			
					} catch(NullPointerException npe){
						npe.printStackTrace();
					}
				}

				long sessionComplete = System.currentTimeMillis() - sessionStart;
				addToLog("\nSession complete:\nSession length " + sessionComplete + "ms\n"
						+ "Session transfer amount: " + (totalByteTransfer+1) + "bytes" 
						+ "\n-------------------------------------------");
				writeLog("SocketClient 5mb - 1 Client");

			} catch (FileNotFoundException e) {
				System.out.println("FileNotFound exception");
				e.printStackTrace();
			}catch (UnknownHostException e) {
				System.out.println("Unknown host exception");
				e.printStackTrace();
			} catch (IOException e) {
				System.out.println("IOException!");
				notifyUserDownload("IOException");
				e.printStackTrace();
			} catch (ArrayIndexOutOfBoundsException e){
				System.out.println("arrayoutofbounds exception!");
				e.printStackTrace();
			}

			//house cleaning of streams and socket
			try{							
				System.out.println("closing connections");							
				oos.close();
				bos.close();
				connection.close();
			}
			catch(IOException e){
				System.out.println("Error closing streams, IOException");
				e.printStackTrace();			
			} catch(NullPointerException npe){
				npe.printStackTrace();
			}
		}	
	
	
	//convert a long to a byte[]
	public byte[] longToBytes(long x) {
	    ByteBuffer buffer = ByteBuffer.allocate(8);
	    buffer.putLong(x);
	    return buffer.array();
	}

	//convert a byte[] to a long
	public long bytesToLong(byte[] bytes) {
	    ByteBuffer buffer = ByteBuffer.allocate(8);
	    buffer.put(bytes);
	    buffer.flip();//need flip 
	    return buffer.getLong();
	}
	
	
	
	//add a line to the log string
	private void addToLog(String logMessage){
		if(logText == null)
			logText= "";
		logText= logText + "\n" + logMessage;
	}
	
	//output log string to text file
	private void writeLog(String stringFileName){
		System.out.println("writing log");
		String fileName = stringFileName + " " + new Date().toString() + ".txt";
		
		fileName = fileName.trim();
		File logDirectory = new File(path, "logs");
		logDirectory.mkdir();
		File log = new File(logDirectory, fileName);
		
				
		
		System.out.println(fileName);
		try {
			log.createNewFile();
			PrintWriter out = new PrintWriter(log);
			out.println(logText);
			
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			
			e.printStackTrace();
		}
	}

	
	
	
	//print text to screen
	public void notifyUserDownload(final String msg) {
    	
      handler.postDelayed(new Runnable() {
            public void run() {
            	downloadText.setText(msg);
            	}
            }, 1);
    }
}


