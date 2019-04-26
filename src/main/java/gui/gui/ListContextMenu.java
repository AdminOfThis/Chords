package gui.gui;

import gui.controller.MainController;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

public class ListContextMenu extends ContextMenu {

	private MenuItem mnDelete = new MenuItem("Delete");

	public ListContextMenu() {
		this.getItems().add(mnDelete);
		mnDelete.setOnAction(e -> MainController.getInstance().delete(MainController.getInstance().getCurrentSong()));
	}
}
