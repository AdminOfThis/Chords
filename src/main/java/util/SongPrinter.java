package util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;

import data.Song;

public final class SongPrinter {

	private static final Logger					LOG				= Logger.getLogger(SongPrinter.class);
	private static float						MARGIN_TITLE	= 50;
	private static float						MARGIN_LEFT		= 75;
	private static final PDFont					TITLE_FONT		= PDType1Font.HELVETICA_BOLD;
	private static final int					TITLE_FONT_SIZE	= 18;
	private static final PDFont					META_FONT		= PDType1Font.HELVETICA_OBLIQUE;
	private static final int					META_FONT_SIZE	= 12;
	private static final PDFont					MAIN_FONT		= PDType1Font.HELVETICA;
	private static final int					MAIN_FONT_SIZE	= 14;
	private static final int					SUB_FONT_SIZE	= 12;
	private static final PDFont					CHORD_FONT		= PDType1Font.HELVETICA_BOLD_OBLIQUE;
	private static final PDFont					COMMENT_FONT	= PDType1Font.HELVETICA_BOLD;
	static HashMap<PDPage, PDPageContentStream>	streamMap		= new HashMap<>();
	public static final String					DEFAULT_FOLDER	= "./print/";

	public static boolean print(Song song, File file) {
		checkAndCreateFolder(file);
		PDDocument doc = createDocument(song);
		PDPage page = addPage(doc);
		PDPageContentStream stream = createContentStream(doc, page);
		addMetaInfo(stream, page, song);
		addText(song, doc, page, stream);
		addPageNumbers(doc);
		addComments(doc, song);
		return saveAndClose(doc, file);
	}

	private static void checkAndCreateFolder(File file) {
		File parent = file.getParentFile();
		if (parent != null && (!parent.exists() || !parent.isDirectory())) {
			LOG.info("Parent folder does not exist, creating folder");
			parent.mkdirs();
		}
	}

	private static void addComments(PDDocument doc, Song song) {
		PDPage page = doc.getPages().get(doc.getNumberOfPages() - 1);
		PDPageContentStream s = streamMap.get(page);
		try {
			s.moveTo(0, 0);
			s.setFont(META_FONT, META_FONT_SIZE - 2);
			float maxWidth = 0;
			for (String comment : song.getComments()) {
				float width = META_FONT.getStringWidth(comment) / 1000 * (META_FONT_SIZE - 2);
				maxWidth = Math.max(maxWidth, width);
			}
			s.setLeading(META_FONT_SIZE - 2);
			s.beginText();
			s.newLineAtOffset(page.getMediaBox().getWidth() - maxWidth - 10, 10);
			for (int i = song.getComments().size() - 1; i >= 0; i--) {
				String comment = song.getComments().get(i);
				s.showText(comment);
				s.newLineAtOffset(0, META_FONT_SIZE - 2);
			}
			s.endText();
		}
		catch (IOException e) {
			LOG.error("Problem writing document", e);
		}
	}

	private static void addPageNumbers(PDDocument doc) {
		int i = 1;
		for (PDPage page : doc.getPages()) {
			PDPageContentStream s = streamMap.get(page);
			String text = i + " / " + doc.getPages().getCount();
			try {
				s.setFont(META_FONT, META_FONT_SIZE);
				s.moveTo(0, 0);
				s.beginText();
				s.newLineAtOffset((page.getMediaBox().getWidth() - META_FONT.getStringWidth(text) / 1000 * META_FONT_SIZE) / 2f, 10f);
				s.showText(text);
				s.endText();
			}
			catch (IOException e) {
				LOG.error("Problem writing document", e);
			}
			i++;
		}
	}

	private static void addText(Song song, PDDocument doc, PDPage firstPage, PDPageContentStream firstStream) {
		PDPage page = firstPage;
		PDPageContentStream stream = firstStream;
		try {
			formatStream(stream, page);
			int lineCounter = 0;
			for (String line : song.getText()) {
				if (lineCounter > 32) {
					stream.endText();
					page = addPage(doc);
					stream = createContentStream(doc, page);
// addTitle(stream, page, song);
					formatStream(stream, page);
					lineCounter = 0;
				}
				int linesAdded = addLine(stream, line);
				lineCounter += linesAdded;
			}
			stream.endText();
		}
		catch (IOException e) {
			LOG.error("Problem writing document", e);
		}
	}

