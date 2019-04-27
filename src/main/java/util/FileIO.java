package util;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import data.Song;

public final class FileIO {

	private static final Logger	LOG				= Logger.getLogger(FileIO.class);
	public static final String	FILE_EXTENSION	= ".crd";
	public static final String	DEFAULT_FOLDER	= "./data/";
	public static final String	PROPERTIES_FILE	= "./settings.cfg";
	private static final String	TAG_KEY			= "Tags";
	private static Properties	props			= new Properties();

	public static boolean save(Song song) {
		File file = new File(DEFAULT_FOLDER + song.getName() + FILE_EXTENSION);
		checkAndCreateFolder(file);
		checkForNameChange(song);
		XMLEncoder encoder = null;
		try {
			encoder = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(file)));
			encoder.writeObject(song);
			encoder.close();
			return true;
		}
		catch (Exception fileNotFound) {
			fileNotFound.printStackTrace();
		}
		return false;
	}

	private static void checkForNameChange(Song song) {
		if (song.getOldName() != null) {
			File oldFile = new File(DEFAULT_FOLDER + song.getOldName() + FILE_EXTENSION);
			if (oldFile.exists()) {
				LOG.info("Renaming song file from \"" + song.getOldName() + "\" to \"" + song.getName() + "\"");
				oldFile.delete();
			}
		}
	}

	private static void checkAndCreateFolder(File file) {
		File parent = file.getParentFile();
		if (parent != null && (!parent.exists() || !parent.isDirectory())) {
			LOG.info("Parent folder does not exist, creating folder");
			parent.mkdirs();
		}
	}

	public static boolean save(List<Song> songList) {
		boolean result = true;
		for (Song s : songList) {
			boolean res = save(s);
			if (!res) {
				result = false;
			}
		}
		return result;
	}

	public static Song load(File file) {
		Song song = null;
		try {
			if (file.getName().endsWith(FILE_EXTENSION)) {
				XMLDecoder decoder = null;
				try {
					decoder = new XMLDecoder(new BufferedInputStream(new FileInputStream(file)));
				}
				catch (FileNotFoundException e) {
					System.out.println("ERROR: File dvd.xml not found");
				}
				song = (Song) decoder.readObject();
				song.setChanged(false);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return song;
	}

	public static List<Song> loadFolder(File file) {
		ArrayList<Song> result = new ArrayList<>();
		if (file != null && file.exists() && file.isDirectory()) {
			for (File f : file.listFiles()) {
				Song s = load(f);
				if (s != null) {
					result.add(s);
				}
			}
		}
		return result;
	}

	public static List<Song> loadFolder() {
		return loadFolder(new File(DEFAULT_FOLDER));
	}

	public static boolean delete(Song song) {
		File file = new File(DEFAULT_FOLDER + song.getName() + FILE_EXTENSION);
		if (file.exists()) {
			file.delete();
			LOG.info("Song file \"" + file.getName() + "\" deleted.");
			return true;
		} else {
			return true;
		}
	}

	public static void saveProperties() throws IOException {
		FileOutputStream fr = new FileOutputStream(new File(PROPERTIES_FILE));
		props.store(fr, "Properties");
		fr.close();
		System.out.println("After saving properties: " + props);
	}

	public static void loadProperties() throws IOException {
		File file = new File(PROPERTIES_FILE);
		if (file.exists()) {
			FileInputStream fi = new FileInputStream(file);
			props.load(fi);
			fi.close();
			System.out.println("After Loading properties: " + props);
		}
	}

	public static List<String> getTags() {
		List<String> result = new ArrayList<>();
		String tags = props.getProperty(TAG_KEY, "");
		for (String tag : tags.split(";")) {
			result.add(tag);
		}
		return result;
	}

	public static void addTag(String tag) {
		String tags = props.getProperty(TAG_KEY, "");
		if (!tags.isEmpty()) {
			tags += ";";
		}
		tags += tag;
		props.setProperty(TAG_KEY, tags);
		try {
			saveProperties();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
}
