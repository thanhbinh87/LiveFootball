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
package com.sun.lwuit.animations;

import com.sun.lwuit.Button;
import com.sun.lwuit.Component;
import com.sun.lwuit.Container;
import com.sun.lwuit.Dialog;
import com.sun.lwuit.Graphics;
import com.sun.lwuit.Image;
import com.sun.lwuit.Painter;
import com.sun.lwuit.RGBImage;
import com.sun.lwuit.plaf.UIManager;

/**
 * Contains common transition animations including the following:
 * <ol>
 * <li>Slide - the exiting form slides out of the screen while the new form slides in. 
 * <li>Fade - components fade into/out of the screen
 * </ol>
 * <p>Instances of this class are created using factory methods.
 * 
 * @author Shai Almog, Chen Fishbein
 */
public final class CommonTransitions extends Transition {
    private Motion motion;
    private static final int TYPE_EMPTY = 0;
    private static final int TYPE_SLIDE = 1;
    private static final int TYPE_FADE = 2;
    private static final int TYPE_FAST_SLIDE = 3;
    
    /**
     * Slide the transition horizontally
     * @see #createSlide
     */
    public static final int SLIDE_HORIZONTAL = 0;

    /**
     * Slide the transition vertically
     * @see #createSlide
     */
    public static final int SLIDE_VERTICAL = 1;
    
    private int slideType;
    private int speed;
    private int position;
    private int transitionType;
    private Image buffer;
    private Image secondaryBuffer;

    private static boolean defaultLinearMotion = false;
    private boolean linearMotion = defaultLinearMotion;
    private boolean motionSetManually;
    
    /**
     * The transition is a special case where we "keep" an allocated buffer
     */
    private RGBImage rgbBuffer;
    private boolean forward;
    private boolean drawDialogMenu;

    private boolean firstFinished;
    
    private CommonTransitions(int type) {
        transitionType = type;
    }

    /**
     * Creates an empty transition that does nothing. This has the same effect as
     * setting a transition to null.
     * 
     * @return empty transition
     */
    public static CommonTransitions createEmpty() {
        CommonTransitions t = new CommonTransitions(TYPE_EMPTY);
        return t;
    }

    /**
     * Creates a slide transition with the given duration and direction, this differs from the
     * standard slide animation by focusing on speed rather than on minimizing heap usage.
     * This method works by creating two images and sliding them which works much faster for
     * all devices however takes up more ram. Notice that this method of painting doesn't
     * support various basic LWUIT abilities such as translucent menus/dialogs etc.
     *
     * @param type type can be either vertically or horizontally, which means
     * the movement direction of the transition
     * @param forward forward is a boolean value, represent the directions of
     * switching forms, for example for a horizontally type, true means
     * horizontally movement to right.
     * @param duration represent the time the transition should take in millisecond
     * @return a transition object
     */
    public static CommonTransitions createFastSlide(int type, boolean forward, int duration) {
        return createFastSlide(type, forward, duration, false);
    }

    /**
     * Creates a slide transition with the given duration and direction
     * 
     * @param type type can be either vertically or horizontally, which means 
     * the movement direction of the transition
     * @param forward forward is a boolean value, represent the directions of 
     * switching forms, for example for a horizontally type, true means 
     * horizontally movement to right.
     * @param duration represent the time the transition should take in millisecond
     * @return a transition object
     */
    public static CommonTransitions createSlide(int type, boolean forward, int duration) {
        return createSlide(type, forward, duration, false);
    }

    /**
     * Creates a slide transition with the given duration and direction
     * 
     * @param type type can be either vertically or horizontally, which means 
     * the movement direction of the transition
     * @param forward forward is a boolean value, represent the directions of 
     * switching forms, for example for a horizontally type, true means 
     * horizontally movement to right.
     * @param duration represent the time the transition should take in millisecond
     * @param drawDialogMenu indicates that the menu (softkey area) of the dialog 
     * should be kept during a slide transition. This is only relevant for 
     * dialog in/out transitions.
     * @return a transition object
     */
    public static CommonTransitions createSlide(int type, boolean forward, int duration, boolean drawDialogMenu) {
        CommonTransitions t = new CommonTransitions(TYPE_SLIDE);
        t.slideType = type;
        t.forward = forward;
        if ((type==SLIDE_HORIZONTAL) && (UIManager.getInstance().getLookAndFeel().isRTL())) {
        	t.forward=!t.forward;
        }
        t.speed = duration;
        t.position = 0;
        t.drawDialogMenu = drawDialogMenu;
        return t;
    }

