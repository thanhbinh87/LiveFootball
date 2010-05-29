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

import com.sun.lwuit.animations.Transition;
import com.sun.lwuit.geom.Dimension;
import com.sun.lwuit.layouts.BorderLayout;
import com.sun.lwuit.plaf.LookAndFeel;
import com.sun.lwuit.plaf.Style;
import com.sun.lwuit.plaf.UIManager;
import java.util.Hashtable;

/**
 * A dialog is a form that occupies a part of the screen and appears as a modal
 * entity to the developer. Dialogs allow us to prompt users for information and
 * rely on the information being available on the next line after the show method.
 * <p>Modality indicates that a dialog will block the calling thread even if the
 * calling thread is the EDT. Notice that a dialog will not release the block
 * until dispose is called even if show() from another form is called!
 * <p>To determine the size of the dialog use the show method that accepts 4 integer
 * values, notice that these values accept margin from the four sides rather than x, y, width
 * and height values!
 * <p>To style the dialog you would usually want to style the content pane rather than
 * the dialog itself.
 *
 * @author Shai Almog
 */
public class Dialog extends Form {
    /**
     * Indicates whether the dialog has been disposed
     */
    private boolean disposed;

    /**
     * Constant indicating the type of alert to indicate the sound to play or
     * icon if none are explicitly set
     */
    public static final int TYPE_NONE = 0;
    
    /**
     * Constant indicating the type of alert to indicate the sound to play or
     * icon if none are explicitly set
     */
    public static final int TYPE_ALARM = 1;
    /**
     * Constant indicating the type of alert to indicate the sound to play or
     * icon if none are explicitly set
     */
    public static final int TYPE_CONFIRMATION = 2;
    /**
     * Constant indicating the type of alert to indicate the sound to play or
     * icon if none are explicitly set
     */
    public static final int TYPE_ERROR = 3;
    /**
     * Constant indicating the type of alert to indicate the sound to play or
     * icon if none are explicitly set
     */
    public static final int TYPE_INFO = 4;
    /**
     * Constant indicating the type of alert to indicate the sound to play or
     * icon if none are explicitly set
     */
    public static final int TYPE_WARNING = 5;
    /**
     * Indicates the time in which the alert should be disposed
     */
    private long time;
    /**
     * Indicates the last command selected by the user in this form
     */
    private Command lastCommandPressed;
    /**
     * Indicates that this is a menu preventing getCurrent() from ever returning this class
     */
    private boolean menu;
    private String dialogUIID;
    private String dialogTitleUIID;
    private int dialogType;
    private int top = -1;
    private int bottom;
    private int left;
    private int right;
    private boolean includeTitle;
    private String position;

    /**
     * Indicates whether LWUIT should try to automatically adjust a showing dialog size
     * when a screen size change event occurs
     */
    private static boolean autoAdjustDialogSize = true;

    /**
     * Default screen orientation position for the upcoming dialog. By default
     * the dialog will be shown at hardcoded coordinates, this method allows us 
     * to pack the dialog appropriately in one of the border layout based locations
     * see BorderLayout for futher details.
     */
    private static String defaultDialogPosition;

    /**
     * Allows a developer to indicate his interest that the dialog should no longer
     * scroll on its own but rather rely on the scrolling properties of internal 
     * scrollable containers. This flag only affects the static show methods within
     * this class.
     */
    private static boolean disableStaticDialogScrolling;

    /**
     * Determines whether the execution of a command on this dialog implicitly 
     * disposes the dialog. This defaults to true which is a sensible default for
     * simple dialogs.
     */
    private boolean autoDispose = true;

    private boolean modal = true;

    /**
     * The default type for dialogs
     */
    private static int defaultDialogType = TYPE_INFO;
    
    /**
     * Places commands as buttons at the bottom of the standard static dialogs rather than
     * as softbuttons. This is especially appropriate for devices such as touch devices and
     * devices without the common softbuttons (e.g. blackberries). 
     * The default value is false
     */
    private static boolean commandsAsButtons;

    private boolean disposeWhenPointerOutOfBounds = false;
    /**
     * Constructs a Dialog with a title
     * 
     * @param title the title of the dialog
     */
    public Dialog(String title) {
        this();
        setTitle(title);
    }

    /**
     * Constructs a Dialog with a title
     * 
     */
    public Dialog() {
        this("Dialog", "DialogTitle");
    }

