package com.heeere.android.dnssdtuto;

//A server set up on socket programming to listen for a connection
//and receive message and transfer files
//matthew watkins
//September 2013

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Date;

import android.os.Looper;
import android.widget.TextView;

public class SocketServer extends Thread{

	private Socket sock;
	private File path;    
    private String logText;
    private int endOfFileProtocol = 0;
    private final int BUFFER_SIZE = 16 * 1024;
	
	//default constructor
	public SocketServer(Socket socket, File filePath, TextView text, int choice){			
			sock = socket;
			path = filePath;
			endOfFileProtocol = choice;
	}
	
	public void run(){
		if (endOfFileProtocol == 1){
			runFileSizeBlock();
		}
		else if (endOfFileProtocol == 2){
			runEndOfFileMarker();
		}
	}
	
	//run method of thread
	//file length block
	public void runFileSizeBlock(){		
		
		long sessionStart = System.currentTimeMillis();
		
		try{
			Looper.prepare();
			}catch(Exception e){
				System.out.println("looper error");
			}
		
		String date = new Date().toString();
		addToLog("New Session: Socket - Server 1 clients -------------------------" + "\n"
				 +"at: " + date + "\nFile Length Block: File Size Block\tBuffer Size: " + BUFFER_SIZE);
		
		long totalFileTransfer = 0;
		
		//set up streams
		BufferedInputStream inSocket = null;
		BufferedInputStream inFile = null;

		try{

			String[] messages = new String[2];

			//receive message from other device
			inSocket = new BufferedInputStream(sock.getInputStream());
			messages = getMessagesFromDevice(inSocket);
			
			byte[] pingData = new byte[8];
			inSocket.read(pingData,0,8);
			OutputStream os = sock.getOutputStream();
			os.write(pingData,0,8);
			
			while(!(messages[0].equals("finish"))){
				
				System.out.println("got message, sending file: " + messages[1]);
				long fileSendStart = System.currentTimeMillis();
				
				//set up file
				path.mkdirs();
				File myFile = new File (path, messages[1]);
				int fileLength = (int) myFile.length();
				totalFileTransfer+=fileLength;
				byte[] fileSize = getByteFromInt(fileLength);
				

				//make streams for file reading
				byte [] fileArray = new byte [BUFFER_SIZE];
				inFile = new BufferedInputStream(new FileInputStream(myFile), 1024);
				
				//make stream for file sending over socket
				
				os.write(fileSize, 0 , 4);
				os.flush();
				
				int count = 0;
				
				while((count = inFile.read(fileArray))> 0){					
					os.write(fileArray, 0, count);				
				}
				os.flush();
				long fileReadTime = System.currentTimeMillis() - fileSendStart;
				System.out.println("send complete");	
				System.out.println("send duration: " + fileReadTime + "ms");
				System.out.println("length of current file being sent: " + fileLength);
				addToLog("\nSent file: " + messages[1]
					   + "\tDuration: " + fileReadTime +" ms"
					   + "\tfile size: " + fileLength + " bytes");

				System.out.println("sent File, receiving text message");

				//receive next message from other device
				messages = getMessagesFromDevice(inSocket);
				
				//read and send ping data for latency test
				inSocket.read(pingData,0,8);
				os.write(pingData,0,8);
			}
			
			//house cleaning of streams and socket
			inFile.close();
			inSocket.close();
			sock.close();
			
			long sessionTime = System.currentTimeMillis() - sessionStart;
			addToLog("\nSession complete:\nSession length " + sessionTime + "ms\t"
									  + "Session transfer amount: " + totalFileTransfer + "bytes" 
									  + "\n-------------------------------------------");
			writeLog();
			System.out.println("files transferred, closing connections");

		}catch (UnknownHostException e) {
			System.out.println("Unknown host exception");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Error sending file, IOException");
			e.printStackTrace();
		}
		
		//if exception hits, still do house cleaning
		finally{
			System.out.println("closing connections in finally");
			//house cleaning of streams and socket
			try{
				inFile.close();
				inSocket.close();
				sock.close();
				}
			catch(Exception e){
				System.out.println("Error closing streams");
				e.printStackTrace();
			}
		}
	}
	
