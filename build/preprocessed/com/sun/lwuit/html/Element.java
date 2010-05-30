/*
 * Copyright 2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
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

import com.sun.lwuit.Component;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * The Element class defines a single HTML element with its attributes and children.
 * Due to its hierarchial nature, this class can be used for a single "leaf" Element, for more complex elements (with child elements), and up to describing the entire document.
 *
 * @author Ofir Leitner
 */
class Element {

//////////////////////////////////
// Tags                         //
//////////////////////////////////

 //HTML Tag ID codes:
 static final int TAG_CSS_SELECTOR = -2;
 static final int TAG_UNSUPPORTED = -1;

//Structure Module
 static final int TAG_BODY = 0;
 static final int TAG_HEAD = 1;
 static final int TAG_HTML = 2;
 static final int TAG_TITLE = 3;

//Text Module -
 static final int TAG_ABBR = 4;    // No visual effect
 static final int TAG_ACRONYM = 5; // No visual effect
 static final int TAG_ADDRESS = 6; // No visual effect
 static final int TAG_BLOCKQUOTE = 7;
 static final int TAG_BR = 8;
 static final int TAG_CITE = 9;
 static final int TAG_CODE = 10;
 static final int TAG_DFN = 11;
 static final int TAG_DIV = 12;
 static final int TAG_EM = 13;
 static final int TAG_H1 = 14;
 static final int TAG_H2 = 15;
 static final int TAG_H3 = 16;
 static final int TAG_H4 = 17;
 static final int TAG_H5 = 18;
 static final int TAG_H6 = 19;
 static final int TAG_KBD = 20;
 static final int TAG_P = 21;
 static final int TAG_PRE = 22;
 static final int TAG_Q = 23;
 static final int TAG_SAMP = 24;
 static final int TAG_SPAN = 25;   // While this is not parsed, it will be kept in the DOM and as such CSS will affect its class/ID
 static final int TAG_STRONG = 26;
 static final int TAG_VAR = 27;

 //Hypertext Module -
 static final int TAG_A = 28;

 //List Module -
 static final int TAG_DL = 29;
 static final int TAG_DT = 30;
 static final int TAG_DD = 31;
 static final int TAG_OL = 32;
 static final int TAG_UL = 33;
 static final int TAG_LI = 34;

//Basic Forms Module -

 static final int TAG_FORM = 35;
 static final int TAG_INPUT = 36;
 static final int TAG_LABEL = 37;
 static final int TAG_SELECT = 38;
 static final int TAG_OPTION = 39;
 static final int TAG_TEXTAREA = 40;

 //Basic Tables Module -
 static final int TAG_CAPTION = 41;
 static final int TAG_TABLE = 42;
 static final int TAG_TD = 43;
 static final int TAG_TH = 44;
 static final int TAG_TR = 45;

 //Image Module -
 static final int TAG_IMG = 46;

 //Object Module -
 static final int TAG_OBJECT = 47; // Not supported
 static final int TAG_PARAM = 48;  // Not supported

 //Metainformation Module
 static final int TAG_META = 49;

 //Link Module -
 static final int TAG_LINK = 50;

 //Base Module
 static final int TAG_BASE = 51;

 //XHTML Mobile Profile additons
 static final int TAG_HR = 52;
 static final int TAG_OPTGROUP = 53; // Currently still not supported as LWUIT's ComboBox can't display option groups, but will display as a regular ComboBox
 static final int TAG_STYLE = 54;
 static final int TAG_B = 55;
 static final int TAG_I = 56;
 static final int TAG_BIG = 57;
 static final int TAG_SMALL = 58;
 static final int TAG_FIELDSET = 59;

 //Text nodes (not an actual tag - text segments are added by the parser as the 'text' tag
 static final int TAG_TEXT = 60;