    Dialog(String dialogUIID, String dialogTitleUIID) {
        super();
        this.dialogUIID = dialogUIID;
        this.dialogTitleUIID = dialogTitleUIID;
        getContentPane().setUIID(dialogUIID);
        getTitleComponent().setUIID(dialogTitleUIID);
        super.getStyle().setBgTransparency(0);
        super.getStyle().setBgImage(null);
        setSmoothScrolling(false);
        deregisterAnimated(this);
    }

    
    /**
     * Simple setter to set the Dialog Style
     * 
     * @param style
     */
    public void setDialogStyle(Style style){
        getContentPane().setUnSelectedStyle(style);
    }

    /**
     * Simple getter to get the Dialog Style
     * 
     * @return the style of the dialog
     */
    public Style getDialogStyle(){
        return getContentPane().getStyle();
    }

    /**
     * Initialize the default transition for the dialogs overriding the forms
     * transition
     * 
     * @param laf the default transition for the dialog
     */
    void initLaf(LookAndFeel laf) {
        setTransitionOutAnimator(laf.getDefaultDialogTransitionOut());
        setTransitionInAnimator(laf.getDefaultDialogTransitionIn());
    }

    /**
     * This method shows the form as a modal alert allowing us to produce a behavior
     * of an alert/dialog box. This method will block the calling thread even if the
     * calling thread is the EDT. Notice that this method will not release the block
     * until dispose is called even if show() from another form is called!
     * <p>Modal dialogs Allow the forms "content" to "hang in mid air" this is especially useful for
     * dialogs where you would want the underlying form to "peek" from behind the 
     * form. 
     * 
     * @param top space in pixels between the top of the screen and the form
     * @param bottom space in pixels between the bottom of the screen and the form
     * @param left space in pixels between the left of the screen and the form
     * @param right space in pixels between the right of the screen and the form
     * @param includeTitle whether the title should hang in the top of the screen or
     * be glued onto the content pane
     * @return the last command pressed by the user if such a command exists
     */
    public Command show(int top, int bottom, int left, int right, boolean includeTitle) {
        return show(top, bottom, left, right, includeTitle, true);
    }

    /**
     * This method shows the form as a modal alert allowing us to produce a behavior
     * of an alert/dialog box. This method will block the calling thread even if the
     * calling thread is the EDT. Notice that this method will not release the block
     * until dispose is called even if show() from another form is called!
     * <p>Modal dialogs Allow the forms "content" to "hang in mid air" this is especially useful for
     * dialogs where you would want the underlying form to "peek" from behind the 
     * form. 
     * 
     * @param top space in pixels between the top of the screen and the form
     * @param bottom space in pixels between the bottom of the screen and the form
     * @param left space in pixels between the left of the screen and the form
     * @param right space in pixels between the right of the screen and the form
     * @param includeTitle whether the title should hang in the top of the screen or
     * be glued onto the content pane
     * @param modal indicates the dialog should be modal set to false for modeless dialog
     * which is useful for some use cases
     * @return the last command pressed by the user if such a command exists
     */
    public Command show(int top, int bottom, int left, int right, boolean includeTitle, boolean modal) {
        this.top = top;
        this.bottom = bottom;
        if(isRTL()){
            this.left = right;
            this.right = left;            
        }else{
            this.left = left;
            this.right = right;
        }
        this.includeTitle = includeTitle;
        setDisposed(false);
        this.modal = modal;
        lastCommandPressed = null;
        super.showModal(this.top, this.bottom, this.left, this.right, includeTitle, modal, false);
        return lastCommandPressed;
    }

    /**
     * Indicates the time (in milliseconds) afterwhich the dialog will be disposed 
     * implicitly
     * 
     * @param time a milliseconds time used to dispose the dialog
     */
    public void setTimeout(long time) {
        this.time = System.currentTimeMillis() + time;
        super.registerAnimatedInternal(this);
    }

    /**
     * Shows a modal prompt dialog with the given title and text.
     * 
     * @param title The title for the dialog optionally null;
     * @param text the text displayed in the dialog
     * @param type the type of the alert one of TYPE_WARNING, TYPE_INFO, 
     * TYPE_ERROR, TYPE_CONFIRMATION or TYPE_ALARM
     * @param icon the icon for the dialog, can be null
     * @param okText the text to appear in the command dismissing the dialog
     * @param cancelText optionally null for a text to appear in the cancel command
     * for canceling the dialog
     * @return true if the ok command was pressed or if cancelText is null. False otherwise.
     */
    public static boolean show(String title, String text, int type, Image icon, String okText, String cancelText) {
        return show(title, text, type, icon, okText, cancelText, 0);
    }

    /**
     * @inheritDoc
     */
    void sizeChangedInternal(int w, int h) {
        autoAdjust(w, h);
        super.sizeChangedInternal(w, h);
        Form frm = getPreviousForm();
        if(frm != null){
            frm.sizeChangedInternal(w, h);
        }        
    }

