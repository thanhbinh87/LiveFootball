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
package com.sun.lwuit.browser;

import com.sun.lwuit.Button;
import com.sun.lwuit.Component;
import com.sun.lwuit.Container;
import com.sun.lwuit.Display;
import com.sun.lwuit.Graphics;
import com.sun.lwuit.Image;
import com.sun.lwuit.TextField;
import com.sun.lwuit.animations.Motion;
import com.sun.lwuit.events.ActionEvent;
import com.sun.lwuit.events.ActionListener;
import com.sun.lwuit.html.HTMLComponent;
import com.sun.lwuit.layouts.BorderLayout;
import com.sun.lwuit.layouts.BoxLayout;
import com.sun.lwuit.util.Resources;
import java.io.IOException;
import java.util.Vector;

/**
 * A navigation toolbar that adds the back,forward,stop,refresh and home functionality
 *
 * @author Ofir Leitner
 */
public class BrowserToolbar extends Container implements ActionListener {

    /**
     * The prefix of the button images in the resource file
     */
    private static String BUTTON_IMAGE_PREFIX="nav_button_";

    /**
     * The prefix of the disabled button images in the resource file
     */
    private static String BUTTON_IMAGE_PREFIX_DISABLED="nav_button_disabled_";

    /**
     * The individual suffix names of the button images in the resource file
     */
    private static String[] imgFiles= {"back","stop","forward","refresh","home"};


    // Constants to identify the various navigation buttons
    private static int BTN_BACK = 0;
    private static int BTN_CANCEL = 1;
    private static int BTN_NEXT = 2;
    private static int BTN_REFRESH = 3;
    private static int BTN_HOME = 4;

    /**
     * The duration it takes the toolbar to slide in and out
     */
    private static int SLIDE_DURATION = 300;

    Button[] navButtons = new Button[imgFiles.length];
    Image[] buttonsImages = new Image[imgFiles.length];
    TextField address;
    HTMLComponent htmlC;
    Vector back=new Vector();
    Vector forward=new Vector();
    String homePage;
    String currentURL;
    Motion slide;
    boolean slidingOut;
    int prefH;

    private boolean backRequested=true;

    /**
     * A constant determining by which factor to scale the bar's buttons horizotally, specified as percentage of the original size (100 = original size).
     * This is useful especially when using touch screens with very high resolutions or high DPI ratio.
     * This value can be modified in the JAD property navbar_scalew
     */
    static int scaleHorizontal = 100;

    /**
     * A constant determining by which factor to scale the bar's buttons horizotally, specified as percentage of the original size (100 = original size).
     * This is useful especially when using touch screens with very high resolutions or high DPI ratio.
     * This value can be modified in the JAD property navbar_scaleh
     */
    static int scaleVertical = 100;