 /**
 * Defines the tag names, these are specified according to the tag constants numbering.
 */
static final String[] TAG_NAMES = {
    "body","head","html","title"
    ,"abbr","acronym","address","blockquote","br","cite","code","dfn","div","em","h1","h2","h3","h4","h5","h6","kbd","p","pre","q","samp","span","strong","var"
    ,"a"
    ,"dl","dt","dd","ol","ul","li"
    ,"form","input","label","select","option","textarea"
    ,"caption","table","td","th","tr"
    ,"img"
    ,"object","param"
    ,"meta"
    ,"link"
    ,"base"
    ,"hr","optgroup","style","b","i","big","small","fieldset","text"
};


//////////////////////////////////
// Attributes                   //
//////////////////////////////////

//Tag attributes:
 static final int ATTR_CLASS = 0;
 static final int ATTR_ID = 1;
 static final int ATTR_STYLE = 2;
 static final int ATTR_TITLE = 3;
 static final int ATTR_XMLNS = 4;
 static final int ATTR_XMLLANG = 5;
 static final int ATTR_ALIGN = 6;
 static final int ATTR_BGCOLOR = 7;
 static final int ATTR_LINK = 8;
 static final int ATTR_TEXT = 9;
 static final int ATTR_VERSION = 10;
 static final int ATTR_CITE = 11;
 static final int ATTR_ACCESSKEY = 12;
 static final int ATTR_CHARSET = 13;
 static final int ATTR_HREF = 14;
 static final int ATTR_HREFLANG = 15;
 static final int ATTR_REL = 16;
 static final int ATTR_REV = 17;
 static final int ATTR_TABINDEX = 18;
 static final int ATTR_TYPE = 19;
 static final int ATTR_ACTION = 20;
 static final int ATTR_ENCTYPE = 21;
 static final int ATTR_METHOD = 22;
 static final int ATTR_WIDTH = 23;
 static final int ATTR_HEIGHT = 24;
 static final int ATTR_ALT = 25;
 static final int ATTR_HSPACE = 26;
 static final int ATTR_VSPACE = 27;
 static final int ATTR_LONGDESC = 28;
 static final int ATTR_LOCALSRC = 29;
 static final int ATTR_SRC = 30;
 static final int ATTR_SIZE = 31;
 static final int ATTR_CHECKED = 32;
 static final int ATTR_EMPTYOK = 33;
 static final int ATTR_FORMAT = 34;
 static final int ATTR_ISTYLE = 35;
 static final int ATTR_MAXLENGTH = 36;
 static final int ATTR_NAME = 37;
 static final int ATTR_VALUE = 38;
 static final int ATTR_FOR = 39;
 static final int ATTR_XMLSPACE = 40;
 static final int ATTR_MULTIPLE = 41;
 static final int ATTR_SELECTED = 42;
 static final int ATTR_ABBR = 43;
 static final int ATTR_AXIS = 44;
 static final int ATTR_COLSPAN = 45;
 static final int ATTR_HEADERS = 46;
 static final int ATTR_ROWSPAN = 47;
 static final int ATTR_SCOPE = 48;
 static final int ATTR_VALIGN = 49;
 static final int ATTR_START = 50;
 static final int ATTR_MEDIA = 51;
 static final int ATTR_LABEL = 52;
 static final int ATTR_SUMMARY = 53;
 static final int ATTR_CONTENT = 54;
 static final int ATTR_HTTPEQUIV = 55;
 static final int ATTR_SCHEME = 56;
 static final int ATTR_COLS = 57;
 static final int ATTR_ROWS = 58;

 // Unsupported attributes in XHTML-MP that we DO support
 static final int ATTR_DIR = 59; //Currently only supported in the html tag
 static final int ATTR_BORDER = 60;

  /**
  * Defines the allowed attribute names, these are specified according to the ATTR_* constants numbering.
  */
 private static final String[] ATTRIBUTE_NAMES = {
    "class", "id", "style", "title", "xmlns", "xml:lang", "align", "bgcolor", "link", "text", "version", "cite",
    "accesskey", "charset", "href", "hreflang", "rel", "rev", "tabindex", "type", "action", "enctype", "method",
    "width", "height", "alt", "hspace", "vspace", "longdesc", "localsrc", "src", "size", "checked", "emptyok",
    "format", "istyle", "maxlength", "name", "value", "for", "xml:space", "multiple", "selected","abbr","axis",
    "colspan","headers","rowspan","scope","valign","start","media","label","summary","content","http-equiv","scheme",
    "cols","rows","dir","border"
 };

 /**
  * This array defines the 6 common attributes that each tag has
  */
 private static final int[] COMMON_ATTRIBUTES = {ATTR_CLASS,ATTR_ID,ATTR_STYLE,ATTR_TITLE,ATTR_XMLNS,ATTR_XMLLANG};

