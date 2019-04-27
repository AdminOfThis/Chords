package main;

import java.io.IOException;

import gui.FXMLUtil;
import gui.MainGUI;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import util.FileIO;

public class Main extends MainGUI {

	private static final String	POM_TITLE	= "Chords";
	private static final String	GUI_PATH	= "/fxml/Main.fxml";
	private static final String	LOGO		= "/icons/icon50.png";

	public static void main(String[] args) {
		MainGUI.initialize(POM_TITLE);
		LOG.info("Started");
		try {
			FileIO.loadProperties();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		launch(args);
	}

	@Override
	public void start(Stage stage) throws Exception {
		Parent p = FXMLUtil.loadFXML(GUI_PATH);
		Scene scene = new Scene(p);
		stage.setScene(scene);
		stage.show();
		FXMLUtil.setIcon(stage, LOGO);
		stage.setTitle(getReadableTitle());
		stage.setOnCloseRequest(e -> close());
	}

	@Override
	public boolean close() {
		LOG.info("Bye");
		Platform.exit();
		System.exit(0);
		return true;
	}

	@Override
	public String getTitle() {
		return null;
	}

	@Override
	public String getPOMTitle() {
		return POM_TITLE;
	}
}