	private static void formatStream(PDPageContentStream stream, PDPage page) {
		try {
			stream.setFont(MAIN_FONT, MAIN_FONT_SIZE);
			stream.beginText();
			stream.setLeading(MAIN_FONT_SIZE * 1.5f);
			stream.newLineAtOffset(MARGIN_LEFT, page.getMediaBox().getHeight() - MARGIN_TITLE * 2.f);
		}
		catch (IOException e) {
			LOG.error("Problem writing document", e);
		}
	}

	private static PDPageContentStream createContentStream(PDDocument doc, PDPage page) {
		PDPageContentStream stream = null;
		try {
			stream = new PDPageContentStream(doc, page);
			streamMap.put(page, stream);
		}
		catch (IOException e) {
			LOG.error("Problem writing document", e);
		}
		return stream;
	}

	private static int addLine(PDPageContentStream stream, String line) {
		try {
			String rawLine = Song.getCleanLine(line);
			int chordIndex = -1;
			Map<Integer, String> chordMap = new LinkedHashMap<>();
			while (line.indexOf("[", chordIndex) >= 0) {
				chordIndex = line.indexOf("[", chordIndex);
				if (line.indexOf("]", chordIndex) - chordIndex < Song.MAX_CHORD_LENGTH) {
					// false match, should not take more than 3 chars to closing brackets
					try {
						String chord = line.substring(chordIndex + 1, line.indexOf("]", chordIndex));
						if (chord != null) {
							chordMap.put(chordIndex, chord);
						}
					}
					catch (Exception e) {
// System.out.println("Kacke");
					}
				}
				chordIndex++;
			}
			if (rawLine.isBlank()) {
				stream.setFont(COMMENT_FONT, SUB_FONT_SIZE);
			} else {
				stream.setFont(CHORD_FONT, MAIN_FONT_SIZE);
			}
			int totalOffset = 0;
			for (Entry<Integer, String> entry : chordMap.entrySet()) {
				String chord = formatChord(entry.getValue());
				if (chord != null) {
					int index = entry.getKey();
					String subLine = line.substring(0, index);
					int missing = subLine.length() - Song.getCleanLine(subLine).length();
					String substring = rawLine.substring(0, index - missing);
					float offset = (MAIN_FONT.getStringWidth(substring) / 1000.0f * MAIN_FONT_SIZE) - totalOffset;
					if (totalOffset > 10 && offset < 10) {
						System.out.println("BREAK " + chord);
						stream.newLineAtOffset(10, 0);
					} else {
						stream.newLineAtOffset(offset, 0);
					}
					totalOffset += offset;
					stream.showText(chord);
				}
			}
			int lines = 0;
			if (!chordMap.isEmpty()) {
				stream.newLineAtOffset(0, -1.2f * MAIN_FONT_SIZE);
				stream.setFont(MAIN_FONT, MAIN_FONT_SIZE);
				line += 1;
			} else {
				stream.setFont(MAIN_FONT, SUB_FONT_SIZE);
			}
			stream.newLineAtOffset(-totalOffset, 0);
			if (!rawLine.isBlank()) {
				stream.showText(rawLine);
				lines += 1;
			}
			if (chordMap.isEmpty() || !rawLine.isBlank()) {
				stream.newLine();
			}
			return lines;
		}
		catch (IOException e) {
			LOG.info("Problem creating file", e);
		}
		return 0;
	}

	private static String formatChord(final String value) {
		try {
			String chord = value;
			if (value.length() == 1 && value.matches("[a-z]")) {
				chord = value.toUpperCase() + "m";
			}
			chord = chord.substring(0, 1).toUpperCase() + chord.substring(1).toLowerCase();
			LOG.debug("Changed key \"" + value + "\" to \"" + chord + "\"");
			return chord;
		}
		catch (Exception e) {
			System.out.println("Shit");
			return null;
		}
	}

