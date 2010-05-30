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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * The parser class is used to parse an XHTML-MP 1.0 document into a DOM object (Element).
 * Unsupported tags and attributes as well as comments are dropped in the parsing process.
 * The parser is also used to external CSS files, embedded CSS segments and CSS within the 'style' attribute.
 * 
 * @author Ofir Leitner
 */
class Parser {

    /**
     * The most common char entities strings. When a char entity is found these will be compared against first.
     */
    private static final String[] COMMON_CHAR_ENTITIES = {"nbsp", // White space
                                                          "lt", // lesser-than
                                                          "gt", // greater-than
                                                          "amp", // ampersand
                                                          "quot", //quotation mark
                                                          "apos", // apostrophe
                                                          "bull", //bullet
                                                          "euro"}; //euro

    /**
     * The numericals value of the most common char entities strings above.
     */
    private static final int[] COMMON_CHAR_ENTITIES_VALS = {160, // "nbsp", // White space
                                                            60, // "lt", // lesser-than
                                                            62, // "gt", // greater-than
                                                            38, // "amp", // ampersand
                                                            34, // "quot", //quotation mark
                                                            39, // "apos", // apostrophe
                                                            8226, // "bull", //bullet
                                                            8364}; // "euro"}; //euro

    /**
     * The parser is a singleton, this static member holds its instance
     */
    static Parser instance;

    /**
     * The list of empty tags (i.e. tags that naturally don't have any children).
     * This is used to enable empty tags to be closed also in a non-strict way (i.e. &lt;br&gt; instead of &lt;br&gt/;)
     * some of these tags are not a part of the XHTML-MP 1.0 standard, but including them here allows a more smooth parsing if the document is not strictly XHTML-MP 1.0
     */
    static String[] EMPTY_TAGS = {"br","link","meta","base","area","basefont","col","frame","hr","img","input","isindex","param"};


   /**
    * This is a list of ISO 8859-1 Symbols that can be used as HTML char entities
    */
    private static final String[] CHAR_ENTITY_STRINGS = {
        "iexcl","cent","pound","curren","yen","brvbar","sect","uml","copy","ordf","laquo","not","shy","reg","macr","deg","plusmn","sup2","sup3","acute",
        "micro","para","middot","cedil","sup1","ordm","raquo","frac14","frac12","frac34","iquest","Agrave","Aacute","Acirc","Atilde","Auml","Aring","AElig",
        "Ccedil","Egrave","Eacute","Ecirc","Euml","Igrave","Iacute","Icirc","Iuml","ETH","Ntilde","Ograve","Oacute","Ocirc","Otilde","Ouml","times","Oslash",
        "Ugrave","Uacute","Ucirc","Uuml","Yacute","THORN","szlig","agrave","aacute","acirc","atilde","auml","aring","aelig","ccedil","egrave","eacute","ecirc",
        "euml","igrave","iacute","icirc","iuml","eth","ntilde","ograve","oacute","ocirc","otilde","ouml","divide","oslash","ugrave","uacute","ucirc","uuml",
        "yacute","thorn","yuml"};

   /**
    * This hashtable contains user defined char entities that can be added via HTMLComponent.addCharEntity
    */
   private static Hashtable USER_DEFINED_CHAR_ENTITIES;

   /**
    * The supported CSS media types, this is relevant for CSS at-rules (i.e. @import and @media)
    * The default values according to the WCSS specs the default one is "handheld" and "all" (Which is always accepted)
    */
   private static String[] SUPPORTED_MEDIA_TYPES = {"all","handheld"};

    /**
     * Returns or creates the Parser's single instance
     * 
     * @return the Parser's instance
     */
    static Parser getInstance() {
        if (instance==null) {
            instance=new Parser();
        }
        return instance;
    }

    /**
     * Sets the supported CSS media types to the given strings.
     * Usually the default media types ("all","handheld") should be suitable, but in case this runs on a device that matches another profile, the developer can specify it here.
     *
     * @param supportedMediaTypes A string array containing the media types that should be supported
     */
    static void setCSSSupportedMediaTypes(String[] supportedMediaTypes) {
        SUPPORTED_MEDIA_TYPES=supportedMediaTypes;
    }

    /**
     * Matches the given string to the given options and returns the matching value, or -1 if none found.
     *
     * @param str The string to compare
     * @param options The options to match the string against
     * @return The appropriate matching value: If the string equals (case ignored) to the option in the X position of the options array, the int X will be returned. If the string didn't match any of the options -1 is returned.
     */
    static int getStringVal(String str,String[] options) {
        return getStringVal(str, options, null, -1);
    }

    /**
     * Matches the given string to the given options and returns the matching value, or -1 if none found.
     *
     * @param str The string to compare
     * @param options The options to match the string against
     * @param vals The values to match to each option (According to the position in the array), this can be null.
     * @return The appropriate matching value: If the string equals (case ignored) to the option in the X position of the options array, this returns the value in the X position of the vals array, or simply X if vals is null. If the string didn't match any of the options -1 is returned.
     */
    static int getStringVal(String str,String[] options,int[] vals) {
        return getStringVal(str, options, vals, -1);
    }

    /**
     * Matches the given string to the given options and returns the matching value, or the default one if none found.
     *
     * @param str The string to compare
     * @param options The options to match the string against
     * @param defaultValue The default value to return if the string was null or not found among the options
     * @return The appropriate matching value: If the string equals (case ignored) to the option in the X position of the options array, the int X will be returned. If the string didn't match any of the options the defaultValue is returned.
     */
    static int getStringVal(String str,String[] options,int defaultValue) {
        return getStringVal(str, options, null, defaultValue);
    }

    /**
     * Matches the given string to the given options and returns the matching value, or the default one if none found.
     * 
     * @param str The string to compare
     * @param options The options to match the string against
     * @param vals The values to match to each option (According to the position in the array), this can be null.
     * @param defaultValue The default value to return if the string was null or not found among the options
     * @return The appropriate matching value: If the string equals (case ignored) to the option in the X position of the options array, this returns the value in the X position of the vals array, or simply X if vals is null. If the string didn't match any of the options the defaultValue is returned.
     */
    static int getStringVal(String str,String[] options,int[] vals,int defaultValue) {
        if (str!=null) {
            for(int i=0;i<options.length;i++) {
                if (str.equalsIgnoreCase(options[i])) {
                    if (vals!=null) {
                        return vals[i];
                    } else {
                        return i;
                    }
                }
            }
        }
        return defaultValue;
    }

