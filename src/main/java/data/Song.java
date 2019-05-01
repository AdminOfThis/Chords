package data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Song implements Serializable, Cloneable {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= -6044484874179480864L;
	public static final int		MAX_CHORD_LENGTH	= 9;
	private String				name;
	private transient String	oldName;
	private transient boolean	changed				= false;
	private String				key;
	private String				author;
	private int					capo				= 0;
	private List<String>		comments			= new ArrayList<>();
	private List<String>		text				= new ArrayList<>();
	private List<String>		tagList				= new ArrayList<>();

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

	@XmlElementWrapper(name = "text")
	@XmlElement(name = "line")
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

	@XmlElementWrapper(name = "comments")
	@XmlElement(name = "comment")
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
		String rawLine = line.replaceAll("\\[.{1," + MAX_CHORD_LENGTH + "}?\\]", "");
		rawLine = rawLine.replaceAll("  ", " ");
		if (rawLine.startsWith(" ")) {
			rawLine = rawLine.replaceFirst(" ", "");
		}
		return rawLine;
	}

	public void addTag(String tag) {
		if (!tagList.contains(tag)) {
			tagList.add(tag);
		}
	}

	@XmlElementWrapper(name = "tags")
	@XmlElement(name = "tag")
	public List<String> getTags() {
		return tagList;
	}

	public void removeTag(String tag) {
		tagList.remove(tag);
	}
}
