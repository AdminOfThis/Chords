package data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Song implements Serializable, Cloneable {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= -4337440346930134851L;
	private String				name;
	private transient String	oldName;
	private String				key;
	private String				author;
	private int					capo				= 0;
	private List<String>		comments			= Collections.synchronizedList(new ArrayList<String>());
	private List<String>		text				= Collections.synchronizedList(new ArrayList<String>());
	private boolean				changed				= false;

	public Song() {}

	public Song(String name) {
		this.setName(name);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		if (this.name != null && !this.name.equals(name) && oldName == null) {
			oldName = this.name;
		}
		if (name != null) {
			name = name.trim();
		}
		this.name = name;
	}

	public List<String> getText() {
		return text;
	}

	public void setText(List<String> text) {
		this.text.clear();
		for (String s : text) {
			this.text.add(s.trim());
		}
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		if (key != null) {
			key = key.trim();
		}
		this.key = key;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		if (author != null) {
			author = author.trim();
		}
		this.author = author;
	}

	public List<String> getComments() {
		return comments;
	}

	public void setComments(List<String> comments) {
		this.comments = comments;
	}

	public void addComment(String comment) {
		comments.add(comment);
	}

	public Song copy() {
		try {
			return (Song) this.clone();
		}
		catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void setChanged(boolean changed) {
		this.changed = changed;
	}

	public boolean isChanged() {
		return changed;
	}

	public int getCapo() {
		return capo;
	}

	public void setCapo(int capo) {
		this.capo = capo;
	}

	public String getOldName() {
		return oldName;
	}

	public static String getCleanLine(String line) {
		String rawLine = line.replaceAll("\\[.{1,3}?\\]", "");
		rawLine = rawLine.replaceAll("  ", " ");
		return rawLine.trim();
	}
}
