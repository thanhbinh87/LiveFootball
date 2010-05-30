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
import com.sun.lwuit.Graphics;
import com.sun.lwuit.geom.Rectangle;
import com.sun.lwuit.plaf.Border;
import com.sun.lwuit.plaf.Style;
import com.sun.lwuit.plaf.UIManager;

/**
 * Allows CSS borders that can be composed of different borders for each side (TOP/LEFT/RIGHT/BOTTOM)
 *
 * @author Ofir Leitner
 */
class CSSBorder extends Border {

    Border[] borders = new Border[4]; // An array with the border per side ((TOP/LEFT/RIGHT/BOTTOM)
    Component cmp; // The component this border belongs to

    /**
     * Creates a CSSBorder using the specified borders array for the specified component
     * 
     * @param borders An array with the borders per side ((TOP/LEFT/RIGHT/BOTTOM)
     * @param cmp The component to apply the border to
     * @return A CSSBorder representing the borders specified or null if borders contain only null values.
     */
    static CSSBorder createCSSBorder(Border[] borders,Component cmp) {
        boolean allnull=true;
        for(int i=0;i<borders.length;i++) {
            if (borders[i]!=null) {
                allnull=false;
                break;
            }
        }
        if (allnull) {
            return null;
        } else {
            return new CSSBorder(borders,cmp);
        }
    }

    /**
     * Merges this CSSBorder object with the border array that represents 4 borders each per side.
     * The newBorders array can contain null elements - only actual borders will override this CSSBorder values
     * 
     * @param newBorders An array with the borders per side ((TOP/LEFT/RIGHT/BOTTOM)
     */
    void mergeBorder(Border[] newBorders) {
        for(int i=0;i<newBorders.length;i++) {
            if (newBorders[i]!=null) {
                borders[i]=newBorders[i];
            }
        }
    }

    CSSBorder(Border[] borders,Component ui) {
        this.borders=borders;
        this.cmp=ui;
    }


    /**
     * {@inheritDoc}
     */
    public void paint(Graphics g, Component c) {
        //int originalColor = g.getColor();
        int x = c.getX();
        int y = c.getY();
        int width = c.getWidth();
        int height = c.getHeight();
        Style style=c.getStyle();

        boolean drawLeft=true;
        boolean drawRight=true;

        if (c instanceof HTMLLink) {
            HTMLLink link=((HTMLLink)c);
            HTMLLink parentLink=link.parentLink;
            if (parentLink==null) {
                if (link.childLinks!=null) {
                    drawRight=false;
                }
            } else {
                drawLeft=false;
                if (parentLink.childLinks.lastElement()!=link) {
                    drawRight=false;
                }
            }
        }
        if (UIManager.getInstance().getLookAndFeel().isRTL()) {
           boolean temp=drawLeft;
           drawLeft=drawRight;
           drawRight=temp;
        }

        if (borders[Component.TOP]!=null) {
            Rectangle clip=saveClip(g);
            g.clipRect(x, y, width, style.getPadding(Component.TOP));
            borders[Component.TOP].paint(g, c);
            restoreClip(g, clip);
        }

        if (borders[Component.BOTTOM]!=null) {
            Rectangle clip=saveClip(g);
            g.clipRect(x, y+height-style.getPadding(Component.BOTTOM), width, style.getPadding(Component.BOTTOM));
            borders[Component.BOTTOM].paint(g, c);
            restoreClip(g, clip);
        }

        if ((drawLeft) && (borders[Component.LEFT]!=null))  {
            Rectangle clip=saveClip(g);
            g.clipRect(x, y+style.getPadding(Component.TOP), style.getPadding(Component.LEFT),height-style.getPadding(Component.TOP)-style.getPadding(Component.BOTTOM));
            borders[Component.LEFT].paint(g, c);
            restoreClip(g, clip);
        }

        if ((drawRight) && (borders[Component.RIGHT]!=null)) {
            Rectangle clip=saveClip(g);
            g.clipRect(x+width-style.getPadding(Component.RIGHT), y+style.getPadding(Component.TOP), style.getPadding(Component.RIGHT),height-style.getPadding(Component.TOP)-style.getPadding(Component.BOTTOM));
            borders[Component.RIGHT].paint(g, c);
            restoreClip(g, clip);
        }

    }

    /**
     * Utility method used to save the current clip area
     * 
     * @param g The grpahics to obtain the clip area from
     * @return A Rectangle object representing the current clip area
     */
    private Rectangle saveClip(Graphics g) {
        return new Rectangle(g.getClipX(), g.getClipY(), g.getClipWidth(), g.getClipHeight());
    }

    /**
     * Utility method used to restore a previously saved clip area
     * 
     * @param g The graphics to apply the clip area on
     * @param rect A Rectangle representing the saved clip area
     */
    private void restoreClip(Graphics g,Rectangle rect) {
        g.setClip(rect.getX(), rect.getY(), rect.getSize().getWidth(), rect.getSize().getHeight());
    }

    /**
     * {@inheritDoc}
     */
    public void paintBorderBackground(Graphics g, Component c) {
        // do nothing - CSS borders do not include round and image borders
    }

}
