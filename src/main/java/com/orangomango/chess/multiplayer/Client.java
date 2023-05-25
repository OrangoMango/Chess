package com.orangomango.chess.multiplayer;

import java.io.*;
import java.net.*;

import javafx.scene.paint.Color;

import com.orangomango.chess.Logger;

public class Client{
	private String ip;
	private int port;
	private Socket socket;
	private BufferedWriter writer;
	private BufferedReader reader;
	private Color color;
	
	public Client(String ip, int port, Color color){
		this.ip = ip;
		this.port = port;
		try {
			this.socket = new Socket(ip, port);
			this.writer = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream()));
			this.reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
			sendMessage(color == Color.WHITE ? "WHITE" : "BLACK");
			String response = getMessage();
			if (response != null){
				if (response.equals("FULL")){
					Logger.writeInfo("Server is full");
					System.exit(0);
				} else {
					this.color = response.equals("WHITE") ? Color.WHITE : Color.BLACK;
				}
			} else {
				Logger.writeError("Invalid server response");
				System.exit(0);
			}
		} catch (IOException ex){
			close();
		}
	}
	
	public Color getColor(){
		return this.color;
	}
	
	public boolean isConnected(){
		return this.socket != null && this.socket.isConnected();
	}
	
	public void sendMessage(String message){
		try {
			this.writer.write(message);
			this.writer.newLine();
			this.writer.flush();
		} catch (IOException ex){
			close();
		}
	}
	
	public String getMessage(){
		try {
			String message = this.reader.readLine();
			return message;
		} catch (IOException ex){
			close();
			return null;
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
