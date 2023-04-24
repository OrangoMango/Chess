package com.orangomango.chess.multiplayer;

import java.io.*;
import java.net.*;

import javafx.scene.paint.Color;

public class Client{
	private String ip;
	private int port;
	private Socket socket;
	private BufferedWriter writer;
	private BufferedReader reader;
	
	public Client(String ip, int port, Color color){
		this.ip = ip;
		this.port = port;
		try {
			this.socket = new Socket(ip, port);
			this.writer = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream()));
			this.reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
			sendMessage(color == Color.WHITE ? "WHITE" : "BLACK");
		} catch (IOException ex){
			close();
		}
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
