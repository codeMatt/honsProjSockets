package com.heeere.android.dnssdtuto;

//A server set up on socket programming to listen for a connection
//and receive message and transfer files
//matthew watkins
//September 2013

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class SocketServer implements Runnable{

	private Socket sock;
	File path;
	
	//default constructor
	public SocketServer(Socket socket, File filePath){
			
			sock = socket;
			path = filePath;			
		
	}
	
	//run method of thread
	public void run(){

		System.out.println("Socket server instance started");
		
		//set up streams
		BufferedInputStream ois = null;
		FileInputStream fis = null;
		BufferedInputStream bis = null;

		try{

			String[] messages = new String[2];

			//receive message from other device
			ois = new BufferedInputStream(sock.getInputStream());
			messages = getMessagesFromDevice(ois);
			
			
			while(!(messages[0].equals("finish"))){

				System.out.println("got message, sending file: " + messages[1]);

				//set up file
				path.mkdirs();
				File myFile = new File (path, messages[1]);
				int fileLength = (int) myFile.length();
				byte[] fileSize = getByteFromInt(fileLength);

				//read data from file into byte[]
				byte [] mybytearray = new byte [fileLength];
				fis = new FileInputStream(myFile);
				bis = new BufferedInputStream(fis, 1024);
				bis.read(mybytearray,0,mybytearray.length);	

				System.out.println("length of current file being sent: " + fileLength);

				//send data to other device from byte[]
				OutputStream os = sock.getOutputStream();
				os.write(fileSize, 0 , 4);
				os.write(mybytearray,0,mybytearray.length);
				os.flush();

				System.out.println("sent File, receiving text message");

				//receive next message from other device
				messages = getMessagesFromDevice(ois);
			}

			System.out.println("all files transferred...");
			System.out.println("closing connections");
			//house cleaning of streams and socket
			bis.close();
			ois.close();
			fis.close();									
			sock.close();

		}catch (UnknownHostException e) {
			System.out.println("Unknown host exception");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Error sending file, IOException");
			e.printStackTrace();
		}
		
		//if exception hits, still do housecleaning
		finally{
			System.out.println("closing connections in finally");
			//house cleaning of streams and socket
			try{
				bis.close();
				ois.close();
				fis.close();									
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

		String[] messages = new String[2];

		try{
			byte[] intention = new byte[1];							
			ois.read(intention, 0 , 1);

			if(intention[0] == 1){							

				messages[0] = "get";
				byte[] fileNameLength= new byte[4];	
				ois.read(fileNameLength,0,4);			

				int fileNameLengthInt = getIntFromByte(fileNameLength);

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
