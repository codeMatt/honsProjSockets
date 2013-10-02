package com.heeere.android.dnssdtuto;
//This is main activity class in an android application that registers a jmdns service
//and transfers files between registered devices
//matthew watkins
//August 2013

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import android.app.Activity;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class DnssdDiscovery extends Activity {

	//instance variables for handling jmdns
    private android.net.wifi.WifiManager.MulticastLock lock;
    private android.os.Handler handler = new android.os.Handler();
    private EditText nameText;
    private ArrayList<DeviceInstance> deviceList = new ArrayList<DeviceInstance> ();
    private int  ipAddress;
    private ServerSocketHandler FileServer;
    private BluetoothDiscoverer bTDisc;
    private Context context;
    private File path;
    
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
        context = this;
        
		if(isExternalStorageWritable()){
			path = (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
		}
		else{
			path = getFilesDir();
		}
                
        //UI Features------------------------------------------------
        //enter device name textbox
        nameText = (EditText) findViewById(R.id.nameString); 
        
        //register your service button ----------------------------
        Button registerButton = (Button) findViewById (R.id.button1);
        registerButton.setOnClickListener(new OnClickListener() {
        	 
			@Override
			public void onClick(View arg0) {
				
				deviceName = (String)nameText.getText().toString();

				FileServer = new ServerSocketHandler(path);
				FileServer.start();	
				
				bTDisc = new BluetoothDiscoverer(deviceName, context);
				bTDisc.start();
				
				showIP();
				
		        new Thread(){
	        	public void run() {        		
	        		setUp();        		
	        		}
	        	}.start();

			}
		});      
        
        //get db file button  ------------------------------
        Button getFileButton = (Button) findViewById (R.id.button2);
        getFileButton.setOnClickListener(new OnClickListener() {       	 
			@Override
			public void onClick(View arg0) {
				EditText address = (EditText) findViewById (R.id.addressString);   
				String tempAddress = address.getText().toString();				
				String[] files = {"newTestFile.txt"};		
				
				SocketClient getFile = new SocketClient(tempAddress, getFilesDir(), files, 5000);
				
				getFile.start();
				
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
				
				} 
		}); 
        
        
        
        	
       createDummyFile();
    }   

    private String type = "_share._tcp.local.";
    private JmDNS jmdns = null;
    private ServiceListener listener = null;
    private ServiceInfo serviceInfo;
    private String deviceName = "name";

    
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

	//method to print out the ip address of current device
	private void showIP(){
		
		//convert integer ip address into a string
		String address =  (ipAddress & 0xFF ) + "." +
	               ((ipAddress >> 8 ) & 0xFF) + "." +
	               ((ipAddress >> 16 ) & 0xFF) + "." +
	               ( (ipAddress >> 24 ) & 0xFF) ;
		
		
		
		notifyUser("IP Address: " + address);
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
	
	//print text to screen
	private void notifyUser(final String msg) {
    	
      handler.postDelayed(new Runnable() {
            public void run() {
            	TextView t = (TextView)findViewById(R.id.text);
            	t.setText(t.getText()+"\n-"+msg);
            	}
            }, 1);
    }

	//method to make dummy files for file transfers
    private void createDummyFile(){
    	
    	
    	String text = "This is a tester file created in the app dnssddemo";
    	String text2 = "This is a tester file created in the app dnssddemo, it is a longer file than the first file for testing purposes, just for me. I don't like have good grammar";
		
    	
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
    protected void onDestroy() {

    	//close server socket connection
    	if(FileServer != null)
    		FileServer.closeSocket();

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

    	//turn of bluetooth
    	if(bTDisc!=null){
    		bTDisc.shutdownBluetooth();
    	}

    	if(lock.isHeld())
    		lock.release();

    	super.onStop();
    }
}