    /**
     * Auto adjust size of the dialog.
     * This method is triggered from a sizeChanged event.
     * 
     * @param w width of the screen
     * @param h height of the screen
     */
    protected void autoAdjust(int w, int h) {
        if (autoAdjustDialogSize) {
            // if the dialog is packed we can scale it far more accurately based on intention
            if (position != null) {
                Component contentPane = getContentPane();
                Component title = getTitleComponent();

                Style contentPaneStyle = contentPane.getStyle();
                Style titleStyle = title.getStyle();

                int menuHeight = 0;
                if (getSoftButtonCount() > 1) {
                    Component menuBar = getSoftButton(0).getParent();
                    Style menuStyle = menuBar.getStyle();
                    menuHeight = menuBar.getPreferredH() + menuStyle.getMargin(false, TOP) + menuStyle.getMargin(false, BOTTOM);
                }
                int prefHeight = contentPane.getPreferredH() + contentPaneStyle.getMargin(false, TOP) + contentPaneStyle.getMargin(false, BOTTOM);
                int prefWidth = contentPane.getPreferredW() + contentPaneStyle.getMargin(isRTL(), LEFT) + contentPaneStyle.getMargin(isRTL(), RIGHT);
                prefWidth = Math.min(prefWidth, w);
                h = h - menuHeight - title.getPreferredH() - titleStyle.getMargin(false, TOP) - titleStyle.getMargin(false, BOTTOM);
                int topBottom = Math.max(0, (h - prefHeight) / 2);
                int leftRight = Math.max(0, (w - prefWidth) / 2);
                int top = topBottom, bottom = topBottom;
                int left = leftRight, right = leftRight;

                if (position.equals(BorderLayout.EAST)) {
                    left = Math.max(0, w - prefWidth);
                    right = 0;
                } else {
                    if (position.equals(BorderLayout.WEST)) {
                        right = 0;
                        left = Math.max(0, w - prefWidth);
                    } else {
                        if (position.equals(BorderLayout.NORTH)) {
                            top = 0;
                            bottom = Math.max(0, h - prefHeight);
                        } else {
                            if (position.equals(BorderLayout.SOUTH)) {
                                top = Math.max(0, h - prefHeight);
                                bottom = 0;
                            }
                        }
                    }
                }

                Style contentStyle = getDialogStyle();
                if (includeTitle) {
                    titleStyle.setMargin(Component.TOP, top, true);
                    titleStyle.setMargin(Component.BOTTOM, 0, true);
                    titleStyle.setMargin(Component.LEFT, left, true);
                    titleStyle.setMargin(Component.RIGHT, right, true);

                    contentStyle.setMargin(Component.TOP, 0, true);
                    contentStyle.setMargin(Component.BOTTOM, bottom, true);
                    contentStyle.setMargin(Component.LEFT, left, true);
                    contentStyle.setMargin(Component.RIGHT, right, true);
                } else {
                    titleStyle.setMargin(Component.TOP, 0, true);
                    titleStyle.setMargin(Component.BOTTOM, 0, true);
                    titleStyle.setMargin(Component.LEFT, 0, true);
                    titleStyle.setMargin(Component.RIGHT, 0, true);

                    contentStyle.setMargin(Component.TOP, top, true);
                    contentStyle.setMargin(Component.BOTTOM, bottom, true);
                    contentStyle.setMargin(Component.LEFT, left, true);
                    contentStyle.setMargin(Component.RIGHT, right, true);
                }
            } else {
                int oldW = getWidth();
                int oldH = getHeight();
                if (oldW != w || oldH != h) {

                    float ratioW = ((float) w) / ((float) oldW);
                    float ratioH = ((float) h) / ((float) oldH);

                    Style s = getDialogStyle();

                    s.setMargin(TOP, (int) (s.getMargin(false, TOP) * ratioH));
                    s.setMargin(BOTTOM, (int) (s.getMargin(false, BOTTOM) * ratioH));
                    s.setMargin(LEFT, (int) (s.getMargin(isRTL(), LEFT) * ratioW));
                    s.setMargin(RIGHT, (int) (s.getMargin(isRTL(), RIGHT) * ratioW));

                    Style titleStyle = getTitleStyle();
                    titleStyle.setMargin(TOP, (int) (titleStyle.getMargin(false, TOP) * ratioH));
                    titleStyle.setMargin(LEFT, (int) (titleStyle.getMargin(isRTL(), LEFT) * ratioW));
                    titleStyle.setMargin(RIGHT, (int) (titleStyle.getMargin(isRTL(), RIGHT) * ratioW));
                }
            }
        }

        top = -1;
        bottom = -1;
        left = -1;
        right = -1;
        position = null;
    }
    
