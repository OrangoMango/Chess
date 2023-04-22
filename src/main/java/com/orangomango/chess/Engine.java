package com.orangomango.chess;

import java.io.*;

public class Engine{
	private static final String COMMAND = "stockfish";
	
	private OutputStreamWriter writer;
	private BufferedReader reader;
	
	public Engine(){
		try {
			Process process = Runtime.getRuntime().exec(COMMAND);
			this.writer = new OutputStreamWriter(process.getOutputStream());
			this.reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			getOutput(20);
		} catch (IOException ex){
			ex.printStackTrace();
		}
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
			String line;
			while ((line = this.reader.readLine()) != null){
				if (line.equals("readyok")){
					break;
				} else {
					builder.append(line+"\n");
				}
			}
		} catch (Exception ex){
			ex.printStackTrace();
		}
		return builder.toString();
	}
	
	public String getBestMove(String fen, int moveTime){
		writeCommand("position fen "+fen);
		writeCommand("go movetime "+moveTime);
		String output = getOutput(moveTime+50).split("bestmove ")[1].split(" ")[0];
		char[] c = output.toCharArray();
		return String.valueOf(c[0])+String.valueOf(c[1])+" "+String.valueOf(c[2])+String.valueOf(c[3]);
	}
}
