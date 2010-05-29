/*
 * Copyright 2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Founadation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */
package com.sun.lwuit.html;

import com.sun.lwuit.Button;
import com.sun.lwuit.CheckBox;
import com.sun.lwuit.ComboBox;
import com.sun.lwuit.Command;
import com.sun.lwuit.Component;
import com.sun.lwuit.Container;
import com.sun.lwuit.Display;
import com.sun.lwuit.Font;
import com.sun.lwuit.Form;
import com.sun.lwuit.Graphics;
import com.sun.lwuit.Label;
import com.sun.lwuit.List;
import com.sun.lwuit.RadioButton;
import com.sun.lwuit.TextArea;
import com.sun.lwuit.TextField;
import com.sun.lwuit.animations.Motion;
import com.sun.lwuit.events.ActionEvent;
import com.sun.lwuit.events.ActionListener;
import com.sun.lwuit.geom.Dimension;
import com.sun.lwuit.layouts.BorderLayout;
import com.sun.lwuit.layouts.BoxLayout;
import com.sun.lwuit.layouts.FlowLayout;
import com.sun.lwuit.plaf.Border;
import com.sun.lwuit.plaf.Style;
import com.sun.lwuit.plaf.UIManager;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * HTMLComponent is a LWUIT Component that renders HTML documents that conform to the XHTML Mobile Profile 1.0
 *
 * @author Ofir Leitner
 */
public class HTMLComponent extends Container implements ActionListener {

    /**
     * If true a full screen width will be assumed, this helps to make less internal components as text is aggregated to long labels
     * If false, then the width is flexible, but every word will be rendered as a separate label.
     * Note that if false, RTL texts will display in
     */
    static final boolean FIXED_WIDTH = false;

    /**
     * A constant that can be used to obfuscate out HTMLInputFormat if unnecessary
     */
    static final boolean SUPPORT_INPUT_FORMAT = true;

    /**
     * A constant that can be used to obfuscate out all CSS related code if unnecessary
     */
    static final boolean SUPPORT_CSS = true;

    /**
     * If true, when a page is requested, the previous page is immediately removed, thus helping conserve memory (Since only 1 page is held in memory simultanously)
     * In next releases a better and external control over this will be provided.
     */
    private static final boolean CLEAN_ON_PAGE_REQUEST = false;

    /**
     * If true, then table cells will be preset with a size according to their preferred size, before the table begins calculating needed sizes
     * This also requires loading all images in table cells prior to page display (Unlike the normal way in which they can be completed afterwards)
     * This is needed due to some complexity when calculating sizes of tables with complex containers as cells, and especially with nested tables
     */
    static final boolean TABLES_LOCK_SIZE = true;

    // Indentation for various tags
    private static final int INDENT_BLOCKQUOTE = 20;
    private static final int INDENT_DD = 20;
    private static final int INDENT_OL = 15; //Ordered list
    private static final int INDENT_UL = 15; //Unordered list

    // The minimum number of visible items of a multiple combobox (Unless it has less)
    private static int MIN_MULTI_COMBOBOX_ITEMS = 4;
    private static int MAX_MULTI_COMBOBOX_ITEMS = 6;

    /**
     * The default size (in characters) of an input textfield
     */
    private static final int DEFAULT_TEXTFIELD_SIZE = 20;

    /**
     * The default oolumns (in characters) of an textarea
     */
    private static final int DEFAULT_TEXTAREA_COLS = 20;

    /**
     * The default rows (in characters) of an textarea
     */
    private static final int DEFAULT_TEXTAREA_ROWS = 2;

    /**
     * Indicates a Justify alignment. Text justification is enabled only in FIXED_WIDTH mode
     */
    static final int JUSTIFY = 5;

    /**
     * The thickness of a line drawn by the HR tag
     */
    private static final int HR_THICKNESS = 3;

    /**
     * The time it takes a marquee to move across the screen (See -wap-marquee in CSS)
     */
    private static final int MARQUEE_DELAY = 6000;

    private static final boolean MATCH_EXACT_FONTS_ONLY = false;

    private static final boolean MATCH_BITMAP_FONTS_ONLY = true;

    private static final boolean MATCH_SAME_FONT_FAMILY_ONLY = true;

