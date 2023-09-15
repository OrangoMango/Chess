package com.orangomango.chess.ui;

import javafx.scene.layout.GridPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.GridPane;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Insets;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;

import java.util.function.Consumer;

import dev.webfx.extras.webtext.HtmlText;
import dev.webfx.stack.ui.controls.dialog.DialogUtil;
import dev.webfx.stack.ui.controls.dialog.DialogCallback;

import static com.orangomango.chess.MainApplication.MAIN_SCENE;

public class UiTextField extends UiObject implements Clickable{
	private String placeHolder;

	public UiTextField(UiScreen screen, GraphicsContext gc, Rectangle2D rect, String placeHolder){
		super(screen, gc, rect);
		this.placeHolder = placeHolder;
	}

	@Override
	public void click(double x, double y){
		if (getAbsoluteRect().contains(x-this.screen.getRect().getMinX(), y-this.screen.getRect().getMinY())){
			createTextAlert("Insert value", s -> this.placeHolder = s);
		}
	}

	private void createTextAlert(String headerText, Consumer<String> onSuccess){
        GridPane gridPane = new GridPane();
        gridPane.setPadding(new Insets(5, 5, 5, 5));
        gridPane.setHgap(5);
        gridPane.setVgap(5);
        gridPane.setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));
        HtmlText header = new HtmlText("<b>"+headerText+"</b>");
        TextField field = new TextField();
        Button ok = new Button("OK");
        ok.setDefaultButton(true);
        Button cancel = new Button("CANCEL");
        cancel.setCancelButton(true);
        gridPane.add(header, 0, 0, 2, 1);
        gridPane.add(field, 0, 1, 2, 1);
        gridPane.add(cancel, 0, 2);
        gridPane.add(ok, 1, 2);
        DialogCallback callback = DialogUtil.showModalNodeInGoldLayout(gridPane, MAIN_SCENE);
        field.requestFocus();
        ok.setOnAction(e -> {
        	callback.closeDialog();
                onSuccess.accept(field.getText());
        });
        cancel.setOnAction(e -> callback.closeDialog());
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