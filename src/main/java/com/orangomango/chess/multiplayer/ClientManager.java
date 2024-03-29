package com.orangomango.chess.multiplayer;

import java.io.*;
import java.net.*;

import javafx.scene.paint.Color;

import com.orangomango.chess.Logger;

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
		Logger.writeInfo("Client connected");
		listen();
	}
	
	public void reply(){
		String message = null;
		for (ClientManager c : Server.clients){
			if (c != this && c.getColor() == this.color){
				this.color = this.color == Color.WHITE ? Color.BLACK : Color.WHITE;
			}
		}
		if (Server.clients.size() > 2){
			message = "FULL";
			Server.clients.remove(this);
		} else {
			message = this.color == Color.WHITE ? "WHITE" : "BLACK";
		}
		message += ";"+Server.fen+";"+Server.timeControl.split("\\+")[0]+";"+Server.timeControl.split("\\+")[1];
		try {
			this.writer.write(message);
			this.writer.newLine();
			this.writer.flush();
		} catch (IOException ex){
			close();
		}
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
