package gui.gui;

import java.util.Optional;

import data.Song;
import gui.FXMLUtil;
import gui.controller.MainController;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.ListCell;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextInputDialog;
import util.FileIO;

public class ListContextMenu extends ContextMenu {

	private MenuItem	mnDelete	= new MenuItem("Delete");
	private MenuItem	mnAddTag	= new MenuItem("New Tag");
	private Menu		tags		= new Menu("Tags");

	public ListContextMenu(ListCell<Song> cell) {
		this.getItems().add(mnDelete);
		mnDelete.setOnAction(e -> MainController.getInstance().delete(MainController.getInstance().getCurrentSong()));
		this.getItems().add(new SeparatorMenuItem());
		this.getItems().add(mnAddTag);
		mnAddTag.setOnAction(e -> addTag());
		this.getItems().add(tags);
		for (String tag : FileIO.getTags()) {
			CheckMenuItem item = new CheckMenuItem(tag);
			tags.getItems().add(item);
			Song song = cell.getItem();
			item.setSelected(song.getTags().contains(tag));
			item.setOnAction(e -> {
				if (cell.getItem().getTags().contains(tag)) {
					song.removeTag(tag);
					song.setChanged(true);
				} else {
					song.addTag(tag);
					song.setChanged(true);
				}
			});
		}
	}

	private void addTag() {
		Dialog<String> dialog = new TextInputDialog();
		FXMLUtil.setStyleSheet(dialog.getDialogPane());
		Optional<String> result = dialog.showAndWait();
		if (result.isPresent()) {
			FileIO.addTag(result.get());
		}
	}
}