    /**
     * Shows a modal prompt dialog with the given title and text.
     * 
     * @param title The title for the dialog optionally null;
     * @param text the text displayed in the dialog
     * @param type the type of the alert one of TYPE_WARNING, TYPE_INFO, 
     * TYPE_ERROR, TYPE_CONFIRMATION or TYPE_ALARM
     * @param icon the icon for the dialog, can be null
     * @param okText the text to appear in the command dismissing the dialog
     * @param cancelText optionally null for a text to appear in the cancel command
     * for canceling the dialog
     * @param timeout a timeout after which null would be returned if timeout is 0 inifinite time is used
     * @return true if the ok command was pressed or if cancelText is null. False otherwise.
     */
    public static boolean show(String title, String text, int type, Image icon, String okText, String cancelText, long timeout) {
        Command[] cmds;
        Command okCommand = new Command(okText);
        if (cancelText != null) {
            cmds = new Command[]{new Command(cancelText), okCommand};
        } else {
            cmds = new Command[]{okCommand};
        }
        return show(title, text, okCommand, cmds, type, icon, timeout) == okCommand;
    }

    /**
     * Shows a modal prompt dialog with the given title and text.
     * 
     * @param title The title for the dialog optionally null;
     * @param text the text displayed in the dialog
     * @param cmds commands that are added to the form any click on any command
     * will dispose the form
     * @param type the type of the alert one of TYPE_WARNING, TYPE_INFO, 
     * TYPE_ERROR, TYPE_CONFIRMATION or TYPE_ALARM
     * @param icon the icon for the dialog, can be null
     * @param timeout a timeout after which null would be returned if timeout is 0 inifinite time is used
     * @return the command pressed by the user
     */
    public static Command show(String title, String text, Command[] cmds, int type, Image icon, long timeout) {
        return show(title, text, null, cmds, type, icon, timeout);
    }

    /**
     * Shows a modal prompt dialog with the given title and text.
     * 
     * @param title The title for the dialog optionally null;
     * @param text the text displayed in the dialog
     * @param defaultCommand command to be assigned as the default command or null
     * @param cmds commands that are added to the form any click on any command
     * will dispose the form
     * @param type the type of the alert one of TYPE_WARNING, TYPE_INFO, 
     * TYPE_ERROR, TYPE_CONFIRMATION or TYPE_ALARM
     * @param icon the icon for the dialog, can be null
     * @param timeout a timeout after which null would be returned if timeout is 0 inifinite time is used
     * @return the command pressed by the user
     */
    public static Command show(String title, String text, Command defaultCommand, Command[] cmds, int type, Image icon, long timeout) {
        return show(title, text, defaultCommand, cmds, type, icon, timeout, null);
    }

    /**
     * Shows a modal prompt dialog with the given title and text.
     * 
     * @param title The title for the dialog optionally null;
     * @param text the text displayed in the dialog
     * @param cmds commands that are added to the form any click on any command
     * will dispose the form
     * @param type the type of the alert one of TYPE_WARNING, TYPE_INFO, 
     * TYPE_ERROR, TYPE_CONFIRMATION or TYPE_ALARM
     * @param icon the icon for the dialog, can be null
     * @param timeout a timeout after which null would be returned if timeout is 0 inifinite time is used
     * @param transition the transition installed when the dialog enters/leaves
     * @return the command pressed by the user
     */
    public static Command show(String title, String text, Command[] cmds, int type, Image icon, long timeout, Transition transition) {
        return show(title, text, null, cmds, type, icon, timeout, transition);
    }

    /**
     * Shows a modal prompt dialog with the given title and text.
     * 
     * @param title The title for the dialog optionally null;
     * @param text the text displayed in the dialog
     * @param defaultCommand command to be assigned as the default command or null
     * @param cmds commands that are added to the form any click on any command
     * will dispose the form
     * @param type the type of the alert one of TYPE_WARNING, TYPE_INFO, 
     * TYPE_ERROR, TYPE_CONFIRMATION or TYPE_ALARM
     * @param icon the icon for the dialog, can be null
     * @param timeout a timeout after which null would be returned if timeout is 0 inifinite time is used
     * @param transition the transition installed when the dialog enters/leaves
     * @return the command pressed by the user
     */
    public static Command show(String title, String text, Command defaultCommand, Command[] cmds, int type, Image icon, long timeout, Transition transition) {
        Hashtable h =  UIManager.getInstance().getResourceBundle();
        if(h != null && text != null) {
            Object o = h.get(text);
            if(o != null) {
                text = (String)o;
            }
        } 
        TextArea t = new TextArea(text, 3, 30);
        t.setUIID("DialogBody");
        t.setEditable(false);
        return show(title, t, defaultCommand, cmds, type, icon, timeout, transition);
    }

