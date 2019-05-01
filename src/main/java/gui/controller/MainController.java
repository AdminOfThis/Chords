package gui.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import data.Song;
import gui.FXMLUtil;
import gui.MainGUI;
import gui.gui.ListContextMenu;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;
import util.FileIO;
import util.SongPrinter;

public class MainController implements Initializable {

	private static final Logger		LOG			= LogManager.getLogger(MainController.class);
	private static MainController	instance;
	@FXML
	private MenuItem				mnSave;
	@FXML
	private TextField				txtName, txtAuthor, txtKey, txtCapo;
	@FXML
	private TextField				txtSearch;
	@FXML
	private ListView<Song>			list;
	@FXML
	private TextArea				txtArea;
	@FXML
	private Button					btnPrint;
	@FXML
	private Label					lblStatus;
	@FXML
	private ToggleButton			tglContent, tglPreview, tglChords;
	@FXML
	private ImageView				image;
	@FXML
	private BorderPane				root, imgParent;
	private TextInputControl[]		changeWatchList;
	private List<Song>				songList	= new ArrayList<>();
	private Thread					background;
	long							lastSpace	= 0;

	public static MainController getInstance() {
		return instance;
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		instance = this;
		changeWatchList = new TextInputControl[] { txtArea, txtName, txtAuthor, txtKey, txtCapo };
		tglPreview.selectedProperty().addListener((obs, oldV, newV) -> {
			if (newV) {
				imgParent.setCenter(image);
			} else {
				imgParent.setCenter(null);
			}
		});
		image.fitWidthProperty().bind(imgParent.widthProperty());
		image.fitHeightProperty().bind(imgParent.heightProperty());
		btnPrint.disableProperty().bind(list.getSelectionModel().selectedItemProperty().isNull());
		txtArea.disableProperty().bind(list.getSelectionModel().selectedItemProperty().isNull());
		txtName.disableProperty().bind(list.getSelectionModel().selectedItemProperty().isNull());
		txtAuthor.disableProperty().bind(list.getSelectionModel().selectedItemProperty().isNull());
		txtKey.disableProperty().bind(list.getSelectionModel().selectedItemProperty().isNull());
		txtCapo.disableProperty().bind(list.getSelectionModel().selectedItemProperty().isNull());
		mnSave.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
		txtSearch.textProperty().addListener(e -> {
			search();
		});
		txtCapo.textProperty().addListener((obs, oldV, newV) -> {
			if (!newV.matches("[123]*")) {
				txtCapo.setText(oldV);
			}
		});
		list.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		list.setCellFactory(e -> {
			ListCell<Song> cell = new ListCell<>() {

				@Override
				protected void updateItem(Song item, boolean empty) {
					super.updateItem(item, empty);
					if (empty || item == null) {
						setText(null);
					} else {
						String s = "";
						if (item.isChanged()) {
							s += "*";
						}
						s += item.getName();
						setText(s);
					}
				}
			};
			cell.setOnContextMenuRequested(ex -> {
				ListContextMenu con = new ListContextMenu(cell);
				con.show(cell, ex.getScreenX(), ex.getScreenY());
			});
			return cell;
		});
		list.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
			if (oldItem != null && oldItem.isChanged() && songList.contains(oldItem)) {
				ButtonType result = displayUnsavedWarning();
				if (result == ButtonType.YES) {
					save(oldItem);
					display(newItem);
				} else if (result == ButtonType.NO) {
					oldItem.setChanged(false);
					display(newItem);
				} else {
					list.getSelectionModel().select(oldItem);
				}
			} else {
				display(newItem);
			}
		});
		root.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
			if (e.getCode() == KeyCode.ALT_GRAPH) {
				e.consume();
				return;
			}
		});
		txtArea.setOnKeyReleased(e -> {
			if (e.isAltDown() && e.isControlDown() && e.getCode() == KeyCode.DIGIT9) {
				txtArea.positionCaret(txtArea.getCaretPosition() - 1);
			}
		});
		txtArea.setOnKeyPressed(e -> {
			if (e.getCode() == KeyCode.SPACE) {
				if (System.currentTimeMillis() - lastSpace < 100) {
					tglChords.fire();
					e.consume();
				}
				lastSpace = System.currentTimeMillis();
			}
		});
		txtArea.setOnKeyTyped(e -> {
			if (tglChords.isSelected()) {
				int caretPos = txtArea.getCaretPosition();
				String sbefore = null;
				if (caretPos > 0) {
					sbefore = txtArea.getText().substring(caretPos - 1, caretPos);
				}
				String safter = null;
				if (caretPos >= txtArea.getText().length()) {
					safter = txtArea.getText().substring(caretPos, caretPos + 1);
				}
				if (sbefore == null || safter == null || (!sbefore.equals("[") && !safter.equals("]"))) {
					String text = txtArea.getText(0, caretPos - 1) + "[" + txtArea.getText(caretPos - 1, txtArea.getText().length());
					txtArea.setText(text);
					txtArea.positionCaret(caretPos);
					String text2 = txtArea.getText(0, caretPos + 1) + "]" + txtArea.getText(caretPos + 1, txtArea.getText().length());
					txtArea.setText(text2);
					txtArea.positionCaret(caretPos + 1);
				}
				e.consume();
			}
		});
		txtArea.textProperty().addListener(e -> checkForChange());
		txtName.textProperty().addListener(e -> checkForChange(txtName, getCurrentSong()));
		List<Song> loadedSongs = FileIO.loadFolder();
		songList.addAll(loadedSongs);
		search();
		LOG.info("Initialized MainController");
	}

	private ButtonType displayUnsavedWarning() {
		Alert alert = new Alert(AlertType.CONFIRMATION);
		FXMLUtil.setStyleSheet(alert.getDialogPane());
		alert.setTitle("Unsaved changes");
		alert.setHeaderText("Save changes?");
		alert.setContentText("Do you want to save the changes made to the current song?\r\nAll unsaved changes will be lost");
		alert.getButtonTypes().clear();
		alert.getButtonTypes().add(ButtonType.YES);
		alert.getButtonTypes().add(ButtonType.NO);
		alert.getButtonTypes().add(ButtonType.CANCEL);
		Optional<ButtonType> result = alert.showAndWait();
		if (result.isPresent()) {
			return result.get();
		} else {
			return ButtonType.CANCEL;
		}
	}

	private void checkForChange() {
		new Thread() {

			@Override
			public void run() {
				Song song = getCurrentSong();
				if (song != null) {
					boolean changed = !compareLists(song.getText(), getText());
					if (changed) {
						song.setChanged(true);
						createPreview();
					}
				}
				Platform.runLater(() -> list.refresh());
			}
		}.start();
	}

	private void checkForChange(TextField field, Song value) {
		Song song = getCurrentSong();
		if (song != null && value != null) {
			if (!field.getText().trim().equals(value.getName())) {
				song.setChanged(true);
			}
		}
		list.refresh();
	}

	private void display(Song song) {
		if (song == null) {
			for (TextInputControl c : changeWatchList) {
				c.clear();
			}
		} else {
			StringBuilder sb = new StringBuilder();
			boolean first = true;
			for (String line : song.getText()) {
				if (!first) {
					sb.append("\r\n");
				} else {
					first = false;
				}
				sb.append(line);
			}
			txtArea.setText(sb.toString());
			// name
			txtName.setText(song.getName());
			txtAuthor.setText(song.getAuthor());
			txtKey.setText(song.getKey());
			if (song.getCapo() != 0) {
				txtCapo.setText(song.getCapo() + "");
			} else {
				txtCapo.clear();
			}
			song.setChanged(false);
			if (tglPreview.isSelected()) {
				createPreview();
			}
			list.refresh();
			if (song.getName().equals("New Song")) {
				txtName.positionCaret(0);
				txtName.selectPositionCaret(txtName.getText().length());
				Platform.runLater(() -> txtName.requestFocus());
			}
		}
	}

	public Song getCurrentSong() {
		return list.getSelectionModel().getSelectedItem();
	}

	private List<String> getText() {
		String text = txtArea.getText();
		text = text.trim();
		String[] split = text.split("\n");
		ArrayList<String> result = new ArrayList<>();
		for (String s : split) {
			result.add(s.trim());
		}
		return result;
	}

	public void close(ActionEvent e) {
		MainGUI.getInstance().close();
	}

	public void save(ActionEvent e) {
		save(getCurrentSong());
	}

	public void save(Song song) {
		createPreview();
		if (song != null && song.isChanged()) {
			showStatus("Saving");
			song.setText(getText());
			song.setName(txtName.getText());
			song.setAuthor(txtAuthor.getText());
			song.setKey(txtKey.getText());
			if (txtCapo.getText().isEmpty()) {
				song.setCapo(0);
			} else {
				song.setCapo(Integer.parseInt(txtCapo.getText()));
			}
			boolean success = FileIO.save(song);
			if (success) {
				showStatus("Saved");
				song.setChanged(false);
			}
		}
		list.refresh();
	}

	public void print(ActionEvent e) {
		showStatus("Printing");
		for (Song song : list.getSelectionModel().getSelectedItems()) {
			if (song != null) {
				File pdf = new File(SongPrinter.DEFAULT_FOLDER + song.getName() + ".pdf");
				SongPrinter.print(song, pdf);
			}
		}
		showStatus("Printed");
	}

	private Song getUnsavedSong() {
		Song song = getCurrentSong();
		if (song != null) {
			if (song.isChanged()) {
				song = song.copy();
				song.setText(getText());
				song.setName(txtName.getText().trim());
				String author = txtAuthor.getText();
				if (author != null && !author.isBlank()) {
					song.setAuthor(author.trim());
				}
				String key = txtKey.getText();
				if (key != null && !key.isBlank()) {
					song.setKey(key.trim());
				}
				String capo = txtCapo.getText();
				if (capo != null && !capo.isBlank()) {
					song.setCapo(Integer.parseInt(capo.trim()));
				}
			}
		}
		return song;
	}

	public void newSong(ActionEvent e) {
		if (getCurrentSong() != null && getCurrentSong().isChanged()) {
			ButtonType result = displayUnsavedWarning();
			if (result == ButtonType.YES) {
				save(new ActionEvent());
			} else if (result == ButtonType.NO) {
				getCurrentSong().setChanged(false);
			} else {
				return;
			}
		}
		Song song = new Song("New Song");
		songList.add(song);
		list.getItems().add(song);
		list.getSelectionModel().select(song);
		display(song);
		song.setChanged(true);
		list.refresh();
	}

	private boolean compareLists(List<String> a, List<String> b) {
		if (a.size() == b.size()) {
			for (int i = 0; i < a.size(); i++) {
				if (!a.get(i).trim().equals(b.get(i).trim())) { return false; }
			}
		} else {
			return false;
		}
		return true;
	}

	public void showStatus(String status) {
		lblStatus.setText(status);
		Timeline line = new Timeline();
		KeyFrame key = new KeyFrame(Duration.seconds(5), e -> {
			lblStatus.setText("");
		});
		line.getKeyFrames().add(key);
		line.playFromStart();
	}

	public void delete(Song song) {
		if (song != null) {
			Alert alert = new Alert(AlertType.CONFIRMATION);
			alert.setTitle("Delete" + song.getName());
			alert.setHeaderText("Do you want to delete " + song.getName() + "?");
			alert.setContentText("There is no way to recover the song once deleted.");
			FXMLUtil.setStyleSheet(alert.getDialogPane());
			Optional<ButtonType> result = alert.showAndWait();
			if (result.isPresent() && result.get() == ButtonType.OK) {
				if (FileIO.delete(song)) {
					songList.remove(song);
					search();
				}
			}
		}
	}

	private void search() {
		list.getItems().clear();
		String search = txtSearch.getText().toLowerCase();
		if (search == null || search.isEmpty()) {
			list.getItems().addAll(songList);
		} else {
			for (Song s : songList) {
				if (s.getName().toLowerCase().contains(search)) {
					list.getItems().add(s);
				}
				if (tglContent.isSelected()) {
					for (String text : s.getText()) {
						if (Song.getCleanLine(text).toLowerCase().contains(search)) {
							list.getItems().add(s);
						}
					}
				}
			}
		}
	}

	public void tglContent(ActionEvent e) {
		if (tglContent.isSelected()) {
			tglContent.setText("Content");
		} else {
			tglContent.setText("Title");
		}
		search();
	}

	private synchronized void createPreview() {
		if (background == null || !background.isAlive()) {
			background = new Thread() {

				@Override
				public void run() {
					FileIO.cleanUpPreview();
					File temp = null;
					try {
						temp = File.createTempFile(SongPrinter.CHORD_PREVIEW, ".tmp");
						SongPrinter.print(getUnsavedSong(), temp);
						File pic = SongPrinter.pdfToPic(temp);
						Platform.runLater(() -> {
							try {
								image.setImage(new Image(new FileInputStream(pic)));
							}
							catch (FileNotFoundException e) {
								e.printStackTrace();
							}
							finally {
								if (!pic.delete()) {
									pic.deleteOnExit();
								}
							}
						});
					}
					catch (Exception e1) {
						e1.printStackTrace();
					}
					finally {
						temp.delete();
					}
				}
			};
			background.start();
		}
	}
}