    /**
     * Adds the given symbol and code to the user defined char entities table
     * 
     * @param symbol The symbol to add
     * @param code The symbol's code
     */
    static void addCharEntity(String symbol,int code) {
        if (USER_DEFINED_CHAR_ENTITIES==null) {
            USER_DEFINED_CHAR_ENTITIES=new Hashtable();
        }
        USER_DEFINED_CHAR_ENTITIES.put(trimCharEntity(symbol),new Integer(code));
    }

    /**
     * Adds the given symbols array  to the user defined char entities table with the startcode provided as the code of the first string, startcode+1 for the second etc.
     * Some strings in the symbols array may be null thus skipping code numbers.
     *
     * @param symbols The symbols to add
     * @param startcode The symbol's code
     */
    static void addCharEntitiesRange(String[] symbols,int startcode) {
        if (USER_DEFINED_CHAR_ENTITIES==null) {
            USER_DEFINED_CHAR_ENTITIES=new Hashtable();
        }
        for(int i=0;i<symbols.length;i++) {
            if (symbols[i]!=null) {
                USER_DEFINED_CHAR_ENTITIES.put(trimCharEntity(symbols[i]),new Integer(startcode+i));
            }
        }
    }

    /**
     * Trims unneeded & and ; from the symbol if exist
     *
     * @param symbol The char entity symbol
     * @return A trimmed char entity without & and ;
     */
    private static String trimCharEntity(String symbol) {
        if (symbol.startsWith("&")) {
            symbol=symbol.substring(1);
        }
        if (symbol.endsWith(";")) {
            symbol=symbol.substring(0, symbol.length()-1);
        }
        return symbol;
    }


    /**
     * This method translates between a HTML char entity string to the according char code.
     * The string is first compared to the 6 most popular strings: nbsp,quot,apos,amp,lt and gt.
     * If not found, the search continues to a wider string array of char codes 161-255 which are supported in ISO-8859-1
     * In addition 'euro' was added as it is out of the regular ISO-8859-1 table but popular.
     * 
     * @param symbol The symbol to lookup
     * @return The char code of the symbol, or -1 if none found
     */
    private static int getCharEntityCode(String symbol) {
        // First tries the most popular char entities
        int val=getStringVal(symbol, COMMON_CHAR_ENTITIES, COMMON_CHAR_ENTITIES_VALS);
        if (val!=-1) {
            return val;
        } else {
            // Not one of the most popular char codes, proceed to check the ISO-8859-1 symbols array
            val=getStringVal(symbol, CHAR_ENTITY_STRINGS);
            if (val!=-1) {
                return val+161;
            }

            // Not found in the standard symbol table, see if it is in the user defined symbols table
            if (USER_DEFINED_CHAR_ENTITIES!=null) {
                Object charObj=USER_DEFINED_CHAR_ENTITIES.get(symbol);
                if (charObj!=null) {
                    return ((Integer)charObj).intValue();
                }
            }

            // Not found anywhere
            return -1;
        }
    }


    /**
     * Converts a char entity to the matching character.
     * This handles both numbered and symbol char entities (The latter is done via getCharEntityCode)
     *
     * @param charEntity The char entity to convert
     * @return A string containing a single char, or an empty string if the char entity couldn't be converted
     */
    private String convertCharEntity(String charEntity,HTMLCallback callback) {
        int charCode=-1;
        if (charEntity.startsWith("#")) { //numbered char entity
            if (charEntity.startsWith("#x")) { //hex
                try {
                    charCode=Integer.parseInt(charEntity.substring(2),16);
                } catch (NumberFormatException nfe) {
                    //if not a number - simply ignore char entity
                }
            } else {
                try {
                    charCode=Integer.parseInt(charEntity.substring(1));
                } catch (NumberFormatException nfe) {
                    //if not a number - simply ignore char entity
                }
            }
        } else { //not numbered, rather a symbol
            charCode=getCharEntityCode(charEntity);
        }

        if (charCode!=-1) {
            return ""+(char)charCode;
        } else {
            notifyError(callback, HTMLCallback.ERROR_UNRECOGNIZED_CHAR_ENTITY,null,null,null, "Unrecognized char entity: "+charEntity);
            return "&"+charEntity+";"; // Another option is to return an empty string, but returning the entity will unravel bugs and will also allow ignoring common mistakes such as using the & char (instead of &apos;)
        }

    }

