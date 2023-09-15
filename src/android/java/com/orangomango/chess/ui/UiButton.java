package com.orangomango.chess.ui;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.geometry.Rectangle2D;

import com.orangomango.chess.MainApplication;

public class UiButton extends UiObject implements Clickable{
	private Runnable onClick;
	private Image image;
	private boolean on;
	private UiButton connection;

	public UiButton(UiScreen screen, GraphicsContext gc, Rectangle2D rect, Image image, Runnable onClick){
		super(screen, gc, rect);
		this.image = image;
		this.onClick = onClick;
	}

	@Override
	public void click(double x, double y){
		if (getAbsoluteRect().contains(x-this.screen.getRect().getMinX(), y-this.screen.getRect().getMinY())){
			if (this.connection != null && this.on) return;
			this.onClick.run();
			MainApplication.vibrator.vibrate(125);
			this.on = !this.on;
			if (this.connection != null){
				this.connection.on = !this.on;
			}
		}
	}

	// Only one connection is allowed
	public void connect(UiButton btn, boolean isOn){
		this.connection = btn;
		this.on = isOn;
	}

	public boolean isOn(){
		return this.on;
	}

	@Override
	public void render(){
		Rectangle2D abs = getAbsoluteRect();
		gc.drawImage(this.image, abs.getMinX(), abs.getMinY(), abs.getWidth(), abs.getHeight());
		if (this.connection != null && this.on){
			gc.save();
			gc.setGlobalAlpha(0.6);
			gc.setFill(Color.BLACK);
			gc.fillRect(abs.getMinX(), abs.getMinY(), abs.getWidth(), abs.getHeight());
			gc.restore();
		}
	}
}