    /**
     * Shows a modal prompt dialog with the given title and text.
     * 
     * @param title The title for the dialog optionally null;
     * @param text the text displayed in the dialog
     * @param okText the text to appear in the command dismissing the dialog
     * @param cancelText optionally null for a text to appear in the cancel command
     * for canceling the dialog
     * @return true if the ok command was pressed or if cancelText is null. False otherwise.
     */
    public static boolean show(String title, String text, String okText, String cancelText) {
        return show(title, text, defaultDialogType, null, okText, cancelText);
    }

    /**
     * Shows a modal dialog with the given component as its "body" placed in the
     * center. 
     * 
     * @param title title for the dialog
     * @param body component placed in the center of the dialog
     * @param cmds commands that are added to the form any click on any command
     * will dispose the form
     * @return the command pressed by the user
     */
    public static Command show(String title, Component body, Command[] cmds) {
        return show(title, body, cmds, defaultDialogType, null);
    }

    /**
     * Shows a modal dialog with the given component as its "body" placed in the
     * center. 
     * 
     * @param title title for the dialog
     * @param body component placed in the center of the dialog
     * @param cmds commands that are added to the form any click on any command
     * will dispose the form
     * @param type the type of the alert one of TYPE_WARNING, TYPE_INFO, 
     * TYPE_ERROR, TYPE_CONFIRMATION or TYPE_ALARM
     * @param icon the icon for the dialog, can be null
     * @return the command pressed by the user
     */
    public static Command show(String title, Component body, Command[] cmds, int type, Image icon) {
        return show(title, body, cmds, type, icon, 0);
    }

    /**
     * Shows a modal dialog with the given component as its "body" placed in the
     * center. 
     * 
     * @param title title for the dialog
     * @param body component placed in the center of the dialog
     * @param cmds commands that are added to the form any click on any command
     * will dispose the form
     * @param type the type of the alert one of TYPE_WARNING, TYPE_INFO, 
     * TYPE_ERROR, TYPE_CONFIRMATION or TYPE_ALARM
     * @param icon the icon for the dialog, can be null
     * @param timeout a timeout after which null would be returned if timeout is 0 inifinite time is used
     * @return the command pressed by the user
     */
    public static Command show(String title, Component body, Command[] cmds, final int type, Image icon, long timeout) {
        return show(title, body, cmds, type, icon, timeout, null);
    }
    
    /**
     * Shows a modal dialog with the given component as its "body" placed in the
     * center. 
     * 
     * @param title title for the dialog
     * @param body component placed in the center of the dialog
     * @param cmds commands that are added to the form any click on any command
     * will dispose the form
     * @param type the type of the alert one of TYPE_WARNING, TYPE_INFO, 
     * TYPE_ERROR, TYPE_CONFIRMATION or TYPE_ALARM
     * @param icon the icon for the dialog, can be null
     * @param timeout a timeout after which null would be returned if timeout is 0 inifinite time is used
     * @param transition the transition installed when the dialog enters/leaves
     * @return the command pressed by the user
     */
    public static Command show(String title, Component body, Command[] cmds, int type, Image icon, long timeout, Transition transition) {
        return show(title, body, null, cmds, type, icon, timeout, transition);
    }

