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
package com.sun.lwuit.layouts;

import com.sun.lwuit.Component;
import com.sun.lwuit.Container;
import com.sun.lwuit.geom.Dimension;
import com.sun.lwuit.plaf.Style;
import com.sun.lwuit.plaf.UIManager;

/**
 * Flows elements in a row so they can spill over when reaching line end
 *
 * @author Nir Shabi
 */
public class FlowLayout extends Layout{
    

    private int orientation = Component.LEFT;
    
    
    /** 
     * Creates a new instance of FlowLayout with left alignment
     */
    public FlowLayout() {
    }
      
    /** 
     * Creates a new instance of FlowLayout with the given orientation one of
     * LEFT, RIGHT or CENTER
     * 
     * @param orientation the orientation value
     */
    public FlowLayout(int orientation) {
        this.orientation = orientation;
    }

    /**
     * @inheritDoc
     */    
    public void layoutContainer(Container parent) {
        int x = parent.getStyle().getPadding(parent.isRTL(), Component.LEFT);
        int width = parent.getLayoutWidth() - parent.getSideGap() - parent.getStyle().getPadding(parent.isRTL(), Component.RIGHT) - x;

        boolean rtl = parent.isRTL();
        if(rtl) {
        	x += parent.getSideGap();
        }
        int initX = x;

        int y = parent.getStyle().getPadding(false, Component.TOP);
        int rowH=0;
        int start=0;
                
        int numOfcomponents = parent.getComponentCount();
        for(int i=0; i< numOfcomponents; i++){
            Component cmp = parent.getComponentAt(i);
            Style style = cmp.getStyle();
            
            cmp.setWidth(cmp.getPreferredW());
            cmp.setHeight(cmp.getPreferredH());
            
            if((x == parent.getStyle().getPadding(parent.isRTL(), Component.LEFT)) || ( x+ cmp.getPreferredW() < width) ) {
                // We take the actual LEFT since drawing is done in reverse
                x += cmp.getStyle().getMargin(false, Component.LEFT);
            	if(rtl) {
                	cmp.setX(Math.max(width + initX - (x - initX) - cmp.getPreferredW(), style.getMargin(false, Component.LEFT)));
            	} else {
            		cmp.setX(x);
            	}

                cmp.setY(y + cmp.getStyle().getMargin(cmp.isRTL(), Component.TOP));
                
                x += cmp.getPreferredW() + cmp.getStyle().getMargin(false, Component.RIGHT);
                rowH = Math.max(rowH, cmp.getPreferredH() + cmp.getStyle().getMargin(false, Component.TOP)+ cmp.getStyle().getMargin(false, Component.BOTTOM));
            } else {
                moveComponents(parent, parent.getStyle().getPadding(parent.isRTL(), Component.LEFT), y, width - x, rowH, start, i);
                x = initX+cmp.getStyle().getMargin(false, Component.LEFT);
                y += rowH;

                if(rtl) {
                	cmp.setX(Math.max(width + initX - (x - initX) - cmp.getPreferredW(), style.getMargin(false, Component.LEFT)));
                } else {
                	cmp.setX(x);
                }

                cmp.setY(y + cmp.getStyle().getMargin(false, Component.TOP));
                rowH = cmp.getPreferredH()+ cmp.getStyle().getMargin(false, Component.TOP)+ cmp.getStyle().getMargin(false, Component.BOTTOM);
                x += cmp.getPreferredW()+ cmp.getStyle().getMargin(false, Component.RIGHT);
                start = i;
                
            }
        }
        moveComponents(parent, parent.getStyle().getPadding(parent.isRTL(), Component.LEFT), y, width - x, rowH, start, numOfcomponents);
    }
    
    
    private void moveComponents(Container target, int x, int y, int width, int height, int rowStart, int rowEnd ) {
        switch (orientation) {
            case Component.CENTER:
                // this will remove half of last gap
                if (target.isRTL()) {
                	x -= (width) / 2;  
                } else {
                	x += (width) / 2;
                }
                break;
            case Component.RIGHT:
                   x+=width;  // this will remove the last gap 
                break;
        }
        for (int i = rowStart ; i < rowEnd ; i++) {
            Component m = target.getComponentAt(i);            
            m.setX(m.getX()+ x);
            m.setY(y + m.getStyle().getMargin(false, Component.TOP));            
        }        
    }    

    /**
     * @inheritDoc
     */    
    public  Dimension getPreferredSize(Container parent) {
        int parentWidth = parent.getWidth();
        if(parentWidth == 0){
            parent.invalidate();
        }
        int width = 0;
        int height = 0;
        int w = 0;
        int numOfcomponents = parent.getComponentCount();
        Style parentStyle = parent.getStyle();
        int parentPadding = parentStyle.getPadding(Component.LEFT) + parentStyle.getPadding(Component.RIGHT);

        for(int i=0; i< numOfcomponents; i++){
            Component cmp = parent.getComponentAt(i);
            height = Math.max(height, cmp.getPreferredH() + cmp.getStyle().getMargin(false, Component.TOP)+ cmp.getStyle().getMargin(false, Component.BOTTOM));
            int prefW = cmp.getPreferredW()+ cmp.getStyle().getMargin(false, Component.RIGHT)+ cmp.getStyle().getMargin(false, Component.LEFT);
            w += prefW;
            //we need to break a line
            if(parentWidth > parentPadding && w >= parentWidth){
                height += cmp.getPreferredH() + cmp.getStyle().getMargin(false, Component.TOP)+ cmp.getStyle().getMargin(false, Component.BOTTOM);
                width = Math.max(w, width);
                w = prefW;
            }
            
        }

        width = Math.max(w, width);
        
        return new Dimension(width + parent.getStyle().getPadding(false, Component.LEFT)+ parent.getStyle().getPadding(false, Component.RIGHT),
            height + parent.getStyle().getPadding(false, Component.TOP)+ parent.getStyle().getPadding(false, Component.BOTTOM));
    }
    
    
}