    public BrowserToolbar(HTMLComponent htmlComponent) {
        Resources toolBarRes=null;
        setUIID("NavToolbar");
        try {
            toolBarRes = Resources.open("/toolbar.res");
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        htmlC=htmlComponent;
        setLayout(new BorderLayout());

        address = new TextField() {

            public void keyReleased(int keyCode) {
                int action=Display.getInstance().getGameAction(keyCode);
                super.keyReleased(keyCode);
                if (action==Display.GAME_FIRE) {
                        htmlC.setPage(address.getText());
                        setEnabled(false);
                }
            }

        };

        Container buttons=new Container();
        buttons.setHandlesInput(true); //to ignore the initial page load

        buttons.setLayout(new BoxLayout(BoxLayout.X_AXIS));
        for(int i=0;i<imgFiles.length;i++) {
            Image img=null;
            Image disabledImg=null;
            img=toolBarRes.getImage(BUTTON_IMAGE_PREFIX+imgFiles[i]);
            disabledImg=toolBarRes.getImage(BUTTON_IMAGE_PREFIX_DISABLED+imgFiles[i]);

            if ((scaleHorizontal!=100) || (scaleVertical!=100)) {
                img=img.scaled(img.getWidth()*scaleHorizontal/100, img.getHeight()*scaleVertical/100);
                disabledImg=disabledImg.scaled(disabledImg.getWidth()*scaleHorizontal/100, disabledImg.getHeight()*scaleVertical/100);
            }

            navButtons[i]=new NavButton(img,disabledImg);

            buttons.addComponent(navButtons[i]);
            navButtons[i].addActionListener(this);
            navButtons[i].setEnabled(false);

        }

        addComponent(BorderLayout.CENTER, address);
        addComponent(BorderLayout.SOUTH,buttons);

    }

    /**
     * Sets the homepage of this toolbar to the specified URL
     * 
     * @param url The homepage
     */
    public void setHomePage(String url) {
        homePage=url;
        currentURL=url;
    }

    /**
     * Called when a page loading has started and sets the buttons disabled/enablked mode accordingly.
     */
    void notifyLoading() {
        address.setEnabled(false);
        navButtons[BTN_CANCEL].setEnabled(true);
        navButtons[BTN_BACK].setEnabled(false);
        navButtons[BTN_NEXT].setEnabled(false);
        navButtons[BTN_HOME].setEnabled(false);
        navButtons[BTN_REFRESH].setEnabled(false);
    }

    /**
     * Called when a page loading has completed and sets the buttons disabled/enablked mode accordingly.
     */
    void notifyLoadCompleted(String url) {
            address.setEnabled(true);
            navButtons[BTN_CANCEL].setEnabled(false);
            navButtons[BTN_BACK].setEnabled(!back.isEmpty());
            navButtons[BTN_NEXT].setEnabled(!forward.isEmpty());
            navButtons[BTN_HOME].setEnabled(homePage!=null);
            navButtons[BTN_REFRESH].setEnabled(true);

            if (!backRequested) {
                back.addElement(currentURL);
                navButtons[BTN_BACK].setEnabled(true);
            } else {
                backRequested=false;
            }
            //currentURL=htmlC.getPageURL();
            currentURL=htmlC.getDocumentInfo().getFullUrl();
            address.setText(currentURL);


    }


    public void actionPerformed(ActionEvent evt) {
         if (evt.getSource()==navButtons[BTN_BACK]) {
             back();
        } else if (evt.getSource()==navButtons[BTN_NEXT]) {
            forward();
        } else if (evt.getSource()==navButtons[BTN_HOME]) {
            home();
        } else if (evt.getSource()==navButtons[BTN_REFRESH]) {
            refresh();
        } else if (evt.getSource()==navButtons[BTN_CANCEL]) {
            stop();
        }

    }

    /**
     * Navigates to the previous page (if any)
     */
    public void back() {
            if (!back.isEmpty()) {
                String url=(String)back.lastElement();
                back.removeElementAt(back.size()-1);
                forward.addElement(currentURL);
                navButtons[BTN_NEXT].setEnabled(true);
                if (back.isEmpty()) {
                    navButtons[BTN_BACK].setEnabled(false);
                }
                backRequested=true;
                htmlC.setPage(url);
            }

    }

    /**
     * Navigates to the next page (if any)
     */
    public void forward() {
            if (!forward.isEmpty()) {
                String url=(String)forward.lastElement();
                forward.removeElementAt(forward.size()-1);
                back.addElement(url);
                if (forward.isEmpty()) {
                    navButtons[BTN_NEXT].setEnabled(false);
                }
                backRequested=true;
                htmlC.setPage(url);
            }
    }

    /**
     * Navigates to the home page (if any)
     */
    public void home() {
        if (homePage!=null) {
            htmlC.setPage(homePage);
        } else {
            System.out.println("No home page was set.");
        }
    }

    /**
     * Refreshes the current page
     */
    public void refresh() {
            backRequested=true;
            htmlC.setPage(currentURL);
    }

    /**
     * Stops the loading of the current page
     */
    public void stop() {
            htmlC.cancel();
    }




    /**
     * Starts the a slide in animation of the toolbar
     */
    public void slideIn() {
        slidingOut=false;
        if (prefH==0) {
            //prefH=getPreferredH();
            prefH=calcPreferredSize().getHeight();
        }
//        if (slide!=null) {
//            slide=Motion.createLinearMotion(slide.getValue(), prefH, SLIDE_DURATION);
//        } else {
            slide=Motion.createLinearMotion(0, prefH, SLIDE_DURATION);
            getComponentForm().registerAnimated(this);
            slide.start();
//        }
    }

    /**
     * Returns true if the toolbar is currently animating a slide, false otherwise
     * 
     * @return true if the toolbar is currently animating a slide, false otherwise
     */
    public boolean isSliding() {
        return (slide!=null);
    }

    /**
     * Starts the a slide out animation of the toolbar
     */
    public void slideOut() {
        slidingOut=true;
        if (prefH==0) {
            prefH=getPreferredH();
        }
//        if (slide!=null) {
//            slide=Motion.createLinearMotion(slide.getValue(), 0, SLIDE_DURATION);
//        } else {
            slide=Motion.createLinearMotion(prefH, 0, SLIDE_DURATION);
            getComponentForm().registerAnimated(this);
            slide.start();
//        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean animate() {
        if (slide!=null) {
            setPreferredH(slide.getValue());
            if (slide.isFinished()) {
                slide=null;
            }
            getParent().revalidate();
            return true;
        } else {
            getComponentForm().deregisterAnimated(this);
            if (slidingOut) {
                getComponentForm().removeComponent(this);
            } else {
                getComponentForm().setFocused(this);
            }
            return false;
        }
    }



}

/**
 * A navigation button., mostly adds the disabled image functionality to Button and adds a frame when focused.
 *
 * @author Ofir Leitner
 */
class NavButton extends Button {

    Image icon,disabledIcon;
    boolean focused;

     NavButton(Image icon,Image disabledIcon) {
         super(icon);
         this.icon=icon;
         this.disabledIcon=disabledIcon;
     }

    public String getUIID() {
        return "NavButton";
    }



    /**
     * {@inheritDoc}
     */
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (enabled) {
            setIcon(icon);
        } else {
            setIcon(disabledIcon);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void focusGained() {
        super.focusGained();
        focused=true;
        repaint();
    }

    /**
     * {@inheritDoc}
     */
    protected void focusLost() {
        super.focusLost();
        focused=false;
        repaint();
    }

    /**
     * {@inheritDoc}
     */
    public void paint(Graphics g) {
        super.paint(g);
        
        if (focused) { // Draws a frame around the button icon if it is focused
            g.setColor(0);
            g.drawRect(getX()+getStyle().getPadding(Component.LEFT), getY()+getStyle().getPadding(Component.TOP),
                       getWidth()-getStyle().getPadding(Component.LEFT)-getStyle().getPadding(Component.RIGHT),
                       getHeight()-getStyle().getPadding(Component.BOTTOM)-getStyle().getPadding(Component.TOP));
        }
    }

}
