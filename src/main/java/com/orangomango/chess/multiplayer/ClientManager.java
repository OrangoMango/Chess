package com.orangomango.chess.multiplayer;

import java.io.*;
import java.net.*;

import javafx.scene.paint.Color;

public class ClientManager{
	private Socket socket;
	private BufferedWriter writer;
	private BufferedReader reader;
	private Color color;
	
	public ClientManager(Socket socket){
		this.socket = socket;
		try {
			this.writer = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream()));
			this.reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
			String message = this.reader.readLine();
			this.color = message.equals("WHITE") ? Color.WHITE : Color.BLACK;
		} catch (IOException ex){
			close();
		}
		System.out.println("Client connected");
		listen();
	}
	
	public Color getColor(){
		return this.color;
	}
	
	private void listen(){
		Thread listener = new Thread(() -> {
			while (this.socket.isConnected()){
				try {
					String message = this.reader.readLine();
					if (message != null){
						broadcast(message);
					}
				} catch (IOException ex){
					close();
				}
			}
		});
		listener.setDaemon(true);
		listener.start();
	}
	
	private void broadcast(String message){
		for (ClientManager cm : Server.clients){
			if (cm == this) continue;
			try {
				cm.writer.write(message);
				cm.writer.newLine();
				cm.writer.flush();
			} catch (IOException ex){
				close();
			}
		}
	}
	
	private void close(){
		try {
			if (this.socket != null) this.socket.close();
			if (this.reader != null) this.reader.close();
			if (this.writer != null) this.writer.close();
		} catch (IOException ex){
			ex.printStackTrace();
		}
	}
}
