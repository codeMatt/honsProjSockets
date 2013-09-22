package com.heeere.android.dnssdtuto;

public class DeviceInstance {
	
	private String address, name;
	private int port;
	
	public DeviceInstance(String dName, String dAddress, int dPort){
		name = dName;
		address = dAddress;
		port = dPort;
	}
	
	public String toString(){
		return name;
	}
	
	public String getAddress(){
		return address;
	}
	
	public int getPort(){
		return port;
	}
}
