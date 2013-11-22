package com.heeere.android.dnssdtuto;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import javax.net.ServerSocketFactory;
import android.widget.TextView;

public class ServerSocketHandler extends Thread{
	
	private ServerSocket connection;
	private File path;
	private int connections, PROTOCOL_CHOICE = 1;
	private Socket sock;
	private TextView textBox;
	private ServerSocketFactory serverSocketFactory = ServerSocketFactory.getDefault();

	public ServerSocketHandler(File file, TextView text, int choice){
		path = file;
		connections = 0;
		textBox = text;
		PROTOCOL_CHOICE = choice;
		
		try {
			connection = serverSocketFactory.createServerSocket(5000);
		}catch (IOException e) {
			System.out.println("Error setting up ServerSocket");
			e.printStackTrace();
		}
		System.out.println("Socket is instantiated, on port 5000");
	}
	
	public int getConnections(){
		return connections;
	}
	
	public ServerSocket getSocket(){
		return connection;
	}
	
	//method to run while while the application is running
	//and wait for a client to connect
	public void run(){
		
		System.out.println("Server handler started");
		
		while (true) {
			try {
				
				Socket clientConnection = null;
				
				clientConnection = connection.accept();
				
				new Thread(new SocketServer(clientConnection, path, textBox, PROTOCOL_CHOICE)).start();
				
			} catch(SocketException se){
				System.out.println("socket exception, most likely socket closed");
				//se.printStackTrace();
				return ;
				
			} catch (IOException e) {
				System.out.println("Error in serversockethandler, io exc");
				e.printStackTrace();
				return ;
			}
		 }
	}
	
	//method to close the server socket to end the thread when the application stops
	public boolean closeSocket(){
		
		try{
			if(sock != null)
				sock.close();
			if(connection !=null)
				connection.close();
		}catch(Exception e){
			System.out.println("unable to close stream");
			e.printStackTrace();
			return false;
		}		
		
		return true;
	}
}