    /**
     * The default font to use
     */
    private static HTMLFont DEFAULT_FONT = new HTMLFont(null,Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM));

    /**
     * The color given to visited links
     */
    private static final int COLOR_VISITED_LINKS = 0x990099;

    /**
     * A hashtable containing all defined HTMLFont objects
     */
    static Hashtable fonts = new Hashtable();

    // Constants for the various possible input types (i.e. allowed values of the INPUT tag)
     private static final int INPUT_CHECKBOX = 0;
     private static final int INPUT_HIDDEN = 1;
     private static final int INPUT_PASSWORD = 2;
     private static final int INPUT_RADIO = 3;
     private static final int INPUT_RESET = 4;
     private static final int INPUT_SUBMIT = 5;
     private static final int INPUT_TEXT = 6;
     private static final int INPUT_BUTTON = 7; //button has no use other than javascript, which is not supported
     private static final int INPUT_FILE = 8; //file upload is not supported
     private static final int INPUT_IMAGE = 9;

    /**
     * Defines the possible values for the type attribute in the input tag, ordered according to the INPUT_* constants
     */
    private static String[] INPUT_TYPE_STRINGS = {
            "checkbox","hidden","password","radio","reset",
            "submit","text","button", "file","image"
    };

    /**
     * Vector holding the possible input type strings.
     */
    private static Vector INPUT_TYPES = new Vector();

     // Ordered list types

     /**
      * Ordered list type legal identifiers as can be defined in the ATTR_TYPE of the OL_TAG
      */
     private static final char[] ORDERED_LIST_TYPE_IDENTIFIERS = {'1','A','a','I','i'};

     /**
      * Inidcates whether this component should load CSS files and consider STYLE tag and attributes when available..
      * Note that CSS requires to create significantlly more containers, as almost every tag needs to be in its own container
      * in order to manipulate style attributes like bgcolor, border etc. This is why the developer can disable CSS.
      * Currently this is a private variable, once CSS is actually available this will be exposed via public getter/setter.
      * Also there's no point setting this to true in this release as it will not lead to actual CSS, but just prepare containers for it.
      */
    boolean loadCSS = SUPPORT_CSS;
    
    int contCount=0; // debug for CSS
     /**
     * Style sheets embedded directly into the style tag (not the style attribute)
     */
    private Vector embeddedCSS;

    /**
     * External style sheets referenced from LINK tags
     */
    private Vector externalCSS;


    /**
     * The default background color of an HTML document, can be changed with the BGCOLOR attribute in the BODY tag or via CSS
     */
    static final int DEFAULT_BGCOLOR = 0xffffff;

    /**
     * The default color of text in HTML documents, can be changed with the TEXT attribute in the BODY tag or via CSS
     */
    static final int DEFAULT_TEXT_COLOR = 0;

    /**
     * The default color of links in HTML documents, can be changed with the LINK attribute in the BODY tag or via CSS
     */
    static private final int DEFAULT_LINK_COLOR = 0x0000ff;


    // Document colors
    private int bgColor=DEFAULT_BGCOLOR;
    private int textColor=DEFAULT_TEXT_COLOR;
    private int linkColor=DEFAULT_LINK_COLOR;

    // Refrences to helper classes and delegates
    private DocumentRequestHandler handler; // The HTMLComponent's request handler
    private RedirectThread redirectThread; // A refrence to a redirection thread if exists
    private ResourceThreadQueue threadQueue; // A reference to the ResourceThreadQueue that handles asynchronous image download
    private HTMLCallback htmlCallback;

    // Page info and status
    private DocumentInfo docInfo; // The DocumentInfo object associated with this document
    private Element document; // The page DOM representation
    private String pageURL; // The current page's URL
    private int pageStatus=HTMLCallback.STATUS_NONE; // The page's status (See HTMLCallback)
    private boolean pageError; // true if there was an error during the page building process
    private String title; // The page's title (as obtained from the TITLE tag in the HEAD tag)
    private int displayWidth; // Used to store the display width to detect width changes later on.
    private boolean cancelled; // true if the page was cancelled (Signals all processing to stop)
    private boolean cancelledCaught; //true if the cancel signal was already caught (Used to avoid multiple cancel handling)
    boolean showImages=true; //true to download image, false otherwise
    private Style pageStyle; // The page's user defined style

    // Links related
    private Hashtable accessKeys = new Hashtable();// A hastable containing all the access keys in this document and their respective links
    private Hashtable anchors;// A hashtable containing all the anchors of this document
    private Hashtable inputFields; // A hashtable containing all the input fields in the page. Used for FOR labels
    Component firstFocusable; // The first focusable link on the page

    //Font
    private HTMLFont defaultFont = DEFAULT_FONT; // The default font

    // The following variables are used when building the component from the document.
    // After the component construction they are not used (Basically they can all be considered as "temporary" variables,
    // but due to the recursive nature of the document building operation, they are defined as members)

    //Containers

    /**
     * The main container is the top-level container to which other containers are added when building the page.
     * After the page was built the main container is added to the HTMLComponent.
     * We do not build directly on the HTMLComponent to allow the building process to be performed not in the EDT thread.
     */
    private Container mainContainer;

    /**
     * curContianer is the current container we are adding components to. AT the start it is the main container.
     * Later on it can be a certain table cell or a fieldset.
     */
    private Container curContainer;

    /**
     * curLine is the basic container to which components are added in the building process.
     * Components are added to it horizontally and when there no more space left (or a newline is needed) - it is added to the curContainer
     */
    private Container curLine;

    // Layouting - NOTE: In FIXED_WIDTH mode this component assumes the full width of the screen and performs text wrapping due to a bug in the combination of FlowLayout inside BoxLayout. In non fixed width mode the variables x & width are not used.
    private int x; // Holds the horizontal position in the curLine container
    private int width; // Holds the width available to each line
    private int leftIndent; // Holds the current left margin (can be changed by tags)
    private boolean lastWasEmpty; // true if the last line was empty, used to prevent unneeded multiple row spacing

    // Font
    private HTMLFont font; // Holds the current used font (starts with defaultFont and changes according to various tags and attributes)

    // Links
    private String link; // The current link address (so when a text is processed, we know to attribute it to this link)
    private HTMLLink mainLink; // The "main" link. If a link spans over 2 or more lines, the first segment is considered the "main" link and is responsible for highlighting all segments when focused.
    private char accesskey='\0'; // The current accesskey for a link
    private String anchor; // The current page anchor
    private boolean linkVisited; // True if the current link address was visited

    // Forms
    private HTMLForm curForm; // The current HTMLForm to which input fields should be added
    private TextArea curTextArea; // The current TextArea
    private List curComboBox; // The current ComboBox
    private boolean optionTag; // true if an option tag is open
    private boolean optionSelected; //true if the current option is marked as selected (i.e. the default selection)
    private String optionValue; // The current option value
    private Hashtable textfieldsToForms; // A hashtable that maps all input fields to their corresponding forms

    // Lists
    private int ulLevel; //Unordered list level (Used to display the right bullet)
    private int olIndex; //Index of the current item in the list
    private Vector olUpperLevelIndex;// = new Vector(); //Used to save the index when nesting
    private int listType; // The type of the current list
    private int listIndent; // Holds the current list margin

    // Tables
    private Vector tables;// A vector used for nesting, when a table contains a table ,the current one is "pushed" into the vector, to be "popped" later when the nested table processing ends.
    private Vector tableCells;// A vector used for nesting of table cells
    HTMLTableModel curTable; // The model of the currently collected table

    // Misc. tags
    private Vector fieldsets; // A vector used to support FIELDSET tag nesting
    private int preTagCount=0; // A counter used to support PRE tag nesting
    private int quoteTagCount=0; // A counter used to support QUOTE tag nesting
    private String labelForID; // Collects <label for="id">

    private Vector containers=new Vector();
    //private Vector elementsWithStyleAttr=new Vector();
    Vector marqueeComponents = new Vector();
    private Motion marqueeMotion;

    /**
     * This static segment sets up the INPUT_TYPES vector with values from INPUT_TYPE_STRINGS.
     * This is used later on for lookup.
     */
    static {
        for(int i=0;i<INPUT_TYPE_STRINGS.length;i++) {
            INPUT_TYPES.addElement(INPUT_TYPE_STRINGS[i]);
        }
    }

    /**
     * Sets the given LWUIT font for use with HTMLComponents.
     *
     * The font key can contain information about the following attributes:
     *
     * * The font family - i.e. Arial, Times New Roman etc.
     * * The font size - in pixels (i.e. 12, 14 etc.)
     * * The font style - bold, italic or both (no need to specify plain)
     * * The font tag assignments - if this font should be used for any HTML specific tags they should be specified - i.e. h1, kbd etc.
     * 
     * The key is just a concatenation of all attributes seperated with a dot.
     * Examples for valid keys:
     * arial.12 - Describes a plain font from the arial family in size 12
     * arial.16.bold - A bold arial font in size 16
     * courier.italic.bold.20 - A bold and italic courier font in size 20
     * arial.20.h1 - A plain arial font, size 20, that should be used for contents of the H1 tag
     * code.kbd.samp - A font that should be used for the CODE, KBD and SAMP tags
     * 
     * Note that the order of the attributes is not important and also that the case is ignored.
     * This means that arial.12.bold.italic.h3 is equivalent to itALIc.H3.arial.BOLD.12
     * 
     * Also note that while you do not have to provide all the info for the font, but the info helps the rendering engine to reuse fonts when suitable.
     * For example, if you have a 16px arial bold font which you want to use for H2, you can simply add it as "h2", but if you add it as "arial.16.bold.h2"
     * then if the current font is arial.16 and the renderer encounters a B tag, it will know it can use the font you added as the bold counterpart of the current font.
     * 
     * When adding system fonts there is no need to describe the font, the usage of setFont with system fonts is usually just to assign them for tags.
     * The rendering engine knows to derive bold/italic/bigger/smaller fonts from other system fonts (default or tag fonts) even if not added.
     *      
     * @param fontKey The font key in the format described above
     * @param font The actual LWUIT font object
     */
    public static void addFont(String fontKey, Font font) {
        //TODO - validation at add time
        fonts.put(fontKey,new HTMLFont(fontKey.toLowerCase(),font));
    }

    /**
     * Adds the given symbol and code to the user defined char entities table.
     * Symbols do not need to include leading & and trailing ; - these will be trimmed if given as the symbol
     *
     * @param symbol The symbol to add
     * @param code The symbol's code
     */
    public static void addCharEntity(String symbol,int code) {
        Parser.addCharEntity(symbol, code);
    }

    /**
     * Adds support for a special key to be used as an accesskey.
     * The CSS property -wap-accesskey supports special keys, for example "phone-send" that may have different key codes per device.
     * This method allows pairing between such keys to their respective key codes.
     * Note that these keys are valid only for -wap-aceesskey in CSS files, and not for the XHTML accesskey attribute.
     *
     * @param specialKeyName The name of the special key as denoted in CSS files
     * @param specialKeyCode The special key code
     */
    public static void addSpecialKey(String specialKeyName,int specialKeyCode) {
        if (SUPPORT_CSS) {
            CSSEngine.addSpecialKey(specialKeyName, specialKeyCode);
        } else {
            System.out.println("HTMLCoponent.addSpecialKey: LWUIT was compiled with HTMLComponent.SUPPORT_CSS false, this method has no effect");
        }
    }

    /**
     * Adds the given symbols array  to the user defined char entities table with the startcode provided as the code of the first string, startcode+1 for the second etc.
     * Some strings in the symbols array may be null thus skipping code numbers.
     *
     * @param symbols The symbols to add
     * @param startcode The symbol's code
     */
    public static void addCharEntitiesRange(String[] symbols,int startcode) {
        Parser.addCharEntitiesRange(symbols, startcode);
    }

    /**
     * Sets the maximum number of threads to use for image download
     *
     * @param threadsNum the maximum number of threads to use for image download
     */
    public static void setMaxThreads(int threadsNum) {
        ResourceThreadQueue.setMaxThreads(threadsNum);
    }

    /**
     * Sets the supported CSS media types to the given strings.
     * Usually the default media types ("all","handheld") should be suitable, but in case this runs on a device that matches another profile, the developer can specify it here.
     *
     * @param supportedMediaTypes A string array containing the media types that should be supported
     */
    public static void setCSSSupportedMediaTypes(String[] supportedMediaTypes) {
        if (SUPPORT_CSS) {
            Parser.setCSSSupportedMediaTypes(supportedMediaTypes);
        } else {
            System.out.println("HTMLCoponent.setCSSSupportedMediaTypes: LWUIT was compiled with HTMLComponent.SUPPORT_CSS false, this method has no effect");
        }
    }


    /**
     * Constructs HTMLComponent
     * 
     * @param handler The HttpRequestHandler to which all requestes for links will be sent
     */
    public HTMLComponent(DocumentRequestHandler handler) {
        //setLayout(new BoxLayout(BoxLayout.Y_AXIS));
        setLayout(new BorderLayout());
        this.handler=handler;
        threadQueue=new ResourceThreadQueue(this);
        setHandlesInput(true);
        setScrollableY(true);
        setScrollableX(false);
        setSmoothScrolling(true);

        //Create some default fonts
        HTMLFont italic=new HTMLFont(null,Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_ITALIC, Font.SIZE_MEDIUM));
        HTMLFont monospace=new HTMLFont(null, Font.createSystemFont(Font.FACE_MONOSPACE, Font.STYLE_PLAIN, Font.SIZE_MEDIUM));

        //Assign default fonts to some tags
        fonts.put(Element.TAG_NAMES[Element.TAG_EM], italic);
        fonts.put(Element.TAG_NAMES[Element.TAG_STRONG], new HTMLFont(null,Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM)));
        fonts.put(Element.TAG_NAMES[Element.TAG_DFN], italic);
        fonts.put(Element.TAG_NAMES[Element.TAG_CODE], monospace);
        fonts.put(Element.TAG_NAMES[Element.TAG_SAMP], monospace);
        fonts.put(Element.TAG_NAMES[Element.TAG_KBD], monospace);
        fonts.put(Element.TAG_NAMES[Element.TAG_VAR], italic);
        fonts.put(Element.TAG_NAMES[Element.TAG_CITE], italic);
        fonts.put(Element.TAG_NAMES[Element.TAG_PRE], monospace);
        fonts.put(Element.TAG_NAMES[Element.TAG_H1], new HTMLFont(null, Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE)));
        fonts.put(Element.TAG_NAMES[Element.TAG_H2], new HTMLFont(null, Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_ITALIC, Font.SIZE_LARGE)));
        fonts.put(Element.TAG_NAMES[Element.TAG_H3], new HTMLFont(null, Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM)));

    }

    /**
     * Returns the document request handler
     *
     * @return the document request handler
     */
    public DocumentRequestHandler getRequestHandler() {
        return handler;
    }

    /**
     * Returns the DocumentInfo that currently represents the document loaded/shown
     *
     * @return the DocumentInfo that currently represents the document loaded/shown
     */
    public DocumentInfo getDocumentInfo() {
        return docInfo;
    }

    /**
     * Sets an HTMLCallback to listen to this HTMLCOmponent
     * 
     * @param callback The HTMLCallback that will receive events
     */
    public void setHTMLCallback(HTMLCallback callback) {
        htmlCallback=callback;
    }

    /**
     * Returns the HTMLCallback that is set on this HTMLComponent
     *
     * @return the HTMLCallback that is set on this HTMLComponent or null if none
     */
    public HTMLCallback getHTMLCallback() {
        return htmlCallback;
    }


    /**
     * Sets the default font for this HTMLComponent
     * 
     * @param fontKey The font key in the format described in setFont
     * @param font The actual LWUIT font object
     * @see setFont
     */
    public void setDefaultFont(String fontKey, Font font) {
        defaultFont=new HTMLFont(fontKey, font);
        if (fontKey!=null) {
            fonts.put(fontKey.toLowerCase(),defaultFont);
        }
    }

    /**
     * Sets whether this HTMLComponent will download and show linked images or not
     * 
     * @param show true to show images, false otherwise
     */
    public void setShowImages(boolean show) {
        showImages=show;
    }

    /**
     * Sets whether this HTMLComponent will ignore all CSS.directives.
     * This includes external CSS files (which won't be downloaded), embedded CSS segmentsand style tags and attributes.
     * By default this is false.
     *
     * @param show true to ignore CSS directives, false otherwise
     */
    public void setIgnoreCSS(boolean ignore) {
        if (SUPPORT_CSS) { // If not supporting CSS, the loadCSS flag is locked on false
            loadCSS=!ignore;
        }
    }

    /**
     * Scrolls the HTMLComponent several pixels forward/backward.
     *
     * @param pixels The number of pixels to scroll (positive for forward and negative for backward)
     * @param animate true to animate the scrolling, false otherwise
     */
    public void scrollPixels(int pixels,boolean animate) {
        int scrollToY=getScrollY()+pixels;
        scrollTo(scrollToY,animate);
    }

    /**
     * Scrolls the HTMLComponent several pages forward/backward.
     * TO scroll to the start or end of the document, one can provide a very big number.
     * 
     * @param pages The number of pages to scroll (positive for forward and negative for backward)
     * @param animate true to animate the scrolling, false otherwise
     */
    public void scrollPages(int pages,boolean animate) {
        int scrollToY=getScrollY()+getHeight()*pages;
        scrollTo(scrollToY,animate);

    }

    /**
     * Scrolls the HTMLComponent to the denoted Y position
     * @param y The Y-coordinate to scroll to
     * @param animate true to animate the scrolling, false otherwise
     */
    private void scrollTo(int y,boolean animate) {
        if (y<0) {
            y=0;
        } else if (y>getPreferredH()-getHeight()) {
            y=getPreferredH()-getHeight();
        }
        if (animate) {
            scrollRectToVisible(getX(), y, getWidth(), getHeight(), this);
        } else {
            setScrollY(y);
        }
    }

    /**
     * Sets the given string containing HTML code as this HTMLComponent's body
     *
     * @param htmlText The HTML body to set
     */
    public void setBodyText(String htmlText) {
        setBodyText(htmlText, null);
    }

    /**
     * Sets the given string containing HTML code as this HTMLComponent's body.
     * The string is read using the specified encoding. If the encoding is not supported it will be read without encoding
     *
     * @param htmlText The HTML body to set
     * @param encoding The encoding to use when reading the HTML i.e. UTF8, ISO-8859-1 etc.
     * @return true if the encoding succeeded, false otherwise
     */
    public boolean setBodyText(String htmlText,String encoding) {
        return setHTML(htmlText, encoding, null,false);
    }
    
    /**
     * Sets the given string containing HTML code either as this HTMLComponent's body or as the full HTML.
     * The string is read using the specified encoding. If the encoding is not supported it will be read without encoding
     *
     * @param htmlText The HTML to set
     * @param encoding The encoding to use when reading the HTML i.e. UTF8, ISO-8859-1 etc.
     * @param title The HTML title, or null if none (Used only when isFullHTML is false)
     * @param isFullHTML true if this is a full HTML document (with html/body tags), false if this HTML should be used as the HTMLComponent's body
     * @return true if the encoding succeeded, false otherwise
     */
    public boolean setHTML(String htmlText,String encoding,String title,boolean isFullHTML) {
        boolean success=true;
        InputStreamReader isr = getStream(htmlText, encoding, title ,isFullHTML);

        if (isr==null) {
            isr=getStream("Encoding error loading string", null, title, isFullHTML); //TODO - dynamic error messages
            success=false;
        }
        final InputStreamReader isReader=isr;

        new Thread() {
            public void run() {
                Element doc = Parser.getInstance().parse(isReader,HTMLComponent.this);
                documentReady(null, doc);
            }
        }.start();

        return success;
    }

    /**
     * Convenience method that calls getStream(String,String,String,boolean)
     *
     * @param htmlText The HTML to set
     * @param encoding The encoding to use when reading the HTML i.e. UTF8, ISO-8859-1 etc.
     * @return A stream representing the HTML
     */
    private InputStreamReader getStream(String htmlText,String encoding) {
        return getStream(htmlText, encoding, null, false);
    }

    /**
     * Obtains a stream of the given HTML with the given encoding
     *
     * @param htmlText The HTML to set
     * @param encoding The encoding to use when reading the HTML i.e. UTF8, ISO-8859-1 etc.
     * @param title The HTML title, or null if none
     * @param isFullHTML true if this is a full HTML document (with html/body tags), false otherwise
     * @return A stream representing the HTML
     */
    private InputStreamReader getStream(String htmlText,String encoding,String title,boolean isFullHTML) {
        if (!isFullHTML) {
            String titleStr="";
            if (title!=null) {
                titleStr="<head><title>"+title+"</title></head>";
            }
            htmlText="<html>"+titleStr+"<body>"+htmlText+"</body></html>";
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(htmlText.getBytes());
        InputStreamReader isr=null;
        if (encoding!=null) {
            try {
                isr = new InputStreamReader(bais,encoding);
            } catch (UnsupportedEncodingException uee) {
                uee.printStackTrace();
            }
        }
        if (isr==null) { //encoding wasn't specified or failed
            isr = new InputStreamReader(bais);
        }
        return isr;
    }

    /**
     * Cancels the loading of the current page
     */
    public void cancel() {
          cancelled=true;
          cancelRedirectsAndImages();
    }

    /**
     * Adds the specified cssElement which represents an external CSS file to the external CSS vector
     * 
     * @param cssElement The element to add
     */
    void addToExternalCSS(CSSElement cssElement) {
        if (externalCSS==null) {
            externalCSS=new Vector();
        }
        externalCSS.addElement(cssElement);
    }

    /**
     * Adds the specified cssElement which represents an embedded CSS segment to the embedded CSS vector
     *
     * @param cssElement The element to add
     */
    void addToEmebeddedCSS(CSSElement cssElement) {
        if (embeddedCSS==null) {
            embeddedCSS=new Vector();
        }
        embeddedCSS.addElement(cssElement);
    }

    /**
     * Sets this HTMLComponent to render the document in the specified URL
     * 
     * @param pageURL The URL containing the HTML document
     */
    public void setPage(final String pageURL) {
        setPage(new DocumentInfo(pageURL));
    }

    /**
     * Sets the style of the page, allowing for example to set transparency to the main page.
     * This applies not only to the current page, but rather to all pages created with this HTMLComponent instance.
     *
     * @param pageStyle The style to set to the page
     */
    public void setPageStyle(Style pageStyle) {
        this.pageStyle=pageStyle;
        if ((mainContainer!=null) && (pageStyle!=null)) {
            applyPageStyle();
        }
    }

    /**
     * Applies the user defined page style to the main container. This clones the page style and sets the clone to the page, as various CSS directives may change the style of the main container.
     */
    private void applyPageStyle() {
        Style pageStyleCopy=new Style(pageStyle);
        mainContainer.setUnselectedStyle(pageStyleCopy);
        mainContainer.setSelectedStyle(pageStyleCopy);
    }

    /**
     * Sets this page to render the document specified in th DocumentInfo object
     * 
     * @param docInfo Containing info about the document (url, encoding etc)
     */
    void setPage(final DocumentInfo docInfo) {
        cancelRedirectsAndImages();
        if  ((pageStatus==HTMLCallback.STATUS_REQUESTED) ||
                (pageStatus==HTMLCallback.STATUS_CONNECTED) ||
                (pageStatus==HTMLCallback.STATUS_PARSED)) {  //previous page still loading
            cancel();

            // TODO - This mechanism is far from ideal - handle better page life cycle
            int waitTime=0;
            while ((pageStatus!=htmlCallback.STATUS_CANCELLED) && (waitTime<2500)) {
                System.out.println("Waiting for previous page to cancel "+System.currentTimeMillis());
                try { Thread.sleep(50); } catch (Exception e) { }
                waitTime+=50;

            }
        }
        cancelled=false;
        cancelledCaught=false;
        
        if (CLEAN_ON_PAGE_REQUEST) {
            cleanup();
            removeAll();
            repaint();
        }
        this.docInfo=docInfo; // Moved here since when getting CSS imports in embedded style segments, we need to know the relative URL

        new Thread() {

            public void run() {
                InputStreamReader isr=null;
                setPageStatus(HTMLCallback.STATUS_REQUESTED);
                try {
                    InputStream is=handler.resourceRequested(docInfo);
                    if (is!=null) {
                        isr = new InputStreamReader(is,docInfo.getEncoding());
                    }
                } catch (Exception uee) {
                    boolean cont=true;
                    if (htmlCallback!=null) {
                        cont=htmlCallback.parsingError(HTMLCallback.ERROR_ENCODING,null ,null,null,"Page encoding "+docInfo.getEncoding()+" failed: "+uee.getMessage());
                    }
                    if (cont) { //retry without the encoding
                        try {
                            isr = new InputStreamReader(handler.resourceRequested(docInfo));
                        } catch (Exception e) {
                            htmlCallback.parsingError(HTMLCallback.ERROR_ENCODING,null ,null,null,"Page loading failed, porbably due to wrong encoding. "+e.getMessage());
                            isr = getStream("Page loading failed, probably due to encoding mismatch.", null);
                            setPageStatus(HTMLCallback.STATUS_ERROR);
                        }
                    } else {
                        isr = getStream("Page encoding not supported", null);
                        setPageStatus(HTMLCallback.STATUS_ERROR);
                    }
                }

                if (cancelled) {
                    isr=getStream("Page loading cancelled by user",null);
                    setPageStatus(HTMLCallback.STATUS_CANCELLED);
                }

                if (isr==null) {
                    if (htmlCallback!=null) {
                        htmlCallback.parsingError(HTMLCallback.ERROR_CONNECTING, null, null, null, "Error connecting to stream");
                    }
                    setPageStatus(HTMLCallback.STATUS_ERROR);
                    isr = getStream("Error connecting to stream", null); //TODO - dynamic error messages
                } else {
                    setPageStatus(HTMLCallback.STATUS_CONNECTED);
                }

                Element newDoc=null;
                
                try {
                    newDoc=Parser.getInstance().parse(isr,HTMLComponent.this);
                } catch (IllegalArgumentException iae) {
                    iae.printStackTrace();
                    setPageStatus(HTMLCallback.STATUS_ERROR);
                    isr = getStream("Parsing error "+iae.getMessage(), null);
                    newDoc=Parser.getInstance().parse(isr,HTMLComponent.this);
                }

                if (cancelled) {
                    isr=getStream("Page loading cancelled by user",null);
                    newDoc=Parser.getInstance().parse(isr,HTMLComponent.this);
                    setPageStatus(HTMLCallback.STATUS_CANCELLED);
                }


                setPageStatus(HTMLCallback.STATUS_PARSED);
                documentReady(docInfo, newDoc);
            }
        }.start();
    }

    /**
     * Called internally after both reading and parsing of the HTML document has completed
     * 
     * @param pageURL The URL of the page
     * @param newDocument An element which is the root of the document
     */
    void documentReady(DocumentInfo docInfo,Element newDocument) {
        this.pageURL=null;
        if (docInfo!=null) {
            this.pageURL=docInfo.getUrl();
        }
        document=newDocument;
        cleanup();
        rebuildPage();
        
        if ((!cancelled) || (cancelledCaught)) {
            Display.getInstance().callSerially(new Runnable() {

                public void run() {

                        if (threadQueue.getCSSCount()==-1) {
                            displayPage();
                        }
                        //if ( ((!showImages) || (threadQueue.getQueueSize()==0)) {
                        if (threadQueue.getQueueSize()==0) {
                            setPageStatus(HTMLCallback.STATUS_COMPLETED);
                        } else {
                            threadQueue.startRunning();
                        }

                        if (pageURL!=null) { // pageURL can be null if the page was set using setBodyText and not setPage
                            int hash=pageURL.indexOf('#');

                            if ((hash!=-1) && (pageURL.length()>hash+1)) { // URL contains an anchor
                                String anchorName=pageURL.substring(hash+1);
                                goToAnchor(anchorName);
                            }
                        }


                }
            });
        } else { // Page was cancelled
            setPageStatus(HTMLCallback.STATUS_CANCELLED);
            InputStreamReader isr=getStream("Page loading cancelled by user",null);
            Element newDoc=Parser.getInstance().parse(isr,this);
            documentReady(docInfo, newDoc);
        }

    }


    void cssCompleted() {
        Display.getInstance().callSerially(new Runnable() {

            public void run() {
                displayPage();
            }
        });
    }

    /**
     * Actually displays the HTML page - this should be run on EDT
     */
    void displayPage() {
        removeAll();
        addComponent(BorderLayout.CENTER,mainContainer);
        setScrollY(0);

        revalidate();
        repaint();

        //Places the focus on the first link in the page, as long as it is within the visible area of the first page
        if (getComponentForm()!=null) {
            if (firstFocusable!=null) {
                if (firstFocusable.getY()<getHeight()) {
                    getComponentForm().setFocused(firstFocusable);
                } else {
                    getComponentForm().setFocused(mainContainer);
                }
            } else {
                    mainContainer.setFocusable(true); // If there are no focused components, the main container will become focusable, thus enabling it to be pixel-scrolled
                    getComponentForm().setFocused(mainContainer);                
            }

            if (marqueeComponents.size()>0) {
                getComponentForm().registerAnimated(HTMLComponent.this);
                int dir=UIManager.getInstance().getLookAndFeel().isRTL()?1:-1;
                marqueeMotion=Motion.createLinearMotion(0, dir*HTMLComponent.this.getWidth(), MARQUEE_DELAY/2);
                marqueeMotion.start();
            }


        }

        setPageStatus(HTMLCallback.STATUS_DISPLAYED);
    }


    public boolean animate() {
        boolean result=super.animate();
        if (marqueeMotion==null) {
            return result;
        }
        for (Enumeration e=marqueeComponents.elements();e.hasMoreElements();) {
            Component cmp=(Component)e.nextElement();
            cmp.setX(marqueeMotion.getValue());
        }
        if (marqueeMotion.isFinished()) {
            int dir=UIManager.getInstance().getLookAndFeel().isRTL()?1:-1;
            marqueeMotion=Motion.createLinearMotion(dir*-HTMLComponent.this.getWidth(), dir*HTMLComponent.this.getWidth(), MARQUEE_DELAY);
            marqueeMotion.start();
        }

        return true;
    }




    /**
     * Returns the HTML page's title as described in its TITLE tag
     *
     * @return the HTML page's title as described in its TITLE tag
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns the page's URL
     * 
     * @return the current page's URL
     */
    public String getPageURL() {
        return pageURL;
    }

    /**
     * Returns the page status
     *
     * @return the page status (One of the STATUS_* constants in HTMLCallback)
     */
    public int getPageStatus() {
        return pageStatus;
    }

    /**
     * Sets the page status to the given one
     *
     * @param status The new page status
     */
    void setPageStatus(final int status) {
        if ((!pageError) || (status==HTMLCallback.STATUS_REQUESTED)) { //Once the page error flag has been raised the page status doesn't change until a new page is requested
            pageStatus=status;
            if (htmlCallback!=null) {
                if (Display.getInstance().isEdt()) {
                    htmlCallback.pageStatusChanged(this, status, pageURL);
                } else {
                    Display.getInstance().callSerially(new Runnable() {
                        public void run() {
                            htmlCallback.pageStatusChanged(HTMLComponent.this, status, pageURL);
                        }
                    });
                }
            }
            pageError=((status==HTMLCallback.STATUS_ERROR) || (status==HTMLCallback.STATUS_CANCELLED));
            cancelledCaught=(status==HTMLCallback.STATUS_CANCELLED);
        }
    }

    /**
     * Returns the closest font by the given parameters
     * 
     * @param family The font family
     * @param size The font size
     * @param style The font style (Font.STYLE_PLAIN or Font.STYLE_ITALIC)
     * @param weight The font weight (Font.STYLE_PLAIN or Font.STYLE_BOLD)
     * @return the closest font the HTMLComponent could find or null if none found.
     */
    Font getClosestFont(String family, int size, int style, int weight) {
        final int FACTOR_FONT_FAMILY = 30;
        final int FACTOR_FONT_SIZE = 5;
        final int FACTOR_FONT_STYLE = 10;
        HTMLFont bestFit=null;
        int bestFitDistance=10000;
        Enumeration e=fonts.elements();
        while (e.hasMoreElements()) {
            int curFontDistance=0;
            HTMLFont hFont=(HTMLFont)e.nextElement();
            if ((MATCH_BITMAP_FONTS_ONLY) && (hFont.isSystemFont())) {
                continue;
            }
            if (family!=null) {
                if (!hFont.getFamily().equalsIgnoreCase(family)) {
                    if (MATCH_SAME_FONT_FAMILY_ONLY) {
                        continue;
                    }
                    curFontDistance+=FACTOR_FONT_FAMILY;
                }
            }
            if (size>0) {
                curFontDistance+=Math.abs(size-hFont.getSizeInPixels())*FACTOR_FONT_SIZE;
            }

            if (weight>=0) {
                if ((weight & Font.STYLE_BOLD)!=(hFont.getStyle() & Font.STYLE_BOLD)) {
                    curFontDistance+=FACTOR_FONT_STYLE;
                }
            }
            if (style>=0) {
                if ((style & Font.STYLE_ITALIC)!=(hFont.getStyle() & Font.STYLE_ITALIC)) {
                    curFontDistance+=FACTOR_FONT_STYLE;
                }
            }
            if (curFontDistance<bestFitDistance) {
                bestFitDistance=curFontDistance;
                bestFit=hFont;

                if (curFontDistance==0) { //exact match
                    break;
                }

            }

        }
        if ((bestFit==null) || ((MATCH_EXACT_FONTS_ONLY) && (bestFitDistance!=0))) {
            return null;
        } else {
            return bestFit.getFont();
        }
    }

    /**
     * Returns the HTMLFont that holds the given font.
     * This is in fact a "reverse lookup" for HTMLFonts
     * 
     * @param font The font to search
     * @return the HTMLFont that holds the given font.
     */
    HTMLFont getHTMLFont(Font font) {
        for (Enumeration e=fonts.elements();e.hasMoreElements();) {
            HTMLFont hFont=(HTMLFont)e.nextElement();
            if (hFont.getFont()==font) {
                return hFont;
            }
        }
        return null;
    }

    /**
     * Returns true if there's at least one smallcaps font in the fonts repository, or false otherwise
     * 
     * @return true if there's at least one smallcaps font in the fonts repository, or false otherwise
     */
    boolean isSmallCapsFontAvailable() {
        for (Enumeration e=fonts.elements();e.hasMoreElements();) {
            HTMLFont hFont=(HTMLFont)e.nextElement();
            if (hFont.getFamily().equals(CSSElement.SMALL_CAPS_STRING)) {
                return true;
            }
        }
        return false;
    }

    /**
     * A utility method to convert between Element TAG_* constants and HTMLFont font attribute constants.
     * 
     * @param tag The tag that describes the font attribute 
     * @param font The font for which we want a counterpart font
     * @return the counterpart font for the given font in teh specified tag sense.
     */
    private HTMLFont getCounterpartFont(int tag,HTMLFont font) {
        int attribute=-1;
        switch(tag) {
            case Element.TAG_B:
                attribute=HTMLFont.BOLD;
                break;
            case Element.TAG_I:
                attribute=HTMLFont.ITALIC;
                break;
            case Element.TAG_BIG:
                attribute=HTMLFont.BIG;
                break;
            case Element.TAG_SMALL:
                attribute=HTMLFont.SMALL;
                break;
        }
        if (attribute==-1) {
            return font;
        }
        return getCounterpartFontByAttribute(attribute, font);
    }

    /**
     * Returns the counterpart font according to the given attribute and font
     * 
     * @param attribute The requested attribute, one of HTMLFont.BOLD/ITALIC/BIG/SMALL
     * @param font The source font
     * @return the counterpart font according to the given attribute and font
     */
    HTMLFont getCounterpartFontByAttribute(int attribute,HTMLFont font) {
        HTMLFont cFont=font.getCounterpartFont(attribute);
        if (cFont!=null) {
             return cFont;
        }

        HTMLFont bestFit=null;
        Enumeration e=fonts.elements();
        while (e.hasMoreElements()) {
            HTMLFont hFont=(HTMLFont)e.nextElement();
            if (hFont.isCounterpart(attribute,font)) {
                if (attribute==HTMLFont.BIG) { //takes the closests font (i.e. big but not biggest)
                    if ((bestFit==null) || (bestFit.getSize()>hFont.getSize())) {
                        bestFit=hFont;
                    }
                } else if (attribute==HTMLFont.SMALL) {
                    if ((bestFit==null) || (bestFit.getSize()<hFont.getSize())) {
                        bestFit=hFont;
                    }
                } else { // BOLD, ITALIC
                    font.setCounterpartFont(attribute,hFont);
                    return hFont;
                }
            }
        }

        if (bestFit==null) {
            bestFit=font;
        }
        font.setCounterpartFont(attribute,bestFit);
        return bestFit;
    }

    /**
     * Rebuilds the HTMLComponent, this is called usually after a new page was loaded.
     */
    private void cleanup() {
        displayWidth=Display.getInstance().getDisplayWidth();

        //reset all building process values
        
        leftIndent=0;
        x=0;

        containers=new Vector();
        //embeddedCSS=null; // Shouldn't nullify as embedded style tag is collected in the head phase which is before..
        marqueeComponents = new Vector();
        marqueeMotion=null;
        anchors=new Hashtable();
        anchor=null;
        
        accesskey='\0';
        for (Enumeration e=accessKeys.keys();e.hasMoreElements();) {
            int keyCode=((Integer)e.nextElement()).intValue();
            getComponentForm().removeKeyListener(keyCode,this);
        }
        accessKeys.clear(); //=new Hashtable();

        fieldsets=new Vector();
        
        curTable=null;
        tables=new Vector();
        tableCells=new Vector();
        
        ulLevel=0;
        olIndex=Integer.MIN_VALUE;
        olUpperLevelIndex=new Vector();
        listType=HTMLListIndex.LIST_NUMERIC;
        
        font=defaultFont;

        labelForID=null;
        inputFields=new Hashtable();

        link=null;
        linkVisited=false;
        mainLink=null;
        firstFocusable=null;

        curForm=null;
        curTextArea=null;
        curComboBox=null;
        textfieldsToForms=new Hashtable();

        optionTag=false;
        optionSelected=false;

        preTagCount=0;
        quoteTagCount=0;

        mainContainer=new Container();

        if (pageStyle!=null) {
            applyPageStyle();
        }

        mainContainer.setScrollableX(false);
        mainContainer.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        curContainer=mainContainer;
        curLine=new Container();
        lastWasEmpty=false;

        width=Display.getInstance().getDisplayWidth()-getStyle().getMargin(Component.LEFT)-getStyle().getPadding(Component.LEFT)-
                getStyle().getMargin(Component.RIGHT)-getStyle().getPadding(Component.RIGHT)-10; // The -10 is arbitrary to avoid edge cases
    }

    /**
     * Handles a LINK tag and extracts any linked CSS file
     * 
     * @param linkTag AN element containing the LINK tag
     */
    private void handleLinkTag(Element linkTag) {
        String linkType=linkTag.getAttributeById(Element.ATTR_TYPE);
        String media=linkTag.getAttributeById(Element.ATTR_MEDIA);
        String href=linkTag.getAttributeById(Element.ATTR_HREF);
        String charset=linkTag.getAttributeById(Element.ATTR_CHARSET);
        if ((linkType!=null) && (linkType.equalsIgnoreCase("text/css")) && (href!=null) && (Parser.getInstance().mediaTypeMatches(media))) {
            if (docInfo!=null) {
                threadQueue.addCSS(docInfo.convertURL(href),charset);
            } else {
                if (DocumentInfo.isAbsoluteURL(href)) {
                    threadQueue.addCSS(href,charset);
                } else {
                    if (htmlCallback!=null) {
                        htmlCallback.parsingError(HTMLCallback.ERROR_NO_BASE_URL, linkTag.getName(),linkTag.getAttributeName(new Integer(Element.ATTR_HREF)),href,"Ignoring CSS file referred in a LINK tag ("+href+"), since page was set by setBody/setHTML so there's no way to access relative URLs");
                    }

                }
            }
        }
    }

    private void rebuildPage() {

        // Scan for LINK tags directly under the ROOT element, these are in fact converted <?xml-stylesheet ... ?> processing instructions
        if ((SUPPORT_CSS) && (loadCSS)) {
            for(int i=0;i<document.getNumChildren();i++) {
                Element child=document.getChildAt(i);
                if (child.getId()==Element.TAG_LINK) {
                    handleLinkTag(child);
                }
            }
        }

        // Get the HTML root tag and extract the HEAD and BODY (Note that the document tag is ROOT which contains HTML and so on.
        Element html=document.getChildById(Element.TAG_HTML);
        Element body=null;
        Element head=null;
        if (html!=null) {
            String dir=html.getAttributeById(Element.ATTR_DIR);

            // TODO - Should be applyRTL, but it doesn't work, setRTL is problematic since it changes the direction of the whole application, and we only need to change the component
            UIManager.getInstance().getLookAndFeel().setRTL((dir!=null) && (dir.equals("rtl")));
            body=html.getChildById(Element.TAG_BODY);
            head=html.getChildById(Element.TAG_HEAD);
        }

        // Fetch the document's title
        title=null;
        if (head!=null) {
            Element baseTag=head.getChildById(Element.TAG_BASE);
            if (baseTag!=null) {
                String baseURL=baseTag.getAttributeById(Element.ATTR_HREF);
                if ((baseURL!=null) && (docInfo!=null)) {
                    docInfo.setBaseURL(baseURL);
                }
            }
            Element titleTag=head.getChildById(Element.TAG_TITLE);
            if (titleTag!=null) {
                Element titleText=titleTag.getChildById(Element.TAG_TEXT);
                if (titleText!=null) {
                    title=titleText.getAttributeById(Element.ATTR_TITLE);
                }
            }
            
            if ((SUPPORT_CSS) && (loadCSS)) { // Scan for LINK tags under the HEAD tag
                for(int i=0;i<head.getNumChildren();i++) {
                    Element child=head.getChildAt(i);
                    if (child.getId()==Element.TAG_LINK) {
                        handleLinkTag(child);
                    }
                }
            }

        }

        if (htmlCallback!=null) {
            Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    htmlCallback.titleUpdated(HTMLComponent.this, title);
                }
            });
        }

        if (body!=null) {
            bgColor=Element.getColor(body.getAttributeById(Element.ATTR_BGCOLOR),-1);
            textColor=Element.getColor(body.getAttributeById(Element.ATTR_TEXT),DEFAULT_TEXT_COLOR);
            linkColor=Element.getColor(body.getAttributeById(Element.ATTR_LINK),DEFAULT_LINK_COLOR);
            if (bgColor!=-1) {
                mainContainer.getUnselectedStyle().setBgColor(bgColor);
                mainContainer.getUnselectedStyle().setBgTransparency(0);
                mainContainer.getSelectedStyle().setBgColor(bgColor);
                mainContainer.getSelectedStyle().setBgTransparency(0);
            } else if (pageStyle==null) {
                mainContainer.getUnselectedStyle().setBgColor(DEFAULT_BGCOLOR);
                mainContainer.getUnselectedStyle().setBgTransparency(0);
                mainContainer.getSelectedStyle().setBgColor(DEFAULT_BGCOLOR);
                mainContainer.getSelectedStyle().setBgTransparency(0);
            }
            processTag(body,Component.LEFT);
            newLine(Component.LEFT); //flush buffer
            
            if  ((SUPPORT_CSS) && (loadCSS)) {
                body.setAssociatedComponents(mainContainer);
                if (threadQueue.getCSSCount()==-1) { // If there are no pending external CSS, we can already process the CSS
                    applyAllCSS(); // Note that this doesn't have to be on EDT as the main container is still not displayed
                }
            }
            
        } else {
            System.out.println("no BODY tag was found in page.");
        }

        if (!cancelled) {
            checkRedirect(head);
        }
    }

    /**
     * Applies the CSS according to the following order (As the spec determines):
     * - External CSS (CSS files linked with the LINk tag)
     * - Embedded CSS (STYLE tag)
     * - Inline CSS (STYLE attribute)
     */
    void applyAllCSS() {
        Element html=document.getChildById(Element.TAG_HTML);
        Element body=null;
        if (html!=null) {
            body=html.getChildById(Element.TAG_BODY);
        }
        if (body!=null) {
            CSSEngine.getInstance().applyCSS(body, this, externalCSS, embeddedCSS);
        }
    }

    /**
     * Checks if there's a refresh or redirect directive in the META tag and if so executes accordfingly.
     * 
     * @param head The HEAD element of the document
     */
    private void checkRedirect(Element head) {
        if (head!=null) {
            Element meta=head.getChildById(Element.TAG_META);
            if (meta!=null) {
                String httpequiv=meta.getAttributeById(Element.ATTR_HTTPEQUIV);
                if ((httpequiv!=null) && (httpequiv.equalsIgnoreCase("refresh"))) {
                    String content=meta.getAttributeById(Element.ATTR_CONTENT);
                    if (content!=null) {
                        int seperator=content.indexOf(';');
                        String redirectURL=null;
                        if (seperator!=-1) {
                            String tempUrl=content.substring(seperator+1);

                            redirectURL="";
                            for(int i=0;i<tempUrl.length();i++) {
                                char ch=tempUrl.charAt(i);
                                if (!Parser.isWhiteSpace(ch)) {
                                    redirectURL+=ch;
                                }
                            }
                            
                            if (redirectURL.startsWith("url=")) { // i.e. 10;url=http://....
                                redirectURL=redirectURL.substring(4);
                            }

                            content=content.substring(0, seperator);
                        }
                        int redirectTime=-1;
                        try {
                            redirectTime=Integer.parseInt(content);
                            redirectThread=new RedirectThread(this, redirectTime, redirectURL);
                            new Thread(redirectThread).start();
                        } catch (NumberFormatException nfe) {
                            //Wasn't a number - ignore tag
                        }

                    }
                }
            }
        }

    }

    /**
     * Cancels redirects if exists. This is useful when the page has a meta tag with redirection/refresh in X seconds, and in that time the user clicked a link
     */
    void cancelRedirectsAndImages() {
        if (redirectThread!=null) {
            redirectThread.cancel();
            redirectThread=null;
        }
        threadQueue.discardQueue();
        embeddedCSS=null;
        externalCSS=null;
        if (getComponentForm()!=null) {
            marqueeMotion=null;
            getComponentForm().deregisterAnimated(this);
        }

    }

    /**
     * Adds the container representing the current line to the current container and creates a new one
     */
    private void newLine(int align) {
      //if (curLine.getPreferredH()==0)  {
      if (curLine.getComponentCount()==0) { // If no components are present, create a vertical spacing in the size of the font height
          curLine.setPreferredH(font.getHeight());
      }
      lastWasEmpty=(curLine.getComponentCount()==0);
      curContainer.addComponent(curLine);
      curLine=new Container();
      curLine.getStyle().setBgTransparency(0);
      if (!FIXED_WIDTH) {
        curLine.setLayout(new FlowLayout(align));
      }
      curLine.setScrollableX(false);

      curLine.getStyle().setMargin(Component.LEFT,leftIndent);
      x=leftIndent;

    }

    /**
     * Same as newLine, but only if the current line container is not empty
     */
    private void newLineIfNotEmpty(int align) {
        if (curLine.getComponentCount()>0) {
            newLine(align);
        }
    }

    /**
     * Same as newLine, but only if the last line container was not empty
     */
    private void newLineIfLastWasNotEmpty(int align) {
        if (!lastWasEmpty) {
            newLine(align);
        }
    }

    /**
     * Shows a text that has been marked with the html PRE tag which means the text will be displayed as is 
     * without truncation of white spaces and new lines when reaching the end of the screen.
     * 
     * @param text The text to display
     * @param align The current horizontal alignment
     * @return a vector containing components each representing a sentence fragment 
     */
    Vector showPreTagText(String text,int align) {
        Vector comps=new Vector();
        if ((text==null) || (text.equals(""))) {
            return comps; //no text to show
        }

        String line="";
        for(int c=0;c<text.length();c++) {
            char ch=text.charAt(c);
            if ((ch==10) || (ch==13)) {
                if (!line.equals("")) {
                    comps.addElement(addString(line, align));
                    newLine(align);
                    line="";
                }
            } else {
                line+=ch;
            }

        }
        if (!line.equals("")) {
            comps.addElement(addString(line, align));
            newLine(align);
        }
        return comps;
    }

    Vector getWords(String text,int align,boolean returnComps) {
        Vector words=new Vector();
        String word="";
        for(int c=0;c<text.length();c++) {
            char ch=text.charAt(c);
            if ((ch==' ') || (ch==10) || (ch==13) || (ch=='\t') || (ch=='\n')) {
                if (!word.equals("")) {
                    if (returnComps) {
                        words.addElement(addString(word+' ', align));
                    } else {
                        words.addElement(word);
                    }
                    word="";
                }
            } else if ((!returnComps) && (font.stringWidth(word+ch)>width-leftIndent)) { //break words that are longer than the component's width
                   words.addElement(word);
                    word=""+ch;
            } else {
                word+=ch;
            }
        }
        if (!word.equals("")) {
            if (returnComps) {
                words.addElement(addString(word, align));
            } else {
                words.addElement(word);
            }
        }

        return words;
    }

    /*
     * This method is used in non FIXED_WIDTH mode
     */
    private Vector showText(String text,int align) {
        return getWords(text, align, true);
    }


    /**
     * Shows the given text. This method breaks lines as necessary and adds the text either as regular labels or links.
     *
     * @param text The text to display
     * @param align The current horizontal alignment
     */
    private Vector showTextFixedWidth(String text,int align) {
            Vector comps = new Vector();
            if ((text==null) || (text.equals(""))) {
                return comps; //no text to show
            }
            int spaceW=width-x;

            Vector words=getWords(text,align,false);

            if (words.size()>0) {
                int w=0;
                String wordStr="";
                if ((Parser.isWhiteSpace(text.charAt(0))) && (curLine.getComponentCount()!=0)) { //leading space is trimmed if it is in the first component of the line
                    wordStr=" "; //leading space
                }

                while (w<words.size()) {
                    String nextWord=(String)words.elementAt(w);
                    String space="";
                    if ((!wordStr.equals("")) && (!wordStr.equals(" "))) {
                        space=" ";
                    }
                    if (font.stringWidth(wordStr+space+nextWord)>spaceW-2) {
                        comps.addElement(addString(wordStr,align));
                        newLineIfNotEmpty(align);
                        spaceW=width-x;
                        wordStr=nextWord;
                    } else {
                        wordStr+=space+nextWord;
                    }
                    w++;
                }
                if (Parser.isWhiteSpace(text.charAt(text.length()-1))) {
                    wordStr+=" "; //trailing space
                }

                comps.addElement(addString(wordStr,align));
            }

            return comps;
    }

    /**
     * Adds the given text to the container as a label or a link.
     * The string given here does not need line breaking as this was calculated before in the calling method.
     * 
     * @param str The text to display
     * @param align The current horizontal alignment
     */
    private Label addString(String str,int align) {
        Label lbl=null;
        int color=textColor;

        if (link!=null) {
            lbl=new HTMLLink(str,link,this,mainLink,linkVisited);
            color=linkColor;
            if (linkVisited) {
                color=COLOR_VISITED_LINKS;
            }

            lbl.getSelectedStyle().setFont(font.getFont());
            if (mainLink==null) {
                mainLink=(HTMLLink)lbl;
            }
            if (accesskey!='\0') {
                addAccessKey(accesskey, lbl, false);//accessKeys.put(new Integer(accesskey), lbl);
                accesskey='\0'; // To prevent the access key from adding again to all words of the link
            }
            lbl.getSelectedStyle().setMargin(0,0,0,0);
            lbl.getSelectedStyle().setPadding(0,0,0,0);

        } else {
            if (labelForID!=null) {
                lbl=new ForLabel(str, this, labelForID);
                if (accesskey!='\0') {
                    addAccessKey(accesskey, lbl, false);//accessKeys.put(new Integer(accesskey), lbl);
                    accesskey='\0'; // To prevent the access key from adding again to all words of the link
                }
                labelForID=null;
            } else {
                lbl=new Label(str);
            }

        }
        lbl.getStyle().setMargin(0,0,0,0);
        lbl.getStyle().setPadding(0,0,0,0);
        lbl.getUnselectedStyle().setFgColor(color);
        lbl.getSelectedStyle().setFgColor(color);

        //lbl.setVerticalAlignment(Component.BOTTOM); //TODO - This still doesn't align as label alignment refers to the text alignment in relation to its icon (if exists)
        lbl.getUnselectedStyle().setFont(font.getFont());
        lbl.setGap(0);
        lbl.setTickerEnabled(false);
        lbl.setEndsWith3Points(false);
        if (align!=JUSTIFY) { // Regular LWUIT labels do not support justify. We acheive justification in fixed width mode below
            lbl.setAlignment(align);
        }

        lbl.getUnselectedStyle().setBgTransparency(0);

        curLine.addComponent(lbl);

        if (anchor!=null) {
            anchors.put(anchor,lbl);

        }

        if (FIXED_WIDTH) {
            if (align!=Component.LEFT) {
                if (align==JUSTIFY) {  // Text justification algorithm
                    Vector words=getWords(str,align,false);
                    if (words.size()>1) {
                        int spaceW=font.getFont().stringWidth(" ");
                        int spacesToAdd=(width-lbl.getPreferredW())/spaceW;
                        int spacesPerWord=spacesToAdd/(words.size()-1);
                        int addtlSpaces=spacesToAdd%(words.size()-1);
                        String newStr=(String)words.elementAt(0);
                        for(int i=1;i<words.size();i++) {
                            for(int j=0;j<spacesPerWord;j++) {
                                newStr+=' ';
                            }
                            if (i<=addtlSpaces) {
                                newStr+=' ';
                            }
                            newStr+=' '+(String)words.elementAt(i);
                        }
                        lbl.setText(newStr);
                    }
                } else {
                    lbl.setPreferredW(width);
                }
                x=width;
                newLine(align);
            } else {
                x+=lbl.getPreferredW();
            }
        }

        return lbl;

    }

    /**
     * Adds the given access key to make it focus on the given component
     *
     * @param accessKey The accesskey key code
     * @param cmp The component that should be focused when the access key is pressed
     * @param override If true, cancel any previous accesskey associated with this component (Relevant for CSS -wap-accesskey)
     */
    void addAccessKey(int accessKey,Component cmp,boolean override) {
        if ((override) && (accessKeys.contains(cmp))) {
            Hashtable newAccessKeys=new Hashtable();
            for (Enumeration e=accessKeys.keys();e.hasMoreElements();) {
                Object key=e.nextElement();
                Component c=(Component)accessKeys.get(key);
                if (c!=cmp) {
                    newAccessKeys.put(key, c);
                }
            }
            accessKeys=newAccessKeys;
        }
        
        accessKeys.put(new Integer(accessKey),cmp);
        Form form=getComponentForm();
        if (form!=null) {
            form.addKeyListener(accessKey,this);
        }
    }

    /**
     * Overrides initComponent to add the key listeners to the access keys when the component is first added to the form/displayed
     * This is useful when the component is added only after the page was read
     */
    protected void initComponent() {
        super.initComponent();
        for (Enumeration e=accessKeys.keys();e.hasMoreElements();) {
            int keyCode=((Integer)e.nextElement()).intValue();
            getComponentForm().addKeyListener(keyCode,this);
        }
    }

    /**
     * If the component is taken off for any reason, makes sure access keys are not active
     */
    protected void deinitialize() {
        super.deinitialize();
        for (Enumeration e=accessKeys.keys();e.hasMoreElements();) {
            int keyCode=((Integer)e.nextElement()).intValue();
            getComponentForm().removeKeyListener(keyCode,this);
        }
    }



    /**
     * Handles the IMG tag. This includes calculating its size (if available), applying any links/accesskeys and adding it to the download queue
     *
     * @param imgElement the IMG element
     * @param align th current alignment
     * @param cmd The submit command of a form, used only for INPUT type="image"
     */
    private void handleImage(Element imgElement,int align,Command cmd) {

            String imageUrl = imgElement.getAttributeById(Element.ATTR_SRC);
            Label imgLabel=null;
            if (imageUrl!=null) {

                String alignStr=imgElement.getAttributeById(Element.ATTR_ALIGN);

                    // Image width and height
                    int iWidth=calcSize(getWidth(), imgElement.getAttributeById(Element.ATTR_WIDTH),0,false);
                    int iHeight=calcSize(getHeight(), imgElement.getAttributeById(Element.ATTR_HEIGHT),0,false);

                    // Whitespace on the image sides (i.e. Margins)
                    int hspace=getInt(imgElement.getAttributeById(Element.ATTR_HSPACE));
                    int vspace=getInt(imgElement.getAttributeById(Element.ATTR_VSPACE));

                    int totalWidth=iWidth+hspace*2;

                    if ((FIXED_WIDTH) && (x+totalWidth>=width)) {
                        newLine(align);
                    }

                    // Alternative image text, shown until image is loaded.
                    String altText=imgElement.getAttributeById(Element.ATTR_ALT);

                    if (link!=null) { // This image is inside an A tag with HREF attribute
                        imgLabel=new HTMLLink(altText,link,this,mainLink,false);
                        if (mainLink==null) {
                            mainLink=(HTMLLink)imgLabel;
                        }
                        if (accesskey!='\0') {
                            addAccessKey(accesskey, imgLabel, false);//accessKeys.put(new Integer(accesskey), imgLabel);
                        }

                    } else if (cmd!=null) { //Special case of an image submit button
                        imgLabel=new Button(cmd);
                        if ((altText!=null) && (!altText.equals(""))) {
                            imgLabel.setText(altText);
                        }
                        if (firstFocusable==null) {
                            firstFocusable=imgLabel;
                        }

                    } else {
                        imgLabel=new Label(altText);
                    }

                    
                    if ((iWidth!=0) || (iHeight!=0)) { // reserve space while loading image if either width or height are specified, otherwise we don't know how much to reserve
                        iWidth+=imgLabel.getStyle().getPadding(Component.LEFT)+imgLabel.getStyle().getPadding(Component.RIGHT);
                        iHeight+=imgLabel.getStyle().getPadding(Component.TOP)+imgLabel.getStyle().getPadding(Component.BOTTOM);
                        imgLabel.setPreferredSize(new Dimension(iWidth,iHeight));
                    } else { // If no space is reserved, make a minimal text, otherwise LWUIT won't calculate the size right after the image loads
                        if ((imgLabel.getText()==null) || (imgLabel.getText().equals(""))) {
                            imgLabel.setText(" ");
                        }
                    }

                    // It is important that the padding of the image component itself will be all 0
                    // This is because when the image is loaded, its preferred size is checked to see if its width/height were preset by the width/height attribute
                    imgLabel.getSelectedStyle().setPadding(0,0,0,0);
                    imgLabel.getUnselectedStyle().setPadding(0,0,0,0);

                    imgLabel.getSelectedStyle().setFont(font.getFont());
                    imgLabel.getUnselectedStyle().setFont(font.getFont());


                    imgLabel.getUnselectedStyle().setBorder(Border.createLineBorder(1));
                    imgLabel.getSelectedStyle().setBorder(Border.createLineBorder(1));
                    imgLabel.getUnselectedStyle().setBgTransparency(0);
                    imgLabel.getSelectedStyle().setBgTransparency(0);

                    Container imgCont=new Container(new BorderLayout());
                    imgCont.addComponent(BorderLayout.CENTER,imgLabel);
                    imgCont.getSelectedStyle().setMargin(vspace, vspace, hspace, hspace);
                    imgCont.getUnselectedStyle().setMargin(vspace, vspace, hspace, hspace);


                    curLine.addComponent(imgCont);
                    x+=totalWidth;

                    //Alignment
                    imgLabel.setAlignment(getHorizAlign(alignStr,align,false));
                    imgLabel.setVerticalAlignment(getVertAlign(alignStr,Component.CENTER));

                    if (showImages) {
                        if (docInfo!=null) {
                            imageUrl=docInfo.convertURL(imageUrl);
                            threadQueue.add(imgLabel, imageUrl);
                        } else {
                            if (DocumentInfo.isAbsoluteURL(imageUrl)) {
                                threadQueue.add(imgLabel, imageUrl);
                            } else {
                                if (htmlCallback!=null) {
                                    htmlCallback.parsingError(HTMLCallback.ERROR_NO_BASE_URL, imgElement.getName(),imgElement.getAttributeName(new Integer(Element.ATTR_SRC)),imageUrl,"Ignoring Image file referred in an IMG tag ("+imageUrl+"), since page was set by setBody/setHTML so there's no way to access relative URLs");
                                }
                            }
                        }
                        
                    }

                    if (loadCSS) {
                        imgElement.setAssociatedComponents(imgCont);
                    }

            }
    }

    /**
     * Handles the INPUT tag
     * 
     * @param element The input element
     * @param align The current aligment
     */
    private void handleInput(Element element,int align) {
        String type=element.getAttributeById(Element.ATTR_TYPE);
        if (type==null) {
            return;
        }
        int typeID=INPUT_TYPES.indexOf(type.toLowerCase());

        String name=element.getAttributeById(Element.ATTR_NAME);
        String id=element.getAttributeById(Element.ATTR_ID);
        String value=element.getAttributeById(Element.ATTR_VALUE);
        if (typeID==-1) {
            return;
        }
        if (value==null) {
            value="";
        }

        Component cmp=null;
        switch(typeID) {
            case INPUT_CHECKBOX:
                CheckBox cb=new CheckBox();
                if (element.getAttributeById(Element.ATTR_CHECKED)!=null) {
                    cb.setSelected(true);
                }
                cmp=cb;
                if (curForm!=null) {
                    curForm.addCheckBox(name, cb,value);
                }
                break;
            case INPUT_HIDDEN:
                if (curForm!=null) {
                    curForm.addInput(name, value,null);
                }
                break;
            case INPUT_TEXT:
            case INPUT_PASSWORD:
                TextField tf = new TextField(value);
                if (typeID==INPUT_PASSWORD) {
                    tf.setConstraint(TextField.PASSWORD);
                }

                if (SUPPORT_INPUT_FORMAT) {
                    HTMLInputFormat inputFormat=HTMLInputFormat.getInputFormat(element.getAttributeById(Element.ATTR_FORMAT));
                    if (inputFormat!=null) {
                        tf=(TextField)inputFormat.applyConstraints(tf);
                        if (curForm!=null) {
                            curForm.setInputFormat(tf, inputFormat);
                        }
                    }
                    String emptyOk=element.getAttributeById(Element.ATTR_EMPTYOK);
                    if ((emptyOk!=null) && (curForm!=null)) {
                        if (emptyOk.equalsIgnoreCase("true")) {
                            curForm.setEmptyOK(tf, true);
                        } else if (emptyOk.equalsIgnoreCase("false")) {
                            curForm.setEmptyOK(tf, false);
                        }
                    }
                }

                int size=getInt(element.getAttributeById(Element.ATTR_SIZE));
                int maxlen=getInt(element.getAttributeById(Element.ATTR_MAXLENGTH));

                if (size==0) {
                    size=DEFAULT_TEXTFIELD_SIZE;
                }

                if (maxlen!=0) {
                    tf.setMaxSize(maxlen);
                    if (size>maxlen) {
                        size=maxlen;
                    }
                }

                tf.setPreferredW(tf.getStyle().getFont().stringWidth("W")*size);
                tf.getSelectedStyle().setFont(font.getFont());
                tf.getUnselectedStyle().setFont(font.getFont());
                cmp=tf;
                if (curForm!=null) {
                    curForm.addInput(name, cmp,value);
                    textfieldsToForms.put(tf, curForm);
                }
                break;
            case INPUT_RADIO:
                RadioButton rb=new RadioButton(" ");
                if (element.getAttributeById(Element.ATTR_CHECKED)!=null) {
                    rb.setSelected(true);
                }

                cmp=rb;
                if (curForm!=null) {
                    curForm.addRadioButton(name, rb,value);
                }
                break;
            case INPUT_BUTTON: // No support for input type button, as these make sense only with javascript
                break;
            case INPUT_RESET:
                Command resetCmd=null;
                if (curForm!=null) {
                    if (!value.equals("")) {
                        curForm.setResetText(value);
                    }
                    resetCmd=curForm.getResetCommand();
                }
                if (resetCmd==null) {
                    resetCmd=new Command(HTMLForm.getDefaultResetText()); //dummy command - no form so it won't do anything
                }
                Button resetButton=new Button(resetCmd);
                cmp=resetButton;
                
                break;
            case INPUT_SUBMIT:
                Command submitCmd=null;
                if (curForm!=null) {
                    if (!value.equals("")) {
                        curForm.setSubmitText(value);
                    }
                    submitCmd=curForm.getSubmitCommand();
                    curForm.hasSubmitButton=true;
                }
                if (submitCmd==null) {
                    submitCmd=new Command(HTMLForm.getDefaultSubmitText()); //dummy command - no form so it won't do anything
                }
                Button submitButton=new Button(submitCmd);
                cmp=submitButton;
                break;
            case INPUT_IMAGE: // Image submit is not officially supported in XHTML-MP 1.0 but was added anyway, but pixel data submission is not supported (i.e. name.x=xx&name.y=yy)
                submitCmd=null;
                if (curForm!=null) {
                    submitCmd=curForm.getSubmitCommand();
                    curForm.hasSubmitButton=true;
                }
                handleImage(element, align,submitCmd);
                break;
            case INPUT_FILE:
                //TODO - issue a parsing error???, perhaps catch in Parser
                break;
        }

        if (cmp!=null)  {
            String aKey=element.getAttributeById(Element.ATTR_ACCESSKEY);
            if ((aKey!=null) && (aKey.length()==1)) {
                addAccessKey(aKey.charAt(0), cmp, false);//accessKeys.put(new Integer(aKey.charAt(0)), cmp);
            }
            if (loadCSS) {
                element.setAssociatedComponents(cmp);
            }
            if ((curForm!=null) && (curForm.action==null)) { //Form that submits to a forbidden link
                cmp.setEnabled(false);
            } else if (firstFocusable==null) {
                firstFocusable=cmp;
            }

            if (id!=null) {
                inputFields.put(id,cmp);
            }
        }

        addCmp(cmp,align);

    }

    /**
     * Sets input format validation for a TextArea or TextField
     * This is called from the CSSEngine, and since it is done after the TextArea has been added it is a bit more complicated
     * 
     * @param inputField The TextArea to place the input format validation on
     * @param inputFormat The string representing the input format
     * @return The same TextArea or a new instance representing the element
     */
    TextArea setInputFormat(final TextArea inputField,String inputFormat) {
        TextArea returnInputField=inputField;
        if (SUPPORT_INPUT_FORMAT) {
            HTMLForm form=(HTMLForm)textfieldsToForms.get(inputField);
            if (form!=null) {
                HTMLInputFormat format=HTMLInputFormat.getInputFormat(inputFormat);
                if (format!=null) {
                    form.setInputFormat(inputField, format);
                    final TextArea newInputField=format.applyConstraints(inputField);
                    returnInputField=newInputField;
                    
                    // Replace operation must be done on the EDT if the form is visible
                    if (Display.getInstance().getCurrent()!=inputField.getComponentForm()) { // ((inputField.getComponentForm()==null) ||
                        inputField.getParent().replace(inputField, newInputField, null); // Applying the constraints may return a new instance that has to be replaced in the form
                    } else {
                        Display.getInstance().callSerially(new Runnable() {
                            public void run() {
                                inputField.getParent().replace(inputField, newInputField, null); // Applying the constraints may return a new instance that has to be replaced in the form
                            }
                        });
                    }
                    if (firstFocusable==inputField) {
                        firstFocusable=newInputField;
                    }
                }
            }
        }
        return returnInputField;
    }

    /**
     * Sets an input-required restriction on the given input field
     * 
     * @param inputField The TextArea to place the input required restriction on
     * @param inputRequired true if input is required (i.e. emptyok=false), false if input is not required (i.e. emptyok=true)
     */
    void setInputRequired(TextArea inputField,boolean inputRequired) {
        if (SUPPORT_INPUT_FORMAT) {
            HTMLForm form=(HTMLForm)textfieldsToForms.get(inputField);
            if (form!=null) {
                if (inputRequired) { // Note that input-required is the reverse from emptyok...
                    form.setEmptyOK(inputField, false);
                } else {
                    form.setEmptyOK(inputField, true);
                }
            }
        }
    }

    /**
     * Adds the given component to the curLine container after performing size checks
     * 
     * @param cmp The component to add
     */
    private void addCmp(Component cmp,int align) {
        if (cmp!=null) {
            if ((FIXED_WIDTH) && (x+cmp.getPreferredW()>width)) {
                newLine(align);
            }
            curLine.addComponent(cmp);
            x+=cmp.getPreferredW();
        }
    }

    /**
     * Updates the current margin with the given delta
     * 
     * @param delta The pixels to increment (positive value) or decrement (negative value) from the current margin
     */
    private void updateMargin(int delta) {
        leftIndent+=delta;
        x+=delta;
        curLine.getStyle().setMargin(Component.LEFT, leftIndent);
    }


    /**
     * Handles a single table cell (a TD tag)
     *
     * @param tdTag The TD tag element
     * @param align The current alignment
     */
    private void handleTableCell(Element tdTag,int align) {
            newLineIfNotEmpty(align);
            tableCells.addElement(curContainer);
            Container cell=new Container();
            cell.getStyle().setBgTransparency(0);
            cell.setLayout(new BoxLayout(BoxLayout.Y_AXIS));
            
            int border=0;
            Element trTag=tdTag.getParent();
            while ((trTag!=null) && (trTag.getId()!=Element.TAG_TR)) { // Though in strict XHTML TR can only contain TD/TH - in some HTMLs TR doesn't have to be the direct parent of the tdTag, i.e.: <tr><b><td>...</td>... </b></tr>
                trTag=trTag.getParent();
            }

            if (trTag!=null) { // Null checks to prevent exceptions for a TD tag without table etc.
                Element tableTag=trTag.getParent();
                while ((tableTag!=null) && (tableTag.getId()!=Element.TAG_TABLE)) { // Though in strict XHTML TABLE can only contain TR - in some HTMLs it might be different
                    tableTag=tableTag.getParent();
                }

                if (tableTag!=null) {
                    border=getInt(tableTag.getAttributeById(Element.ATTR_BORDER));
                }
            }
            cell.getUnselectedStyle().setPadding(border, border, border, border);
            cell.getSelectedStyle().setPadding(border, border, border, border);

            //Constraint constraint = new Constraint();
            CellConstraint constraint = new CellConstraint();

            int halign=getHorizAlign(tdTag.getParent().getAttributeById(Element.ATTR_ALIGN),align,false); // Get the default TR alignment
            int valign=getVertAlign(tdTag.getParent().getAttributeById(Element.ATTR_VALIGN),Component.CENTER); // Get the default TR valignment
            halign=getHorizAlign(tdTag.getAttributeById(Element.ATTR_ALIGN),halign,false);
            valign=getVertAlign(tdTag.getAttributeById(Element.ATTR_VALIGN),valign);
            int colspan=getInt(tdTag.getAttributeById(Element.ATTR_COLSPAN));
            int rowspan=getInt(tdTag.getAttributeById(Element.ATTR_ROWSPAN));
            String cWidth=tdTag.getAttributeById(Element.ATTR_WIDTH);
            int pW=getPercentage(cWidth);
            if ((pW>0) && (pW<100)) {
                //constraint.setWidthPercentage(pW); //TODO - Setting a width constraint currently makes the field width 0 - needs to be fixed in TableLayout
            } else {
                pW=getInt(cWidth);
                if (pW!=0) {
                    cell.setPreferredW(pW);
                }
            }
            String cHeight=tdTag.getAttributeById(Element.ATTR_HEIGHT);
            int pH=getPercentage(cHeight);
            if ((pH>0) && (pH<100)) {
                //constraint.setHeightPercentage(pH); //TODO - Setting a height constraint currently makes the field height 0 - needs to be fixed in TableLayout
            } else {
                pH=getInt(cHeight);
                if (pH!=0) {
                    cell.setPreferredH(pH);
                }
            }

            constraint.setHorizontalAlign(halign);
            constraint.setVerticalAlign(valign);

            //TODO - re-enable spanning code currently causes exceptions
            /*if (colspan>1) {
                constraint.setHorizontalSpan(colspan);
            }
            if (rowspan>1) {
                constraint.setVerticalSpan(rowspan);
            }*/

            curContainer=cell;
            if (curTable!=null) {
                curTable.addCell(cell,(tdTag.getId()==Element.TAG_TH),constraint);
            }

            if (loadCSS) {
                tdTag.setAssociatedComponents(cell);
                if (trTag!=null) {
                    trTag.addAssociatedComponent(cell);
                }
            }

    }

    /**
     * Processes the given tag. This is the main processing method that calls all others and uses itself in a recursive manner.
     * 
     * @param element The element to process
     * @param align The current alignment 
     */
    private void processTag(Element element,int align) {
        if ((cancelled) && (!cancelledCaught)) {
            return;
        }
        int curAlign=align;

        HTMLFont oldFont=font;
        for(int i=0;i<element.getNumChildren();i++) {
            if ((cancelled) && (!cancelledCaught)) {
                break;
            }
            Element child=element.getChildAt(i);

            // Process Tag Open
            switch (child.getId()) {
                case Element.TAG_TEXT:
                    String text=child.getAttributeById(Element.ATTR_TITLE);
                    if ((curComboBox!=null) && (optionTag)) { // Text is inside an OPTION tag, i.e. belongs to a ComboBox
                        OptionItem oi = new OptionItem(text, optionValue);
                        curComboBox.addItem(oi);
                        if (optionSelected) {
                            curComboBox.setSelectedItem(oi);
                            if (curForm!=null) {
                                curForm.setDefaultValue(curComboBox, oi);
                            }
                        }
                    } else if (curTextArea!=null) { // Text is inside of a TEXTAREA tag
                        curTextArea.setText(text);
                        if (curForm!=null) {
                            curForm.setDefaultValue(curTextArea,text);
                        }
                    } else {
                      Vector comps=null;
                      if (preTagCount!=0) {
                          comps=showPreTagText(text, curAlign);
                      } else {
                          
                          if (FIXED_WIDTH) {
                            comps=showTextFixedWidth(text, curAlign);
                          } else {
                            comps=showText(text, curAlign);
                          }
                      }
                      if (loadCSS) {
                          child.setAssociatedComponents(comps);
                      }
                    }
                    break;
                case Element.TAG_A:
                    link=child.getAttributeById(Element.ATTR_HREF);
                    if ((link!=null) && (docInfo==null) && (!DocumentInfo.isAbsoluteURL(link))) {
                        if (htmlCallback!=null) {
                            htmlCallback.parsingError(HTMLCallback.ERROR_NO_BASE_URL, child.getName(),child.getAttributeName(new Integer(Element.ATTR_HREF)),link,"Disabling relative link ("+link+"), since page was set by setBody/setHTML so there's no way to access relative URLs");
                        }
                        link=null;
                    }
                    if ((link!=null) && (htmlCallback!=null)) {
                        int linkProps=htmlCallback.getLinkProperties(this, convertURL(link));
                        if ((linkProps & HTMLCallback.LINK_FORBIDDEN)!=0) {
                            link=null;
                        } else if ((linkProps & HTMLCallback.LINK_VISTED)!=0) {
                            linkVisited=true;
                        }
                    }

                    anchor=child.getAttributeById(Element.ATTR_NAME);

                    if (link!=null) {
                        String aKey=child.getAttributeById(Element.ATTR_ACCESSKEY);
                        if ((aKey!=null) && (aKey.length()==1)) {
                            accesskey=aKey.charAt(0);
                        }
                    }
                    break;
                case Element.TAG_H1:
                case Element.TAG_H2:
                case Element.TAG_H3:
                case Element.TAG_H4:
                case Element.TAG_H5:
                case Element.TAG_H6:
                    font=(HTMLFont)fonts.get(child.getName());
                    if (font==null) {
                        font=oldFont;
                    }
                    // No break here intentionally
                case Element.TAG_P:
                    curAlign=getHorizAlign(child.getAttributeById(Element.ATTR_ALIGN),align,true);
                    adjustAlignment(align, curAlign);
                    newLineIfNotEmpty(curAlign);
                    newLineIfLastWasNotEmpty(curAlign);
                    pushContainer(child);
                    break;
                case Element.TAG_DIV: 
                    curAlign=getHorizAlign(child.getAttributeById(Element.ATTR_ALIGN),align,true);
                    adjustAlignment(align, curAlign);
                    newLineIfNotEmpty(curAlign);
                    pushContainer(child);
                    break;
                case Element.TAG_FIELDSET:
                    newLineIfNotEmpty(curAlign);
                    Container newCont=new Container();
                    newCont.setUIID("HTMLFieldset");
                    if (fieldsets.size()==0) { // First fieldset shouldn't have margin
                        newCont.getStyle().setMargin(Component.LEFT,0);
                    }
                    newCont.setLayout(new BoxLayout(BoxLayout.Y_AXIS));
                    curContainer.addComponent(newCont);
                    fieldsets.addElement(curContainer);
                    curContainer=newCont;
                    if (loadCSS) {
                        child.setAssociatedComponents(newCont);
                    }

                    break;
                case Element.TAG_BR:
                    if (loadCSS) {
                        child.setAssociatedComponents(curLine);
                    }
                    newLine(curAlign);
                    break;
                case Element.TAG_DL:
                    newLineIfNotEmpty(curAlign);
                    newLine(curAlign);
                    pushContainer(child);
                    break;
                case Element.TAG_DT:
                    newLineIfNotEmpty(curAlign);
                    pushContainer(child);
                    break;
                case Element.TAG_UL:
                    newLineIfNotEmpty(curAlign);
                    ulLevel++;
                    listIndent+=INDENT_UL;
                    if ((ulLevel==1) && (olIndex==Integer.MIN_VALUE)) { //newline only if it's the first list
                        newLine(curAlign);
                    }
                    pushContainer(child);
                    break;
                case Element.TAG_OL:
                    newLineIfNotEmpty(curAlign);
                    if (olIndex!=Integer.MIN_VALUE) {
                        String indexStr=ORDERED_LIST_TYPE_IDENTIFIERS[listType]+""+olIndex;
                        olUpperLevelIndex.addElement(indexStr); //new Integer(olIndex));
                    }
                    olIndex=getInt(child.getAttributeById(Element.ATTR_START),1); //olIndex=1;
                    listType=getOrderedListType(child);
                    listIndent+=INDENT_OL;
                    if ((olUpperLevelIndex.size()==0) && (ulLevel==0)) { //newline only if it's the first list
                        newLine(curAlign);
                    }
                    pushContainer(child);
                    break;
                case Element.TAG_LI:
                    Container listItemCont = new Container(new BorderLayout());
                    listItemCont.getStyle().setMargin(Component.LEFT,leftIndent+listIndent);
                    curContainer.addComponent(listItemCont);
                    containers.addElement(curContainer);

                    HTMLListItem bullet=null;
                    if (child.getParent().getId()==Element.TAG_OL) {
                          olIndex=getInt(child.getAttributeById(Element.ATTR_VALUE),olIndex);
                          int itemListType=getOrderedListType(child,listType);
                          HTMLListIndex listIndex=new HTMLListIndex(olIndex, itemListType);
                          listIndex.getUnselectedStyle().setFgColor(textColor);
                          listIndex.getSelectedStyle().setFgColor(textColor);
                          listIndex.getUnselectedStyle().setFont(font.getFont());
                          bullet=listIndex;
                    } else {
                        bullet=new HTMLBullet(getUnorderedListType(child,ulLevel), font.getFont().getHeight(),textColor,this);
                    }
                    Container bulletCont=new Container(new BorderLayout());
                    bulletCont.addComponent(BorderLayout.NORTH,bullet);
                    listItemCont.addComponent(BorderLayout.WEST, bulletCont);

                    Container listItemText=new Container(new BoxLayout(BoxLayout.Y_AXIS));
                    listItemCont.addComponent(BorderLayout.CENTER, listItemText);
                    curContainer=listItemText;
                    if (loadCSS) {
                        child.setAssociatedComponents(listItemText);
                    }
                    break;

                case Element.TAG_BLOCKQUOTE:
                    newLineIfNotEmpty(curAlign);
                    updateMargin(INDENT_BLOCKQUOTE);
                    newLine(curAlign);
                    pushContainer(child);
                    break;
                case Element.TAG_DD:
                    newLineIfNotEmpty(curAlign);
                    updateMargin(INDENT_DD);
                    pushContainer(child);
                    break;
                case Element.TAG_HR:
                    newLineIfNotEmpty(curAlign);
                    Label hr=new Label();
                    hr.setUIID("HTMLHR");
                    //hr.getStyle().setBorder(Border.createBevelRaised());
                    int hrWidth=calcSize(width, child.getAttributeById(Element.ATTR_WIDTH), width,false);
                    int hrHeight=getInt(child.getAttributeById(Element.ATTR_SIZE), HR_THICKNESS);
                    hr.setPreferredW(hrWidth);
                    hr.setPreferredH(hrHeight);
                    curLine.addComponent(hr);
                    newLine(curAlign);
                    if (loadCSS) {
                        child.setAssociatedComponents(hr);
                    }
                    break;
                case Element.TAG_STYLE:
                    break;
                case Element.TAG_IMG:
                    handleImage(child,curAlign,null);
                    break;

                case Element.TAG_PRE:
                    preTagCount++;
                    pushContainer(child);
                case Element.TAG_EM:
                case Element.TAG_STRONG:
                case Element.TAG_DFN:
                case Element.TAG_CODE:
                case Element.TAG_SAMP:
                case Element.TAG_KBD:
                case Element.TAG_VAR:
                case Element.TAG_CITE:
                    font=(HTMLFont)fonts.get(child.getName());
                    if (font==null) {
                        font=oldFont;
                    }
                    break;

                case Element.TAG_B:
                case Element.TAG_I:
                case Element.TAG_BIG:
                case Element.TAG_SMALL:
                    font=getCounterpartFont(child.getId(), font);
                    break;
                case Element.TAG_FORM:
                    curForm=new HTMLForm(this,child.getAttributeById(Element.ATTR_ACTION),child.getAttributeById(Element.ATTR_METHOD),child.getAttributeById(Element.ATTR_ENCTYPE));
                    pushContainer(child);
                    break;
                case Element.TAG_INPUT:
                    handleInput(child,curAlign);
                    break;
                case Element.TAG_SELECT:
                    String multi = child.getAttributeById(Element.ATTR_MULTIPLE);

                    if (multi!=null) {
                        curComboBox=new MultiComboBox(true);
                        Container comboCont=new Container(new BorderLayout());
                        curComboBox.setItemGap(0);
                        comboCont.setUIID("ComboBox");
                        curComboBox.setUIID("List");
                        comboCont.addComponent(BorderLayout.CENTER,curComboBox);
                    } else {
                        curComboBox=new HTMLComboBox();
                    }
                    String name = child.getAttributeById(Element.ATTR_NAME);

                    if (curForm!=null) {
                        curForm.addInput(name,curComboBox,null);
                    }
                    if (loadCSS) {
                        child.setAssociatedComponents(curComboBox);
                    }
                    break;
                case Element.TAG_OPTGROUP:
                    if (curComboBox!=null) {
                        String label=child.getAttributeById(Element.ATTR_LABEL);
                        if (label!=null) {
                            curComboBox.addItem(label);
                        }
                    }
                    break;
                case Element.TAG_OPTION:
                    optionTag=true;
                    optionValue = child.getAttributeById(Element.ATTR_VALUE);
                    if ((curComboBox!=null) && (child.getAttributeById(Element.ATTR_SELECTED)!=null)) {
                        optionSelected=true;
                    }
                    break;
                case Element.TAG_TEXTAREA:
                    curTextArea = new TextArea(getInt(child.getAttributeById(Element.ATTR_ROWS),DEFAULT_TEXTAREA_ROWS),
                            getInt(child.getAttributeById(Element.ATTR_COLS),DEFAULT_TEXTAREA_COLS));

                    addCmp(curTextArea,curAlign);
                    if (loadCSS) {
                        child.setAssociatedComponents(curTextArea);
                    }
                    String aKey=element.getAttributeById(Element.ATTR_ACCESSKEY);
                    if ((aKey!=null) && (aKey.length()==1)) {
                        addAccessKey(aKey.charAt(0), curTextArea, false);//accessKeys.put(new Integer(aKey.charAt(0)), curTextArea);
                    }

                    break;
                case Element.TAG_Q:
                    addQuote(curAlign);
                    quoteTagCount++;
                    break;
                case Element.TAG_TABLE:
                    newLineIfNotEmpty(curAlign);
                    if (curTable!=null) {
                        tables.addElement(curTable);
                        HTMLTableModel newTable = new HTMLTableModel();
                        curTable=newTable;
                    } else {
                        curTable=new HTMLTableModel();
                    }
                    width=width/2; // TODO - this is temporary until issues with layout managers are solved. The idea is that we arbitrarily divide the size by a factor knowing that probably there are several cells

                    break;
                case Element.TAG_TR:
                    break;
                case Element.TAG_TH:
                case Element.TAG_TD:
                    if (curTable!=null) {
                        handleTableCell(child,curAlign);
                    }
                    break;
               case Element.TAG_LABEL:
                   labelForID=child.getAttributeById(Element.ATTR_FOR);
                   aKey=child.getAttributeById(Element.ATTR_ACCESSKEY);
                   if ((aKey!=null) && (aKey.length()==1)) {
                       accesskey=aKey.charAt(0);
                   }
                   break;

            }

            if (child.getNumChildren()>0) {
                processTag(child,curAlign);
            }

            // Process close tag
            switch(child.getId()) {
                case Element.TAG_H1:
                case Element.TAG_H2:
                case Element.TAG_H3:
                case Element.TAG_H4:
                case Element.TAG_H5:
                case Element.TAG_H6:
                    font=oldFont;
                case Element.TAG_P:
                    curAlign=align; //Restore previous alignment
                    newLineIfNotEmpty(curAlign);
                    popContainer();
                    newLine(curAlign);
                    break;
                case Element.TAG_DIV:
                    curAlign=align; //Restore previous alignment
                    newLineIfNotEmpty(curAlign);
                    popContainer();
                    break;
                case Element.TAG_FIELDSET:
                    newLineIfNotEmpty(curAlign);
                    Container upperContainer=(Container)fieldsets.lastElement();
                    //upperContainer.addComponent(curContainer);
                    curContainer=upperContainer;
                    fieldsets.removeElement(curContainer);
                    break;
                case Element.TAG_BLOCKQUOTE:
                    newLineIfNotEmpty(curAlign);
                    newLine(curAlign);
                    updateMargin(-INDENT_BLOCKQUOTE);
                    popContainer();
                    break;
                case Element.TAG_DT:
                    popContainer();
                    break;
                case Element.TAG_DD:
                    newLineIfNotEmpty(curAlign);
                    updateMargin(-INDENT_DD);
                    popContainer();
                    break;
                case Element.TAG_DL:
                    newLine(curAlign);
                    popContainer();
                    break;
                case Element.TAG_A:
                    link=null;
                    linkVisited=false;
                    mainLink=null;
                    anchor=null;
                    accesskey='\0';
                    break;
                case Element.TAG_UL:
                    ulLevel--;
                    if ((ulLevel==0) && (olIndex==Integer.MIN_VALUE)) {
                        newLine(curAlign);
                    }
                    listIndent-=INDENT_UL;
                    popContainer();
                    break;
                case Element.TAG_OL:
                    if (olUpperLevelIndex.size()!=0) {
                        String indexStr=(String)olUpperLevelIndex.lastElement();
                        olUpperLevelIndex.removeElementAt(olUpperLevelIndex.size()-1);
                        listType=getOrderedListType(indexStr.charAt(0),HTMLListIndex.LIST_NUMERIC);
                        olIndex=getInt(indexStr.substring(1));
                    } else {
                        olIndex=Integer.MIN_VALUE;
                    }
                    if ((olIndex==Integer.MIN_VALUE) && (ulLevel==0)) {
                        newLine(curAlign); //new line only if it is the last nested list
                    }
                    listIndent-=INDENT_OL;
                    popContainer();

                    break;
                case Element.TAG_LI:
                    if (olIndex!=Integer.MIN_VALUE) {
                        olIndex++;
                    }
                    newLineIfNotEmpty(curAlign);
                    // We can't use popContainer, since with LI the container is pushed even when CSS is ignored, to provide the spacing between the list item bullet/number and the text (in a nested way if needed)
                    Container prevContainer=(Container)containers.lastElement();
                    curContainer=prevContainer;
                    containers.removeElement(curContainer);

                    //popContainer();
                    //curContainer=listItemParentContainer;
                    break;


                case Element.TAG_PRE:
                    preTagCount--;
                    popContainer();
                case Element.TAG_EM:
                case Element.TAG_STRONG:
                case Element.TAG_DFN:
                case Element.TAG_CODE:
                case Element.TAG_SAMP:
                case Element.TAG_KBD:
                case Element.TAG_VAR:
                case Element.TAG_CITE:
                case Element.TAG_B:
                case Element.TAG_I:
                case Element.TAG_BIG:
                case Element.TAG_SMALL:
                    font=oldFont;
                    break;
                case Element.TAG_FORM:
                    if ((curForm!=null) && (!curForm.hasSubmitButton) && (curForm.getNumFields()>0)) { // This is a fix for forms with no submit buttons which can be resulted due to the fact XHTML-MP doesn't support the BUTTON tag and also input type button with javascript
                        Button submitButton=new Button(curForm.getSubmitCommand());
                        addCmp(submitButton,curAlign);
                        curForm.hasSubmitButton=true;
                    }
                    curForm=null;
                    popContainer();
                    break;
                case Element.TAG_TEXTAREA:
                    String name = child.getAttributeById(Element.ATTR_NAME);

                    if (curForm!=null) { // This was moved to the end tag to enable auto complete support (i.e. if there's an autocomplete it overrides the default value)
                        curForm.addInput(name,curTextArea,null);
                    }

                    curTextArea=null;
                    break;
                case Element.TAG_SELECT:
                    if (curComboBox instanceof MultiComboBox) {
                        Container comboCont=curComboBox.getParent();
                        int minSize=Math.min(MIN_MULTI_COMBOBOX_ITEMS, curComboBox.size());
                        int maxSize=Math.min(curComboBox.size(),MAX_MULTI_COMBOBOX_ITEMS);
                        int size=Math.min(maxSize,Math.max(getInt(child.getAttributeById(Element.ATTR_SIZE)),minSize));

                        Component renderCmp=curComboBox.getRenderer().getListCellRendererComponent(curComboBox, "X", 0, false);
                        comboCont.setPreferredH((renderCmp.getPreferredH()+renderCmp.getStyle().getMargin(Component.TOP)+renderCmp.getStyle().getMargin(Component.BOTTOM)+curComboBox.getItemGap())*size
                                +curComboBox.getStyle().getPadding(Component.TOP)+curComboBox.getStyle().getPadding(Component.BOTTOM));
                        comboCont.setPreferredW(100);

                        addCmp(comboCont, curAlign);
                    } else {
                        addCmp(curComboBox, curAlign);
                    }

                    curComboBox=null;
                    break;
                case Element.TAG_OPTION:
                    optionTag=false;
                    optionSelected=false;
                    optionValue=null;
                    break;
                case Element.TAG_Q:
                    quoteTagCount--;
                    addQuote(curAlign);
                    break;
                case Element.TAG_TABLE:
                    newLineIfNotEmpty(curAlign);
                    curTable.commitRowIfNotEmpty(); // For a case that TR was not closed properly
                    if (curTable.getRowCount()!=0) { //Don't add an empty table (Creates an exception in TableLayout and useless)
                        if (TABLES_LOCK_SIZE) {
                            for(int r=0;r<curTable.getRowCount();r++) {
                                for(int c=0;c<curTable.getColumnCount();c++) {
                                    Component cmp=(Component)curTable.getValueAt(r, c);
                                    if (cmp!=null) { // Can be null for cells that are "spanned over"
                                        cmp.setPreferredSize(cmp.getPreferredSize());
                                    }
                                }
                            }
                        }

                        HTMLTable table=new HTMLTable(curTable);
                        table.getStyle().setBgTransparency(0);
                        if (loadCSS) {
                            child.setAssociatedComponents(table);
                        }
                        
                        int border=getInt(child.getAttributeById(Element.ATTR_BORDER));
                        if (border>0) {
                            table.getUnselectedStyle().setBorder(Border.createLineBorder(border));
                            table.getSelectedStyle().setBorder(Border.createLineBorder(border));
                            table.getUnselectedStyle().setPadding(border, border, border, border);
                            table.getSelectedStyle().setPadding(border, border, border, border);
                        } else {
                            table.getUnselectedStyle().setBorder(null);
                            table.getSelectedStyle().setBorder(null);
                            table.setDrawBorder(false);
                        }

                        addCmp(table,curAlign);
                        newLineIfNotEmpty(curAlign);
                    }

                    if (tables.size()==0) {
                        curTable=null;
                    } else {
                        curTable=(HTMLTableModel)tables.lastElement();
                        tables.removeElement(curTable);
                    }
                    width=width*2; // TODO - this is temporary until issues with layout managers are solved.
                    if (width>displayWidth) {
                        width=displayWidth;
                    }
                    break;
                case Element.TAG_TR:
                    if (curTable!=null) {
                        curTable.commitRow();
                    }
                    break;
                case Element.TAG_TH:
                case Element.TAG_TD:
                    if (curTable!=null) {
                        newLineIfNotEmpty(curAlign);
                        curContainer=(Container)tableCells.lastElement();
                        tableCells.removeElement(curContainer);
                    }
                    break;
               case Element.TAG_LABEL:
                   labelForID=null;
                   accesskey='\0';
                   break;


            }

        }
    }

    /**
     * The following replaces the layout of the curLine container with the correct alignment, in non fixed width alignment is done by flowlayout's orientation.
     * In case the line is not empty, a new line will be opened by newLineIfNotEmpty below
     *
     * @param align The general alignment of the element
     * @param curAlign The alignment of the specific component we want to
     */
    private void adjustAlignment(int align,int curAlign) {
        if ((!FIXED_WIDTH) && (align!=curAlign)) {
            if (curLine.getComponentCount()==0) {
                curLine.setLayout(new FlowLayout(curAlign));
            }
        }
    }

    /**
     * Figures out what is the appropriate list type (i.e. which bullet types) for the unordered list in question
     *
     * @param element The element containing the UL tag
     * @param defaultType The default list type
     * @return The UL list type
     */
    private int getUnorderedListType(Element element,int defaultType) {
        String listTypeStr=element.getAttributeById(Element.ATTR_TYPE);
        int type=HTMLBullet.convertULString(listTypeStr);
        if (type==-1) {
            type=defaultType;
        }
        return type;
    }

    /**
     * Figures out the appropriate list type for the given Ordered List element according to its attributes
     * This calls the getOrderedListType(Element,int) with LIST_NUMERIC as the default type
     *
     * @param element The OL element
     * @return The OL list type
     */
    private int getOrderedListType(Element element) {
        return getOrderedListType(element, HTMLListIndex.LIST_NUMERIC);
    }
    
    /**
     * Figures out the appropriate list type for the given Ordered List element according to its attributes
     *
     * @param element The OL element
     * @param defaultListType The default list type
     * @return The OL list type
     */
    private int getOrderedListType(Element element,int defaultListType) {
        String listTypeStr=element.getAttributeById(Element.ATTR_TYPE);
        if ((listTypeStr!=null) && (listTypeStr.length()>0)) {
            char c=listTypeStr.charAt(0);
            return getOrderedListType(c, defaultListType);
        }
        return defaultListType;
    }

    /**
     * Figures out the appropriate list type for the given list type identifier
     * 
     * @param c The list identifier (one of ORDERED_LIST_TYPE_IDENTIFIERS) 
     * @param defaultListType The default list type
     * @return The OL list type
     */
    private int getOrderedListType(char c,int defaultListType) {
        for(int j=0;j<ORDERED_LIST_TYPE_IDENTIFIERS.length;j++) {
            if (c==ORDERED_LIST_TYPE_IDENTIFIERS[j]) {
                return j;
            }
        }
        return defaultListType;
    }

    private void pushContainer(Element element) {
        if (loadCSS) {
            Container cont=new Container(new BoxLayout(BoxLayout.Y_AXIS));
            cont.setScrollableX(false);
            //cont.getStyle().setBgColor(Element.COLOR_VALS[contCount]);
            //contCount=(contCount+1)%Element.COLOR_VALS.length; // debug for CSS
            //cont.getStyle().setBgTransparency(128);
            cont.getStyle().setBgTransparency(0);
            element.setAssociatedComponents(cont);
            curContainer.addComponent(cont);
            containers.addElement(curContainer);
            curContainer=cont;
        }
    }

    private void popContainer() {
        if (loadCSS) {
            Container prevContainer=(Container)containers.lastElement();
            curContainer=prevContainer;
            containers.removeElement(curContainer);
        }
    }

    /**
     * Adds a quote according to the current quote count 
     * 
     * @param curAlign The current horizontal alignment
     */
    private void addQuote(int curAlign) {
        String quote=null;
        if (quoteTagCount==0) { 
            quote="\"";
        } else {
            quote="'";
        }
        if ((FIXED_WIDTH) && (width-x<font.stringWidth(quote))) {
            newLine(curAlign);
        }
        addString(quote, curAlign);
    }


    /**
     * Converts a textual horizontal alignment description to a LWUIT alignment constant
     * 
     * @param alignment The string describing the alignment
     * @param defaultAlign The default alignment if the string cannot be converted
     * @param allowJustify true to allow justify alignment, false to return center if justify is mentioned
     * @return Component.LEFT, RIGHT or CENTER or the defaultAlign in case no match was found.
     */
    private int getHorizAlign(String alignment,int defaultAlign,boolean allowJustify) {
        if (alignment!=null) {
            if (alignment.equals("left")) {
                return Component.LEFT;
            } else if (alignment.equals("right")) {
                return Component.RIGHT;
            } else if ((alignment.equals("center")) || (alignment.equals("middle")))  {
                return Component.CENTER;
            } else if (alignment.equals("justify")) {
                return ((allowJustify) && (FIXED_WIDTH))?JUSTIFY:Component.CENTER;
            }
        }

        return defaultAlign; //unknown alignment

    }

    /**
     * Converts a textual vertical alignment description to a LWUIT alignment constant
     *
     * @param alignment The string describing the alignment
     * @param defaultAlign The default alignment if the string cannot be converted
     * @return Component.TOP, BOTTOM or CENTER or the defaultAlign in case no match was found.
     */
    private int getVertAlign(String alignment,int defaultAlign) {
        if (alignment!=null) {
            if (alignment.equals("top")) {
                return Component.TOP;
            } else if (alignment.equals("bottom")) {
                return Component.BOTTOM;
            } else if ((alignment.equals("center")) || (alignment.equals("middle")))  {
                return Component.CENTER;
            }
        }
        return defaultAlign;

    }

    /**
     * A convenience method to convert a string to an int.
     * This is mainly intended to save the try-catch for NumericFormatExceptions
     * 
     * @param intStr The string describing the integer
     * @param defaultValue The value to return if the string is not numeric
     * @return the integer value of the string, or defaultValue if the string is not numeric
     */
    static int getInt(String intStr,int defaultValue) {
        try {
            int num=Integer.parseInt(intStr);
            return num;
        } catch (NumberFormatException nfe) {
            return defaultValue;
        }
    }

    /**
     * A convenience method to convert a string to an int.
     * This is mainly intended to save the try-catch for NumericFormatExceptions
     *
     * @param intStr The string describing the integer
     * @return the integer value of the string, or 0 if the string is not numeric
     */
    static int getInt(String intStr) {
        return getInt(intStr,0);
    }

    /**
     * A convenience method to convert a string describing a percentage to an int.
     * 
     * @param percent The string representing the percentage
     * @return The percentage integer value (i.e. 80% will return 80) or 0 if the string is not a percentage
     */
    private int getPercentage(String percent) {
        if ((percent==null) || (!percent.endsWith("%"))) {
            return 0;
        }
        return getInt(percent.substring(0, percent.length()-1));

    }

    /**
     * Calculates width or height of an element according to its original size, requested size and default size
     * 
     * @param origDim The original width/height
     * @param requestedDim The string describing the requested width/height either in pixels or in percentage
     * @param defaultDim The default width/height to return in case the calculation fails
     * @return The new width/height according to the parameters
     */
    static int calcSize(int origDim,String requestedDim,int defaultDim,boolean negativeAllowed) {
        if (requestedDim==null) {
            return defaultDim;
        }
        boolean percent=false;

        if (requestedDim.endsWith("%")) {
            percent=true;
            requestedDim=requestedDim.substring(0, requestedDim.length()-1);
        } else if (requestedDim.endsWith("px")) { // Pixels can be described either simply as '20' or as '20px'
            requestedDim=requestedDim.substring(0, requestedDim.length()-2);
        }

        int dim=0;
        try {
            dim=Integer.parseInt(requestedDim);
        } catch (Exception e) {
            //dim=-1;
            return origDim;
        }

        if ((dim<0) && (!negativeAllowed)) { //Dimension was negative
            return origDim;
        }

        if (percent) {
            return origDim*dim/100;
        } else {
            return dim;
        }
    }

    /**
     * Returns the input fields of this HTMLComponent, used by the FOR label mecahnism.
     * 
     * @return the input fields of this HTMLComponent
     */
    Hashtable getInputFields() {
        return inputFields;
    }

    /**
     * Focuses on the given component and if it's a checkbox/radiobutton selects it
     * This is used both for ForLabels and for access keys
     *
     * @param cmp The component to focus and select
     */
    void selectComponent(Component cmp) {
        getComponentForm().setFocused(cmp);
        getComponentForm().scrollComponentToVisible(cmp);
        if (cmp instanceof RadioButton) {
            ((RadioButton)cmp).setSelected(true);
        } else if (cmp instanceof CheckBox) {
            CheckBox cb=((CheckBox)cmp);
            cb.setSelected(!cb.isSelected());
        }
    }

    /**
     * Converts the given URL to an absolute URL based on the current page's URL
     *
     * @param url The url to convert (Can be relative)
     * @return The absolute URL representing the given URL in relation to the current one.
     */
    String convertURL(String url) {
        if (docInfo!=null) {
            return docInfo.convertURL(url);
        } else {
            return url; // No conversion is possible if we don't have a DocumentInfo object (in cases of setBody from a string and not from a document address), absolute URLs should have no problem, and as for relative the RequestHandler will have to handle these cases
        }
    }

    /**
     * Jumps to the given anchor
     * 
     * @param anchorName The anchor to jump to
     */
    void goToAnchor(String anchorName) {
        Label anchorCmp=(Label)anchors.get(anchorName);
        if (anchorCmp!=null) {
            int cx=anchorCmp.getX();
            int cy=anchorCmp.getY();
            int h= getHeight();
            if (anchorCmp.getAbsoluteY()-getY()+h>getPreferredH()) {
                h=getPreferredH()-(anchorCmp.getAbsoluteY()-getY());
            }
            scrollRectToVisible(cx, cy, getWidth(), h, anchorCmp);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void layoutContainer() {
        if ((FIXED_WIDTH) && (displayWidth!=0) && (Display.getInstance().getDisplayWidth()!=displayWidth)) {
            new Thread() {
                public void run() {
                    cleanup();
                    rebuildPage(); //screen form factor changed - landscape/portrait
                }
            }.start();
        }
        super.layoutContainer();
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent evt) {
        if (getComponentForm().getFocused() instanceof TextField) {
            return;
        }
        int keyCode=evt.getKeyEvent();
        Object obj = accessKeys.get(new Integer(keyCode));
        if (obj!=null) {
            if (obj instanceof HTMLLink) {
                HTMLLink htmlLink = (HTMLLink)obj;
                htmlLink.actionPerformed(null);
            } else if (obj instanceof ForLabel) {
                ((ForLabel)obj).triggerAction();
            } else if (obj instanceof Component) {
                selectComponent((Component)obj);
            }
        }
    }

    ResourceThreadQueue getThreadQueue() {
        return threadQueue;
    }

}
/**
 * A thread used to refresh or redirect to another page in X seconds
 *
 * @author Ofir Leitner
 */
class RedirectThread implements Runnable {

    int seconds;
    String url;
    HTMLComponent htmlC;
    boolean cancelled;

    public RedirectThread(HTMLComponent htmlC,int seconds,String url) {
        this.seconds=seconds;
        this.url=url;
        this.htmlC=htmlC;
    }

     public void run() {
        try {
            Thread.sleep(seconds*1000);
        } catch (InterruptedException ie) {
            System.out.println("Warning: Redirect/Refresh thread sleep interrupted, page may refresh sooner than expected.");
        }
        if (!cancelled) {
            boolean redirect=(url!=null);
            url=htmlC.convertURL(url);
            if (redirect) {
                htmlC.setPageStatus(HTMLCallback.STATUS_REDIRECTED);
            }

            htmlC.setPage(url); 
        }
    }

    public void cancel() {
        cancelled=true;
    }

}

/**
 * A label that when clicked focuses (and toggels when applicable) a certain input field.
 * This implements the LABEL html tag
 * 
 * @author Ofir Leitner
 */
class ForLabel extends Label {

    String id;
    HTMLComponent htmlC;

    ForLabel(String labelText,HTMLComponent htmlC,String id) {
        super(labelText);
        this.id=id;
        this.htmlC=htmlC;
    }

    /**
     * {@inheritDoc}
     */
    public void pointerReleased(int x, int y) {
        triggerAction();
        super.pointerPressed(x, y);
    }

    /**
     * Triggers the needed action for this ForLabel
     */
    void triggerAction() {
        Component cmp = (Component)htmlC.getInputFields().get(id);
        if (cmp!=null) {
            htmlC.selectComponent(cmp);
        }
    }
}

/**
 * A simple class used to hold both a display text and an actual value for a combobox item.
 * The renderer takes the display text, while the form submission process takes the value.
 *
 * @author Ofir Leitner
 */
class OptionItem {

    String text;
    String value;

    OptionItem(String text,String value) {
        this.text=text;
        if (value==null) { // If no value was specified, the value is the display text
            this.value=text;
        } else {
            this.value=value;
        }
    }

    String getValue() {
        return value;
    }

    public String toString() {
        return text;
    }

}

/**
 * 
 * @author ofir
 */
abstract class HTMLListItem extends Label {

    /**
     * Sets the list style to the given value
     *
     * @param type The list type
     */
    abstract public void setStyleType(int type);

    /**
     * Sets an image to replace the list's bullets
     * 
     * @param imageUrl A URL containing the image to use as the list's bullets
     */
    abstract public void setImage(String imageUrl);

}

class HTMLListIndex extends HTMLListItem {

    int index;
    int listType;

    static final String[] CSS_OL_TYPES = {"decimal","upper-alpha","lower-alpha","upper-roman","lower-roman","none"};

     /**
     *  Numeric ordered list type (1 ,2, 3)
     */
    static final int LIST_NUMERIC = 0;

    /**
     * Uppercase ordered list type (A, B, C)
     */
     private static final int LIST_UPPERCASE = 1;

     /**
      * Lowercase ordered list type (a, b, c)
      */
     private static final int LIST_LOWERCASE = 2;

     /**
      * Roman numerals uppercase ordered list type (I,II,III,IV,V ......)
      */
     private static final int LIST_ROMAN_UPPER = 3;

     /**
      * Roman numerals lowercase ordered list type (i, ii, iii, iv, v ...)
      */
     private static final int LIST_ROMAN_LOWER = 4;

     /**
      * A list with no numbers at all - can be triggered with CSS
      */
     private static final int LIST_NONE = 5;

     /**
      * Strings representing roman numerals 0-9
      */
     private static final String[] ROMAN_NUMERALS_ONES={"","I","II","III","IV","V","VI","VII","VIII","IX"};

     /**
      * Strings representing roman numeral tens (10,20,30 etc.)
      */
     private static final String[] ROMAN_NUMERALS_TENS={"","X","XX","XXX","XL","L","LX","LXX","LXXX","XC"};


    /**
     * Returns a list index text according to the list type
     *
     * @param index The list index
     * @param type The list type
     * @return The index converted according to the list style
     */
    private String getListIndexString(int index,int type) {
        if (index<=0) {
            return index+". ";
        }
        switch(type) {
            case LIST_NUMERIC:
                return index+". ";
            case LIST_UPPERCASE:
                return getLiteral(index,'A');
            case LIST_LOWERCASE:
                return getLiteral(index,'a');
            case LIST_ROMAN_UPPER:
                return getRomanIndexString(index);
            case LIST_ROMAN_LOWER:
                return getRomanIndexString(index).toLowerCase();
        }
        return "    "; //In case of "none" - To keep indentation
    }

    static String getLiteral(int index,char baseChar) {
        String literal="";
        while (index>0) {
            literal=(char)((index%26)+baseChar-1)+literal;
            index=index/26;
        }

        return literal+". ";
    }

    /**
     * Converts a number to a roman numeral (Up to 99, should suffice for HTML lists...)
     *
     * @param index The list index to convert
     * @return A roman numeral representing the given index
     */
    private String getRomanIndexString(int index) {
        index=index%100;
        return ROMAN_NUMERALS_TENS[index/10]+ROMAN_NUMERALS_ONES[index%10]+". ";
    }


    public HTMLListIndex(int index,int listType) {
        this.index=index;
        this.listType=listType;
        setText(getListIndexString(index, listType));
        getStyle().setMargin(0,0,0,0);
        getStyle().setPadding(0,0,0,0);
        getUnselectedStyle().setBgTransparency(0);

    }

    public void setStyleType(int type) {
        if (type!=-1) {
            listType=type-4;
            if (listType<0) {
                listType=5; //LIST_NONE
            }
            setText(getListIndexString(index, listType));
            repaint();
        }
    }

    public void setImage(String imageUrl) {
       // do nothing, has no meaning for ordered lists
    }

}

/**
 * A simple class drawing a bullet in various styles
 * 
 * @author Ofir Leitner
 */
class HTMLBullet extends HTMLListItem {

    static final String[] CSS_UL_TYPES = {"none","disc","circle","square"};

     // Unordered list types

     /**
      * Indicates no bullet should be displayed
      */
     private static final int BULLET_NONE = 0;

    /**
      * Indicates the disc style (full circle) for an unordered list
      */
     private static final int BULLET_DISC = 1;

     /**
      * Indicates the circle (empty circle) style for an unordered list
      */
     private static final int BULLET_CIRCLE = 2;

     /**
      * Indicates the square (full square) style for an unordered list
      */
     private static final int BULLET_SQUARE = 3;

    int level;
    int fontHeight;
    int color;
    HTMLComponent htmlC;

    static int convertULString(String listTypeStr) {
        if (listTypeStr==null) {
            return -1;
        }

        for(int j=0;j<CSS_UL_TYPES.length;j++) {
            if (listTypeStr.equalsIgnoreCase(CSS_UL_TYPES[j])) {
                return j;
            }
        }

        return -1;
    }


    public HTMLBullet(int level,int fontHeight,int color,HTMLComponent htmlC) {
        this.level=level;
        this.fontHeight=fontHeight;
        this.color=color;
        this.htmlC=htmlC;
        getStyle().setBgTransparency(0);
        setFocusable(false);
    }

    public void setStyleType(int type) {
        if (type!=-1) {
            level=type;
            setShouldCalcPreferredSize(true);
            repaint();
        }
    }

    public void setImage(String imageUrl) {
        if (imageUrl!=null) {
            //setText(" "); // Due to a LWUIT bug Labels with an icon only and no text can be cut
            htmlC.getThreadQueue().add(this, htmlC.convertURL(imageUrl));
        }
    }

    /**
     * {@inheritDoc}
     */
    public void paint(Graphics g) {
        if (getIcon()!=null) {
            super.paint(g);
            return;
        }
        int size=fontHeight/3;
        g.setColor(color);
        if (level==BULLET_DISC) { // Filled circle
            size+=2;
            g.fillArc(getX()+(getWidth()-size)/2, getY()+(getHeight()-size)/2, size,size,0,360);
        } else if (level==BULLET_CIRCLE) { // Empty circle
            g.drawArc(getX()+(getWidth()-size)/2, getY()+(getHeight()-size)/2, size,size,0,360);
        } else if (level==BULLET_SQUARE) { // Filled square
            g.fillRect(getX()+(getWidth()-size)/2, getY()+(getHeight()-size)/2, size,size);
        } 
    }

    /**
     * {@inheritDoc}
     */
    protected Dimension calcPreferredSize() {
        if (getIcon()!=null) {
            return super.calcPreferredSize();
        }
        if (level==BULLET_NONE) {
            return new Dimension(0,fontHeight);
        } else {
            return new Dimension(fontHeight,fontHeight);
        }
    }

}

/**
 * HTMLComboBox overrides ComboBox to allow usage of MultiComboBox as its list.
 * This is done for OPTGROUP labels support (Note that multiple features of MultiComboBox will be switched off)
 *
 * @author Ofir Leitner
 */
class HTMLComboBox extends ComboBox {

    /**
     * {@inheritDoc}
     */
    protected List createPopupList() {
        List l = new MultiComboBox(getModel(),false);
        l.setSmoothScrolling(isSmoothScrolling());
        l.setFixedSelection(getFixedSelection());
        l.setItemGap(getItemGap());
        l.setUIID("ComboBoxList");
        return l;
    }

}