 /**
  * This array defines the allowed attributes for each tag, according to the XHTML-MP 1.0 spec
  */
 private static final int[][] TAG_ATTRIBUTES = {
    //Structure Module
     {
        ATTR_BGCOLOR, // #rrggbb | colors  // Deprecated but supported
        ATTR_LINK, // #rrggbb | colors     // Deprecated but supported
        ATTR_TEXT //#rrggbb | colors       // Deprecated but supported
     }, // BODY = 0;
     {}, // HEAD = 1;
     {
        //ATTR_VERSION, // = //WAPFORUM//DTD XHTML Mobile 1.0//EN // We don't use the version attribute
        ATTR_DIR

     }, // HTML = 2;
     {}, // TITLE = 3;

    //Text Module -
     {}, // ABBR = 4;
     {}, // ACRONYM = 5;
     {}, // ADDRESS = 6;
     {
         //ATTR_CITE // URL // Not supported by any of the major browsers
     }, // BLOCKQUOTE = 7;
     {}, // BR = 8;
     {}, // CITE = 9;
     {}, // CODE = 10;
     {}, // DFN = 11;
     {
         ATTR_ALIGN // top/bottom/left/right
     }, // DIV = 12;
     {}, // EM = 13;
     {
        ATTR_ALIGN // top/bottom/left/right
     }, // H1 = 14;
     {
         ATTR_ALIGN // top/bottom/left/right
     }, // H2 = 15;
     {
         ATTR_ALIGN // top/bottom/left/right
     }, // H3 = 16;
     {
         ATTR_ALIGN // top/bottom/left/right
     }, // H4 = 17;
     {
         ATTR_ALIGN // top/bottom/left/right
     }, // H5 = 18;
     {
         ATTR_ALIGN // top/bottom/left/right
     }, // H6 = 19;
     {}, // KBD = 20;
     {
        ATTR_ALIGN // left | center | right | justify
     }, // P = 21;
     {
        //ATTR_XMLSPACE, // preserve // We don't use this attribute
     }, // PRE = 22;
     {
        //ATTR_CITE // URL // The cite attribute is not supported by any of the major browsers.
     }, // Q = 23;
     {}, // SAMP = 24;
     {}, // SPAN = 25;
     {}, // STRONG = 26;
     {}, // VAR = 27;

     //Hypertext Module -
     {
        ATTR_ACCESSKEY, // character
        //ATTR_CHARSET, // cdata // The charset attribute is not supported in any of the major browsers.
        ATTR_HREF, // URL
        //ATTR_HREFLANG, // ((?i)[A-Z]{1,8}(-[A-Z]{1,8})*)? // The hreflang attribute is not supported in any of the major browsers.
        //ATTR_REL, // nmtokens  // Not used by browsers
        //ATTR_REV, // nmtokens  // Not used by browsers
        //ATTR_TABINDEX, // number 
        //ATTR_TYPE, // cdata // Should specify the MIME type of the document, but we don't use it anyway
        ATTR_NAME // Note: Name on the a tag (anchor) is not supported on XHTML-MP 1.0, but we support it
     }, // A = 28;

     //List Module -
     {}, // DL = 29;
     {}, // DT = 30;
     {}, // DD = 31;
     {
        ATTR_START, // number  // Deprecated but supported
        ATTR_TYPE // cdata     // Deprecated but supported
     }, // OL = 32;
     {}, // UL = 33;
     {
        ATTR_TYPE, // cdata    // Deprecated but supported
        ATTR_VALUE // number   // Deprecated but supported
     }, // LI = 34;
    //Basic Forms Module -
     {
        ATTR_ACTION, // URL
        ATTR_ENCTYPE, // cdata
        ATTR_METHOD, // get | post

     }, // FORM = 35;
     {
        ATTR_ACCESSKEY, // character
        ATTR_CHECKED, // checked
        ATTR_EMPTYOK, // true | false    // This attribute was said to be supported on XHTML-MP1 on various sources, but verified as not
        ATTR_FORMAT, // cdata            // Deprecated but still supported
        //ATTR_ISTYLE, // cdata            // This attribute was said to be supported on XHTML-MP1 on various sources, but verified as not
        //ATTR_LOCALSRC, // cdata          // This attribute was said to be supported on XHTML-MP1 on various sources, but verified as not
        ATTR_MAXLENGTH, // number
        ATTR_NAME, // cdata
        ATTR_SIZE, // cdata
        ATTR_SRC, // URL
        ATTR_TABINDEX, // number
        ATTR_TYPE, // text | password | checkbox | radio | submit | reset | hidden
        ATTR_VALUE, // cdata
     }, // INPUT = 36;
     {
        ATTR_ACCESSKEY,
        ATTR_FOR
     }, // LABEL = 37;
     {
        ATTR_MULTIPLE, // multiple
        ATTR_NAME, // cdata
        ATTR_SIZE, // number
        ATTR_TABINDEX, // number
     }, // SELECT = 38;
     {
        ATTR_SELECTED, // selected
        ATTR_VALUE // cdata
     }, // OPTION = 39;
     {
        ATTR_ACCESSKEY, // character
        ATTR_COLS, // number
        ATTR_NAME, // cdata
        ATTR_ROWS, // number
        ATTR_TABINDEX, // number
     }, // TEXTAREA = 40;

     //Basic Tables Module -
     {
        ATTR_ALIGN // top/bottom/left/right
     }, // CAPTION = 41;
     {
        //ATTR_SUMMARY, // cdata  // The summary attribute makes no visual difference in ordinary web browsers.
        ATTR_BORDER
     }, // TABLE = 42;
     {
        //ATTR_ABBR, // cdata  // The abbr attribute makes no visual difference in ordinary web browsers.
        ATTR_ALIGN, // left | center | right
        //ATTR_AXIS, // cdata  // The axis attribute is not supported by any of the major browsers
        ATTR_COLSPAN, // number
        //ATTR_HEADERS, // IDREFS // The headers attribute makes no visual difference in ordinary web browsers.
        ATTR_ROWSPAN, // number
        //ATTR_SCOPE, // row | col // // The scope attribute makes no visual difference in ordinary web browsers.
        ATTR_VALIGN, // top | middle | bottom
        ATTR_WIDTH,  // number or % - deprecated but still supported
        ATTR_HEIGHT, // number or % - deprecated but still supported
     }, // TD = 43;
     {
        //ATTR_ABBR, // cdata  // The abbr attribute makes no visual difference in ordinary web browsers.
        ATTR_ALIGN, // left | center | right
        //ATTR_AXIS, // cdata  // The axis attribute is not supported by any of the major browsers
        ATTR_COLSPAN, // number
        //ATTR_HEADERS, // IDREFS // The headers attribute makes no visual difference in ordinary web browsers.
        ATTR_ROWSPAN, // number
        //ATTR_SCOPE, // row | col // // The scope attribute makes no visual difference in ordinary web browsers.
        ATTR_VALIGN, // top | middle | bottom
        ATTR_WIDTH,  // number or % - deprecated but still supported
        ATTR_HEIGHT, // number or % - deprecated but still supported

     }, // TH = 44;
     {
        ATTR_ALIGN, // = left | center | right
        ATTR_VALIGN // top | middle | bottom
     }, // TR = 45;

     //Image Module -
     {
        ATTR_ALIGN, // top | middle | bottom | left | right
        ATTR_ALT, // cdata
        ATTR_HEIGHT, // number[%]
        ATTR_HSPACE, // number
        //ATTR_LOCALSRC, // cdata          // This attribute was said to be supported on XHTML-MP1 on various sources, but verified as not
        //ATTR_LONGDESC, // URL //The longdesc attribute is not supported by any of the major browsers.
        ATTR_SRC, // URL
        ATTR_VSPACE, // number
        ATTR_WIDTH, // number[%]

     }, // IMG = 46;

     //Object Module -
     {
    /*    ATTR_ARCHIVE, // &URLs
        ATTR_CLASSID, // URL
        ATTR_CODEBASE, // URL
        ATTR_CODETYPE, // cdata
        ATTR_DATA, // URL
        ATTR_DECLARE, // declare
        ATTR_HEIGHT, // number[%]
        ATTR_NAME, // cdata
        ATTR_STANDBY, // cdata
        ATTR_TABINDEX, // number
        ATTR_TYPE, // cdata
        ATTR_WIDTH, // number[%] */
     }, // OBJECT = 47;
     {
       /* ATTR_NAME, //  cdata
        ATTR_TYPE, //  cdata
        ATTR_VALUE, //  cdata
        ATTR_VALUETYPE // data | ref | object */

     }, // PARAM = 48;

     //Metainformation Module
     {
        ATTR_CONTENT, // cdata
        ATTR_HTTPEQUIV, // nmtoken
        //ATTR_NAME, // nmtoken  // We do not make any use of this attribute
        //ATTR_SCHEME, // cdata  // We do not make any use of this attribute
     }, // META = 49;

     //Link Module -
     {
        //ATTR_CHARSET, // cdata  //The charset attribute is not supported by any of the major browsers.
        ATTR_HREF, // URL
        //ATTR_HREFLANG, // ((?i)[A-Z]{1,8}(-[A-Z]{1,8})*)? // The hreflang attribute is not supported in any of the major browsers.
        ATTR_MEDIA, // cdata
        ATTR_REL, // nmtokens
        //ATTR_REV, // nmtokens // The rev attribute is not supported in any of the major browsers.
        ATTR_TYPE, // cdata
     }, // LINK = 50;

     //Base Module -
     {
        ATTR_HREF // URL
     }, // BASE = 51;

     //XHTML-MP
     {
        ATTR_ALIGN, // left | center | right
        ATTR_SIZE, // number     // Deprecated but still supported
        ATTR_WIDTH, // number[%] // Deprecated but still supported

     }, // HR = 52;
     {
        ATTR_LABEL
     }, // OPTGROUP = 53
     {
        ATTR_MEDIA, // cdata
        ATTR_TYPE, // cdata
        //ATTR_XMLSPACE, // preserve  // We don't use this attribute

     }, // STYLE = 54
     {}, //B = 55;
     {}, //I = 56;
     {}, //BIG = 57;
     {}, //SMALL = 58;
     {}, //FIELDSET = 59;
     {}, //TEXT = 60;


 };




//////////////////////////////////
// Types                        //
//////////////////////////////////

// These are the possible types for an attribute. Types define what values are acceptable to the attribute.
static final int TYPE_NUMBER = 0;
static final int TYPE_PIXELS_OR_PERCENTAGE = 1;
static final int TYPE_COLOR = 2;
static final int TYPE_ALIGN = 3; //note: there are different types of align - TD allows only left,center,right DIV allows also justify, CAPTION allows top,bottom,left,right - however we don't get to that resolution
static final int TYPE_CHAR = 4;
static final int TYPE_URL = 5;
static final int TYPE_CDATA = 6;
static final int TYPE_NMTOKENS = 7;
static final int TYPE_ID = 8;
static final int TYPE_XMLNS = 9;
static final int TYPE_LANG_CODE = 10; // ((?i)[A-Z]{1,8}(-[A-Z]{1,8})*)?
static final int TYPE_VERSION = 11;
static final int TYPE_HTTP_METHOD = 12; // get / post
static final int TYPE_BOOLEAN = 13;
static final int TYPE_CHECKED = 14;
static final int TYPE_IDREF = 15;
static final int TYPE_PRESERVE = 16;
static final int TYPE_MULTIPLE = 17;
static final int TYPE_SELECTED = 18;
static final int TYPE_IDREFS = 19;
static final int TYPE_SCOPE = 20; // row / col
static final int TYPE_VALIGN = 21; // top/middle/bottom
static final int TYPE_NMTOKEN = 22; // top/middle/bottom
static final int TYPE_DIRECTION = 23; // ltr/rtl

static final int TYPE_CSS_LENGTH = 24; // thin/medium/thick + pixels with various suffixes
static final int TYPE_CSS_LENGTH_OR_PERCENTAGE = 25; // thin/medium/thick + pixels with various suffixes
static final int TYPE_CSS_URL = 26; 

