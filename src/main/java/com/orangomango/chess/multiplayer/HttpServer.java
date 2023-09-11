package com.orangomango.chess.multiplayer;

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
	private volatile String oldData;
	private volatile boolean running;

	public HttpServer(String host, String game){
		this.host = host;
		this.game = game;
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
					if (!data.equals(this.oldData)){
						String[] text = data.split(" ");
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

	public synchronized String getData(){
		try {
			URL url = new URL(this.host+String.format("?game=%s", this.game));
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

	public void sendMove(String move){
		new Thread(() -> {
			try {
				URL url = new URL(this.host+String.format("?game=%s&move=%s", this.game, move));
				url.openStream();
				this.oldData = getData();
				System.out.println("Move sent: "+move);
			} catch (IOException ex){
				ex.printStackTrace();
			}
		}).start();
	}
}