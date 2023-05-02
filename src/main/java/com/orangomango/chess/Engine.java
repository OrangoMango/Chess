package com.orangomango.chess;

import java.io.*;

public class Engine{
	private static final String COMMAND = "stockfish";
	
	private OutputStreamWriter writer;
	private BufferedReader reader;
	private boolean running = true;
	
	public Engine(){
		try {
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
	
	public String getBestMove(String fen, int moveTime){
		writeCommand("position fen "+fen);
		writeCommand("go movetime "+moveTime);
		String output = getOutput(moveTime+150).split("bestmove ")[1].split(" ")[0];
		if (output.trim().equals("(none)")) return null;
		char[] c = output.toCharArray();
		return String.valueOf(c[0])+String.valueOf(c[1])+" "+String.valueOf(c[2])+String.valueOf(c[3]);
	}
	
	public String getEval(String fen){
		writeCommand("position fen "+fen);
		writeCommand("eval");
		String output = getOutput(50).split("Final evaluation: ")[1].split("\\(")[0].trim();
		return output;
	}
}