 /**
  * This array assigns a type to each of the attributes.
  */
 private static final int[] ATTRIBUTE_TYPES = {
    TYPE_NMTOKENS, //"class",
    TYPE_ID, //"id",
    TYPE_CDATA, //"style",
    TYPE_CDATA, //"title",
    TYPE_XMLNS, //"xmlns",
    TYPE_LANG_CODE, //"xml:lang",
    TYPE_ALIGN, //"align",
    TYPE_COLOR, //"bgcolor",
    TYPE_COLOR, //"link",
    TYPE_COLOR, //"text",
    TYPE_VERSION, //"version",
    TYPE_URL, //"cite",
    TYPE_CHAR, //"accesskey",
    TYPE_CDATA, //"charset",
    TYPE_URL, //"href",
    TYPE_LANG_CODE, //"hreflang",
    TYPE_NMTOKENS, //"rel",
    TYPE_NMTOKENS, //"rev",
    TYPE_NUMBER, //"tabindex",
    TYPE_CDATA, //"type",
    TYPE_URL, //"action",
    TYPE_CDATA, //"enctype",
    TYPE_HTTP_METHOD, //"method",
    TYPE_PIXELS_OR_PERCENTAGE, //"width",
    TYPE_PIXELS_OR_PERCENTAGE, //"height",
    TYPE_CDATA, //"alt",
    TYPE_NUMBER, //"hspace",
    TYPE_NUMBER, //"vspace",
    TYPE_URL, //"longdesc",
    TYPE_CDATA, //"localsrc",
    TYPE_URL, //"src",
    TYPE_CDATA, //"size",
    TYPE_CHECKED, //"checked",
    TYPE_BOOLEAN, //"emptyok",
    TYPE_CDATA, //"format",
    TYPE_CDATA, //"istyle",
    TYPE_NUMBER, //"maxlength",
    TYPE_CDATA, //"name",
    TYPE_CDATA, //"value",
    TYPE_IDREF, //"for",
    TYPE_PRESERVE, //"xml:space",
    TYPE_MULTIPLE, //"multiple",
    TYPE_SELECTED, //"selected",
    TYPE_CDATA, //"abbr",
    TYPE_CDATA, //"axis",
    TYPE_NUMBER, //"colspan",
    TYPE_IDREFS, //"headers",
    TYPE_NUMBER, //"rowspan",
    TYPE_SCOPE, //"scope",
    TYPE_VALIGN, //"valign",
    TYPE_NUMBER, //"start",
    TYPE_CDATA, //"media",
    TYPE_CDATA, //"label",
    TYPE_CDATA, //"summary",
    TYPE_CDATA, //"content",
    TYPE_NMTOKEN, //"http-equiv",
    TYPE_CDATA, //"scheme",
    TYPE_NUMBER, //"cols",
    TYPE_NUMBER, //"rows"
    TYPE_DIRECTION, //"dir"
    TYPE_NUMBER // "border"
 };

/**
 * Some types accept only a specific set of strings. For these this array defines the allowed strings.
 * If the value is null it means that the type has another rule set (for example numnbers only).
 * This is checked against in teh DOM building process.
 */
 private static String[][] ALLOWED_STRINGS = {
    null, // TYPE_NUMBER = 0;
    null, // TYPE_PIXELS_OR_PERCENTAGE = 1;
    null, // TYPE_COLOR = 2;
    {"left","right","top","bottom","center","middle","justify"}, // TYPE_ALIGN = 3;
    null, // TYPE_CHAR = 4;
    null, // TYPE_URL = 5;
    null, // TYPE_CDATA = 6;
    null, // TYPE_NMTOKENS = 7;
    null, // TYPE_ID = 8;
    null, // TYPE_XMLNS = 9;
    null, // TYPE_LANG_CODE = 10; // ((?i)[A-Z]{1,8}(-[A-Z]{1,8})*)?
    null, // TYPE_VERSION = 11;
    {"get","post"}, // TYPE_HTTP_METHOD = 12; // get / post
    {"true","false"}, // TYPE_BOOLEAN = 13;
    {"checked"}, // TYPE_CHECKED = 14;
    null, // TYPE_IDREF = 15;
    {"default","preserve"}, // TYPE_PRESERVE = 16;
    {"multiple"}, // TYPE_MULTIPLE = 17;
    {"selected"}, // TYPE_SELECTED = 18;
    null, // TYPE_IDREFS = 19;
    {"row","col"}, // TYPE_SCOPE = 20; // row / col
    {"top","bottom","middle"}, // TYPE_VALIGN = 21; // top/middle/bottom
    null, // TYPE_NMTOKEN = 22; // top/middle/bottom
    {"ltr","rtl"}, // TYPE_DIRECTION
};

// Additional constants used to define allowed characters for specific types
private static final int DIGITS = 1;
private static final int HEX = 2;
private static final int ABC = 4;


//////////////////////////////////
// Colors                       //
//////////////////////////////////

//HTML-MP colors
static final int COLOR_AQUA = 0x00ffff;
static final int COLOR_BLACK = 0x000000;
static final int COLOR_BLUE = 0x0000ff;
static final int COLOR_FUCHSIA = 0xff00ff;
static final int COLOR_GRAY = 0x808080;
static final int COLOR_GREEN = 0x008000;
static final int COLOR_LIME = 0x00ff00;
static final int COLOR_MAROON = 0x800000;
static final int COLOR_NAVY = 0x000080;
static final int COLOR_OLIVE = 0x808000;
static final int COLOR_PURPLE = 0x800080;
static final int COLOR_RED = 0xff0000;
static final int COLOR_SILVER = 0xc0c0c0;
static final int COLOR_TEAL = 0x008080;
static final int COLOR_WHITE = 0xffffff;
static final int COLOR_YELLOW = 0xffff00;


/**
 * Defines the allowed color string that are acceptable as a value to color attributes
 */
static final String[] COLOR_STRINGS = {
    "aqua","black","blue","fuchsia","gray","green","lime","maroon",
    "navy","olive","purple","red","silver","teal","white","yellow"
};

/**
 * Assigns a color constant to each of the colors defined in COLOR_STRINGS
 */
static final int[] COLOR_VALS = {
    COLOR_AQUA,COLOR_BLACK,COLOR_BLUE,COLOR_FUCHSIA,COLOR_GRAY,COLOR_GREEN,COLOR_LIME,COLOR_MAROON,
    COLOR_NAVY,COLOR_OLIVE,COLOR_PURPLE,COLOR_RED,COLOR_SILVER,COLOR_TEAL,COLOR_WHITE,COLOR_YELLOW
};

static int getColor(String colorStr,int defaultColor) {
    if ((colorStr==null) || (colorStr.equals(""))) {
        return defaultColor;
    }
    if (colorStr.charAt(0)!='#') {

        if (colorStr.startsWith("rgb(")) {
            colorStr=colorStr.substring(4);
            char[] tokens= {',',',',')'};
            int weight=256*256;
            int color=0;
            for(int i=0;i<3;i++) {
                int index=colorStr.indexOf(tokens[i]);
                if (index==-1) {
                    return defaultColor; // Unparsed color
                }
                String channelStr=colorStr.substring(0, index).trim();

                int channel=HTMLComponent.calcSize(255, channelStr, 0,true);
                channel=Math.min(channel, 255); // Set to 255 if over 255
                channel=Math.max(channel, 0); // Set to 0 if negative

                color+=channel*weight;
                colorStr=colorStr.substring(index+1);
                weight/=256;
            }
            return color;

        } else {
            for(int i=0;i<COLOR_STRINGS.length;i++) {
                if (colorStr.equalsIgnoreCase(COLOR_STRINGS[i])) {
                    return COLOR_VALS[i];
                }
            }
        }
    } else {
        colorStr=colorStr.substring(1);
    }

    if (colorStr.length()==3) { // shortened format rgb - translated to rrggbb
        String newColStr="";
        for(int i=0;i<3;i++) {
            newColStr+=colorStr.charAt(i)+""+colorStr.charAt(i);
        }
        colorStr=newColStr;
    }

    try {
        int color=Integer.parseInt(colorStr,16);
        return color;
    } catch (NumberFormatException nfe) {
        return defaultColor;
    }
}


// Member variables:

