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

import com.sun.lwuit.TextArea;

/**
 * HTMLCallback is used to dispatch document lifecycle events.
 * Most methods are called on the EDT thread, except parsingError, getAutoComplete and getLinkProperties
 *
 * @author Ofir Leitner
 */
public interface HTMLCallback {


    //////////////////////////////////
    // Error constants              //
    //////////////////////////////////

    /**
     * Error code denoting that an unsupported tag (by XHTML-MP 1.0 standards) was found in the HTML
     */
    public static int ERROR_TAG_NOT_SUPPORTED = 0;

    /**
     * Error code denoting that an unsupported attribute (by XHTML-MP 1.0 standards) was found in the HTML
     */
    public static int ERROR_ATTRIBUTE_NOT_SUPPORTED = 1;

    /**
     * Error code denoting that an  invalid attribute value was found in the HTML
     */
    public static int ERROR_ATTIBUTE_VALUE_INVALID = 2;

    /**
     * Error code denoting that a tag was not closed properly in the HTML
     */
    public static int ERROR_NO_CLOSE_TAG = 3;

    /**
     * Error code denoting that an  invalid character entity was found
     * A character entity is HTML codes that start with an ampersand and end with semicolon and denote special/reserved chars
     * Char entities can be added by using HTMLComponent.addCharEntity
     */
    public static int ERROR_UNRECOGNIZED_CHAR_ENTITY = 4;

    /**
     * Error code denoting that a tag was not closed  prematurely
     */
    public static int ERROR_UNEXPECTED_TAG_CLOSING = 5;

    /**
     * Error code denoting that the parser bumped into an unexpected character
     */
    public static int ERROR_UNEXPECTED_CHARACTER = 6;

    /**
     * Error code denoting that an image referenced from the HTML was not found
     */
    public static int ERROR_IMAGE_NOT_FOUND = 7;

    /**
     * Error code denoting that an image referenced from the HTML could not be loaded
     */
    public static int ERROR_IMAGE_BAD_FORMAT = 8;

      /**
     * Error code denoting that the encoding the page needed according to its charset (usually specified in the content-type response header) is unsupported in the device
     */
    public static int ERROR_ENCODING = 9;

    /**
     * Error code denoting that a connection to the resource provider (i.e. server) could not be made
     */
    public static int ERROR_CONNECTING = 10;

    /**
     * Error code denoting that an unsupported CSS attribute (by XHTML-MP 1.0 standards) was found in the HTML or external CSS files
     */
    public static int ERROR_CSS_ATTRIBUTE_NOT_SUPPORTED = 11;

    /**
     * Error code denoting that an invalid attribute value was found in the CSS
     */
    public static int ERROR_CSS_ATTIBUTE_VALUE_INVALID = 12;

    /**
     * Error code denoting that a CSS file referenced from the HTML or from another external CSS file was not found
     */
    public static int ERROR_CSS_NOT_FOUND = 13;

    /**
     * Error code denoting that a relative URL was referenced from a document with no base URL (A document that was loaded via setBody/setHTML and not via setPage)
     * In this case the return value of parsingError is not considered - parsing continues and the resource at the URL (CSS file/image) is ignored
     */
    public static int ERROR_NO_BASE_URL = 14;


    //////////////////////////////////
    // Page status constants        //
    //////////////////////////////////

    /**
     * This is returned in the page status if no page has been set to teh HTMLComponent
     */
    public static int STATUS_NONE = -3;

    /**
     * The page couldn't load completely because of parsing errors
     */
    public static int STATUS_ERROR = -2;

    /**
     * The page loading was cancelled before it could be completed
     */
    public static int STATUS_CANCELLED = -1;
    
    /**
     * The page was requested from the request handler
     */
    public static int STATUS_REQUESTED = 0;

    /**
     * The stream was received
     */
    public static int STATUS_CONNECTED = 1;

    /**
     * The page was parsed
     */
    public static int STATUS_PARSED = 2;

    /**
     *  The page was displayed on screen - but at this stage some images and CSS files may still be loading in the background
     */
    public static int STATUS_DISPLAYED = 3;

