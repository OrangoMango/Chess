package com.orangomango.chess.multiplayer;

import javafx.scene.paint.Color;

import java.util.function.Consumer;
import java.net.URLEncoder;

import dev.webfx.platform.fetch.Fetch;
import dev.webfx.platform.fetch.Response;
import dev.webfx.platform.scheduler.Scheduler;

import com.orangomango.chess.MainApplication;

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

	public HttpServer(String host, String game, String color, Runnable onSuccess){
		this.host = host;
		this.game = game;
		getUid(color, data -> {
			if (data.equals("FULL")){
				this.full = true;
			} else {
				this.uid = data.split(";")[0];
				this.color = data.split(";")[1];
			}
			this.running = true;
			onSuccess.run();
		});
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
		getData(data -> {
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
			if (this.running){
				Scheduler.scheduleDelay(100, this::listen);
			}
		}, false);
	}

	public void stop(){
		this.running = false;
	}

	public void delete(){
		try {
			sendRequest(MainApplication.format("?game=%s&delete=1&uid=%s", URLEncoder.encode(this.game, "UTF-8"), this.uid), null);
		} catch (Exception ex){
			ex.printStackTrace();
		}
	}

	public void getHeader(Consumer<String> onSuccess){
		getData(data -> onSuccess.accept(data.split(",").length == 0 ? null : data.split(",")[0]), true);
	}

	public void sendHeader(String header){
		try {
			sendRequest(MainApplication.format("?game=%s&header=%s&uid=%s", URLEncoder.encode(this.game, "UTF-8"), URLEncoder.encode(header, "UTF-8"), this.uid), null);
		} catch (Exception ex){
			ex.printStackTrace();
		}
	}

	public void sendMove(String move){
		try {
			sendRequest(MainApplication.format("?game=%s&move=%s&uid=%s", URLEncoder.encode(this.game, "UTF-8"), URLEncoder.encode(move, "UTF-8"), this.uid), null);
		} catch (Exception ex){
			ex.printStackTrace();
		}
	}

	private void getData(Consumer<String> onSuccess, boolean store){
		try {
			Fetch.fetch(this.host+MainApplication.format("?game=%s", URLEncoder.encode(this.game, "UTF-8"))).onSuccess(response -> {
				String text = response.text().result();
				if (store) this.oldData = text;
				onSuccess.accept(text);
			});
		} catch (Exception ex){
			ex.printStackTrace();
		}
	}

	private void getUid(String color, Consumer<String> onSuccess){
		try {
			Fetch.fetch(this.host+MainApplication.format("?game=%s&color=%s", URLEncoder.encode(this.game, "UTF-8"), color)).onSuccess(response -> {
				onSuccess.accept(response.text().result());
			});
		} catch (Exception ex){
			ex.printStackTrace();
		}
	}

	private void sendRequest(String param, Consumer<String> onSuccess){
		Fetch.fetch(this.host+param).onSuccess(response -> {
			String result = response.text().result();
			getData(r -> this.oldData = r, false);
			if (onSuccess != null) onSuccess.accept(result);
		});
	}
}