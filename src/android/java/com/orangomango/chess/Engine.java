package com.orangomango.chess;

import java.io.*;

import javafx.scene.paint.Color;
import javafxports.android.FXActivity;

public class Engine{
	private static String COMMAND = FXActivity.getInstance().getFilesDir().getAbsolutePath()+"/stockfish";
	
	private OutputStreamWriter writer;
	private BufferedReader reader;
	private boolean running = true;

	static {
		File dir = FXActivity.getInstance().getExternalFilesDir(null);
		String found = null;
		for (File file : dir.listFiles()){
			// Custom stockfish file
			if (file.getName().startsWith("stockfish")){
				file.setExecutable(true);
				found = file.getAbsolutePath();
				break;
			}
		}
		if (found != null) COMMAND = found;
	}
	
	public Engine(){
		try {
			File eng = new File(COMMAND);
			eng.setExecutable(true);
			Process process = Runtime.getRuntime().exec(COMMAND);
			this.writer = new OutputStreamWriter(process.getOutputStream());
			this.reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			getOutput(20);
		} catch (IOException ex){
			ex.printStackTrace();
			this.running = false;
		}
	}
	
	public boolean isRunning(){
		return this.running;
	}
	
	public void writeCommand(String command){
		try {
			this.writer.write(command+"\n");
			this.writer.flush();
		} catch (IOException ex){
			ex.printStackTrace();
		}
	}
	
	public String getOutput(int time){
		StringBuilder builder = new StringBuilder();
		try {
			Thread.sleep(time);
			writeCommand("isready");
			while (true){
				String line = this.reader.readLine();
				if (line == null || line.equals("readyok")){
					break;
				} else {
					builder.append(line+"\n");
				}
			}
		} catch (Exception ex){
			ex.printStackTrace();
			System.exit(0);
		}
		return builder.toString();
	}
	
	public String getBestMove(Board b){
		writeCommand("position fen "+b.getFEN());
		if (b.getMoves().size() < 5){
			writeCommand("go movetime 500");
		} else {
			writeCommand(String.format("go wtime %s btime %s winc %s binc %s", b.getTime(Color.WHITE), b.getTime(Color.BLACK), b.getIncrementTime(), b.getIncrementTime()));
		}
		String output = "";
		while (true) {
			try {
				String line = this.reader.readLine();
				if (line != null && line.contains("bestmove")){
					output = line.split("bestmove ")[1].split(" ")[0];
					break;
				}
			} catch (IOException ex){
				ex.printStackTrace();
			}
		}
		if (output.trim().equals("(none)")) return null;
		char[] c = output.toCharArray();
		return String.valueOf(c[0])+String.valueOf(c[1])+" "+String.valueOf(c[2])+String.valueOf(c[3])+(c.length == 5 ? " "+String.valueOf(c[4]) : "");
	}
	
	public String getEval(String fen){
		try {
			writeCommand("position fen "+fen);
			writeCommand("eval");
			String output = getOutput(50).split("Final evaluation ")[1].split("\\(")[0].trim();
			return output;
		} catch (Exception ex){
			return null;
		}
	}
}