    /**
     * The element name
     */
    String name;

    /**
     * The tag ID. Upon construction of tag with a name, a lookup is performed to assign it an ID accordign to the TAG_NAMES array.
     * THe ID is used to find what are the allowed attributes, and also prevents the need for further string parsing later on.
     */
    int id=TAG_UNSUPPORTED;

    /**
     * A vector containing this element's children
     */
    Vector children=new Vector();

    /**
     * This element's parent
     */
    Element parent;

    /**
     * A hashtable containing this element's attributes
     */
    Hashtable attributes=new Hashtable();

    /**
     * A vector holding all associate components
     */
     private Vector comps;

     /**
      * If true than the UI components where calculated automatically
      */
     boolean calculatedUi = false;


    /**
     * Empty constructor, only to enable CSSElement not to call the Element(String) constructor
     */
    protected Element() {
    }

    /**
     * Constructor for Element. This mostly sets up the element's ID.
     * 
     * @param name The Element's name
     */
    Element(String name) {
        int i=0;
        int tagId=-1;

        while((tagId==-1) && (i<TAG_NAMES.length)) {
            if (TAG_NAMES[i].equals(name)) {
                tagId=i;
            } else {
                i++;
            }
        }
        id=tagId;
    }

    /**
     * Sets the given component or Vector of components to be associated with this element.
     * This is used internally to apply CSS styling.
     * 
     * @param obj The component (or vector of components) representing this Element
     */
    void setAssociatedComponents(Object obj) {
        if (obj instanceof Vector) {
            comps=(Vector)obj;
        } else {
            comps=new Vector();
            comps.addElement(obj);
        }
    }

