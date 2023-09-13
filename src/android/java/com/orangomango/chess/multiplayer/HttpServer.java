package com.orangomango.chess.multiplayer;

import javafx.scene.paint.Color;

import java.net.*;
import java.io.*;

public class HttpServer{
	@FunctionalInterface
	public static interface MoveRequest{
		public void applyMove(int time, String p1, String p2, String prom);
	}

	private String host;
	private String game;
	private MoveRequest onRequest;
	private String uid, color;
	private boolean full;
	private volatile String oldData = "";
	private volatile boolean running;

	public HttpServer(String host, String game, String color){
		this.host = host;
		this.game = game;
		String data = getUid(color);
		if (data.equals("FULL")){
			this.full = true;
		} else {
			this.uid = data.split(";")[0];
			this.color = data.split(";")[1];
		}
	}

	public boolean isFull(){
		return this.full;
	}

	public String getRoom(){
		return this.game;
	}

	public Color getColor(){
		return this.color.equals("WHITE") ? Color.WHITE : Color.BLACK;
	}

	public void setOnRequest(MoveRequest mr){
		this.onRequest = mr;
	}

	public void listen(){
		this.running = true;
		Thread listener = new Thread(() -> {
			while (this.running){
				try {
					String data = getData();
					if (data.split(",").length == 2 && !data.equals(this.oldData)){
						String[] text = data.split(",")[1].split(" ");
						String[] parts = text[text.length-1].split(";");
						if (parts.length == 4){
							if (this.onRequest != null){
								this.onRequest.applyMove(Integer.parseInt(parts[0]), parts[1].equals("null") ? null : parts[1], parts[2].equals("null") ? null : parts[2], parts[3].equals("null") ? null : parts[3]);
							}
							this.oldData = data;
						}
					}
					Thread.sleep(100);
				} catch (InterruptedException ex){
					ex.printStackTrace();
				}
			}
		});
		listener.setDaemon(true);
		listener.start();
	}

	public void stop(){
		this.running = false;
	}

	public void delete(){
		try {
			sendRequest(String.format("?game=%s&delete=1&uid=%s", URLEncoder.encode(this.game, "UTF-8"), this.uid));
		} catch (IOException ex){
			ex.printStackTrace();
		}
	}

	public String getHeader(){
		String data = getData();
		return data.split(",").length == 0 ? null : data.split(",")[0];
	}

	public void sendHeader(String header){
		try {
			sendRequest(String.format("?game=%s&header=%s&uid=%s", URLEncoder.encode(this.game, "UTF-8"), URLEncoder.encode(header, "UTF-8"), this.uid));
		} catch (IOException ex){
			ex.printStackTrace();
		}
	}

	public void sendMove(String move){
		try {
			sendRequest(String.format("?game=%s&move=%s&uid=%s", URLEncoder.encode(this.game, "UTF-8"), URLEncoder.encode(move, "UTF-8"), this.uid));
		} catch (IOException ex){
			ex.printStackTrace();
		}
	}

	public synchronized String getData(){
		try {
			URL url = new URL(this.host+String.format("?game=%s", URLEncoder.encode(this.game, "UTF-8")));
			StringBuilder builder = new StringBuilder();
			BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
			reader.lines().forEach(builder::append);
			reader.close();
			return builder.toString();
		} catch (IOException ex){
			ex.printStackTrace();
			return null;
		}
	}

	private String getUid(String color){
		try {
			URL url = new URL(this.host+String.format("?game=%s&color=%s", URLEncoder.encode(this.game, "UTF-8"), color));
			StringBuilder builder = new StringBuilder();
			BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
			reader.lines().forEach(builder::append);
			reader.close();
			return builder.toString();
		} catch (IOException ex){
			ex.printStackTrace();
			return null;
		}
	}

	private void sendRequest(String param){
		new Thread(() -> {
			try {
				URL url = new URL(this.host+param);
				url.openStream();
				this.oldData = getData();
			} catch (IOException ex){
				ex.printStackTrace();
			}
		}).start();
	}
}