    /**
     * Shows a modal dialog with the given component as its "body" placed in the
     * center. 
     * 
     * @param title title for the dialog
     * @param body component placed in the center of the dialog
     * @param defaultCommand command to be assigned as the default command or null
     * @param cmds commands that are added to the form any click on any command
     * will dispose the form
     * @param type the type of the alert one of TYPE_WARNING, TYPE_INFO, 
     * TYPE_ERROR, TYPE_CONFIRMATION or TYPE_ALARM
     * @param icon the icon for the dialog, can be null
     * @param timeout a timeout after which null would be returned if timeout is 0 inifinite time is used
     * @param transition the transition installed when the dialog enters/leaves
     * @return the command pressed by the user
     */
    public static Command show(String title, Component body, Command defaultCommand, Command[] cmds, int type, Image icon, long timeout, Transition transition) {
        Dialog dialog = new Dialog(title);
        dialog.dialogType = type;
        dialog.setTransitionInAnimator(transition);
        dialog.setTransitionOutAnimator(transition);
        dialog.lastCommandPressed = null;
        dialog.setLayout(new BorderLayout());
        if(cmds != null) {
            if(commandsAsButtons) {
                Container buttonArea = new Container();
                dialog.addComponent(BorderLayout.SOUTH, buttonArea);
                if(cmds.length > 0) {
                    for(int iter = 0 ; iter < cmds.length ; iter++) {
                        buttonArea.addComponent(new Button(cmds[iter]));
                    }
                    buttonArea.getComponentAt(0).requestFocus();
                }
            } else {
                for(int iter = 0 ; iter < cmds.length ; iter++) {
                    dialog.addCommand(cmds[iter]);
                }
            }

            // for the special case of one command map it to the back button
            if(cmds.length == 1) {
                dialog.setBackCommand(cmds[0]);
            } else {
                // map two commands to fire and back
                if(cmds.length == 2 && defaultCommand == null) {
                    defaultCommand = cmds[0];
                    dialog.setBackCommand(cmds[1]);
                }
            }
        }
        dialog.addComponent(BorderLayout.CENTER, body);
        if (icon != null) {
            dialog.addComponent(BorderLayout.EAST, new Label(icon));
        }
        if (timeout != 0) {
            dialog.setTimeout(timeout);
        }
        if(body.isScrollable() || disableStaticDialogScrolling){
            dialog.setScrollable(false);
        }
        dialog.show();
        return dialog.lastCommandPressed;
    }
    
    /**
     * @inheritDoc
     */
    protected void onShow() {
        if (dialogType > 0) {
            Display.getInstance().playDialogSound(dialogType);
        }
    }
    
    /**
     * @inheritDoc
     */
    public void showBack() {
        showImpl(true);
    }

    /**
     * The default version of show modal shows the dialog occupying the center portion
     * of the screen.
     */
    public void show() {
        showImpl(false);
    }

    /**
     * The default version of show modal shows the dialog occupying the center portion
     * of the screen.
     */
    private void showImpl(boolean reverse) {
        // this behavior allows a use case where dialogs of various sizes are layered
        // one on top of the other
        setDisposed(false);
        if(top > -1) {
            show(top, bottom, left, right, includeTitle, modal);
        } else {
            if(modal) {
                if(defaultDialogPosition == null) {
                    super.showModal(reverse);
                } else {
                    showPacked(defaultDialogPosition, true);
                }
            } else {
                showModeless();
            }
        }
    }

    /**
     * Shows a modeless dialog which is useful for some simpler use cases such as
     * progress indication etc...
     */
    public void showModeless() {
        // this behavior allows a use case where dialogs of various sizes are layered 
        // one on top of the other
        modal = false;
        setDisposed(false);
        if(top > -1) {
            show(top, bottom, left, right, includeTitle, false);
        } else {
            if(defaultDialogPosition == null) {
                showDialog(false, false);
            } else {
                showPacked(defaultDialogPosition, false);
            }
        }
    }
    
    /**
     * Convenience method to show a dialog sized to match its content.
     * 
     * @param position one of the values from the BorderLayout class e.g. BorderLayout.CENTER, BorderLayout.NORTH etc.
     * @param modal whether the dialog should be modal or modaless
     * @return the command selected if the dialog is modal and disposed via a command
     */
    public Command showPacked(String position, boolean modal) {
        this.position = position;
        int height = Display.getInstance().getDisplayHeight();
        int width = Display.getInstance().getDisplayWidth();
        if(top > -1){
            refreshTheme();
        }
        Component contentPane = getContentPane();
        Component title = getTitleComponent();
        
        Style contentPaneStyle = contentPane.getStyle();
        Style titleStyle = title.getStyle();
        
        int menuHeight = 0;
        if(getSoftButtonCount() > 1) {
            Component menuBar = getSoftButton(0).getParent();
            Style menuStyle = menuBar.getStyle();
            menuHeight = menuBar.getPreferredH() + menuStyle.getMargin(false, TOP) + menuStyle.getMargin(false, BOTTOM);
        }
        int prefHeight = contentPane.getPreferredH() + contentPaneStyle.getMargin(false, TOP) + contentPaneStyle.getMargin(false, BOTTOM);
        int prefWidth = contentPane.getPreferredW() + contentPaneStyle.getMargin(isRTL(), LEFT) + contentPaneStyle.getMargin(isRTL(), RIGHT);
        prefWidth = Math.min(prefWidth, width);
        height = height - menuHeight - title.getPreferredH() - titleStyle.getMargin(false, TOP) - titleStyle.getMargin(false, BOTTOM);
        int topBottom = Math.max(0, (height - prefHeight) / 2);
        int leftRight = Math.max(0, (width - prefWidth) / 2);
        
        if(position.equals(BorderLayout.CENTER)) {
            show(topBottom, topBottom, leftRight, leftRight, true, modal);
            return lastCommandPressed;
        } 
        if(position.equals(BorderLayout.EAST)) {
            show(topBottom, topBottom, Math.max(0, width - prefWidth), 0, true, modal);
            return lastCommandPressed;
        } 
        if(position.equals(BorderLayout.WEST)) {
            show(topBottom, topBottom, 0, Math.max(0, width - prefWidth), true, modal);
            return lastCommandPressed;
        } 
        if(position.equals(BorderLayout.NORTH)) {
            show(0, Math.max(0, height - prefHeight), leftRight, leftRight, true, modal);
            return lastCommandPressed;
        } 
        if(position.equals(BorderLayout.SOUTH)) {
            show(Math.max(0, height - prefHeight), 0, leftRight, leftRight, true, modal);
            return lastCommandPressed;
        } 
        throw new IllegalArgumentException("Unknown position: " + position);
    }