    /**
     * Adds the given component to be associated with this element.
     * This is used internally to apply CSS styling.
     * 
     * @param cmp The component to add
     */
    void addAssociatedComponent(Component cmp) {
        if (comps==null) {
            comps=new Vector();
        }
        comps.addElement(cmp);
    }

    /**
     * Adds the specified Element as a child to this element.
     * If the specified element was found to be unsupported (i.e. it's ID is TAG_UNSUPPORTED, it is not added.
     * 
     * @param childElement The child element
     */
    void addChild(Element childElement) {
        if (childElement.getId()!=TAG_UNSUPPORTED) {
            children.addElement(childElement);
            childElement.setParent(this);
        }
    }
    
    /**
     * Sets this element parent, done interanlly in addChild
     * 
     * @param parent The element's parent
     */
    private void setParent(Element parent) {
        this.parent=parent;
    }

    /**
     * Returns this Element's parent
     *
     * @return this Element's parent
     */
    Element getParent() {
        return parent;
    }

    /**
     * Returns whether this element supports the common core attributes.
     * These are attributes most HTML tags support, with a few exceptions that are checked here.
     * Note that to be exact the common atributes are divided to 2 groups: core attributes (class,id,title,style) and language attributes (xmlns,xml:lang)
     * The tags checked here all don't support the core attributes but in fact may support the language attributes.
     * Since the language attributes are not implemented anyway, this is not critical.
     * For reference, tags that do not support the language attributes in XHTML-MP1 are: param, hr, base, br
     * 
     * @return true if core attributes are supported, false otherwise
     */
    private boolean supportsCoreAttributes() {
        return ((id!=TAG_STYLE) && (id!=TAG_META) && (id!=TAG_HEAD) && (id!=TAG_HTML) && (id!=TAG_TITLE) && (id!=TAG_PARAM) && (id!=TAG_BASE));
    }