    /**
     * The page and all of its referenced images and CSS files were loaded completely
     */
    public static int STATUS_COMPLETED = 4;

    /**
     * The page was redirected to another URL
     */
    public static int STATUS_REDIRECTED = 5;



    //////////////////////////////////
    // Field type constants         //
    //////////////////////////////////

    /**
     * A text field
     */
    public static int FIELD_TEXT = 0;

    /**
     * A password field
     */
    public static int FIELD_PASSWORD = 1;


    //////////////////////////////////
    // Link property constants      //
    //////////////////////////////////

    /**
     * A regular link
     */
    public static int LINK_REGULAR = 0;

    /**
     * A link that was visited before
     */
    public static int LINK_VISTED = 1;

    /**
     * A forbidden link (not to be rendered as a link but as a regular label)
     */
    public static int LINK_FORBIDDEN = 2;


    //////////////////////////////////
    // Interface methods            //
    //////////////////////////////////

    /**
     * Called when the page's title is updated
     * 
     * @param htmlC The HTMLComponent that triggered the event
     * @param title The new title
     */
    public void titleUpdated(HTMLComponent htmlC, String title);

    /**
     *  Called when encountering an error while parsing the HTML document.
     *  When implementing this, the developer should return true if the error should be ignored and the document needs to be further parsed, or false to stop parsing and issue an error to the user
     *  Note that this method is always called NOT on the EDT thread.
     * 
     * @param errorId The error ID, one of the ERROR_* constants
     * @param tag The tag in which the error occured (Can be null for non-tag related errors)
     * @param attribute The attribute in which the error occured (Can be null for non-attribute related errors)
     * @param value The value in which the error occured (Can be null for non-value related errors)
     * @param description A verbal description of the error
     * @return true to continue parsing, false to stop
     */
    public boolean parsingError(int errorId,String tag,String attribute,String value,String description);

    /**
     *  Called when the page status has been changed
     * 
     * @param htmlC The HTMLComponent in which the status change occured
     * @param status The new status, one of the STATUS_* constants
     * @param url The URL of the page
     */
    public void pageStatusChanged(HTMLComponent htmlC, int status,String url);
    
    /**
     * Called whenever a field is submitted to a form. 
     * This can be used to perform sanity checks and/or to store values for auto complete.
     * 
     * @param htmlC The HTMLComponent in which this event occured
     * @param ta The TextArea/TextField of this field
     * @param actionURL The action URL of the form
     * @param id The ID of the field
     * @param value The value entered
     * @param type The type of the field, one of the FIELD_* constants
     * @param errorMsg The error message if any error occured (i.e. input validation error) or null if no error occured
     * @return The string to submit to the form (Should return value if nothing changed)
     */
    public String fieldSubmitted(HTMLComponent htmlC,TextArea ta,String actionURL,String id,String value,int type,String errorMsg);

    /**
     * Called on form creation and enabled implementations of this method to return a value to preset in a form field.
     * This can be used to auto complete previously entered  value
     * Note that this method is always called NOT on the EDT thread.
     *
     * @param htmlC The HTMLComponent in which this event occured
     * @param actionURL The action URL of the form
     * @param id The ID of the field
     * @return The string to place in the indicated field
     */
    public String getAutoComplete(HTMLComponent htmlC,String actionURL,String id);

    /**
     * Returns properties about the given link to indicate to HTMLComponent how to render it
     * Note that this method is always called NOT on the EDT thread.
     *
     * @param htmlC The HTMLComponent
     * @param url The Link URL
     * @return LINK_REGULAR or LINK_VISITED or LINK_FORBIDDEN or a mask of those
     */
    public int getLinkProperties(HTMLComponent htmlC, String url);

    /**
     * Called when a link is clicked. This can be used to process links that needs additional/alternate handling than fetching an HTML.
     *
     * @param htmlC The HTMLComponent
     * @param url The Link URL
     * @return true if regular link processing should continue, false otherwise
     */
    public boolean linkClicked(HTMLComponent htmlC, String url);

}
