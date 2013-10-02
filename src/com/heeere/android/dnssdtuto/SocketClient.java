package com.heeere.android.dnssdtuto;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import android.os.Environment;

public class SocketClient extends Thread {

	File path;
	String address;
	String[] fileList;
	int port;

	public SocketClient(String socketAddress, File filePath, String[] fileList, int socketPort){
		path = filePath;
		address = socketAddress;
		this.fileList = fileList;
		port = socketPort;
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

	//Initialize the socket connection and file transfer
	public void run(/*final String address, final int port, final String[] fileList*/){

		System.out.println("File receiving initiated From: " + address);	


		//Opening The socket & sending the file------------------------------------------------------
		//System.out.println("Starting file sending process");  		

		Socket connection = null;
		BufferedOutputStream oos = null;
		BufferedOutputStream bos = null;
		FileOutputStream fos= null;

		try {

			byte [] mybytearray;
			long start = System.currentTimeMillis();												
			int filesLeft = fileList.length;
			int currentFile = 0;						
			connection = new Socket (address, port);  		

			while(filesLeft > 0){

				//Sending messages to device 
				//including what action to be taken, the length of the filename and the filename
				byte[] action = {1};
				byte[]fileName = fileList[currentFile++].getBytes();
				int filenameLength = fileName.length;							
				byte[] nameLength = ByteBuffer.allocate(4).putInt(filenameLength).array();

				oos = new BufferedOutputStream(connection.getOutputStream());
				oos.write(action, 0 , 1);
				oos.write(nameLength,0,4);
				oos.write(fileName,0, filenameLength);
				oos.flush();

				//stream initialisation
				InputStream is = connection.getInputStream();
				File receivedFile;
				if(isExternalStorageWritable()){
					receivedFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) , "/received_" + currentFile + ".txt");
				}
				else{
					receivedFile = new File(path, "/received_" + fileName + currentFile + ".txt");
				}
				fos = new FileOutputStream(receivedFile);
				bos = new BufferedOutputStream(fos);

				//receive data from stream
				System.out.println("data reading started");
				int count;

				byte[] fileSizeByte= new byte[4];
				is.read(fileSizeByte, 0 , 4);
				int fileSize = getIntFromByte(fileSizeByte);
				mybytearray = new byte [fileSize];

				//read in file (read in exact amount of bytes that the file contains)
				while ((count = is.read(mybytearray)) < fileSize) {
					is.read(mybytearray, count, (mybytearray.length-count));								
				}
				bos.write(mybytearray); //might have to include this write in the loop for bigger files
										//would be bos.write(mybytearray, count, (mybytearray.length-count));
				bos.flush();

				System.out.println("Data reading complete");
				long end = System.currentTimeMillis();
				System.out.println("time taken = " + String.valueOf(end-start) + "ms");
				filesLeft--;
			}

			//Sending message to device to end session
			byte[] action = {0};
			oos.write(action,0,1);
			oos.flush();


		} catch (FileNotFoundException e) {
			System.out.println("FileNotFound exception");
			e.printStackTrace();
		}catch (UnknownHostException e) {
			System.out.println("Unknown host exception");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("IOException!");
			e.printStackTrace();
		}

		//house cleaning of streams and socket
		try{							
			System.out.println("closing connections");							
			oos.close();
			bos.close();
			fos.close();
			connection.close();
		}
		catch(IOException e){
			System.out.println("Error closing streams, IOException");
			e.printStackTrace();			
		} catch(NullPointerException npe){
			npe.printStackTrace();
		}

	}
}