    /**
     * Adds the specified attribute and value to this Element if it is supported for the Element and has a valid value.
     *
     * @param attribute The attribute's name
     * @param value The attribute's value
     *
     * @return a positive error code or -1 if attribute is supported and valid
     */
    int addAttribute(String attribute,String value) {
        if (id==TAG_UNSUPPORTED) {
            return -1; //No error code for this case since tag not supported error is already notified before
        }

        int attrId=-1;
        int i=0;
        if (supportsCoreAttributes()) {
            while ((attrId==-1) && (i<COMMON_ATTRIBUTES.length)) {
                if (ATTRIBUTE_NAMES[COMMON_ATTRIBUTES[i]].equals(attribute)) {
                    attrId=COMMON_ATTRIBUTES[i];
                } else {
                    i++;
                }
            }
        }

        i=0;
        while ((attrId==-1) && (i<TAG_ATTRIBUTES[id].length)) {
            if (ATTRIBUTE_NAMES[TAG_ATTRIBUTES[id][i]].equals(attribute)) {
                attrId=TAG_ATTRIBUTES[id][i];
            } else {
                i++;
            }
        }

        if (attrId==-1) {
            return HTMLCallback.ERROR_ATTRIBUTE_NOT_SUPPORTED;

        } else {
            if (isValid(ATTRIBUTE_TYPES[attrId], value)) {
                attributes.put(new Integer(attrId), value);
            } else {
                return HTMLCallback.ERROR_ATTIBUTE_VALUE_INVALID;
            }
        }
        
        return -1;
    }

    /**
     * Returns a list of supported attributes for this tag. Note that the list does not include the core attributes that are supported on almost all tags
     * 
     * @return a list of supported attributes for this tag
     */
    String getSupportedAttributesList() {
        if ((id<0) || (id>=TAG_ATTRIBUTES.length)) {
            return "Unknown";
        }
        String list="";
        for (int a=0;a<TAG_ATTRIBUTES[id].length;a++) {
            list+=ATTRIBUTE_NAMES[TAG_ATTRIBUTES[id][a]]+",";
        }
        if (supportsCoreAttributes()) {
            for (int a=0;a<COMMON_ATTRIBUTES.length;a++) {
                list+=ATTRIBUTE_NAMES[COMMON_ATTRIBUTES[a]]+",";
            }
        }

        if (list.endsWith(",")) {
            list=list.substring(0, list.length()-1);
        }
        if (list.equals("")) {
            list="None";
        }
        return list;
    }

    /**
     * Verifies that the specified value conforms with the attribute's type restrictions.
     * This basically checks the attribute type and according to that checks the value.
     *
     * @param attrId The attribute ID
     * @param value The value to be checked
     * @return true if the value is valid for this attribute, false otherwise
     */
    private boolean isValid(int type,String value) {
        if (value==null) { // a null value is invalid for all attributes
            return false;
        }
        if (ALLOWED_STRINGS[type]!=null) {
            return verifyStringGroup(value, ALLOWED_STRINGS[type]);
        }

        switch(type) {
            case TYPE_NUMBER:
                return verify(value, DIGITS, null);
            case TYPE_PIXELS_OR_PERCENTAGE:
                if (value.endsWith("%")) { //percentage
                    value=value.substring(0,value.length()-1);
                } else if (value.endsWith("px")) { //pixels
                    value=value.substring(0,value.length()-2);
                }
                return verify(value, DIGITS, null);
            case TYPE_CHAR:
                return verify(value, DIGITS|ABC, null, 1, 1);
            case TYPE_COLOR:
                if (value.charAt(0)!='#') {
                    return verifyStringGroup(value, COLOR_STRINGS);
                } else {
                    return verify(value.substring(1), HEX, null, 3, 6); //Color can also be #rgb which means #rrggbb (as for 4,5 chars these will also be tolerated)
                }

            default:
                return true;
        }
    }

    /**
     * A convenience method for verifying strings with no length restrictions
     * 
     * @param value The string to be checked
     * @param allowedMask DIGITS or HEX or ABC or a combination of those
     * @param allowedChars Characters that are allowed even if they don't conform to the mask
     * @return true if the string is valid, false otherwise.
     */
    private boolean verify(String value,int allowedMask,char[] allowedChars) {
        return verify(value,allowedMask,allowedChars,-1,-1);
    }
    
    /**
     * Verifies that the specified string conforms with the specified restrictions.
     *
     * @param value The string to be checked
     * @param allowedMask DIGITS or HEX or ABC or a combination of those
     * @param allowedChars Characters that are allowed even if they don't conform to the mask
     * @param minLength Minimum length
     * @param maxLength Maximum length
     * @return true if the string is valid, false otherwise.
     */
    private boolean verify(String value,int allowedMask,char[] allowedChars,int minLength,int maxLength) {
            if ((minLength!=-1) && (value.length()<minLength)) {
                return false;
            }
            if ((maxLength!=-1) && (value.length()>maxLength)) {
                return false;
            }


            int i=0;
            while (i<value.length()) {
                boolean found=false;
                char ch=value.charAt(i);
                if ((allowedMask & HEX)!=0) {
                    if (((ch>='0') && (ch<='9')) ||
                        ((ch>='A') && (ch<='F')) ||
                        ((ch>='a') && (ch<='f'))) {
                        found=true;
                    }
                }

                if ((allowedMask & DIGITS)!=0) {
                    if (((ch>='0') && (ch<='9'))) {
                        found=true;
                    } else if ((i==0) && ((ch=='-') || (ch=='+'))) { // Sign is allowed as the first character
                        found=true;
                    }
                }

                if ((!found) && ((allowedMask & ABC)!=0)) {
                    if (((ch>='a') && (ch<='z')) ||
                        ((ch>='A') && (ch<='Z')))
                    {
                        found=true;
                    }
                }

                if ((!found) && (allowedChars!=null)) {
                    int c=0;
                    while ((!found) && (c<allowedChars.length)) {
                        if (ch==allowedChars[c]) {
                            found=true;
                        } else {
                            c++;
                        }
                    }
                }

                if (!found) {
                    return false;
                }
                i++;

            }
            return true;
    }