    /**
     * Creates a slide transition with the given duration and direction, this differs from the
     * standard slide animation by focusing on speed rather than on minimizing heap usage
     * This method works by creating two images and sliding them which works much faster for
     * all devices however takes up more ram. Notice that this method of painting doesn't
     * support various basic LWUIT abilities such as translucent menus/dialogs etc.
     *
     * @param type type can be either vertically or horizontally, which means
     * the movement direction of the transition
     * @param forward forward is a boolean value, represent the directions of
     * switching forms, for example for a horizontally type, true means
     * horizontally movement to right.
     * @param duration represent the time the transition should take in millisecond
     * @param drawDialogMenu indicates that the menu (softkey area) of the dialog
     * should be kept during a slide transition. This is only relevant for
     * dialog in/out transitions.
     * @return a transition object
     */
    public static CommonTransitions createFastSlide(int type, boolean forward, int duration, boolean drawDialogMenu) {
        CommonTransitions t = new CommonTransitions(TYPE_FAST_SLIDE);
        t.slideType = type;
        t.forward = forward;
        if ((type==SLIDE_HORIZONTAL) && (UIManager.getInstance().getLookAndFeel().isRTL())) {
        	t.forward=!t.forward;
        }
        t.speed = duration;
        t.position = 0;
        t.drawDialogMenu = drawDialogMenu;
        return t;
    }

    /**
     * Creates a transition for fading a form in while fading out the original form
     * 
     * @param duration represent the time the transition should take in millisecond
     * @return a transition object
     */
    public static CommonTransitions createFade(int duration) {
        CommonTransitions t = new CommonTransitions(TYPE_FADE);
        t.speed = duration;
        return t;
    }

