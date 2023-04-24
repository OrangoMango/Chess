package com.orangomango.chess.multiplayer;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server{
	private String ip;
	private int port;
	private ServerSocket server;
	public static List<ClientManager> clients = new ArrayList<>();
	
	public Server(String ip, int port){
		this.ip = ip;
		this.port = port;
		try {
			this.server = new ServerSocket(port, 10, InetAddress.getByName(ip));
		} catch (IOException ex){
			close();
		}
		listen();
		System.out.println("Server started");
	}
	
	private void listen(){
		Thread listener = new Thread(() -> {
			while (!this.server.isClosed()){
				try {
					Socket socket = this.server.accept();
					if (this.clients.size() >= 2){
						System.out.println("Server is full");
					} else {
						ClientManager cm = new ClientManager(socket);
						clients.add(cm);
						for (ClientManager c : clients){
							if (c != cm && c.getColor() == cm.getColor()){
								clients.remove(cm);
								System.out.println("Color already available");
							}
						}
					}
				} catch (IOException ex){
					ex.printStackTrace();
				}
			}
		});
		listener.setDaemon(true);
		listener.start();
	}
	
	private void close(){
		try {
			if (this.server != null) this.server.close();
		} catch (IOException ex){
			ex.printStackTrace();
		}
	}
}