	private static PDPageContentStream addMetaInfo(PDPageContentStream stream, PDPage page, Song song) {
		addTitle(stream, page, song);
		if (song.getKey() != null && !song.getKey().isEmpty()) {
			addKey(stream, page, song);
		}
		if (song.getCapo() != 0) {
			addCapo(stream, page, song);
		}
		return stream;
	}

	private static void addTitle(PDPageContentStream stream, PDPage page, Song song) {
		try {
			float titleWidth = TITLE_FONT.getStringWidth(song.getName()) / 1000 * TITLE_FONT_SIZE;
			stream.beginText();
			stream.setFont(TITLE_FONT, TITLE_FONT_SIZE);
			stream.newLineAtOffset((page.getMediaBox().getWidth() - titleWidth) / 2, page.getMediaBox().getHeight() - MARGIN_TITLE);
			stream.showText(song.getName());
			stream.endText();
			if (song.getAuthor() != null && !song.getAuthor().isEmpty()) {
				stream.moveTo(0, 0);
				stream.beginText();
				stream.setFont(META_FONT, META_FONT_SIZE);
				float authorWidth = META_FONT.getStringWidth(song.getAuthor()) / 1000 * META_FONT_SIZE;
				stream.newLineAtOffset((page.getMediaBox().getWidth() - authorWidth) / 2, page.getMediaBox().getHeight() - MARGIN_TITLE - TITLE_FONT_SIZE);
				stream.showText(song.getAuthor());
				stream.endText();
			}
		}
		catch (IOException e) {
			LOG.info("Problem adding title", e);
		}
	}

	private static void addKey(PDPageContentStream stream, PDPage page, Song song) {
		try {
			String key = "Key: " + song.getKey();
			stream.beginText();
			stream.setFont(META_FONT, META_FONT_SIZE);
			stream.newLineAtOffset((page.getMediaBox().getWidth() - META_FONT.getStringWidth(key) / 1000 * META_FONT_SIZE - 20), page.getMediaBox().getHeight() - MARGIN_TITLE);
			stream.showText(key);
			stream.endText();
		}
		catch (IOException e) {
			LOG.info("Problem adding title", e);
		}
	}

	private static void addCapo(PDPageContentStream stream, PDPage page, Song song) {
		try {
			String key = "Capo: " + song.getCapo();
			stream.beginText();
			stream.setFont(META_FONT, META_FONT_SIZE);
			stream.newLineAtOffset((page.getMediaBox().getWidth() - META_FONT.getStringWidth(key) / 1000 * META_FONT_SIZE - 20), page.getMediaBox().getHeight() - MARGIN_TITLE * 1.3f);
			stream.showText(key);
			stream.endText();
		}
		catch (IOException e) {
			LOG.info("Problem adding title", e);
		}
	}

	private static PDDocument createDocument(Song song) {
		PDDocument document = new PDDocument();
		return document;
	}

	private static PDPage addPage(PDDocument doc) {
		PDPage page = new PDPage();
		doc.addPage(page);
		return page;
	}

	private static boolean saveAndClose(PDDocument doc, File file) {
		try {
			for (PDPageContentStream s : streamMap.values()) {
				s.close();
			}
			doc.save(file);
			return true;
		}
		catch (IOException e) {
			LOG.info("Problem saving file", e);
		}
		try {
			doc.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	public static File pdfToPic(File file) {
		File f = null;
		try {
			PDDocument document = PDDocument.load(file);
			PDFRenderer pdfRenderer = new PDFRenderer(document);
			for (int page = 0; page < document.getNumberOfPages(); ++page) {
				BufferedImage bim = pdfRenderer.renderImageWithDPI(page, 300, ImageType.RGB);
				// suffix in filename will be used as the file format
				f = new File(file.getName() + "-" + (page + 1) + ".png");
				ImageIOUtil.writeImage(bim, f.getName(), 300);
			}
			document.close();
			return f;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