    /**
     * @inheritDoc
     */
    public void initTransition() {
        firstFinished = false;
        if(transitionType == TYPE_EMPTY) {
            return;
        }
        
        Component source = getSource();
        Component destination = getDestination();
        position = 0;
        int w = source.getWidth();
        int h = source.getHeight();
        
        // a transition might occur with illegal source or destination values (common with 
        // improper replace() calls, this may still be valid and shouldn't fail
        if(w <= 0 || h <= 0) {
            return;
        }
        if (buffer == null) {
            buffer = Image.createImage(w, h);
        } else {
            // this might happen when screen orientation changes or a MIDlet moves
            // to an external screen
            if(buffer.getWidth() != w || buffer.getHeight() != h) {
                buffer = Image.createImage(w, h);
                rgbBuffer = null;
                
                // slide motion might need resetting since screen size is different
                motion = null;
            }
        }
        if(transitionType == TYPE_FADE) {
            motion = createMotion(0, 256, speed);
            motion.start();
            
            Graphics g = buffer.getGraphics();
            g.translate(-source.getAbsoluteX(), -source.getAbsoluteY());
            
            if(getSource().getParent() != null){
                getSource().getComponentForm().paintComponent(g);
            }
            //getSource().paintBackgrounds(g);
            g.setClip(0, 0, buffer.getWidth()+source.getAbsoluteX(), buffer.getHeight()+source.getAbsoluteY());
            paint(g, getDestination(), 0, 0);
            if(g.isAlphaSupported()) {
                secondaryBuffer = buffer;
                buffer = Image.createImage(w, h);
            } else {
                rgbBuffer = new RGBImage(buffer.getRGBCached(), buffer.getWidth(), buffer.getHeight());
            }
            
            paint(g, getSource(), 0, 0);
            g.translate(source.getAbsoluteX(), source.getAbsoluteY());
            
        } else {
            if (transitionType == TYPE_SLIDE || transitionType == TYPE_FAST_SLIDE) {
                int dest;
                int startOffset = 0;
                if (slideType == SLIDE_HORIZONTAL) {
                    dest = w;
                    if(destination instanceof Dialog) {
                        startOffset = w - ((Dialog)destination).getContentPane().getWidth();
                        if(forward) {
                            startOffset -= ((Dialog)destination).getContentPane().getStyle().getMargin(destination.isRTL(), Component.LEFT);
                        } else {
                            startOffset -= ((Dialog)destination).getContentPane().getStyle().getMargin(destination.isRTL(), Component.RIGHT);
                        }
                    } else {
                        if(source instanceof Dialog) {
                            dest = ((Dialog)source).getContentPane().getWidth();
                            if(forward) {
                                dest += ((Dialog)source).getContentPane().getStyle().getMargin(source.isRTL(), Component.LEFT);
                            } else {
                                dest += ((Dialog)source).getContentPane().getStyle().getMargin(source.isRTL(), Component.RIGHT);
                            }
                        } 
                    }
                } else {
                    dest = h;
                    if(destination instanceof Dialog) {
                        startOffset = h - ((Dialog)destination).getContentPane().getHeight() -
                            ((Dialog)destination).getTitleComponent().getHeight();
                        if(forward) {
                            startOffset -= ((Dialog)destination).getContentPane().getStyle().getMargin(false, Component.BOTTOM);
                        } else {
                            startOffset -= ((Dialog)destination).getContentPane().getStyle().getMargin(false, Component.TOP);
                            startOffset -= ((Dialog)destination).getTitleStyle().getMargin(false, Component.TOP);
                            if(!drawDialogMenu && ((Dialog)destination).getCommandCount() > 0) {
                                Container p = ((Dialog)destination).getSoftButton(0).getParent();
                                if(p != null) {
                                    startOffset -= p.getHeight();
                                }
                            }
                        }
                    } else {
                        if(source instanceof Dialog) {
                            dest = ((Dialog)source).getContentPane().getHeight() +
                                ((Dialog)source).getTitleComponent().getHeight();
                            if(forward) {
                                dest += ((Dialog)source).getContentPane().getStyle().getMargin(false, Component.BOTTOM);
                            } else {
                                dest += ((Dialog)source).getContentPane().getStyle().getMargin(false, Component.TOP);
                                dest += ((Dialog)source).getTitleStyle().getMargin(false, Component.TOP);
                                if(((Dialog)source).getCommandCount() > 0) {
                                    Container p = ((Dialog)source).getSoftButton(0).getParent();
                                    if(p != null) {
                                        dest += p.getHeight();
                                    }
                                }
                            }
                        } 
                    }
                }
                
                motion = createMotion(startOffset, dest, speed);

                // make sure the destination is painted fully at least once 
                // we must use a full buffer otherwise the clipping will take effect
                Graphics g = buffer.getGraphics();
                
                // If this is a dialog render the tinted frame once since 
                // tinting is expensive
                if(getSource() instanceof Dialog) {
                    paint(g, getDestination(), 0, 0);
                    if(transitionType == TYPE_FAST_SLIDE && !(destination instanceof Dialog)) {
                        Dialog d = (Dialog)source;
                        secondaryBuffer = Image.createImage(d.getContentPane().getWidth(), d.getContentPane().getHeight() +
                                d.getTitleComponent().getHeight());
                        drawDialogCmp(secondaryBuffer.getGraphics(), d);
                    }
                } else {
                    if(getDestination() instanceof Dialog) {
                        paint(g, getSource(), 0, 0);
                        if(transitionType == TYPE_FAST_SLIDE && !(source instanceof Dialog)) {
                            Dialog d = (Dialog)destination;
                            secondaryBuffer = Image.createImage(d.getContentPane().getWidth(), d.getContentPane().getHeight() +
                                    d.getTitleComponent().getHeight());
                            drawDialogCmp(secondaryBuffer.getGraphics(), d);
                        }
                    } else {
                        paint(g, source, -source.getAbsoluteX(), -source.getAbsoluteY());
                        if(transitionType == TYPE_FAST_SLIDE) {
                            secondaryBuffer = Image.createImage(destination.getWidth(), destination.getHeight());
                            paint(secondaryBuffer.getGraphics(), destination, -destination.getAbsoluteX(), -destination.getAbsoluteY());
                        }
                    }
                }
                motion.start();
            }
        }
    }

