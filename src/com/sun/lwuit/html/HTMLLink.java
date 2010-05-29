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

import com.sun.lwuit.Button;
import com.sun.lwuit.events.ActionEvent;
import com.sun.lwuit.events.ActionListener;
import java.util.Enumeration;
import java.util.Vector;

/**
 * HTMLLink is a simple extension of Button that nullifies all its borders and attaches itself as an ActionListener to respond to link clicks.<br>
 * Since a link can be split on several lines, the concept of parent and child links is introduced here. A parent link is the first segment of the link
 * while the children are all the other segments. Only the parent is focusable (so multiple focuses on the same links will be avoided).
 * When the parent is focused, all the children get setFocus(true). Note that for pointer events, the children are also active.
 * 
 * @author Ofir Leitner
 */
class HTMLLink extends Button implements ActionListener {

    String link;
    HTMLComponent htmlC;
    Vector childLinks;
    HTMLLink parentLink;
    boolean linkVisited;
    boolean parentChangesOnFocus;

    /**
     * Constructs the HTMLLink
     * 
     * @param text The link's text
     * @param link The link URL
     * @param htmlC The HTMLComponent this link is in
     * @param parentLink THis link's parent if available
     */
    HTMLLink(String text,String link,HTMLComponent htmlC,HTMLLink parentLink,boolean linkVisited) {
        super(text);
        setUIID("HTMLLink");
        this.link=link;
        this.htmlC=htmlC;
        this.parentLink=parentLink;
        this.linkVisited=linkVisited;

        setTickerEnabled(false);
        addActionListener(this);
        getPressedStyle().setFont(getUnselectedStyle().getFont()); // Temporary patch

        if (parentLink!=null) {
            setFocusable(false);
            parentLink.addChildLink(this);
        }

        if (htmlC.firstFocusable==null) {
            htmlC.firstFocusable=this;
        }

    }

    /**
     * {@inheritDoc}
     */
    protected void focusGained() {
        setChildrenFocused(true);
    }


    /**
     * {@inheritDoc}
     */
    protected void focusLost() {
        setChildrenFocused(false);
    }

    /**
     * Applies or removes the focus style to all this link children (if any)
     *
     * @param focused true to apply focused style, false to remove
     */
    private void setChildrenFocused(boolean focused) {
        if (parentLink!=null) {
            return;
        }
        if (childLinks!=null) {
            for (Enumeration e=childLinks.elements();e.hasMoreElements();) {
                HTMLLink child=(HTMLLink)e.nextElement();
                child.setFocus(focused);
                child.repaint();
            }
        }
        
        // Some CSS attributes, such as margin/padding are applies on the parent container of links and not on them
        // This is because setting a margin per word would not get the desired result.
        // When setting focus to the parent (which is an unfocusable container) it will get the desired properties
        if (parentChangesOnFocus) {
            getParent().setFocus(focused);
            getParent().revalidate();
        }
    }



    /**
     * If this is called it indicates that when the link is focused, its parent should change to focused as well
     */
    void setParentChangesOnFocus() {
        if (parentLink==null) {
            parentChangesOnFocus=true;
        }
    }

    /**
     * Adds the given link as the child of this link
     * 
     * @param childLink The child link to add
     */
    void addChildLink(HTMLLink childLink) {
        if (childLinks==null) {
            childLinks=new Vector();
        }
        childLinks.addElement(childLink);
    }

    /**
     * Triggered when the link is pressed and then it requests the link (or goes to an anchor within the page)
     */
    public void actionPerformed(ActionEvent evt) {
        boolean process=true;
        if (htmlC.getHTMLCallback()!=null) {
            process=htmlC.getHTMLCallback().linkClicked(htmlC, htmlC.convertURL(link));
        }
        if (process){
            if (!link.startsWith("#")) {
                htmlC.setPage(htmlC.convertURL(link));
            } else { //local anchor
                String anchorName=link.substring(1);
                htmlC.goToAnchor(anchorName);
            }
        }
        
    }
}

