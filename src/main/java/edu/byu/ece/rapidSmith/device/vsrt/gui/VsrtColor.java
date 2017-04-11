package edu.byu.ece.rapidSmith.device.vsrt.gui;

import com.trolltech.qt.core.Qt;
import com.trolltech.qt.gui.QColor;


/**
 * QT 4.8.7 deprecated the API of "QColor.white". This class recreates it so that used colors are
 * created only one time at runtime instead of continuously. Colors can be added as needed.
 *
 */
public final class VsrtColor {

	public static final QColor white = new QColor(Qt.GlobalColor.white);
        public static final QColor black = new QColor(Qt.GlobalColor.black);
        public static final QColor red = new QColor(Qt.GlobalColor.red);
        public static final QColor darkRed = new QColor(Qt.GlobalColor.darkRed);
        public static final QColor green = new QColor(Qt.GlobalColor.green);
        public static final QColor darkGreen = new QColor(Qt.GlobalColor.darkGreen);
        public static final QColor blue = new QColor(Qt.GlobalColor.blue);
        public static final QColor darkBlue = new QColor(Qt.GlobalColor.darkBlue);
        public static final QColor cyan = new QColor(Qt.GlobalColor.cyan);
        public static final QColor darkCyan = new QColor(Qt.GlobalColor.darkCyan);
        public static final QColor magenta = new QColor(Qt.GlobalColor.magenta);
        public static final QColor darkMagenta = new QColor(Qt.GlobalColor.darkMagenta);
        public static final QColor yellow = new QColor(Qt.GlobalColor.yellow);
        public static final QColor darkYellow = new QColor(Qt.GlobalColor.darkYellow);
        public static final QColor gray = new QColor(Qt.GlobalColor.gray);
        public static final QColor darkGray = new QColor(Qt.GlobalColor.darkGray);
        public static final QColor lightGray = new QColor(Qt.GlobalColor.lightGray);
        public static final QColor transparent = new QColor(Qt.GlobalColor.transparent);
        public static final QColor darkOrange = new QColor(255,140,0);
}
