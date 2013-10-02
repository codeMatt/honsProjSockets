package com.heeere.android.dnssdtuto;

import java.util.ArrayList;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Looper;

public class BluetoothDiscoverer extends Thread {
	
    private ArrayList<String> bluetoothDevices;
    BluetoothAdapter bluetooth;
    BroadcastReceiver receiver;
    private final static int REQUEST_ENABLE_BT = 1;
    private String deviceName;
    Context bContext;
	
    public BluetoothDiscoverer(String name, Context context){
    	
    	bContext = context;
    	deviceName = name;
    	bluetoothDevices = new ArrayList<String>();
    }
	
	public void run(){
		
		if(setUpBluetooth()){
			
			discoverBluetoothDevices();
			bluetoothAdvertiseOn();
		}
		else{
			System.out.println("failed to initialise bluetooth");
		}
	}
	
	//bluetooth
	private boolean setUpBluetooth(){
		
		Looper.prepare();
		
		bluetooth = BluetoothAdapter.getDefaultAdapter();
		
		//get user to enable bluetooth
		if(!(bluetooth.isEnabled())){
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    ((Activity) bContext).startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}	
		
		if(bluetooth != null && bluetooth.isEnabled())
		{
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
		            // Get the BluetoothDevice object from the Intent
		            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
		            if(device != null){
		            	bluetoothDevices.add(device.getName());
		            	System.out.println("new BT Dev: " + device.getName());
		            }
		        }
		    }
		};		
		// Register the BroadcastReceiver
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		((Activity) bContext).registerReceiver(receiver, filter); // Don't forget to unregister during onDestroy
		
	}
	
	//make this device discoverable on bluetooth
	private void bluetoothAdvertiseOn(){
		
		//this shows a pop up for bluetoth permission that the user has to input "yes"
		Intent discoverableIntent = new
				Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
				discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
				((Activity) bContext).startActivity(discoverableIntent);
	}
	
	//make this device undiscoverable on bluetooth after 1 second
	public void bluetoothAdvertiseOff(){
		
		//this shows a pop up for bluetoth permission that the user has to input "yes"
		Intent discoverableIntent = new
				Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
				discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 1);
				((Activity) bContext).startActivity(discoverableIntent);
	}
	
	//compare the jmdns found device name to the bluetooth discovered devices
	public boolean collocated(String name){		
		
		for(String device : bluetoothDevices){
			if (device.equals(name))
				return true;
		}		
		return false;
	}
	
	public void shutdownBluetooth(){
		
    	if(bluetooth!=null){
    		bluetooth.cancelDiscovery();
    		//bluetoothAdvertiseOff();
    		if(receiver!=null)
    			((Activity) bContext).unregisterReceiver(receiver);
    	}
	}
	
}