    /**
     * Verifies that the specified string equals to one of the allowed strings
     * 
     * @param value The string to be checked
     * @param allowed The list of allowed strings
     * @return true if the string equals to one of the allowed, false otherwise
     */
    boolean verifyStringGroup(String value,String[] allowed) {
        for(int i=0;i<allowed.length;i++) {
            if (value.equalsIgnoreCase(allowed[i])) {
                return true;
            }
        }
        return false;
    }


    /**
     * Simple getter for this Element's name
     * 
     * @return the Element's name
     */
    String getName() {
        if ((id<0) || (id>=TAG_NAMES.length)) {
            return "Unsupported";
        }
        return TAG_NAMES[id];
    }

    /**
     * Simple getter for this Element's ID
     *
     * @return the ELement's ID
     */
    int getId() {
        return id;
    }

    /**
     Returns the number of this Element's children
     * 
     * @return the number of this Element's children
     */
    int getNumChildren() {
        return children.size();
    }

    /**
     * Returns the Element's child positioned at the specified index
     *
     * @param index The requested index
     * @return child number index of this ELement
     * @throws ArrayIndexOutOfBoundsException if the index is bigger than the children's count or smaller than 0
     */
    Element getChildAt(int index) {
        if ((index<0) || (index>=children.size())) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return (Element)children.elementAt(index);

    }

    /**
     * Returns an Element's child by a tag ID (One of the TAG_* constants)
     * 
     * @param id The child's tag ID
     * @return the first child with the specified ID, or null if not found
     */
    Element getChildById(int id) {
        int i=0;
        Element found=null;
        while ((found==null) && (i<children.size())) {
            Element child=(Element)children.elementAt(i);
            if (child.getId()==id) {
                found=child;
            } else {
                i++;
            }
        }
        return found;

    }

    /**
     * Returns an Element's child by a tag name (one of the TAG_NAMES)
     * 
     * @param name The child's tag name
     * @return the first child with the specified name, or null if not found
     */
    Element getChildByName(String name) {
        int i=0;
        Element found=null;
        while ((found==null) && (i<children.size())) {
            Element child=(Element)children.elementAt(i);
            if (child.getName().equalsIgnoreCase(name)) {
                found=child;
            } else {
                i++;
            }
        }
        return found;
    }

    /**
     * Returns an Element's attribute by the attribute's ID (One of the ATTR_* constants)
     *
     * @param id The attribute's ID
     * @return the attribute with the specified ID, or null if not found
     */
    String getAttributeById(int id) {
        return (String)attributes.get(new Integer(id));
    }

    /**
     *
     * {@inheritDoc}
     */
    public String toString() {
        return toString("");
    }

    
    /**
     * Returns the attribute name of the requested attribute
     * 
     * @param attrKey The attribute key, which is typically an Integer object made of its int attrId
     * @return the attribute name of the requested attribute
     */
    String getAttributeName(Integer attrKey) {
        return ATTRIBUTE_NAMES[attrKey.intValue()];
    }

    /**
     * A recursive method for creating a printout of a full tag with its entire hierarchy.
     * This is used by the public method toString().
     *
     * @param spacing Increased by one in each recursion phase to provide with indentation
     * @return the printout of this tag
     */
    private String toString(String spacing) {

        String str=spacing+"<"+getName();
        for(Enumeration e=attributes.keys();e.hasMoreElements();) {
            Integer attrKey=(Integer)e.nextElement();
            String attrStr=getAttributeName(attrKey);

            String val=(String)attributes.get(attrKey);
            str+=" "+attrStr+"='"+val+"' ("+attrKey+")";
        }
        str+=">\n";

        for(int i=0;i<children.size();i++) {
            str+=((Element)children.elementAt(i)).toString(spacing+' ');
        }
        str+=spacing+"</"+getName()+">\n";
        return str;

    }

    /**
     * Returns a vector of Components associated with this Element
     * 
     * @return a vector of Components associated with this Element
     */
    Vector getUi() {
        if (comps==null) { // If no UI exists this may be a tag with children that do have UI, such as TAG_A
            comps=new Vector();
            for (Enumeration e=children.elements();e.hasMoreElements();) {
                Element child = (Element)e.nextElement();
                Vector childUI=child.getUi();
                for (Enumeration e2=childUI.elements();e2.hasMoreElements();) {
                    comps.addElement(e2.nextElement());
                }
            }
            calculatedUi=true;
        }
        return comps;
    }

    /**
     * Causes a recalculation of the UI, if the UI of this element was deduced from children components
     */
    void recalcUi() {
        if (calculatedUi) {
            comps=null;
            calculatedUi=false;
        }
    }

}