    /**
     * This method can be overriden by subclasses to create their own motion object on the fly
     *
     * @param startOffset the start offset for the menu
     * @param dest the destination of the motion
     * @param speed the speed of the motion
     * @return a motion instance
     */
    protected Motion createMotion(int startOffset, int dest, int speed) {
        if(motionSetManually) {
            return motion;
        }
        if(linearMotion) {
            return Motion.createLinearMotion(startOffset, dest, speed);
        }

        return Motion.createSplineMotion(startOffset, dest, speed);
    }

    /**
     * @inheritDoc
     */
    public boolean animate() {
        if(motion == null) {
            return false;
        }
        position = motion.getValue();
        
        // after the motion finished we need to paint one last time otherwise
        // there will be a "bump" in sliding
        if(firstFinished) {
            return false;
        }
        boolean finished = motion.isFinished();
        if(finished) {
            if(!firstFinished) {
                firstFinished = true;
            }
        }
        return true;
    }
    
    /**
     * @inheritDoc
     */
    public void paint(Graphics g) {
        try {
            switch (transitionType) {
                case TYPE_SLIDE:
                    // if this is an up or down slide
                    if (slideType == SLIDE_HORIZONTAL) {
                        paintSlideAtPosition(g, position, 0);
                    } else {
                        paintSlideAtPosition(g, 0, position);
                    }
                    return;
                case TYPE_FAST_SLIDE:
                    // if this is an up or down slide
                    if (slideType == SLIDE_HORIZONTAL) {
                        paintFastSlideAtPosition(g, position, 0);
                    } else {
                        paintFastSlideAtPosition(g, 0, position);
                    }
                    return;
                case TYPE_FADE:
                    paintAlpha(g);
                    return;
            }
        } catch(Throwable t) {
            System.out.println("An exception occurred during transition paint this might be valid in case of a resize in the middle of a transition");
            t.printStackTrace();
        }
    }

    private void paintAlpha(Graphics graphics) {
        // this will always be invoked on the EDT so there is no race condition risk
        if(rgbBuffer != null || secondaryBuffer != null) {
            Component src = getSource();
            int w = src.getWidth();
            int h = src.getHeight();
            int position = this.position;
            if (position > 255) {
                position = 255;
            } else {
                if (position < 0) {
                    position = 0;
                }
            }
            if(secondaryBuffer != null) {
                Component dest = getDestination();                
                int x = dest.getAbsoluteX();
                int y = dest.getAbsoluteY();

                graphics.drawImage(buffer, x, y);
                graphics.setAlpha(position);
                graphics.drawImage(secondaryBuffer, x, y);
                graphics.setAlpha(0xff);
            } else {
                int alpha = position << 24;
                int size = w * h;
                int[] bufferArray = rgbBuffer.getRGB();
                for (int iter = 0 ; iter < size ; iter++) {
                    bufferArray[iter] = ((bufferArray[iter] & 0xFFFFFF) | alpha);
                }
                Component dest = getDestination();                
                int x = dest.getAbsoluteX();
                int y = dest.getAbsoluteY();
                graphics.drawImage(buffer, x, y);
                graphics.drawImage(rgbBuffer, x, y);
            }
        } 
    }

    /**
     * @inheritDoc
     */
    public void cleanup() {
        super.cleanup();
        buffer = null;
        rgbBuffer = null;
        secondaryBuffer = null;
    }

    private void paintSlideAtPosition(Graphics g, int slideX, int slideY) {
        Component source = getSource();
        
        // if this is the first form we can't do a slide transition since we have no source form
        if (source == null) { 
            return;           
        }
        
        Component dest = getDestination();                
        int w = source.getWidth();
        int h = source.getHeight();
                    
        if (slideType == SLIDE_HORIZONTAL) {
            h = 0;
        } else {
            w = 0;
        }

        if(forward) {
            w = -w;
            h = -h;
        } else {
            slideX = -slideX;
            slideY = -slideY;
        }
        g.setClip(source.getAbsoluteX()+source.getScrollX(), source.getAbsoluteY()+source.getScrollY(), source.getWidth(), source.getHeight());
            
        // dialog animation is slightly different... 
        if(source instanceof Dialog) {
            g.drawImage(buffer, 0, 0);
            paint(g, source, -slideX, -slideY);
            return;
        } 
        
        if(dest instanceof Dialog) {
            g.drawImage(buffer, 0, 0);
            paint(g, dest, -slideX - w, -slideY - h);
            return;
        } 
        
        if(source.getParent() != null){
            source.paintBackgrounds(g);
            paint(g, source, slideX , slideY );
        }else{
            g.drawImage(buffer, slideX, slideY);        
        }
        paint(g, dest, slideX + w, slideY + h);
        
    }