    /**
     * Closes the current form and returns to the previous form, releasing the 
     * EDT in the process 
     */
    public void dispose() {
        setDisposed(true);

        // the dispose parent method might send us back to the form while the command
        // within the dialog might be directing us to another form causing a "blip"
        if(!menu) {
            super.dispose();
        }
    }

    /**
     * Shows a modal dialog and returns the command pressed within the modal dialog
     * 
     * @return last command pressed in the modal dialog
     */
    public Command showDialog() {
        lastCommandPressed = null;
        show();
        return lastCommandPressed;
    }
    
    /**
     * Invoked to allow subclasses of form to handle a command from one point
     * rather than implementing many command instances
     * 
     * @param cmd the action command
     */
    protected void actionCommand(Command cmd) {
        // this is important... In a case of nested dialogs based on commands/events a command might be
        // blocked by a different dialog, so when that dialog is disposed (as a result of a command) going
        // back to this dialog will block that command from proceeding and it can be fired again later
        // E.g.:
        // Dialog A has a command which triggers dialog B on top.
        // User presses Cancel in dialog B
        // Dialog A is shown as a result of dialog B dispose method
        // Cancel command event firing is blocked since the dialog B dispose method is now blocking on dialog A show()...
        // When dialog A is disposed using the OK command the OK command is sent correctly and causes dispose
        // EDT is released which also releases the Cancel for dialog B to keep processing...
        // Cancel for dialog B proceeds in the event chain reaching this method....
        // lastCommandPressed can be overrwritten if this check isn't made!!!
        if(!autoDispose || lastCommandPressed == null) {
            lastCommandPressed = cmd;
        }
        if(menu || (autoDispose && cmd.isDisposesDialog())) {
            dispose();
        }
    }

    /**
     * @inheritDoc
     */
    public boolean animate() {
        isTimedOut();
        return false;
    }

    private boolean isTimedOut(){
        if (time != 0 && System.currentTimeMillis() >= time) {
            time = 0;
            dispose();
            deregisterAnimatedInternal(this);
            return true;
        }
        return false;
    }

    /**
     * Indicates that this is a menu preventing getCurrent() from ever returning this class
     */
    boolean isMenu() {
        return menu;
    }

    /**
     * Indicates that this is a menu preventing getCurrent() from ever returning this class
     */
    void setMenu(boolean menu) {
        this.menu = menu;
    }

    /**
     * Prevent a menu from adding the select button
     */
    void addSelectCommand() {
        if (!menu) {
            super.addSelectCommand(getSelectCommandText());
        }
    }

    
    /**
     * Allows us to indicate disposed state for dialogs
     */
    boolean isDisposed() {
        return disposed || isTimedOut();
    }

    /**
     * Allows us to indicate disposed state for dialogs
     */
    void setDisposed(boolean disposed) {
        this.disposed = disposed;
    }

    /**
     * Determines whether the execution of a command on this dialog implicitly 
     * disposes the dialog. This defaults to true which is a sensible default for
     * simple dialogs.
     * 
     * @return true if this dialog disposes on any command
     */
    public boolean isAutoDispose() {
        return autoDispose;
    }


    /**
     * Determines whether the execution of a command on this dialog implicitly 
     * disposes the dialog. This defaults to true which is a sensible default for
     * simple dialogs.
     * 
     * @param autoDispose true if this dialog disposes on any command
     */
    public void setAutoDispose(boolean autoDispose) {
        this.autoDispose = autoDispose;
    }

