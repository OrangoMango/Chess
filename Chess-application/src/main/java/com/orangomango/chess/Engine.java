package com.orangomango.chess;

import javafx.scene.paint.Color;

import java.util.function.Consumer;
import java.net.URLEncoder;

import dev.webfx.platform.fetch.Fetch;
import dev.webfx.platform.fetch.Response;

public class Engine{	
	private String host;
	
	public Engine(String host){
		this.host = host;
	}
	
	public void getBestMove(Board b, Consumer<String> onSuccess){
		try {
			Fetch.fetch(this.host+MainApplication.format("?fen=%s&depth=10&mode=bestmove", URLEncoder.encode(b.getFEN(), "UTF-8"))).compose(Response::json).onSuccess(json -> {
				String output = json.asJsonObject().getString("data").split("bestmove ")[1].split(" ")[0];
				if (output.trim().equals("(none)")){
					output = null;
				} else {
					char[] c = output.toCharArray();
					output = String.valueOf(c[0])+String.valueOf(c[1])+" "+String.valueOf(c[2])+String.valueOf(c[3])+(c.length == 5 ? " "+String.valueOf(c[4]) : "");
				}
				onSuccess.accept(output);
			});
		} catch (Exception ex){
			ex.printStackTrace();
		}
	}
	
	public void getEval(String fen, Consumer<String> onSuccess){
		try {
			Fetch.fetch(this.host+MainApplication.format("?fen=%s&depth=10&mode=eval", URLEncoder.encode(fen, "UTF-8"))).compose(Response::json).onSuccess(json -> {
				String output = json.asJsonObject().getString("data").split("Total evaluation: ")[1].split(" ")[0];
				onSuccess.accept(output);
			});
		} catch (Exception ex){
			ex.printStackTrace();
		}
	}
}