    private void paintFastSlideAtPosition(Graphics g, int slideX, int slideY) {
        if(secondaryBuffer != null) {
            Component source = getSource();

            // if this is the first form we can't do a slide transition since we have no source form
            if (source == null) {
                return;
            }

            Component dest = getDestination();

            int w = buffer.getWidth();
            int h = buffer.getHeight();

            if (slideType == SLIDE_HORIZONTAL) {
                h = 0;
            } else {
                w = 0;
            }

            if(forward) {
                w = -w;
                h = -h;
            } else {
                slideX = -slideX;
                slideY = -slideY;
            }
            g.setClip(source.getAbsoluteX()+source.getScrollX(), source.getAbsoluteY()+source.getScrollY(), source.getWidth(), source.getHeight());

            // dialog animation is slightly different...
            if(source instanceof Dialog) {
                g.drawImage(buffer, 0, 0);
                slideX -= ((Dialog)source).getContentPane().getX();
                slideY -= ((Dialog)source).getContentPane().getY();
                g.drawImage(secondaryBuffer, -slideX, -slideY);
                return;
            }

            if(dest instanceof Dialog) {
                g.drawImage(buffer, 0, 0);
                slideY -= ((Dialog)dest).getTitleComponent().getY();
                slideX -= ((Dialog)dest).getTitleComponent().getX();
                g.drawImage(secondaryBuffer, -slideX - w, -slideY - h);
                return;
            }

            g.drawImage(buffer, slideX, slideY);
            g.drawImage(secondaryBuffer, slideX + w, slideY + h);

        } else {
            paintSlideAtPosition(g, slideX, slideY);
        }

    }

    private void drawDialogCmp(Graphics g, Dialog dlg) {
        Painter p = dlg.getStyle().getBgPainter();
        dlg.getStyle().setBgPainter(null);
        g.translate(-dlg.getTitleComponent().getX(), -dlg.getTitleComponent().getY());
        dlg.getTitleComponent().paintComponent(g, false);
        g.translate(dlg.getTitleComponent().getX(), dlg.getTitleComponent().getY());
        g.setClip(0, 0, dlg.getWidth(), dlg.getHeight());
        g.translate(-dlg.getContentPane().getX(), -dlg.getContentPane().getY() + dlg.getTitleComponent().getHeight());
        dlg.getContentPane().paintComponent(g, false);
        if(drawDialogMenu && dlg.getCommandCount() > 0) {
            Component menuBar = dlg.getSoftButton(0).getParent();
            if(menuBar != null) {
                g.setClip(0, 0, dlg.getWidth(), dlg.getHeight());
                menuBar.paintComponent(g, false);
            }
        }

        dlg.getStyle().setBgPainter(p);
    }

