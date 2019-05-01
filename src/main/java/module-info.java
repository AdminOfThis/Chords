module chords {

	exports data;
	exports util;
	exports main;
	exports gui.controller;
	exports gui.gui;

	opens gui.controller;
	opens data;

	requires util;
	requires transitive java.desktop;
	requires transitive javafx.base;
	requires transitive javafx.controls;
	requires transitive javafx.fxml;
	requires transitive javafx.graphics;
	requires transitive org.apache.logging.log4j;
	requires pdfbox;
	requires pdfbox.tools;
	requires java.xml.bind;
}