    /**
     * This is the entry point for parsing a document and the only non-private member method in this class
     *
     * @param is The InputStream containing the XML
     * @return an Element object describing the parsed document
     */
    Element parse(InputStreamReader is,HTMLComponent htmlC) {
        Element rootElement=new Element("ROOT"); // ROOT is a "dummy" element that all other document elements are added to
        try {
            parseTagContent(rootElement, is, htmlC);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return rootElement;
    }

    /**
     * This method parses tags content. It accumulates text and adds it as a child element in the parent Element.
     * Upon bumping a start tag character it calls the parseTag method.
     * This method is called at first from the parse method, and later on from parseTag (which creates the recursion).
     *
     * @param element The current parent element
     * @param is The InputStream containing the XML
     * @throws IOException
     */
    private void parseTagContent(Element element,InputStreamReader is, HTMLComponent htmlC) throws IOException {
        HTMLCallback callback=htmlC.getHTMLCallback();
        if ((HTMLComponent.SUPPORT_CSS) && (htmlC.loadCSS) && (element.getId() == Element.TAG_STYLE)) { // We aren't strict and don't require text/css in a style tag // && "text/css".equals(element.getAttributeById(Element.ATTR_TYPE)))) {
            CSSElement addTo = parseCSSSegment(is,null,htmlC,null);
            htmlC.addToEmebeddedCSS(addTo);
            return;
        }
        String text=null;
        boolean leadingSpace=false;
        char c=(char)is.read();
        String charEntity=null;

        while((byte)c!=-1) {
            if (c=='<') {
                if (text!=null) {
                    if (charEntity!=null) { //Mistakenly "collected" something that is not a char entity, perhaps misuse of the & character (instead of using &apos;)

                        text+="&"+charEntity;
                        charEntity=null;
                    }
                    if (leadingSpace) {
                        text=" "+text;
                    }
                    Element textElement=new Element("text");
                    textElement.addAttribute("title", text);
                    element.addChild(textElement);
                    text=null;
                    leadingSpace=false;
                    
                }


                Element childElement=parseTag(is,htmlC);
                if (childElement==null) { //was actually an ending tag
                    String closingTag="";
                    c=(char)is.read();
                    while ((c!='>')) {
                        closingTag+=c;
                        c=(char)is.read();
                    }

                    if (closingTag.equalsIgnoreCase(element.getName())) {
                        return;
                    } else if (isEmptyTag(closingTag)) {
                        // do nothing, someone chose to close an empty tag i.e. <img ....></img> or <br></br>
                    } else {
                        notifyError(callback, HTMLCallback.ERROR_NO_CLOSE_TAG, element.getName(), null, null, "Malformed HTML - no appropriate closing tag for "+element.getName());
                    }
                } else if (childElement.getId()!=-1) { //If tag unsupported don't add it
                    element.addChild(childElement);
                }
            } else if (text!=null) {
                if (charEntity!=null) {
                    if (c==';') { //end
                        text+=convertCharEntity(charEntity,callback);
                        charEntity=null;
                    } else {
                        charEntity+=c;
                    }
                } else if (c=='&') { //start char entity
                    charEntity=""; // The & is not included in the string we accumulate
                } else {
                    text+=c;
                }
            } else if (!isWhiteSpace(c)) {
                if (c=='&') { //text starts with a character entity (i.e. &nbsp;)
                    charEntity=""; // The & is not included in the string we accumulate
                    text=""; //Initalize text so it won't be null
                } else {
                    text=""+c;
                }
            } else if (c==' ') {
                leadingSpace=true;
            }
            c=(char)is.read();
        }
    }

    /**
     * Checks if the specified character is a white space or not.
     * Exposed to packaage since used by HTMLComponent as well
     *
     * @param ch The character to check
     * @return true if the character is a white space, false otherwise
     */
    static boolean isWhiteSpace(char ch) {
        return ((ch==' ') || (ch=='\n') || (ch=='\t') || (ch==10) || (ch==13));
    }

    /**
     * This method collects the tag name and all of its attributes.
     * For comments and XML declarations this will call the parseCommentOrXMLDeclaration method.
     * Note that this method returns an Element with a name/id and attrbutes, but not its content which will be done by parseTagContent
     * 
     * @param is The InputStream containing the XML
     * @return The parsed element
     * @throws IOException
     */
    private Element parseTag(InputStreamReader is,HTMLComponent htmlC) throws IOException {
        String tagName="";
        String curAttribute="";
        String curValue="";
        boolean procInst=false;

        HTMLCallback callback=htmlC.getHTMLCallback();
        char c=(char)is.read();
        if (c=='/') {
            return null; //end tag
        } else if (c=='!') {
            c=(char)is.read();
            char c2=(char)is.read();
            if ((c=='-') && (c2=='-')) { //comment
                return parseCommentOrXMLDeclaration(is,"-->");
            } else {
                return parseCommentOrXMLDeclaration(is,">"); //parse doctypes i.e. <!DOCTYPE .... > as comments as well - i.e. ignore them
            }
        } else if (c=='?') {
            procInst=true;
            c=(char)is.read();
            //return parseCommentOrXMLDeclaration(is,">"); //parse XML declaration i.e. <?xml version="1.0" encoding="ISO-8859-1"?> as comments as well - i.e. ignore them
        }

         //read and ignore any whitespaces before tag name
        while (isWhiteSpace(c)) {
            c=(char)is.read();
        }

        //collect tag name
        while ((!isWhiteSpace(c)) && (c!='>') && (c!='/')) {
            tagName+=c;
            c=(char)is.read();
        }

         //read and ignore any whitespaces after tag name
        while (isWhiteSpace(c)) {
            c=(char)is.read();
        }

        tagName=tagName.toLowerCase();
        if (procInst) {
            if (tagName.equals("xml-stylesheet")) { // The XML processing instruction <?xml-stylesheet ... ?> has the same parameters as <link .. > and behaves the same way
                tagName="link";
            } else { // Processing instruction not supported - read till its end
                c=(char)is.read();
                while (c!='>') {
                    c=(char)is.read();
                }
                return new Element("unsupported");
            }
        }
        Element element=new Element(tagName);

        if (element.getId()==-1) {
            notifyError(callback, HTMLCallback.ERROR_TAG_NOT_SUPPORTED, tagName, null, null, "The tag '"+tagName+"' is not supported in XHTML-MP 1.0");

            // If tag is not supported we skip it all till the closing tag.
            // This is especially important for the script tag which may contain '<' and '>' which might confuse the parser
            char lastChar=c;
            while (c!='>') { // Read till the end of the tag
                lastChar=c;
                c=(char)is.read();
            }
            if (lastChar!='/') { // If this is an empty tag, no need to search for its closing tag as there's none...
                String endTag="</"+tagName+">";
                int index=0;
                while(index<endTag.length()) {
                    c=(char)is.read();

                    if ((c>='A') && (c<='Z')) {
                        c=(char)(c-'A'+'a');
                    }
                    if (c==endTag.charAt(index)) {
                        index++;
                    } else {
                        index=0;
                    }
                }
            }

            return element;
        }

        if (c=='>') { //tag declartion ended, process content
            if (!isEmptyTag(tagName)) {
                parseTagContent(element, is, htmlC);
            }
            return element;
        } else if ((c=='/') || ((procInst) && (c=='?'))) { //closed tag - no content
            c=(char)is.read();
            if (c=='>') {
                return element;
            } else {
                notifyError(callback, HTMLCallback.ERROR_UNEXPECTED_CHARACTER, tagName, null, null, "HTML malformed - no > after /");
            }
        }


        while(true) {
            curAttribute=""+c;
            c=(char)is.read();
            while ((!isWhiteSpace(c)) && (c!='=') && (c!='>')) {
                curAttribute+=c;
                c=(char)is.read();
            }

            if (c=='>') { // tag close char shouldn't be found here, but if the HTML is slightly malformed we return the element
                notifyError(callback, HTMLCallback.ERROR_UNEXPECTED_TAG_CLOSING, tagName,curAttribute,null, "Unexpected tag closing in tag "+tagName+", attribute="+curAttribute);
                if (!isEmptyTag(tagName)) {
                    parseTagContent(element, is, htmlC);
                }
                return element;
            }

             //read and ignore any whitespaces after attribute name
            while (isWhiteSpace(c)) {
                c=(char)is.read();
            }

            if (c!='=') {
                notifyError(callback, HTMLCallback.ERROR_UNEXPECTED_CHARACTER, tagName, curAttribute, null, "Unexpected character "+c+", expected '=' after attribute "+curAttribute+" in tag "+tagName);
                if (c=='>') { // tag close char shouldn't be found here, but if the HTML is slightly malformed we return the element
                    if (!isEmptyTag(tagName)) {
                        parseTagContent(element, is, htmlC);
                    }
                    return element;
                }


                continue; //if attribute is not followed by = then process the next attribute
            }

            c=(char)is.read();
             //read and ignore any whitespaces before attribute value
            while (isWhiteSpace(c)) {
                c=(char)is.read();
            }

            char quote=' ';


            if ((c=='"') || (c=='\'')) {
                quote=c;
            } else {
                curValue+=c;
            }

            String charEntity=null;
            boolean ended=false;
            while (!ended) {
                c=(char)is.read();
                if (c==quote) {
                    ended=true;
                    c=(char)is.read();
                } else if ((quote==' ') && ((c=='/') || (c=='>') || (isWhiteSpace(c)))) {
                    ended=true;
                } else if (c=='&') {
                    if (charEntity!=null) {
                        curValue+="&"+charEntity; // Wasn't a char entit, probably a url as a parameter : i.e. param="/test?p=val&pw=val2&p3=val3
                    }
                    charEntity="";
                } else {
                    if (charEntity!=null) {
                        if (c==';') {
                            curValue+=convertCharEntity(charEntity,callback);
                            charEntity=null;
                        } else {
                            charEntity+=c;
                        }
                    } else {
                        curValue+=c;
                    }
                }
            }

            if (charEntity!=null) { // Mistaken something else for a char entity - for example an action which is action="http://domain/test.html?param1=val1&param2=val2"
                curValue+="&"+charEntity;
                charEntity=null;
            }

            curAttribute=curAttribute.toLowerCase();
            int error=element.addAttribute(curAttribute, curValue);

            if (error==HTMLCallback.ERROR_ATTRIBUTE_NOT_SUPPORTED) {
                notifyError(callback, error, tagName, curAttribute, curValue, "Attribute '"+curAttribute+"' is not supported for tag '"+tagName+"'. Supported attributes: "+element.getSupportedAttributesList());
            } else if (error==HTMLCallback.ERROR_ATTIBUTE_VALUE_INVALID) {
                notifyError(callback, error, tagName, curAttribute, curValue, "Attribute '"+curAttribute+"' in tag '"+tagName+"' has an invalid value ("+curValue+")");
            }

             //read and ignore any whitespaces after attribute/value pair
            while (isWhiteSpace(c)) {
                c=(char)is.read();
            }

            if (c=='>') { //tag declartion ended, process content
                if (!isEmptyTag(tagName)) {
                    parseTagContent(element, is, htmlC);
                }
                return element;
            } else if ((c=='/') || ((procInst) && (c=='?'))) { //closed tag - no content
                c=(char)is.read();
                if (c=='>') {
                    return element;
                } else {
                    notifyError(callback, HTMLCallback.ERROR_UNEXPECTED_CHARACTER, tagName, curAttribute, curValue, "HTML malformed - no > after /");
                    //throw new IllegalArgumentException("HTML malformed - no > after / - 2, instead: "+((byte)c));
                }
            }

            curAttribute="";
            curValue="";

        }

    }

    /**
     * This utility method is used to parse comments and XML declarations in the HTML.
     * The comment/declaration is returned as an Element.
     * In the current implementation they will be ommitted from the final DOM (=the root element) as the tag name won't match supported tags.
     *
     * @param is The inputstream
     * @param endTag The endtag to look for
     * @return
     * @throws IOException
     */
    private Element parseCommentOrXMLDeclaration(InputStreamReader is,String endTag) throws IOException {
        int endTagPos=0;
        String text="";
        boolean ended=false;
        while (!ended) {
            char c=(char)is.read();
            if (c==endTag.charAt(endTagPos)) {
                endTagPos++;
                if (endTagPos==endTag.length()) {
                    ended=true;
                }
            } else {
                if (endTagPos!=0) { //add - or -- if it wasn't an end tag eventually
                    text+=endTag.substring(0, endTagPos);
                    endTagPos=0;
                }
                text+=c;
            }
        }

        String elementName=null;
        if (endTag.equals("-->")) {
            elementName="comment";
        } else if (endTag.equals(">")) {
            elementName="XML declaration";
        }

        Element comment = new Element(elementName);
        comment.addAttribute("content", text);
        return comment;
    }

    /**
     * Checks whether the specified tag is an empty tag as defined in EMPTY_TAGS
     *
     * @param tagName The tag name to check
     * @return true if that tag is defined as an empty tag, false otherwise
     */
    private boolean isEmptyTag(String tagName) {
        int i=0;
        boolean found=false;
        while ((i<EMPTY_TAGS.length) && (!found)) {
            if (tagName.equals(EMPTY_TAGS[i])) {
                found=true;
            }
            i++;
        }
        return found;

    }

    /**
     * A utility method used to notify an error to the HTMLCallback and throw an IllegalArgumentException if parsingError returned false
     *
     * @param callback The HTMLCallback
     * @param errorId The error ID, one of the ERROR_* constants in HTMLCallback
     * @param tag The tag in which the error occured (Can be null for non-tag related errors)
     * @param attribute The attribute in which the error occured (Can be null for non-attribute related errors)
     * @param value The value in which the error occured (Can be null for non-value related errors)
     * @param description A verbal description of the error
     */
    private static void notifyError(HTMLCallback callback, int errorId,String tag, String attribute,String value,String description) {
        if (callback!=null) {
            boolean cont=callback.parsingError(errorId,tag,attribute,value,description);
            if (!cont) {
                throw new IllegalArgumentException(description);
            }
        } 
    }

    // ***********
    // CSS Parsing methods from here onward
    // ***********

    /**
     * Handles a CSS comment segment
     *
     * @param r The stream reader
     * @return The next char after the comment
     * @throws IOException
     */
    private char handleCSSComment(ExtInputStreamReader r) throws IOException {
        char c= r.readCharFromReader();
        if (c=='*') {
            char lastC='\0';
            while ((c!='/') || (lastC!='*')) {
                lastC=c;
                c= r.readCharFromReader();
            }
            c= r.readCharFromReader();
            while(((byte)c) != -1 && isWhiteSpace(c)) { //skip white spaces
                c= r.readCharFromReader();
            }
        } else {
            r.unreadChar(c);
            return '/';
        }
        return c;
    }

    /**
     * Reads the next CSS token from the reader
     *
     * @param r The stream reader
     * @param readNewline true to read new lines and not break when they're found, false otherwise
     * @param ignoreCommas true to ignore commas and not break when they're found, false otherwise
     * @param ignoreColons true to ignore colons and not break when they're found, false otherwise
     * @param ignoreWhiteSpaces true to ignore white spaces and not break when they're found, false otherwise
     * @return The next CSS token
     * @throws IOException
     */
    private String nextToken(ExtInputStreamReader r, boolean readNewline,boolean ignoreCommas,boolean ignoreColons,boolean ignoreWhiteSpaces) throws IOException {
        boolean newline = false;
        StringBuffer currentToken = new StringBuffer();
        char c= r.readCharFromReader();

        // read the next token from the CSS stream
        while(((byte)c) != -1 && isWhiteSpace(c)) {
            newline = newline || (c == 10 || c == 13 || c == ';' || ((c == ',') && (!ignoreCommas)));
            if(!readNewline && newline) {
                return null;
            }
            c= r.readCharFromReader();
        }
        if (c==';' && readNewline) { //leftover from compound operation
            c= r.readCharFromReader();
            while(((byte)c) != -1 && isWhiteSpace(c)) { // This was added since after reading ; there might be some more white spaces. However there needs to be a way to combine this with the previous white spaces code or with the revised newline detection and unreading char below
                newline = newline || (c == 10 || c == 13 || c == ';' || ((c == ',') && (!ignoreCommas)));
                c= r.readCharFromReader();
            }


        }
        char segment='\0'; // segment of (...) or "..." or '...'
        while(((byte)c) != -1 && ((!isWhiteSpace(c)) || (segment != '\0') || (ignoreWhiteSpaces)) && c != ';' && ((c != ':') || (segment!='\0') || (ignoreColons))  && ((c != ',') || (segment != '\0') || (ignoreCommas)) && c != '>') { //- : denotes pseudo-classes, would like to keep them as one token

            if ((segment=='\0') && (c=='/')) { //comment start perhaps, if inside brackets - ignore
                c=handleCSSComment(r);
            }

            if ((c == '}' || c == '{' || c == '*' ) && (segment=='\0')) { //enter only if not in the middle of a segment. i.e. '*N'
                newline = true;
                if(currentToken.length() == 0) {
                    if(!readNewline) {
                        r.unreadChar(c);
                        return null;
                    }
                    return "" + c;
                }
                r.unreadChar(c);
                break;
            }
            currentToken.append(c);

            if (c=='(') {
                segment=')';
            } else if ((segment=='\0') && ((c=='\"') || (c=='\''))) { //TODO - This keeps track of one segment only, while in fact there can be "nested" segments - i.e. ("...") which is common in URLs, though not sure it is critical as such pattern works correctly even now // || (c=='`') ?
                segment=c;
            } else if (c==segment) {
                segment='\0';
            }
            c= r.readCharFromReader();
        }
        if ((c==',') && (!ignoreCommas)) {
            currentToken.append(c);
        }

        if((!readNewline) && (c==';')  && (currentToken.length() != 0) ) {
            r.unreadChar(c);
        }

        if(currentToken.length() == 0) {
            return null;
        }
        return currentToken.toString();
    }

    /**
     * Copies all attributes from
     *
     * @param element The element to copy from
     * @param selectors A vector containing grouped selectors to copy the attributes to
     * @param addTo The main element to add the grouped selectors to
     */
    private void copyAttributes(CSSElement element,Vector selectors,Element addTo) {
        if (selectors==null) {
            return;
        }
        for(Enumeration e=selectors.elements();e.hasMoreElements();) {
            CSSElement selector=(CSSElement)e.nextElement();
            addTo.addChild(selector);
            while (selector.getNumChildren()>0) { // This makes sure we get the last nested selector
                selector=selector.getCSSChildAt(0);
            }
            element.copyAttributesTo(selector);
        }
    }

    /**
     * Returns true if the specified CSS media type is unsupported, false otherwise
     *
     * @param media A string identifying the media type (i.e. "handheld")
     * @return true if the specified CSS media type is uspported, false otherwise
     */
    private boolean isMediaTypeSupported(String media) {
        for(int i=0;i<SUPPORTED_MEDIA_TYPES.length;i++) {
            if (media.equalsIgnoreCase(SUPPORTED_MEDIA_TYPES[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if an at-media rule applies to the supported media types
     *
     * @param mediaTypes A string containing all media types the at-media rule allows
     * @return true if one of the supported media types is denoted, false otherwise
     */
    boolean mediaTypeMatches(String mediaTypes) {
        if ((mediaTypes==null) || (mediaTypes.equals(""))) {
            return true;
        }
        int comma=mediaTypes.indexOf(',');
        while (comma!=-1) {
            if (isMediaTypeSupported(mediaTypes.substring(0,comma).trim())) {
                return true;
            }
            mediaTypes=mediaTypes.substring(comma+1);
            comma=mediaTypes.indexOf(',');
        }
        return isMediaTypeSupported(mediaTypes.trim());
    }

    /**
     * Returns the import URL if the specified media matches, or null otherwise
     *
     * @param token The string including the url and media of the import at-rule (example: url("mycss.css") handheld,tv;
     * @return the import URL if the specified media matches, or null otherwise
     */
    private String getImportURLByMediaType(String token) {
        String url=token;
        boolean mediaMatches=true;
        int space=token.indexOf(' ');
        if (space!=-1) {
            url=token.substring(0, space);
            token=token.substring(space+1);
            mediaMatches=mediaTypeMatches(token);
        }
        if (mediaMatches) {
            if (url.startsWith("url(")) {
                url=CSSEngine.getCSSUrl(url);
            }
            return url;
        } else {
            return null;
        }

    }

    /**
     * Handles a media at-rule segment.
     * This method checks if the media type specified in the media at-rule is supported, if it does
     * it returns only the media segment as a separate stream, otherwsie it returns null
     * 
     * @param isr The stream representing the CSS
     * @return An input stream with the relevant media segment or null if the media is not supported
     * @throws IOException on input stream failure
     */
    private ExtInputStreamReader getMediaSegment(ExtInputStreamReader r) throws IOException {
        String token = nextToken(r,true,true,true,true);
        char c= r.readCharFromReader();

        while ((((byte)c) != -1) && (c!='{')) { // Find the first { that marks the start of the media segment
            c= r.readCharFromReader();
        }

        StringBuffer segment=new StringBuffer();
        boolean match=mediaTypeMatches(token);

        int count=1; // counts the number of opened curly brackets
        while (count>0) {
            c= r.readCharFromReader();
            if ((((byte)c)==-1)) {
                break; //end of file
            }
            if (match) {
                segment.append(c);
            }
            if (c=='{') {
                count++;
            } else if (c=='}') {
                count--;
            }
        }

        if (match) {
            return new ExtInputStreamReader(new InputStreamReader(new ByteArrayInputStream(segment.toString().getBytes())));
        } else {
            return null;
        }

    }

    /**
     * Reads a CSS file/stream and returns the tokenized CSS as a single level element tree with the
     * root appearing as a "style".
     * This method is called upon finding linked/external CSS and embedded CSS segments.
     * It handles at-rules such as import/charset/media and forwards relevant segments to the parseCSS method
     *
     * @param isr The InputStreamReader representing the stream
     * @param is The InputStream representing the stream (We need it too, in case encoding changes and we need to create another InputStreamReader)
     * @param htmlC The HTMLComponent
     * @param  pageURL For external CSS the URL of the CSS, for embedded - null
     * @return A CSSElement containing all selectors found in the stream as its children
     * @throws IOException on input stream failure
     */
    CSSElement parseCSSSegment(InputStreamReader isr,InputStream is,HTMLComponent htmlC,String pageURL) throws IOException {
        CSSElement addTo = new CSSElement("style");
        ExtInputStreamReader r = new ExtInputStreamReader(isr);
        DocumentInfo docInfo=null;
        String encoding=htmlC.getDocumentInfo()!=null?htmlC.getDocumentInfo().getEncoding():null;
        String token = nextToken(r,true,false,true,false);
        while(token.startsWith("@")) {
            if (token.equals("@import")) {
                token = nextToken(r,true,true,true,true);
                String url=getImportURLByMediaType(token);
                if (url!=null) {
                    if (docInfo==null) {
                        docInfo=pageURL==null?htmlC.getDocumentInfo():new DocumentInfo(pageURL);
                    }
                    if (docInfo!=null) {
                        htmlC.getThreadQueue().addCSS(docInfo.convertURL(url),encoding); // Referred CSS "inherit" charset from the referring document
                    } else {
                        if (DocumentInfo.isAbsoluteURL(url)) {
                            htmlC.getThreadQueue().addCSS(url,encoding); // Referred CSS "inherit" charset from the referring document
                        } else {
                            notifyError(htmlC.getHTMLCallback(), HTMLCallback.ERROR_NO_BASE_URL, "@import", null, url, "Ignoring CSS file referred in an @import rule ("+url+"), since page was set by setBody/setHTML so there's no way to access relative URLs");
                        }
                    }
                }
            } else if (token.equals("@media")) {
                ExtInputStreamReader mediaReader = getMediaSegment(r); // TODO send is and encoding if any
                if (mediaReader!=null) {
                    parseCSS(mediaReader, htmlC, addTo,null);
                }
            } else if (token.equals("@charset")) {
                token = CSSEngine.omitQuotesIfExist(nextToken(r,true,false,true,false));
                if (is!=null) { // @charset applies only to external style sheet, and the inputstream is null for embedded CSS segments
                    try {
                        ExtInputStreamReader encodedReader=new ExtInputStreamReader(new InputStreamReader(is, token));
                        r=encodedReader;
                        encoding=token;
                    } catch (UnsupportedEncodingException uee) {
                        notifyError(htmlC.getHTMLCallback(), HTMLCallback.ERROR_ENCODING, "@charset", null, token, "External CSS encoding @charset "+token+" directive failed: "+uee.getMessage());
                    }
                }
            }
            token = nextToken(r,true,false,true,false);
        }

        return parseCSS(r, htmlC,  addTo,token);
    }

    /**
     * Reads a CSS file/stream and returns the tokenized CSS as a single level element tree with the
     * root appearing as a "style".
     * This method is called either directly on style attributes.
     *
     * @param r The stream reader containing the CSS segment
     * @param htmlC The HTMLComponent
     * @return A CSSElement containing all selectors found in the stream as its children
     * @throws IOException on input stream failure
     */
    CSSElement parseCSS(InputStreamReader r,HTMLComponent htmlC) throws IOException {
        ExtInputStreamReader er=new ExtInputStreamReader(r);
        return parseCSS(er, htmlC, null,null);
    }

    /**
     * Reads a CSS file/stream and returns the tokenized CSS as a single level element tree with the
     * root appearing as a "style".
     *
     * @param r The stream reader containing the CSS segment
     * @param htmlC The HTMLComponent
     * @param addTo the master CSSElement to add the selectors to (or null to open a new one_
     * @param firstToken A first toekn to process, or null if none
     * @return A CSSElement containing all selectors found in the stream as its children
     * @throws IOException on input stream failure
     */
    CSSElement parseCSS(ExtInputStreamReader r,HTMLComponent htmlC,CSSElement addTo,String firstToken) throws IOException {
        //CSSElement addTo = new CSSElement("style");
        if (addTo==null) {
            addTo = new CSSElement("style");
        }
        CSSElement parent = addTo;
        Vector selectors = new Vector();
        CSSElement lastGroupedParent=null;

        boolean selectorMode = true;
        boolean grouping=false; // Grouping is when selector are grouped, i.e. h1,h2,h3 { ... }
        String token = "";

        //TODO - detect BOM for UTF8 etc.
        while(true) {

            if (firstToken!=null) {
                token=firstToken;
                firstToken=null;
            } else {
                token = nextToken(r,true,false,selectorMode,false);
            }

            if(token == null || token.indexOf("</style") > -1) {
                break;
            }

            if("{".equals(token)) {
                selectorMode = false;
                grouping=false;
                continue;
            }
            if("}".equals(token)) {
                selectorMode = true;
                copyAttributes(parent, selectors,addTo);
                parent = addTo;
                selectors = new Vector();
                lastGroupedParent=null;
                continue;
            }

            // Checks for grouped selectors, note that due to spacing the comma can either appear as a separate token, or at the start of a token or at its end
            // All these scenarios are checked in the following lines of code.
            if ((",".equals(token)) && (selectorMode)) {
                grouping=true;
                continue;
            }

            if(selectorMode) {
                if (token.startsWith(",")) {
                    token=token.substring(1);
                    grouping=true;
                }
                if (grouping) {
                    if (token.endsWith(",")) {
                        token=token.substring(0, token.length()-1);
                    } else {
                        grouping=false; //  there was no comma at the end, so next time it is not a grouped element (unless a comma will be detected as the next token or the start of the next token)
                    }
                    CSSElement entry = new CSSElement(token);
                    selectors.addElement(entry);
                    lastGroupedParent=entry;

                } else {
                    if (token.endsWith(",")) {
                        grouping=true;
                        token=token.substring(0, token.length()-1);
                    }
                    CSSElement entry = new CSSElement(token);
                    if (lastGroupedParent==null) {
                        parent.addChild(entry);
                        parent = entry;
                    } else {
                        lastGroupedParent.addChild(entry);
                        lastGroupedParent=entry;
                    }
                }
            } else {
                boolean compoundToken = false;

                for(int iter = 0 ; iter < CSSElement.CSS_SHORTHAND_ATTRIBUTE_LIST.length ; iter++) {
                    if(CSSElement.CSS_SHORTHAND_ATTRIBUTE_LIST[iter].equals(token)) {
                            compoundToken = true;
                            boolean collattable=CSSElement.CSS_IS_SHORTHAND_ATTRIBUTE_COLLATABLE[iter];
                            int valsAdded=0;
                            token = nextToken(r, false,false,false,false);

                            // This array is used for collatable attributes - the values can't be set as they are read, first we need to see how many values appear and set accordingly
                            String[] tokens = new String[4];
                            while(token!=null) {
                                if (collattable) {
                                    if (valsAdded<tokens.length) {
                                        tokens[valsAdded]=token;
                                        valsAdded++;
                                    }
                                } else {
                                    addShorthandAttribute(token, iter, parent);
                                }

                                token = nextToken(r, false,false,false,false);
                            }

                            // The following assigns the collatable attributes according to CSSElement.CSS_COLLATABLE_ORDER
                            if  ((collattable) && (valsAdded>0)) {
                                for(int i=0;i<CSSElement.CSS_COLLATABLE_ORDER[valsAdded-1].length;i++) {
                                    for(int j=0;j<CSSElement.CSS_COLLATABLE_ORDER[valsAdded-1][i].length;j++) {
                                        int side=CSSElement.CSS_COLLATABLE_ORDER[valsAdded-1][i][j];
                                        addAttributeTo(parent, CSSElement.CSS_SHORTHAND_ATTRIBUTE_INDEX[iter][side], tokens[i], htmlC);
                                    }
                                }
                            }
                            break;
                    }
                }

                // if this is a "regular" css attribute is it one of the supported attributes
                if(!compoundToken) {
                    // We ignore commas when collecting a value, since it can be for example: font-family:arial,tahoma,sans-serif etc.
                    // We also ignore spaces in font-family / access key since the value can be: arial, tahoma / send * , #
                    int result=addAttributeTo(parent, token, nextToken(r,false,true,false,(token.equalsIgnoreCase("-wap-access-key") || (token.equalsIgnoreCase("font-family")))), htmlC);
                    if(result!=-1) {
                        // unsupported token we need to read until the newline
                        //while(nextToken(r, false, false,false) != null && !newline) {} //TODO - what if that happens in the end of the file - do we get into an infinite loop?
                        while(nextToken(r, false, false,false,false) != null) {} //TODO - is newline truly unnecessary ? + what if that happens in the end of the file - do we get into an infinite loop?
                    }
                }
            }
        }
        return addTo;
    }

    /**
     * Adds the specified value to the specified selector as a value to the shorthand attribute whose index is specified
     * This methods deals with the complexity of adding values for shorthand attributes, since they can be specified in any order
     * It also handles multiple-shorthand levels such as the 'border' attribute
     *
     * @param value The attribute's value
     * @param shorthandAttr The attribute's index
     * @param selector The selector to add the attribuet to
     * @return true if succeeded to add, false otherwise (for example invalid value)
     */
    private boolean addShorthandAttribute(String value,int shorthandAttr,CSSElement selector) {
        if (CSSElement.CSS_IS_SHORTHAND_ATTRIBUTE_COLLATABLE[shorthandAttr]) {
            return addCollatableAttribute(value, shorthandAttr, selector);
        }
        for(int i=0;i<CSSElement.CSS_SHORTHAND_ATTRIBUTE_INDEX[shorthandAttr].length;i++) {
            int attrIndex=CSSElement.CSS_SHORTHAND_ATTRIBUTE_INDEX[shorthandAttr][i];

            if (attrIndex>=CSSElement.CSS_STYLE_ID_OFFSET) {

                if (!selector.isAttributeAssigned(attrIndex)) { // Only check if the attribute wasn't set yet
                    int result=selector.addAttribute(attrIndex, value);
                    if (result==-1) { //no error code return - success
                        return true;
                    }
                }
            } else {
                boolean success=addShorthandAttribute(value, attrIndex, selector);
                if (success) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Adds the specified value to the specified selector as a value to the shorthand and collatable attribute whose index is specified
     * This is called from addShorthandAttribute when a shorthand attribute maps to a collatable attribute
     * Note that while usually collatable attributes can have 1-4 values, and are mapped according to CSSElement.CSS_COLLATABLE_ORDER
     * When they are specified as part of a top shorthand attribute, only one value can be specified and it is copied to all base attributes.
     * For example, While the definition 'border-width: 5px 10px' will set the vertical border width to 5 and the horizontal to 10,
     * One cannot specify: 'border: 5px 10px solid red' - but rather has to specify only one value that will be set as the width for all sides.
     *
     * @param value The attribute's value
     * @param shorthandAttr The attribute's index
     * @param selector The selector to add the attribuet to
     * @return true if succeeded to add, false otherwise (for example invalid value)
     */
    private boolean addCollatableAttribute(String value,int shorthandAttr,CSSElement selector) {
        int attrIndex=CSSElement.CSS_SHORTHAND_ATTRIBUTE_INDEX[shorthandAttr][0];
        int result=selector.addAttribute(attrIndex, value);
        if (result==-1) {
            for(int i=1;i<CSSElement.CSS_SHORTHAND_ATTRIBUTE_INDEX[shorthandAttr].length;i++) {
                attrIndex=CSSElement.CSS_SHORTHAND_ATTRIBUTE_INDEX[shorthandAttr][i];
                selector.addAttribute(attrIndex, value);
            }
            return true;
        }
        return false;
    }

    /**
     * Adds the specified attribute and value pair to the specified selector
     *
     * @param selector The selector we're working on
     * @param attrId The attribute's id
     * @param value The attribute value
     * @param htmlC The HTMLComponent (To obtain the HTMLCallback)
     * @return a positive value if an error occured, or -1 otherwise
     */
    private int addAttributeTo(CSSElement selector,int attrId,String value,HTMLComponent htmlC) {
            int error=selector.addAttribute(attrId, value);
            reportAddAttributeError(error,selector, selector.getAttributeName(new Integer(attrId)), value, htmlC);
            return error;
    }

    /**
     * Adds the specified attribute and value pair to the specified selector
     *
     * @param selector The selector we're working on
     * @param attributeName The attribute's name
     * @param value The attribute value
     * @param htmlC The HTMLComponent (To obtain the HTMLCallback)
     * @return a positive value if an error occured, or -1 otherwise
     */
    private int addAttributeTo(CSSElement selector,String attributeName,String value,HTMLComponent htmlC) {
            int error=selector.addAttribute(attributeName, value);
            reportAddAttributeError(error,selector, attributeName, value, htmlC);
            return error;
    }


    /**
     * A helper method that handles reporting of CSS errors to the HTMLCallback (if available)
     *
     * @param errorCode The error code as returned by the CSSElement.addAttribute methods
     * @param selector The selector we're working on
     * @param attributeName The attribute's name
     * @param value The attribute value
     * @param htmlC The HTMLComponent (To obtain the HTMLCallback)
     */
    private void reportAddAttributeError(int errorCode,CSSElement selector,String attributeName,String value,HTMLComponent htmlC) {
        if (errorCode!=-1) {
            if (errorCode==HTMLCallback.ERROR_CSS_ATTRIBUTE_NOT_SUPPORTED) {
                notifyError(htmlC.getHTMLCallback(), errorCode, selector.getName(), attributeName, value, "CSS Attribute '"+attributeName+"' (Appeared in selector '"+selector.getName()+"') is not supported in WCSS.");
            } else if (errorCode==HTMLCallback.ERROR_CSS_ATTIBUTE_VALUE_INVALID) {
                notifyError(htmlC.getHTMLCallback(), errorCode, selector.getName(), attributeName, value, "CSS Attribute '"+attributeName+"' (Appeared in selector '"+selector.getName()+"') has an invalid value ("+value+")");
            }
        }
    }

}

/**
 *  A decorator for InputStreamReader that adds teh ability to "unread" a character
 *  This makes parsing easier, and is used for CSS parsing.
 * 
 * @author Ofir Leitner
 */
class ExtInputStreamReader  {

    char lastCharRead = (char)-1;
    InputStreamReader internalReader;

    ExtInputStreamReader(InputStreamReader isr) {
        internalReader=isr;
    }

    /**
     * "Unreads" a character from the stream by placing it in a member variable to be later retreived by readCharFromReader, used by the CSS Parser
     *
     * @param c The character to unread
     */
    void unreadChar(char c) {
        lastCharRead = c;
    }

    /**
     * Reads the next character from the input stream, used by the CSS Parser
     * If there's an "unread" character in teh buffer it is returned (and no reading is done to the actual stream)
     *
     * @param r The stream reader
     * @return the next character
     * @throws IOException
     */
    char readCharFromReader() throws IOException {
        if(lastCharRead != (char)-1) {
            char c = lastCharRead;
            lastCharRead = (char)-1;
            return c;
        }
        return (char)internalReader.read();
    }



}