    private void paint(Graphics g, Component cmp, int x, int y) {
        int cx = g.getClipX();
        int cy = g.getClipY();
        int cw = g.getClipWidth();
        int ch = g.getClipHeight();
        if(cmp instanceof Dialog) {
            if(transitionType != TYPE_FADE) {
                if(!(getSource() instanceof Dialog && getDestination() instanceof Dialog && 
                        cmp == getDestination())) {
                    Painter p = cmp.getStyle().getBgPainter();
                    cmp.getStyle().setBgPainter(null);
                    g.translate(x, y);
                    Dialog dlg = (Dialog)cmp;
                    g.setClip(0, 0, cmp.getWidth(), cmp.getHeight());
                    dlg.getTitleComponent().paintComponent(g, false);
                    g.setClip(0, 0, cmp.getWidth(), cmp.getHeight());
                    dlg.getContentPane().paintComponent(g, false);
                    g.translate(-x, -y);
                    if(drawDialogMenu && dlg.getCommandCount() > 0) {
                        Component menuBar = dlg.getSoftButton(0).getParent();
                        if(menuBar != null) {
                            g.setClip(0, 0, cmp.getWidth(), cmp.getHeight());
                            menuBar.paintComponent(g, false);
                        }
                    }

                    g.setClip(cx, cy, cw, ch);
                    cmp.getStyle().setBgPainter(p);
                    return;
                }
            } 
            cmp.paintComponent(g, false);
            return;
        }
        //g.clipRect(cmp.getAbsoluteX(), cmp.getAbsoluteY(), cmp.getWidth(), cmp.getHeight());
         g.translate(x, y);
        //g.clipRect(cmp.getAbsoluteX(), cmp.getAbsoluteY(), cmp.getWidth(), cmp.getHeight());
        cmp.paintComponent(g, false);
         g.translate(-x, -y);
        
        g.setClip(cx, cy, cw, ch);
    }
    
    /**
     * Motion represents the physical movement within a transition, it can
     * be replaced by the user to provide a more appropriate physical feel
     * 
     * @return the instanceo of the motion class used by this transition
     */
    public Motion getMotion() {
        return motion;
    }

    /**
     * Motion represents the physical movement within a transition, it can
     * be replaced by the user to provide a more appropriate physical feel
     * 
     * @param motion new instance of the motion class that will be used by the transition
     */
    public void setMotion(Motion motion) {
        motionSetManually = true;
        this.motion = motion;
    }
    
    
    /**
     * @inheritDoc
     */
    public Transition copy(boolean reverse){
        CommonTransitions retVal = null;
        switch(transitionType) {
            case TYPE_FADE:
                retVal = CommonTransitions.createFade(speed);
                break;
            case TYPE_SLIDE: {
                boolean fwd=forward;

                // prevent double reversing of forward due to bidi when copying a transition
                if ((slideType==SLIDE_HORIZONTAL) && (UIManager.getInstance().getLookAndFeel().isRTL())) {
                    fwd=!fwd;
                }
                if(reverse) {
                    retVal = CommonTransitions.createSlide(slideType, !fwd, speed, drawDialogMenu);
                } else {
                    retVal = CommonTransitions.createSlide(slideType, fwd, speed, drawDialogMenu);
                }
                break;
            }
            case TYPE_FAST_SLIDE: {
                boolean fwd=forward;

                // prevent double reversing of forward due to bidi when copying a transition
                if ((slideType==SLIDE_HORIZONTAL) && (UIManager.getInstance().getLookAndFeel().isRTL())) {
                    fwd=!fwd;
                }
                if(reverse) {
                    retVal = CommonTransitions.createFastSlide(slideType, !fwd, speed, drawDialogMenu);
                } else {
                    retVal = CommonTransitions.createFastSlide(slideType, fwd, speed, drawDialogMenu);
                }
                break;
            }
            case TYPE_EMPTY:
                retVal = CommonTransitions.createEmpty();
                break;
        }
        retVal.linearMotion = linearMotion;
        return retVal;
    }

    /**
     * Indicates whether the motion associated with this transition is linear or spline motion
     *
     * @return the linearMotion
     */
    public boolean isLinearMotion() {
        return linearMotion;
    }

    /**
     * Indicates whether the motion associated with this transition is linear or spline motion
     *
     * @param linearMotion the linearMotion to set
     */
    public void setLinearMotion(boolean linearMotion) {
        this.linearMotion = linearMotion;
    }

    /**
     * Indicates whether the motion associated with these transitions by default is linear or spline motion
     *
     * @return the defaultLinearMotion
     */
    public static boolean isDefaultLinearMotion() {
        return defaultLinearMotion;
    }

    /**
     * Indicates whether the motion associated with these transitions by default is linear or spline motion
     *
     * @param aDefaultLinearMotion the defaultLinearMotion to set
     */
    public static void setDefaultLinearMotion(boolean aDefaultLinearMotion) {
        defaultLinearMotion = aDefaultLinearMotion;
    }
}
