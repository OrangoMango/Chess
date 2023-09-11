package com.orangomango.chess.multiplayer;

import java.io.*;
import java.net.*;
import java.util.*;

import com.orangomango.chess.Logger;

public class Server{
	private String ip;
	private int port;
	private ServerSocket server;
	public static String fen, timeControl;
	public static List<ClientManager> clients = new ArrayList<>();
	
	public Server(String ip, int port, String fen, String timeControl){
		this.ip = ip;
		this.port = port;
		Server.fen = fen;
		Server.timeControl = timeControl;
		try {
			this.server = new ServerSocket(port, 10, InetAddress.getByName(ip));
		} catch (IOException ex){
			close();
		}
		listen();
		Logger.writeInfo("Server started");
	}
	
	private void listen(){
		Thread listener = new Thread(() -> {
			while (!this.server.isClosed()){
				try {
					Socket socket = this.server.accept();
					ClientManager cm = new ClientManager(socket);
					clients.add(cm);
					System.out.println("Client connected. "+clients.size()+"/2");
					cm.reply();
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