    /**
     * Default screen orientation position for the upcoming dialog. By default
     * the dialog will be shown at hardcoded coordinates, this method allows us 
     * to pack the dialog appropriately in one of the border layout based locations
     * see BorderLayout for futher details.
     * 
     * @param p for dialogs on the sceen using BorderLayout orientation tags
     */
    public static void setDefaultDialogPosition(String p) {
        defaultDialogPosition = p;
    }
    
    /**
     * Default screen orientation position for the upcoming dialog. By default
     * the dialog will be shown at hardcoded coordinates, this method allows us 
     * to pack the dialog appropriately in one of the border layout based locations
     * see BorderLayout for futher details.
     * 
     * @return position for dialogs on the sceen using BorderLayout orientation tags
     */
    public static String getDefaultDialogPosition() {
        return defaultDialogPosition;
    }

    /**
     * The type of the dialog can be one of TYPE_WARNING, TYPE_INFO, 
     * TYPE_ERROR, TYPE_CONFIRMATION or TYPE_ALARM
     * 
     * @return can be one of TYPE_WARNING, TYPE_INFO, 
     * TYPE_ERROR, TYPE_CONFIRMATION or TYPE_ALARM
     */
    public int getDialogType() {
        return dialogType;
    }

    /**
     * The type of the dialog can be one of TYPE_WARNING, TYPE_INFO, 
     * TYPE_ERROR, TYPE_CONFIRMATION or TYPE_ALARM
     * 
     * @param dialogType can be one of TYPE_WARNING, TYPE_INFO, 
     * TYPE_ERROR, TYPE_CONFIRMATION or TYPE_ALARM
     */
    public void setDialogType(int dialogType) {
        this.dialogType = dialogType;
    }

    /**
     * The default type for dialogs
     * 
     * @param d the default type for the dialog
     */
    public static void setDefaultDialogType(int d) {
        defaultDialogType = d;
    }

    /**
     * The default type for dialogs
     * 
     * @return the default type for the dialog
     */
    public static int getDefaultDialogType() {
        return defaultDialogType;
    }

    /**
     * Indicates whether LWUIT should try to automatically adjust a showing dialog size
     * when a screen size change event occurs
     *
     * @param a true to indicate that LWUIT should make a "best effort" to resize the dialog
     */
    public static void setAutoAdjustDialogSize(boolean a) {
        autoAdjustDialogSize = a;
    }

    /**
     * Allows a developer to indicate his interest that the dialog should no longer
     * scroll on its own but rather rely on the scrolling properties of internal
     * scrollable containers. This flag only affects the static show methods within
     * this class.
     *
     * @param d indicates whether scrolling should be active or not
     */
    public static void setDisableStaticDialogScrolling(boolean d) {
        disableStaticDialogScrolling = d;
    }

    /**
     * Allows a developer to indicate his interest that the dialog should no longer
     * scroll on its own but rather rely on the scrolling properties of internal
     * scrollable containers. This flag only affects the static show methods within
     * this class.
     *
     * @return true if scrolling should be activated, false otherwise
     */
    public static boolean isDisableStaticDialogScrolling() {
        return disableStaticDialogScrolling;
    }


    /**
     * Places commands as buttons at the bottom of the standard static dialogs rather than
     * as softbuttons. This is especially appropriate for devices such as touch devices and
     * devices without the common softbuttons (e.g. blackberries).
     * The default value is false
     *
     * @param commandsAsButtons true to place commands as buttons and not as softbutton keys
     */
    public static void setCommandsAsButtons(boolean c) {
        commandsAsButtons = c;
    }

    /**
     * Places commands as buttons at the bottom of the standard static dialogs rather than
     * as softbuttons. This is especially appropriate for devices such as touch devices and
     * devices without the common softbuttons (e.g. blackberries).
     * The default value is false
     *
     * @return true if commands are placed as buttons and not as softbutton keys
     */
    public static boolean isCommandsAsButtons() {
        return commandsAsButtons;
    }

    /**
     * This flag indicates if the dialog should be disposed if a pointer 
     * released event occurred out of the dialog content.
     * 
     * @param disposeWhenPointerOutOfBounds
     */
    protected void setDisposeWhenPointerOutOfBounds(boolean disposeWhenPointerOutOfBounds) {
        this.disposeWhenPointerOutOfBounds = disposeWhenPointerOutOfBounds;
    }

    /**
     * @inheritDoc
     */
    public void pointerReleased(int x, int y) {
        super.pointerReleased(x, y);
        if(disposeWhenPointerOutOfBounds &&
                !getTitleComponent().contains(x, y) && 
                !getContentPane().contains(x, y) && 
                !getMenuBar().contains(x, y)){
            dispose();
        }
    }
    
    
}
