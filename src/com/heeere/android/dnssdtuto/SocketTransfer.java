package com.heeere.android.dnssdtuto;
//This is main activity class in an android application that registers a jmdns service
//and transfers files between registered devices
//matthew watkins
//August 2013

import java.io.File;
import android.app.Activity;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class SocketTransfer extends Activity {

	//instance variables for handling jmdns
    private android.net.wifi.WifiManager.MulticastLock lock;
    private android.os.Handler handler = new android.os.Handler();
    private EditText addressText;
    private TextView downloadText, uploadText;
    private int ipAddress;
    private ServerSocketHandler FileServer;
    private File path, pathFiles;
    private SocketClient getFile;
	private boolean started = false;
	
	//integer that determines the end of file technique
	//1 means file size, 2 means end of file marker
	private static int PROTOCOL_CHOICE = 1;
    
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
        if(isExternalStorageWritable()){
			File parentpath = (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
			path = new File(parentpath, "Socket_Test_Folder");
			pathFiles = new File(parentpath, "root");
		}
		else{
			path = getFilesDir();
		}
		path.mkdirs();
		System.out.println(path);
                
        //UI Features-----------------------------------------------------
		//----------------------------------------------------------------
        //enter device name textbox
       
        addressText = (EditText) findViewById( R.id.address_string);
        
        downloadText = (TextView) findViewById( R.id.text2);
        
        //register your service button ----------------------------
        Button registerButton = (Button) findViewById (R.id.button1);
        registerButton.setOnClickListener(new OnClickListener() {
        	 
			@Override
			public void onClick(View arg0) {
				if(!started){
					started=true;
					
					FileServer = new ServerSocketHandler(pathFiles, uploadText, PROTOCOL_CHOICE);
					FileServer.start();
					notifyUser("fileserver started");
					
					showIP();	
				}

			}
		});      
        
        //get db file button  ------------------------------
        Button getFileButton = (Button) findViewById (R.id.button2);
        getFileButton.setOnClickListener(new OnClickListener() {       	 
			@Override
			public void onClick(View arg0) {
  
				String tempAddress = addressText.getText().toString();

				
				String [] files = { "100mb number 1.mp3"};
				

				
				getFile = new SocketClient(tempAddress, path, files, 5000, downloadText, PROTOCOL_CHOICE);
				
				getFile.start();
				
			} 
		});       
		//--------------------------------------------------------------------------------------------------------
		//--------------------------------------------------------------------------------------------------------
        
        	
    }   

	
	// Checks if external storage is available for read and write
	private boolean isExternalStorageWritable() {
	    String state = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(state)) {
	        return true;
	    }
	    return false;
	}
	
	//method to print out the ip address of current device
	private void showIP(){
		
		//convert integer ip address into a string
		String address =  (ipAddress & 0xFF ) + "." +
	               ((ipAddress >> 8 ) & 0xFF) + "." +
	               ((ipAddress >> 16 ) & 0xFF) + "." +
	               ( (ipAddress >> 24 ) & 0xFF);
		
		notifyUser("IP Address: " + address);
	}

	//print text to screen
	private void notifyUser(final String msg) {
    	
      handler.postDelayed(new Runnable() {
            public void run() {
            	TextView t = (TextView)findViewById(R.id.text);
            	t.setGravity(Gravity.TOP);
            	t.setText(t.getText()+"\n-"+msg);
            	}
            }, 1);
    }
	
	//print text to screen
	public void notifyUserDownload(final String msg) {
    	
      handler.postDelayed(new Runnable() {
            public void run() {
            	TextView t = (TextView)findViewById(R.id.text2);
            	t.setText(msg);
            	}
            }, 1);
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

    	super.onStop();
    }
}