package com.heeere.android.dnssdtuto;
//This is main activity class in an android app that registers a jmdns service
//and transfers files between registered devices
//matthew watkins
//August 2013

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import android.app.Activity;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.bluetooth.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class DnssdDiscovery extends Activity {

	//instance variables for handling jmdns
    android.net.wifi.WifiManager.MulticastLock lock;
    android.os.Handler handler = new android.os.Handler();
    EditText nameText;
    ArrayList<DeviceInstance> deviceList = new ArrayList<DeviceInstance> ();
    int deviceCount = 0, ipAddress;
    ArrayAdapter<DeviceInstance> spinnerArrayAdapter; 
    private final static int REQUEST_ENABLE_BT = 1;
    ServerSocketHandler FileServer;
    
    //called when the activity is first created.
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        //acquire lock for device discovery
        WifiManager wifi = (WifiManager)
        		getSystemService(android.content.Context.WIFI_SERVICE);
        
        lock = wifi.createMulticastLock("mylockthereturn");
        lock.setReferenceCounted(true);
        lock.acquire();
        
        ipAddress = wifi.getConnectionInfo().getIpAddress();

        //UI Features------------------------------------------------
        //enter device name textbox
        nameText = (EditText) findViewById(R.id.nameString); 
        
        //register your service button ----------------------------
        Button registerButton = (Button) findViewById (R.id.button1);
        registerButton.setOnClickListener(new OnClickListener() {
        	 
			@Override
			public void onClick(View arg0) {
				
				deviceName = (String)nameText.getText().toString();

				FileServer = new ServerSocketHandler(getFilesDir());
				FileServer.start();
				showIP();
				
				/*try {
					register();
				} catch (IOException e) {
					System.out.println("Error parsing name");
					e.printStackTrace();
				}*/
			} 
		});      
        
        //get file button  ------------------------------
        Button getFileButton = (Button) findViewById (R.id.button2);
        getFileButton.setOnClickListener(new OnClickListener() {       	 
			@Override
			public void onClick(View arg0) {
				EditText address = (EditText) findViewById (R.id.addressString);   
				String tempAddress = address.getText().toString();				
				String[] files = {"newTestFile.txt"};
				
				
				SocketClient getFile = new SocketClient(tempAddress, getFilesDir(), files, 5000);
				
				getFile.start();
				
				//initiateFileTransfer(tempAddress, 5000, files);				
			} 
		});       
 
        //get multiple files button ------------------------------
        Button getFilesButton = (Button) findViewById (R.id.button3);
        getFilesButton.setOnClickListener(new OnClickListener() {
       	 
			@Override
			public void onClick(View arg0) {	
					
				EditText address = (EditText) findViewById (R.id.addressString);
				String[] files = { "newTestFile.txt", "secondFile.txt"};				
				String tempAddress = address.getText().toString();
				
				
				SocketClient getFiles = new SocketClient(tempAddress, getFilesDir(), files, 5000);
				
				getFiles.start();
				

				
				//initiateFileTransfer(tempAddress, 5000, files);
				} 
		});
       
        //start the jmdns initialisation, separate thread as networking cannot be done on main thread
        /*new Thread(){
        	public void run() {        		
        		setUp();        		
        		}
        	}.start();*/
        	
       createDummyFile();
    }   

    private String type = "_share._tcp.local.";
    private JmDNS jmdns = null;
    private ServiceListener listener = null;
    private ServiceInfo serviceInfo;
    private String deviceName = "name";
    private ArrayList<String> bluetoothDevices;
    BluetoothAdapter bluetooth;
    BroadcastReceiver receiver;
    
    //method to set up the jmdns service and service listener
	private void setUp() {
		        	
        try {
        	
        	InetAddress setUpAddress = InetAddress.getByAddress(convertToByteAddress(ipAddress));
        	
            jmdns = JmDNS.create(setUpAddress);
            System.out.println("create complete");
            registerJmdns();
            System.out.println("starting listening");
            jmdns.addServiceListener(type, listener = new ServiceListener() {
            	
            	public String[] serviceUrls = null;
            	
                @Override
                public void serviceResolved(ServiceEvent ev) {                	                   
                	serviceUrls = ev.getInfo().getURLs();    
                	deviceList.add(new DeviceInstance (ev.getName(), serviceUrls[0], ev.getInfo().getPort()));
                	notifyUser("New Service Available: " + ev.getName() + " Address: " + serviceUrls[0]);
                }

                @Override
                public void serviceRemoved(ServiceEvent ev) {
                	notifyUser("Service removed: " + ev.getName());
                }

                @Override
                public void serviceAdded(ServiceEvent event) {
                	notifyUser("Service added:");
                    // Required to force serviceResolved to be called again (after the first search)
                    jmdns.requestServiceInfo(event.getType(), event.getName(), 1);
                }
            });
            System.out.println("completed listening");
            
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }
	
	// Checks if external storage is available for read and write
	private boolean isExternalStorageWritable() {
	    String state = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(state)) {
	        return true;
	    }
	    return false;
	}
	
	//convert integer ip address into byte array
	private byte[] convertToByteAddress(int address){
		byte[] byAddress;	
		byAddress = new byte[] {
				(byte) (address & 0xff),
				(byte) (address >> 8 & 0xff),
				(byte) (address >> 16 & 0xff),
				(byte) (address >> 24 & 0xff),				
		};
		return byAddress;		
	}

	//method to print out the ip address of 
	private void showIP(){
		
		
		String address =  (ipAddress & 0xFF ) + "." +
	               ((ipAddress >> 8 ) & 0xFF) + "." +
	               ((ipAddress >> 16 ) & 0xFF) + "." +
	               ( (ipAddress >> 24 ) & 0xFF) ;
		
		
		
		notifyUser("IP Address: " + address);
	}
	
	//call all the relevant methods for the bluetooth discovery
	private void initialiseBluetooth(){
		
		bluetoothDevices = new ArrayList<String>();
		
		if(setUpBluetooth()){
			discoverBluetoothDevices();
			bluetoothAdvertiseOn();
		}
		else{
			System.out.println("failed to initialise bluetooth");
		}
	}
	
	//bluetooth setup method
	private boolean setUpBluetooth(){
		
		bluetooth = BluetoothAdapter.getDefaultAdapter();
		
		//get user to enable bluetooth
		if(!(bluetooth.isEnabled())){
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}	
		
		if(bluetooth != null && bluetooth.isEnabled())
		{
		    // Continue with bluetooth setup.
			bluetooth.setName(deviceName);
			return true;
		}
		else{
			System.out.println("bluetooth disabled or unavailable!");
			return false;
		}
	}
	
	//discover bluetooth devices nearby
	private void discoverBluetoothDevices(){
		
		bluetooth.startDiscovery();
		
		// Create a BroadcastReceiver for ACTION_FOUND
		receiver = new BroadcastReceiver() {
			
		    public void onReceive(Context context, Intent intent) {
		        String action = intent.getAction();
		        // When discovery finds a device
		        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
		        	System.out.println("Found BT DEV");
		            // Get the BluetoothDevice object from the Intent
		            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
		            if(device != null){
		            	bluetoothDevices.add(device.getName());
		            	notifyUser("new BT Dev: " + device.getName());
		            }
		        }
		    }
		};
		
		// Register the BroadcastReceiver
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		registerReceiver(receiver, filter); // Don't forget to unregister during onDestroy
		
	}
	
	//make this device discoverable on bluetooth
	private void bluetoothAdvertiseOn(){
		
		//this shows a pop up for bluetoth permission that the user has to input "yes"
		Intent discoverableIntent = new
				Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
				discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
				startActivity(discoverableIntent);
	}
	
	//make this device undiscoverable on bluetooth after 1 second
	private void bluetoothAdvertiseOff(){
		
		//this shows a pop up for bluetoth permission that the user has to input "yes"
		Intent discoverableIntent = new
				Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
				discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 1);
				startActivity(discoverableIntent);
	}
	
	//method to set up the registration of a jmdns service
	//and the socket host
	private void registerJmdns() throws IOException{
		final int port = 5000;		
		//register service on jmdns protocol
		serviceInfo = ServiceInfo.create("_share._tcp.local.", deviceName, port, "share_service");
		jmdns.registerService(serviceInfo);
		System.out.println("registering complete");		
	}

	//Initialize the socket connection and file transfer
	private void initiateFileTransfer(final String address, final int port, final String[] fileList){

			notifyUser("File receiving initiated From: " + address);	
			Thread fileSending = new Thread (){
				public void run(){
					
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
							
							//stream managements
							InputStream is = connection.getInputStream();
							//System.out.println("inputstream instantiated: ");
							
							File receivedFile;
							
							if(isExternalStorageWritable()){
								receivedFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) , "/received_" + currentFile + ".txt");
							}
							else{
								receivedFile = new File(getFilesDir(), "/received_" + fileName + currentFile + ".txt");
							}
														
							fos = new FileOutputStream(receivedFile);
							bos = new BufferedOutputStream(fos);

							//receive data from stream
							System.out.println("data reading started");
							int count;
							
							byte[] fileSizeByte= new byte[4];
							
							is.read(fileSizeByte, 0 , 4);
							
							int fileSize = getIntFromByte(fileSizeByte);
							System.out.println("Current file size: " + fileSize);							
							mybytearray = new byte [fileSize];
							
							//read in file (read in exact amount of bytes that the file contains)
							while ((count = is.read(mybytearray)) < fileSize) {
								is.read(mybytearray, count, (mybytearray.length-count));								
							}
							bos.write(mybytearray);//might have to include this write in the loop for bigger files
												   //would be bos.write(mybytearray, count, (mybytearray.length-count));
							bos.flush();
							fos.flush();

							System.out.println("Data reading complete");
							
							//write data to array
							//bos.write(mybytearray, 0 , current);
							bos.flush();
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
							notifyUser("closing connections");							
							oos.close();
							bos.close();
							fos.close();
							connection.close();
						} catch(IOException e){
							System.out.println("Error closing streams, IOException");
							e.printStackTrace();
						}
					
				}
			};
			fileSending.start();
		}
	
	//print text to screen
	private void notifyUser(final String msg) {
    	
      handler.postDelayed(new Runnable() {
            public void run() {
            	TextView t = (TextView)findViewById(R.id.text);
            	t.setText(t.getText()+"\n-"+msg);
            	}
            }, 1);

    }
 
	//convert an byte[] into a integer
	private int getIntFromByte(byte[] array){
		
		//System.out.println("in getIntFromByte, array size = " + array.length);
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

	//method to make dummy files for file transfers
    private void createDummyFile(){
    	
    	
    	String text = "This is a tester file created in the app dnssddemo";
    	String text2 = "This is a tester file created in the app dnssddemo, it is a longer file than the first file for testing purposes, just for me. I don't like have good grammar";
		
    	File path;
		
		if(isExternalStorageWritable()){
			path = (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
		}
		else{
			path = getFilesDir();
		}
    	
    	
		File file = new File (path, "newTestFile.txt");
		File file2 = new File (path, "secondFile.txt");
    	
    	try {
    		byte[] data = text.getBytes("UTF8");
    		byte[] data2 = text2.getBytes("UTF8"); 
			file.createNewFile();
			file2.createNewFile();
			if(file.exists() && file2.exists())
			{
			     OutputStream fo = new FileOutputStream(file);              
			     fo.write(data);
			     fo.close();
			     
			     OutputStream fo2 = new FileOutputStream(file2);              
			     fo2.write(data2);
			     fo2.close();
			     System.out.println("2 files created: ");
			     
			}			
		} catch (FileNotFoundException e) {
			System.out.println("dummy file creation failed");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("dummy file creation failed");
			e.printStackTrace();
		}
    }

    @Override
    protected void onStart() {
        super.onStart(); 
    }

    @Override
    //The method to remove the jmdns services when the application ends
    protected void onStop() {
    	
    	if (jmdns != null) {
    		if (listener != null) {
    			jmdns.removeServiceListener(type, listener);
    			listener = null;
    		}
    		jmdns.unregisterAllServices();
    		try {
    			jmdns.close();
    		} catch (IOException e) {
    			System.out.println("Error closing jmdns");
    			e.printStackTrace();
    		}
    		jmdns = null;
    	}
    	
    	//close server socket connection
		if(FileServer != null && FileServer.isInterrupted()){
			try{
			FileServer.getSocket().close();}
			catch(IOException ioe){
				System.out.println("Failed to close server socket");
			}
		}
    	
    	//turn of discovery
    	if(bluetooth!=null){
    		bluetooth.cancelDiscovery();
    		bluetoothAdvertiseOff();
    	}
    	
    	if(receiver != null)
    		this.unregisterReceiver(receiver);
    	
    	//repo.stop();
    	//s.stop();
    	if(lock.isHeld())
    		lock.release();
    	
    	super.onStop();
    }
}