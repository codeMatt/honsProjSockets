package com.heeere.android.dnssdtuto;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ServerSocketHandler extends Thread{
	
	private List<SocketServer> serverConnections = new ArrayList<SocketServer>();
	private ServerSocket connection;
	private File path;
	private int connections;

	public ServerSocketHandler(File file){
		path = file;
		connections = 0;
		
		try {
			connection = new ServerSocket(5000);
		} catch (IOException e) {
			System.out.println("Error setting up ServerSocket");
			e.printStackTrace();
		}
	}
	
	public int getConnections(){
		return connections;
	}
	
	public ServerSocket getSocket(){
		return connection;
	}
	
	public void run(){
		
		Socket sock;
		System.out.println("Server handler started");
		
		 while (true) {
	         try {
				sock = connection.accept(); //block until a client connects
				System.out.println("Server instance started");
				
				SocketServer server = new SocketServer(sock, path);				
				serverConnections.add(server);
				
				connections++;
				server.run();
				
			} catch (IOException e) {
				System.out.println("Error in serversockethandler, io exc");// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		 }
	}
}
