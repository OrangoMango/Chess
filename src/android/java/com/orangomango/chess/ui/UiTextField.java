package com.orangomango.chess.ui;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.geometry.Rectangle2D;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.scene.control.TextInputDialog;

public class UiTextField extends UiObject implements Clickable{
	private String placeHolder;

	public UiTextField(UiScreen screen, GraphicsContext gc, Rectangle2D rect, String placeHolder){
		super(screen, gc, rect);
		this.placeHolder = placeHolder;
	}

	@Override
	public void click(double x, double y){
		if (getAbsoluteRect().contains(x-this.screen.getRect().getMinX(), y-this.screen.getRect().getMinY())){
			TextInputDialog dialog = new TextInputDialog();
			dialog.setTitle("Insert value");
			dialog.setHeaderText("Insert value");
			dialog.showAndWait().ifPresent(s -> this.placeHolder = s);
		}
	}

	public String getValue(){
		return this.placeHolder;
	}

	@Override
	public void render(){
		Rectangle2D abs = getAbsoluteRect();
		gc.save();
		gc.setStroke(Color.BLACK);
		gc.setLineWidth(2);
		gc.strokeRect(abs.getMinX(), abs.getMinY(), abs.getWidth(), abs.getHeight());
		gc.setFill(Color.web("#D7D7D7"));
		gc.fillRect(abs.getMinX(), abs.getMinY(), abs.getWidth(), abs.getHeight());
		gc.setFill(Color.BLACK);
		gc.setFont(new Font("times new roman", abs.getWidth()*0.1));
		gc.setTextAlign(TextAlignment.CENTER);
		gc.fillText(this.placeHolder, abs.getMinX()+abs.getWidth()*0.5, abs.getMinY()+abs.getHeight()*0.65);
		gc.restore();
	}
}