	//SECOND RUN METHOD, end of file marker
	//run method of thread
	public void runEndOfFileMarker(){		
		
		long sessionStart = System.currentTimeMillis();
		
		try{
			Looper.prepare();
			}catch(Exception e){
				System.out.println("looper error");
			}
		String date = new Date().toString();
		addToLog("New Session: Socket - Server 1 client -------------------------" + "\n"
				 + date + "\nEnd of file check: End of file marker\tBuffer Size: " + BUFFER_SIZE);
		
		long totalFileTransfer = 0;
		
		//set up streams
		BufferedInputStream inSocket = null;
		BufferedInputStream inFile = null;

		try{

			String[] messages = new String[2];

			//receive message from other device
			inSocket = new BufferedInputStream(sock.getInputStream());
			messages = getMessagesFromDevice(inSocket);
			
			//read the ping data from the client
			byte[] pingData = new byte[8];
			inSocket.read(pingData,0,8);
			
			//make stream for file sending over socket
			OutputStream os = sock.getOutputStream();
			//send the ping data back
			os.write(pingData,0,8);
			
			System.out.println("got message, sending file: " + messages[1]);

			//set up file
			path.mkdirs();
			File myFile = new File (path, messages[1]);
			int fileLength = (int) myFile.length();
			totalFileTransfer+=fileLength;
			byte[] fileSize = getByteFromInt(fileLength);
			System.out.println("length of current file sent: " + fileLength);
			
			//initialise stream for file reading
			byte [] fileArray = new byte [BUFFER_SIZE];
			inFile = new BufferedInputStream(new FileInputStream(myFile), 8 * 1024);

			//send file size to client
			os.write(fileSize, 0 , 4);
			os.flush();
			
			//set up variables used in sending process
			int count = 0;			
			long fileSendStart = System.currentTimeMillis();
			while((count = inFile.read(fileArray)) > 0){
				os.write(fileArray, 0, count);
			}
			os.flush();
			long fileSendTime = System.currentTimeMillis() - fileSendStart;
		
			System.out.println("send complete");	
			System.out.println("send duration: " + fileSendTime + "ms");
			addToLog("\nSent file: " + messages[1]
					+ "\tDuration: " + fileSendTime +" ms"
					+ "\tfile size: " + fileLength + " bytes");


			//house cleaning of streams and socket
			inFile.close();
			inSocket.close();
			sock.close();
			
			long sessionTime = System.currentTimeMillis() - sessionStart;
			addToLog("\nSession complete:\nSession length " + sessionTime + "ms\t"
									  + "Session transfer amount: " + totalFileTransfer + "bytes" 
									  + "\n-------------------------------------------");
			writeLog();
			System.out.println("files transferred, closing connections");

		}catch (UnknownHostException e) {
			System.out.println("Unknown host exception");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Error sending file, IOException");
			e.printStackTrace();
		}
		
		//if exception hits, still do house cleaning
		finally{
			System.out.println("closing connections in finally");
			//house cleaning of streams and socket
			try{
				inFile.close();
				inSocket.close();
				sock.close();
				}
			catch(Exception e){
				System.out.println("Error closing streams");
				e.printStackTrace();
			}
		}
	}

	//method to receive a message from a device running the same application
	private String[] getMessagesFromDevice(BufferedInputStream ois){
		long messageStart = System.currentTimeMillis();
		
		String[] messages = new String[2];
		int fileNameLengthInt = 0;
		
		try{
			byte[] intention = new byte[1];							
			ois.read(intention, 0 , 1);

			if(intention[0] == 1){							

				messages[0] = "get";
				byte[] fileNameLength= new byte[4];	
				ois.read(fileNameLength,0,4);			

				fileNameLengthInt = getIntFromByte(fileNameLength);

				byte[] filename = new byte[fileNameLengthInt];
				ois.read(filename, 0 , fileNameLengthInt);

				messages[1] = new String (filename);

			}
			else{
				messages[0] = "finish";
				messages[1] = "";
			}
		}catch(IOException e){
			System.out.println("Error recieving messages");
			messages[0] = "finish";
			messages[1] = "";
		}			
		
		String messageDuration = "\nMessages received from client\tDuration: " + (System.currentTimeMillis() - messageStart) + " ms\t" +
													"size: " + (fileNameLengthInt + 5)  + " bytes";
		addToLog(messageDuration);
		
		return messages;			
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

	//convert an integer into a byte[]
	private byte[] getByteFromInt(int number){
		
		ByteBuffer buffer = ByteBuffer.allocate(4);
		buffer.putInt(number);		
		byte[] array = buffer.array();
		
		return array;
	}
	
	//add a line to the log
	private void addToLog(String logMessage){
		if(logText == null)
			logText= "";
		logText= logText + "\n" + logMessage;
	}
	
	//output log string to text file
	private void writeLog(){
		System.out.println("writing log");
		String fileName = "log  Socket Server 3 client - " + new Date().toString() + ".txt";
		File logDirectory = new File(path, "logs");
		logDirectory.mkdir();
		File log = new File(logDirectory, fileName);
		
		
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
	
	//close the sockets
	public boolean closeSocket(){
		try {
			if(sock != null)
				sock.close();
		} catch (IOException e) {			
			System.out.println("Error closing server socket");
			e.printStackTrace();
			return false;			
		}		
		return true;
	}
	
}
