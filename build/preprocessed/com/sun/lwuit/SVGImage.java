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
package com.sun.lwuit;

import com.sun.lwuit.animations.Animation;
import java.io.IOException;
import java.io.InputStream;
import javax.microedition.m2g.ScalableGraphics;
import org.w3c.dom.Document;
import org.w3c.dom.svg.SVGPoint;
import org.w3c.dom.svg.SVGSVGElement;

/**
 * Represents a JSR 226 image using the standard Image API, just like other images
 * in LWUIT this image can be animated. All features of JSR 226 may be accessed 
 * in this image using the standard DOM API.
 *
 * @deprecated Use the SVGImplementationFactory with standard LWUIT Image.createSVG
 * @author Chen Fishbein
 */
public class SVGImage extends Image implements Animation {

    private javax.microedition.m2g.SVGImage im;
    private static final String SVG_NAMESPACE = "http://www.w3.org/2000/svg";
    private boolean animated;
    private long startTime = -1;
    private int id = 0;
    private static int idCounter = 0;
    private static final boolean IS_SUPPORTED;

    static {
        boolean supported = false;
        try {
            Class.forName("javax.microedition.m2g.SVGImage");
            supported = true;
        } catch (Throwable t) {
        }
        IS_SUPPORTED = supported;
    }
    
    SVGImage(javax.microedition.m2g.SVGImage im, boolean animated) {
        super(null);
        this.im = im;
        this.animated = animated;
    }

    /**
     * Create an SVG image
     * 
     * @param stream source stream
     * @param animated whether SVG animations should be supported
     * @return an SVG image
     * @throws java.io.IOException when the stream throws an exception
     */
    public static Image createSVGImage(InputStream stream, boolean animated) throws IOException {
        SVGImage instance = new SVGImage((javax.microedition.m2g.SVGImage) javax.microedition.m2g.SVGImage.createImage(stream, null), animated);
        return instance;
    }

    /**
     * Create an SVG image
     * 
     * @param url source url
     * @param animated whether SVG animations should be supported
     * @return an SVG image
     * @throws java.io.IOException when the stream throws an exception
     */
    public static Image createSVGImage(java.lang.String url, boolean animated) throws IOException {
        return createSVGImage(Display.getInstance().getResourceAsStream(SVGImage.class, url), animated);
    }

    /**
     * @inheritDoc
     */
    public int getWidth() {
        return im.getViewportWidth();
    }

    /**
     * @inheritDoc
     */
    public int getHeight() {
        return im.getViewportHeight();
    }

    /**
     * @inheritDoc
     */
    protected void drawImage(Graphics g, Object nativeGraphics, int x, int y) {

        ScalableGraphics svgGraphics = ScalableGraphics.createInstance();
        javax.microedition.lcdui.Graphics gr = (javax.microedition.lcdui.Graphics)nativeGraphics;


        gr.setClip(g.getTranslateX() + g.getClipX(), g.getTranslateY() + g.getClipY(), g.getClipWidth(), g.getClipHeight());

        svgGraphics.bindTarget(gr);

        svgGraphics.render(x + g.getTranslateX(), y + g.getTranslateY(), im);

        svgGraphics.releaseTarget();
    }

    /**
     * @inheritDoc
     */
    public Image scaled(int width, int height) {
        im.setViewportWidth(width);
        im.setViewportHeight(height);
        return this;
    }

    /**
     * @inheritDoc
     */
    public Image rotate(int degrees) {
        SVGSVGElement e = getSVGElement();
        SVGPoint p = e.getCurrentTranslate();
        p.setX(-getWidth()/2);
        p.setY(-getHeight()/2);
        e.setCurrentRotate(degrees);
        p.setX(0);
        p.setY(0);        
        return this;
    }
    
    /**
     * Get the SVG document object for DOM manipulation
     * 
     * @return the SVG document
     */
    public Document getDocument(){
        return im.getDocument();
    }
    
    private SVGSVGElement getSVGElement(){
        SVGSVGElement retVal = null;
        Document dom = im.getDocument();
        retVal = (SVGSVGElement)dom.getElementById(this.getClass().getName() + id);
        if(retVal == null){
            retVal = (SVGSVGElement) dom.createElementNS(SVG_NAMESPACE, "svg");
            id = idCounter++;
            retVal.setId(this.getClass().getName() + id);
        }
        return retVal;
    }

    /**
     * @inheritDoc
     */
    public boolean animate() {
        long currentTime = System.currentTimeMillis();
        //if this is the first time init the start time.
        if (startTime == -1) {
            startTime = currentTime;
        }
        im.incrementTime((currentTime - startTime) / 1000.0f);
        startTime = currentTime;
        return animated;
    }

    /**
     * @inheritDoc
     */
    public void paint(Graphics g) {
    }

    /**
     * @inheritDoc
     */
    public boolean isAnimation() {
        return true;
    }
    
    /**
     * Returns true if the platform supports SVG
     * 
     * @return true if this platform supports SVG, false otherwise
     */
    public static boolean isSupported() {
        return IS_SUPPORTED;
    }
}
