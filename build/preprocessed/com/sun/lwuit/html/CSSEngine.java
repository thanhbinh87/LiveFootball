/*
 * opyright 2008 Sun Microsystems, Inc.  All Rights Reserved.
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
import com.sun.lwuit.Container;
import com.sun.lwuit.Font;
import com.sun.lwuit.Label;
import com.sun.lwuit.TextArea;
import com.sun.lwuit.geom.Dimension;
import com.sun.lwuit.layouts.BorderLayout;
import com.sun.lwuit.layouts.FlowLayout;
import com.sun.lwuit.plaf.Border;
import com.sun.lwuit.plaf.Style;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * This class is responsible for applying CSS directives to an HTMLComponent
 *
 * @author Ofir Leitner
 */
class CSSEngine {

    private static CSSEngine instance; // The instance of this singleton class
    private static Hashtable specialKeys; // A hashtable containing all recognized special key strings and their keycodes
    private Hashtable matchingFonts = new Hashtable(); // A hashtable used as a cache for quick find of matching fonts

    /**
     * A list of the attributes that can contain a URL, in order to scan them and update relative URLs to an absolute one
     */
    private static final int[] URL_ATTRIBUTES = {CSSElement.CSS_BACKGROUND_IMAGE,CSSElement.CSS_LIST_STYLE_IMAGE};

    /**
     * Denotes that the selector should be applied to the unselected style of the component
     */
    final static int STYLE_UNSELECTED=1;

    /**
     * Denotes that the selector should be applied to the selected style of the component
     */
    final static int STYLE_SELECTED=2;

    /**
     * Denotes that the selector should be applied to the pressed style of the component
     */
    final static int STYLE_PRESSED=4;

    /**
     * The indentation applied on a list when its 'list-style-position' is 'inside' vs. 'outside'
     */
    private final static int INDENT_LIST_STYLE_POSITION = 15;

    // The possible values of the 'text-transform' attribute
    private static final int TEXT_TRANSFORM_NONE = 0;
    private static final int TEXT_TRANSFORM_UPPERCASE = 1;
    private static final int TEXT_TRANSFORM_LOWERCASE = 2;
    private static final int TEXT_TRANSFORM_CAPITALIZE = 3;

    // The possible values of the '-wap-input-required' attribute
    private static final int INPUT_REQUIRED_TRUE = 0;
    private static final int INPUT_REQUIRED_FALSE = 1;

    // The possible values of the 'background-attachment' attribute
    private static final int BG_ATTACHMENT_FIXED = 0;
    private static final int BG_ATTACHMENT_SCROLL = 1;

    // The possible values of the 'white-space' attribute
    private static final int WHITE_SPACE_NORMAL = 0;
    private static final int WHITE_SPACE_PRE = 1;
    private static final int WHITE_SPACE_NOWRAP = 2;

    // The possible values of the 'display' attribute
    private static final int DISPLAY_INLINE=0;
    private static final int DISPLAY_BLOCK=1;
    private static final int DISPLAY_LIST_ITEM=2;
    private static final int DISPLAY_NONE=3;
    private static final int DISPLAY_MARQUEE=4;

    // The possible values of the 'font-variant' attribute
    private static final int FONT_VARIANT_NORMAL=0;
    private static final int FONT_VARIANT_SMALLCAPS=1;

    // The possible values of the 'list-style-position' attribute
    private static final int LIST_STYLE_POSITION_INSIDE = 0;
    private static final int LIST_STYLE_POSITION_OUTSIDE = 1;

    // The possible values of the 'border-style' attribute
    private static final int BORDER_STYLE_NONE = 0;
    private static final int BORDER_STYLE_SOLID = 1;
    private static final int BORDER_STYLE_GROOVE =2;
    private static final int BORDER_STYLE_RIDGE = 3;
    private static final int BORDER_STYLE_INSET = 4;
    private static final int BORDER_STYLE_OUTSET = 5;
    private static final int BORDER_STYLE_DOTTED = 6;
    private static final int BORDER_STYLE_DASHED = 7;
    private static final int BORDER_STYLE_DOUBLE = 8;

    // The possible values of the 'visibility' attribute
    private static final int VISIBILITY_HIDDEN=0;
    private static final int VISIBILITY_VISIBLE=1;

    /**
     * Returns the singleton instance of CSSEngine and creates it if necessary
     *
     * @return The singleton instance of CSSEngine
     */
    static CSSEngine getInstance() {
        if (instance==null) {
            instance=new CSSEngine();
        }
        return instance;
    }

    /**
     * Adds support for a special key to be used as an accesskey.
     * The CSS property -wap-accesskey supports special keys, for example "phone-send" that may have different key codes per device.
     * This method allows pairing between such keys to their respective key codes.
     * Note that these keys are valid only for -wap-aceesskey in CSS files, and not for the XHTML accesskey attribute.
     *
     * @param specialKeyName The name of the special key as denoted in CSS files
     * @param specialKeyCode The special key code
     */
    static void addSpecialKey(String specialKeyName,int specialKeyCode) {
        if (specialKeys==null) {
            specialKeys=new Hashtable();
        }
        specialKeys.put(specialKeyName,new Integer(specialKeyCode));
    }

    /**
     * Sorts the CSS directives by their specificity level
     * 
     * @param css A css vector holding CSSElements, where each element holds CSS selectors as its children
     * @return a flat vector containing CSS selectors, sorted by specificity
     */
    private CSSElement[] sortSelectorsBySpecificity(CSSElement[] css) {
        Vector sortedSelectors=new Vector();

        for(int s=0;s<css.length;s++) {
            CSSElement cssRoot=css[s];
            String cssPageURL=cssRoot.getAttributeById(CSSElement.CSS_PAGEURL);
            DocumentInfo cssDocInfo=null;
            if (cssPageURL!=null) {
                cssDocInfo=new DocumentInfo(cssPageURL);
            }
            for(int iter = 0 ; iter < cssRoot.getNumChildren() ; iter++) {
                CSSElement currentSelector = cssRoot.getCSSChildAt(iter);
                if (cssPageURL!=null) { // Since with external CSS pages, the base URL is that of the CSS file and not of the HTML document, we have to convert relative image URLs to absolute URLs
                    for(int i=0;i<URL_ATTRIBUTES.length;i++) {
                        String imageURL=getCSSUrl(currentSelector.getAttributeById(URL_ATTRIBUTES[i]));
                        if (imageURL!=null) {
                            imageURL=cssDocInfo.convertURL(imageURL);
                            currentSelector.addAttribute(currentSelector.getAttributeName(new Integer(URL_ATTRIBUTES[i])), "url("+imageURL+")");
                        }
                    }
                }
                int i=0;
                int specificity=currentSelector.getSelectorSpecificity(); //Note that it is important to get the specificity outside the loop, so it will necessarily get called (triggering the cal)
                while((i<sortedSelectors.size()) && (specificity>=((CSSElement)sortedSelectors.elementAt(i)).getSelectorSpecificity())) {
                    i++;
                }
                sortedSelectors.insertElementAt(currentSelector, i);
            }
        }

        css = new CSSElement[sortedSelectors.size()];
        for(int i=0;i<sortedSelectors.size();i++) {
            css[i]=(CSSElement)sortedSelectors.elementAt(i);
        }

        return css;
    }

    /**
     * Applies all CSS directives to the given document and HTMLComponent, including external CSS files, embedded CSS segments and inline CSS (Style attribute)
     * This is called by HTMLComponent after the document was fully parsed and all external CSS have been retrieved.
     * This method actually initializes a sorted CSS array to be used by the recursive private applyCSS method.
     *
     * @param document The HTML document to apply the CSS on
     * @param htmlC The HTMLComponent to apply the CSS on
     * @param externalCSS A vector containing CSSElelemnts each being the root of external CSS file (1 per file)
     * @param embeddedCSS A vector containing CSSElelemnts each being the root of embedded CSS segments (1 per segment)
     */
    void applyCSS(Element document,HTMLComponent htmlC,Vector externalCSS,Vector embeddedCSS) {
        int externalSize=0;
        int embeddedSize=0;
        
        if (externalCSS!=null) {
            externalSize=externalCSS.size();
        }
        if (embeddedCSS!=null) {
            embeddedSize=embeddedCSS.size();
        }

        CSSElement[] css = new CSSElement[externalSize+embeddedSize];
        for(int i=0;i<externalSize;i++) {
            css[i]=(CSSElement)externalCSS.elementAt(i);
        }
        for(int i=0;i<embeddedSize;i++) {
            css[i+externalSize]=(CSSElement)embeddedCSS.elementAt(i);
        }

        css=sortSelectorsBySpecificity(css);
        applyCSS(document, htmlC, css, null);
    }
    
    /**
     * A recursive method that tries to match all CSS selectors with the specified element
     *
     * @param element The specific element in the document to apply the CSS on
     * @param htmlC The HTMLComponent to apply the CSS on
     * @param css An array containing selectors sorted by specificity from all the external CSS files and then the embedded CSS segments
     * @param nestedSelectors A vector containing nested selectors, or null if none
     */
    private void applyCSS(Element element,HTMLComponent htmlC,CSSElement[] css,Vector nestedSelectors) { //Vector styleAttributes
        String id=element.getAttributeById(Element.ATTR_ID);
        String className=element.getAttributeById(Element.ATTR_CLASS);

        Vector nextNestedSelectors=new Vector();
        for (int e=0;e<css.length;e++) {
            CSSElement currentSelector = css[e];
            checkSelector(currentSelector, element, htmlC, className, id,nextNestedSelectors);
        }

        if (nestedSelectors!=null) {
            for (Enumeration e=nestedSelectors.elements();e.hasMoreElements();) {
                CSSElement currentSelector=(CSSElement)e.nextElement();
                checkSelector(currentSelector, element, htmlC, className, id,nextNestedSelectors);
            }
        }

        if (nextNestedSelectors.size()==0) {
            nextNestedSelectors=null;
        }

        String styleStr=element.getAttributeById(Element.ATTR_STYLE);
        if (styleStr!=null) {
                CSSElement style=null;
                styleStr="{"+styleStr+"}"; // So it will be parsed correctly
                try {
                    style = Parser.getInstance().parseCSS(new InputStreamReader(new ByteArrayInputStream(styleStr.getBytes())),htmlC);
                    applyStyle(element, style, htmlC);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
        }

        for(int i=0;i<element.getNumChildren();i++) {
            applyCSS(element.getChildAt(i), htmlC,css,nextNestedSelectors);
        }
    }
    
    /**
     * Checks if the given selector matches the given element either by its tag name, class name or id.
     * If there's a match but the selector has children, it means that it is a nested selector, and thus 
     * its only child is added to the nested selectors vector to be checked against the children of this element in the next recursion of applyCSS
     * 
     * @param currentSelector The current CSS selector to check
     * @param element The element to check
     * @param htmlC The HTMLComponent
     * @param className The element's class name (Can be derived from element but since this method is called a lot it is extracted before and sent as a parameter)
     * @param id The element's id (Same comment as in className)
     * @param nextNestedSelectors A vector containing the nested selectors
     */
    private void checkSelector(CSSElement currentSelector,Element element,HTMLComponent htmlC,String className,String id,Vector nextNestedSelectors) {
        if (((currentSelector.getSelectorTag()==null) || (currentSelector.getSelectorTag().equalsIgnoreCase(element.getName()))) &&
            ((currentSelector.getSelectorClass()==null) || (containsClass(className,currentSelector.getSelectorClass()))) &&
            ((currentSelector.getSelectorId()==null) || (currentSelector.getSelectorId().equalsIgnoreCase(id)))) {
                if (currentSelector.getNumChildren()==0) {
                    if ((element.getId()!=Element.TAG_A) ||
                        ((currentSelector.getSelectorPseudoClass() & (CSSElement.PC_LINK+CSSElement.PC_VISITED))==0) || // not link/visited (but can be active/focus)
                        (!(element.getUi().firstElement() instanceof HTMLLink)) ||
                        ((!((HTMLLink)element.getUi().firstElement()).linkVisited) && ((currentSelector.getSelectorPseudoClass() & CSSElement.PC_LINK)!=0)) ||
                        (((HTMLLink)element.getUi().firstElement()).linkVisited) && ((currentSelector.getSelectorPseudoClass() & CSSElement.PC_VISITED)!=0)) {
                        applyStyle(element, currentSelector,htmlC);
                    }
                } else {
                    nextNestedSelectors.addElement(currentSelector.getChildAt(0));

                    // Check if this is a Descendant selector (i.e. div * b - which means match any b that is the descendant (grandchild and on) of div
                    // If so then we pass not only the child selector (i.e. the b) but also the "* b" to allow matching later decendants
                    if ((currentSelector.getSelectorTag()==null) && (currentSelector.getSelectorId()==null) && (currentSelector.getSelectorClass()==null)) {
                        nextNestedSelectors.addElement(currentSelector);
                    }
                }
        }
    }

    /**
     * Checks if the specified class is contained in the specified text
     * This is used for elements that have several classes i.e. class="class1 class2"
     * Note: A simple indexOf could not be used since we need to find whole words and not frgaments of words
     *
     * @param selectorClass The text
     * @param elementClass The word to find in the text
     * @return true if the word is found, false otherwise
     */
    private boolean containsClass(String elementClass,String selectorClass) {
        if ((elementClass==null) || (selectorClass==null)) {
            return false;
        }
        // The spaces addition is to make sure we get a whole word and not a fragment of a word
        elementClass=" "+elementClass+" ";
        
        // Selector can require multiple classes, i.e. class.1class2 (which needs to match to "class 1 class2" and "class2 class1" and also "class1 otherclasses class2"
        int dotIndex=selectorClass.indexOf('.');
        while (dotIndex!=-1) {
            String curWord=selectorClass.substring(0, dotIndex);
            if (elementClass.indexOf(" "+curWord+" ")==-1) {
                return false;
            }
            selectorClass=selectorClass.substring(dotIndex+1);
            dotIndex=selectorClass.indexOf('.');
        }

        return (elementClass.indexOf(" "+selectorClass+" ")!=-1);
    }

    /**
     * Applies the given style attributes to the HTML DOM entry
     *
     * @param element The element to apply the style to
     * @param selector The selector containing the style directives
     * @param htmlC The HTMLComponent
     */
    private void applyStyle(Element element, CSSElement selector, HTMLComponent htmlC) {
        if(element.getUi() != null) {
            for(int iter = 0 ; iter < element.getUi().size() ; iter++) {
                Object o = element.getUi().elementAt(iter);
                if(o instanceof Component) {
                    final Component cmp = (Component)o;
                        applyStyleToUIElement(cmp, selector,element,htmlC);
                }
            }
        }
    }

    /**
     * Returns a mask of the STYLE_* constants of which LWUIT styles this selector should be applied to
     *
     * @param cmp The component in question
     * @param selector The selector
     * @return a mask of the STYLE_* constants of which LWUIT styles this selector should be applied to
     */
    private int getApplicableStyles(Component cmp,CSSElement selector) {
        int result=0;
        if (cmp instanceof HTMLLink) {
            int pseudoClass=selector.getSelectorPseudoClass();
            boolean done=false;
            if ((pseudoClass & CSSElement.PC_FOCUS)!=0) { // Focused (i.e. CSS focus/hover)
                result|=STYLE_SELECTED;
                done=true;
            }
            if ((pseudoClass & CSSElement.PC_ACTIVE)!=0) { // active in CSS means pressed in LWUIT
                result|=STYLE_PRESSED;
                done=true;
            }

            if (!done) {
                result|=STYLE_SELECTED|STYLE_UNSELECTED;
            }
        } else {
                result|=STYLE_SELECTED|STYLE_UNSELECTED;
        }
        return result;
    }

    /**
     * Sets the specified color as the foreground color of the component and all its children
     * 
     * @param cmp The component to work on
     * @param color The color to set
     * @param selector The selector with the color directive
     */
    private void setColorRecursive(Component cmp,int color,CSSElement selector) {
        int styles=getApplicableStyles(cmp, selector);
        if ((styles & STYLE_UNSELECTED)!=0) {
            cmp.getUnselectedStyle().setFgColor(color);
        }
        if ((styles & STYLE_SELECTED)!=0) {
            cmp.getSelectedStyle().setFgColor(color);
        }
        if ((styles & STYLE_PRESSED)!=0) {
            ((HTMLLink)cmp).getPressedStyle().setFgColor(color);
        }

        if (cmp instanceof Container) {
            Container cont=(Container)cmp;
            for(int i=0;i<cont.getComponentCount();i++) {
                if (!(cont.getComponentAt(i) instanceof HTMLLink)) { // A link color is a special case, it is not inherited and applied only if the selector selects the link directly
                    setColorRecursive(cont.getComponentAt(i), color,selector);
                }
            }
        }
    }

    /**
     * Sets the font of the component and all its children to the closest font that can be found according to the specified properties
     * 
     * @param htmlC The HTMLComponent this component belongs to (For the available bitmap fonts table)
     * @param cmp The component to work on
     * @param fontFamily The font family
     * @param fontSize The font size in pixels
     * @param fontStyle The font style - either Font.STYLE_PLAIN or Font.STYLE_ITALIC
     * @param fontWeight The font weight - either Font.STYLE_PLAIN ot Font.STYLE_BOLD
     * @param selector The selector with the font directive
     */
    private void setFontRecursive(HTMLComponent htmlC, Component cmp,String fontFamily,int fontSize,int fontStyle,int fontWeight,CSSElement selector) {
        if (cmp instanceof Container) {
            Container cont=(Container)cmp;
            for(int i=0;i<cont.getComponentCount();i++) {
                setFontRecursive(htmlC,cont.getComponentAt(i), fontFamily,fontSize,fontStyle,fontWeight,selector);
            }
        } else if (cmp instanceof Label) {
            setMatchingFont(htmlC, cmp, fontFamily, fontSize, fontStyle, fontWeight,selector);
        }
    }

    /**
     * Usually we don't have to set visibility in a recursive manner, i.e. suffices to set a top level container as invisible and all its contents are invisible.
     * However, in CSS it is possible that a top level element has visibility:hidden and some child of his has visibility:visible, and then what we do
     * is use the setVisibleParents to make sure all containers containing this child are visible.
     * But since other child components still need to be invsibile - we make sure that all are invisible with this method.
     *
     * @param cmp The component to set visibility on
     * @param visible true to set visible and enabled, false otherwise
     */
    private void setVisibleRecursive(Component cmp,boolean visible) {
        cmp.setEnabled(visible);
        cmp.setVisible(visible);
        if (cmp instanceof Container) {
            Container cont=(Container)cmp;
            for(int i=0;i<cont.getComponentCount();i++) {
                setVisibleRecursive(cont.getComponentAt(i), visible);
            }
        }
    }

    // TODO - This is a problematic implementation since if a text has been converted to UPPERCASE and then due to a child's style attribute it has to change back to none/capitalize - there's no way to restore the original text.
    // Also it has a problem with FIXED_WIDTH mode since when uppercasing for example, labels will grow in size which will take some of them out of the screen, The correct way is working on the elements and not the text, and reconstruct the labels
    /**
     * Sets the specified text transform to the component and all its children
     * 
     * @param cmp The component to work on
     * @param transformType The text transform type, one of the TEXT_TRANSFORM_* constants
     */
    private void setTextTransformRecursive(Component cmp,int transformType) {
        if (cmp instanceof Container) {
            Container cont=(Container)cmp;
            for(int i=0;i<cont.getComponentCount();i++) {
                setTextTransformRecursive(cont.getComponentAt(i), transformType);
            }
        } else if (cmp instanceof Label) {
            Label label=(Label)cmp;
            switch(transformType) {
                case TEXT_TRANSFORM_UPPERCASE:
                    label.setText(label.getText().toUpperCase());
                    break;
                case TEXT_TRANSFORM_LOWERCASE:
                    label.setText(label.getText().toLowerCase());
                    break;
                case TEXT_TRANSFORM_CAPITALIZE:

                    String text=label.getText();

                    String newText="";
                    boolean capNextLetter=true;
                    for(int i=0;i<text.length();i++) {
                        char c=text.charAt(i);
                        if (Parser.isWhiteSpace(c)) {
                            capNextLetter=true;
                        } else if (capNextLetter) {
                            if ((c>='a') && (c<='z')) {
                                c-=32; // 'A' is ASCII 65, and 'a' is ASCII 97, difference: 32
                            }
                            capNextLetter=false;
                        }
                        newText+=c;
                    }
                    label.setText(newText);
                    break;
            }
        }

    }

    /**
     * Sets the alignment of the component and all its children according to the given alignment
     * 
     * @param cmp The component to set the alignment on
     * @param align The alignment - one of left,center,right
     */
    private void setTextAlignmentRecursive(Component cmp,int align) {
        if (cmp instanceof Container) {
            Container cont=(Container)cmp;
            if (cont.getLayout() instanceof FlowLayout) {
                cont.setLayout(new FlowLayout(align));
            }
            for(int i=0;i<cont.getComponentCount();i++) {
                setTextAlignmentRecursive(cont.getComponentAt(i), align);
            }
        } else if ((HTMLComponent.FIXED_WIDTH) && (cmp instanceof Label)) { // In FIXED_WIDTH mode labels are aligned by appling alignment on themselves and enlarging the label size to take the whole width of the screen
            ((Label)cmp).setAlignment(align);
        }
    }

    
    /**
     * Sets the given text indentation to the component and all its children
     * Note: This doesn't really work well with HTMLComponent.FIXED_WIDTH mode since labels there are not single words but rather the whole line, so they get pushed out of the screen
     * 
     * @param cmp The component to set the indentation on
     * @param indent The indentation in pixels
     */
    private void setTextIndentationRecursive(Component cmp,int indent) {
        if (cmp instanceof Container) {
            Container cont=(Container)cmp;
            if ((cont.getLayout() instanceof FlowLayout) && (cont.getComponentCount()>0)) {
                // Note that we don't need to consider the "applicable" styles, as this is a container and will always return selected+unselected
                cont.getComponentAt(0).getUnselectedStyle().setMargin(Component.LEFT, indent);
                cont.getComponentAt(0).getSelectedStyle().setMargin(Component.LEFT, indent);
            }
            for(int i=0;i<cont.getComponentCount();i++) {
                setTextIndentationRecursive(cont.getComponentAt(i), indent);
            }
        }
    }

    /**
     * Turns on the visibilty of all ancestors of the given component
     * 
     * @param cmp The component to work on
     */
    private void setParentsVisible(Component cmp) {
        Container cont=cmp.getParent();
        while (cont!=null) {
            cont.setVisible(true);
            cont=cont.getParent();
        }
    }

    /**
     * Replaces an unwrapped text with a wrapped version, while copying the style of the original text.
     * 
     * @param label The current label that contains the unwrapped text
     * @param words A vector containing one word of the text (without white spaces) in each element
     * @param element The text element
     */
    private void setWrapText(Label label,Vector words,Element element,HTMLComponent htmlC) {
        Style selectedStyle=label.getSelectedStyle();
        Style unselectedStyle=label.getUnselectedStyle();
        Vector ui=new Vector();
        label.setText((String)words.elementAt(0)+' ');
        HTMLLink link=null;
        if (label instanceof HTMLLink) {
            link=(HTMLLink)label;
        }
        ui.addElement(label);
        for(int i=1;i<words.size();i++) {
            Label word=null;
            if (link!=null) {
                word=new HTMLLink((String)words.elementAt(i)+' ',link.link,htmlC,link,link.linkVisited);
            } else {
                word=new Label((String)words.elementAt(i)+' ');
            }
            word.setSelectedStyle(selectedStyle);
            word.setUnselectedStyle(unselectedStyle);
            label.getParent().addComponent(word);
            ui.addElement(word);
        }
        element.setAssociatedComponents(ui);
        label.getParent().revalidate();
    }


    /**
     * Sets this element and all children to have wrapped text.
     * In cases where text is already wrapped no change will be made.
     * This will work only in FIXED_WIDTH mode (Checked before called)
     * Technically all this logic can be found in HTMLComponent.showText, but since we don't want to get into
     * the context of this element (i.e. what was the indentation, alignment etc.), we use this algorithm.
     * 
     * @param element The element to apply text wrapping on
     * @param htmlC The HTMLComponent
     */
    private void setWrapRecursive(Element element,HTMLComponent htmlC) {
        if (element.getId()==Element.TAG_TEXT) {
            String text=element.getAttributeById(Element.ATTR_TITLE);
            final Vector ui=element.getUi();
            if ((text!=null) && (ui!=null) && (ui.size()==1)) { //If it's already wrapped, no need to process
                final Vector words=htmlC.getWords(text, Component.LEFT, false);
                final Label label=(Label)ui.elementAt(0);
                setWrapText(label, words, element,htmlC);
            }
        }

        for(int i=0;i<element.getNumChildren();i++) {
            setWrapRecursive(element.getChildAt(i),htmlC);
        }

    }

    /**
     * Replaces a wrapped text with an unwrapped version.
     * This in fact removes all the labels that contains a single word each, and replaces them with one label that contains the whole text.
     * This way the label is not wrapped.
     *
     * @param label The first label of this text element (can be derived from ui but already fetched before the call)
     * @param ui The vector consisting all components (labels) that contain the text's words
     * @param newText The new text that should replace current components (unwrapped text without extra white spaces)
     * @param element The text element
     */
    private void setNowrapText(Label label,Vector ui,String newText,Element element) {
        label.setText(newText);
        for(int i=1;i<ui.size();i++) {
            Component cmp=(Component)ui.elementAt(i);
            cmp.getParent().removeComponent(cmp);
        }
        if (label instanceof HTMLLink) {
            ((HTMLLink)label).childLinks=new Vector(); // Reset all associated link fragments so we don't have unneeded references
        }
        element.setAssociatedComponents(label);
        label.getParent().revalidate();
    }

    /**
     * Sets this element and all children to have unwrapped text.
     * In cases where text is already unwrapped no change will be made.
     * This will work only in FIXED_WIDTH mode (Checked before called)
     * Technically a lot of this logic can be found in HTMLComponent, but since we don't want to get into
     * the context of this element (i.e. what was the indentation, alignment etc.), we use this algorithm.
     *
     * @param element The element to apply text wrapping on
     */
    private void setNowrapRecursive(final Element element) {
        if (element.getId()==Element.TAG_TEXT) {
            String text=element.getAttributeById(Element.ATTR_TITLE);
            final Vector ui=element.getUi();
            if ((text!=null) && (ui!=null) && (ui.size()>1)) { //If it's just one word or already no-wrapped, no need to process
                String word="";
                String newText="";
                for(int c=0;c<text.length();c++) {
                    char ch=text.charAt(c);
                    if ((ch==' ') || (ch==10) || (ch==13) || (ch=='\t') || (ch=='\n')) {
                        if (!word.equals("")) {
                            newText+=word+" ";
                            word="";
                        }
                    } else {
                        word+=ch;
                    }
                }
                if (!word.equals("")) {
                    newText+=word+" ";
                }

                final Label label=(Label)ui.elementAt(0);
                setNowrapText(label, ui, newText, element);
            }
        }

        for(int i=0;i<element.getNumChildren();i++) {
            setNowrapRecursive(element.getChildAt(i));
        }

        element.recalcUi(); // If children elements' UI was changed, we need to recalc the UI of the parent 

    }

    /**
     * Applies the given CSS directives to the component
     * 
     * @param ui The component representing (part of) the element that the style should be applied to
     * @param selector The style attributes relating to this element
     * @param element The element the style should be applied to
     * @param htmlC The HTMLComponent to which this element belongs to
     * @param focus true if the style should be applied only to the selected state iof the ui (a result of pseudo-class selector a:focus etc.)
     */
    private void applyStyleToUIElement(Component ui, CSSElement selector,Element element,HTMLComponent htmlC) {
        
        int styles=getApplicableStyles(ui, selector); // This is relevant only for non recursive types - otherwise we need to recheck everytime since it depends on the specific UI component class

        // White spaces
        if (!HTMLComponent.FIXED_WIDTH) { // This works well only in non-fixed width mode
            int space=selector.getAttrVal(CSSElement.CSS_WHITE_SPACE);

            if (space!=-1) {
                switch(space) {
                    case WHITE_SPACE_NORMAL:
                        setWrapRecursive(element, htmlC);
                        break;
                    case WHITE_SPACE_NOWRAP:
                        setNowrapRecursive(element);
                        break;
                    case WHITE_SPACE_PRE:
                        // TODO - Not implemented yet
                        break;
                }
            }
        }

        // Input format
        String v=selector.getAttributeById(CSSElement.CSS_WAP_INPUT_FORMAT);
        if ((v!=null) && ((element.getId()==Element.TAG_TEXTAREA) || (element.getId()==Element.TAG_INPUT)) && (ui instanceof TextArea)) {
            v=omitQuotesIfExist(v);
            ui=htmlC.setInputFormat((TextArea)ui, v); // This may return a new instance of TextField taht has to be updated in the tree. This is alos the reason why input format is the first thing checked - see HTMLInputFormat.applyConstraints
            element.setAssociatedComponents(ui);
        }

        // Input emptyOK
        int inputRequired=selector.getAttrVal(CSSElement.CSS_WAP_INPUT_REQUIRED);
        if ((inputRequired!=-1) && ((element.getId()==Element.TAG_TEXTAREA) || (element.getId()==Element.TAG_INPUT)) && (ui instanceof TextArea)) {
            if (inputRequired==INPUT_REQUIRED_TRUE) {
                htmlC.setInputRequired(((TextArea)ui), true);
            } else if (inputRequired==INPUT_REQUIRED_FALSE) {
                htmlC.setInputRequired(((TextArea)ui), false);
            }
        }

        // Display
        int disp=selector.getAttrVal(CSSElement.CSS_DISPLAY);
        switch(disp) {
            case DISPLAY_NONE:
                    if (ui.getParent()!=null) {
                        ui.getParent().removeComponent(ui);
                    } else { //special case for display in the BODY tag
                        ((Container)ui).removeAll();
                    }
                    return;
            case DISPLAY_MARQUEE: // Animate component (ticker-like)
                htmlC.marqueeComponents.addElement(ui);
                break;
           //TODO - support also: block, inline and list-item (All mandatory in WCSS)
        }

        // Visibility
        int visibility=selector.getAttrVal(CSSElement.CSS_VISIBILITY);
        if (visibility!=-1) {
            boolean visible=(visibility==VISIBILITY_VISIBLE);
            setVisibleRecursive(ui,visible);
            if (!visible) {
                return; // Don't waste time on processing hidden elements, though technically the size of the element is still reserved and should be according to style
            } else {
                setParentsVisible(ui); // Need to turn on visibility of all component's parents, in case they were declared hidden
            }
        }

        //
        // Dimensions
        //

        // TODO - debug: Width and Height don't always work - for simple components they usually do, but for containers either they don't have any effect or some inner components (with size restrictions) disappear
        // Width
        int width=selector.getAttrLengthVal(CSSElement.CSS_WIDTH,ui,htmlC.getWidth());

        // Height
        int height=selector.getAttrLengthVal(CSSElement.CSS_HEIGHT,ui,htmlC.getHeight());

        if ((width!=-1) || (height!=-1)) {
            if (width==-1) {
                width=ui.getPreferredW();
            }
            if (height==-1) {
                height=ui.getPreferredH();
            }
            ui.setPreferredSize(new Dimension(width,height));
        }

        //
        // Colors
        //
        
        // Background Color
        int bgColor=selector.getAttrVal(CSSElement.CSS_BACKGROUND_COLOR);
        if (bgColor!=-1)  {
                if ((styles & STYLE_UNSELECTED)!=0) {
                    ui.getUnselectedStyle().setBgColor(bgColor);
                    ui.getUnselectedStyle().setBgTransparency(255);

                }
                if (((styles & STYLE_SELECTED)!=0) 
                        && ((!(ui instanceof HTMLLink)) || (element.getUi().firstElement()==ui))) { // Since background color is used to identify the currently selected link, if it wasn't specified specifically on the link object (But rather in an ancestor of this element) we don't color it
                    ui.getSelectedStyle().setBgColor(bgColor);
                    ui.getSelectedStyle().setBgTransparency(255);
                }
                if ((styles & STYLE_PRESSED)!=0) {
                    ((HTMLLink)ui).getPressedStyle().setBgColor(bgColor);
                    ((HTMLLink)ui).getPressedStyle().setBgTransparency(255);
                }

        }

        // Foreground color
        int fgColor=selector.getAttrVal(CSSElement.CSS_COLOR);
        if ((fgColor != -1) 
                && ((!(ui instanceof HTMLLink)) || (element.getUi().firstElement()==ui))) { // Colors are applied on links only if they were directly set on the link element
            setColorRecursive(ui, fgColor,selector);
        }
        
        // Background Image
        v = selector.getAttributeById(CSSElement.CSS_BACKGROUND_IMAGE);
        if (v!=null) {
            String url=getCSSUrl(v);

            if (url!=null) {

                // Setting an alternative bgPainter that can support CSS background properties
                CSSBgPainter bgPainter=new CSSBgPainter(ui);

                // Background tiling
                byte bgType = (byte)selector.getAttrVal(CSSElement.CSS_BACKGROUND_REPEAT);
                if (bgType==-1) {
                    bgType=Style.BACKGROUND_IMAGE_TILE_BOTH; // default value
                }

                // Note that we don't set transparency to 255, since the image may have its own transparency/opaque areas - we don't want to block the entire component/container entirely
                if ((styles & STYLE_SELECTED)!=0) {
                    ui.getSelectedStyle().setBgPainter(bgPainter);
                    ui.getSelectedStyle().setBackgroundType(bgType);
                }
                if ((styles & STYLE_UNSELECTED)!=0) {
                    ui.getUnselectedStyle().setBgPainter(bgPainter);
                    ui.getUnselectedStyle().setBackgroundType(bgType);
                }
                if ((styles & STYLE_PRESSED)!=0) {
                    ((HTMLLink)ui).getPressedStyle().setBgPainter(bgPainter);
                    ((HTMLLink)ui).getPressedStyle().setBackgroundType(bgType);
                }

                // The background image itself
                if (htmlC.showImages) {
                    if (htmlC.getDocumentInfo()!=null) {
                        htmlC.getThreadQueue().addBgImage(ui,htmlC.convertURL(url),styles);
                    } else {
                        if (DocumentInfo.isAbsoluteURL(url)) {
                            htmlC.getThreadQueue().addBgImage(ui,url,styles);
                        } else {
                            if (htmlC.getHTMLCallback()!=null) {
                                htmlC.getHTMLCallback().parsingError(HTMLCallback.ERROR_NO_BASE_URL, selector.getName() , selector.getAttributeName(new Integer(CSSElement.CSS_BACKGROUND_IMAGE)),url,"Ignoring background image file referred in a CSS file/segment ("+url+"), since page was set by setBody/setHTML so there's no way to access relative URLs");
                            }
                        }
                    }
                }

                for(int i=CSSElement.CSS_BACKGROUND_POSITION_X;i<=CSSElement.CSS_BACKGROUND_POSITION_Y;i++) {
                    int pos=selector.getAttrVal(i);
                    if (pos!=-1) {
                        bgPainter.setPosition(i,pos);
                    }
                }

                // Background attachment: Either 'fixed' (i.e. the bg image is fixed in its position even when scrolling)
                // or 'scroll' (default) which means the it moves with scrolling (Like usually in LWUIT backgrounds)
                if (selector.getAttrVal((CSSElement.CSS_BACKGROUND_ATTACHMENT))==BG_ATTACHMENT_FIXED) {
                       bgPainter.setFixed();
                }

            }
        }

        // TODO - float: none/left/right
        // TODO - clear: none/left/right/both

        // Margin
        Component marginComp=ui;
        if (ui instanceof Label) { // If this is a Label/HTMLLink we do not apply the margin individually to each word, but rather to the whole block
            marginComp=ui.getParent();
        } else if ((element.getId()==Element.TAG_LI) && (ui.getParent().getLayout() instanceof BorderLayout)) {
            marginComp=ui.getParent();
        }
        for(int i=CSSElement.CSS_MARGIN_TOP;i<=CSSElement.CSS_MARGIN_RIGHT;i++) {
            int marginPixels=-1;

            if ((i==CSSElement.CSS_MARGIN_TOP) || (i==CSSElement.CSS_MARGIN_BOTTOM)) {
                marginPixels=selector.getAttrLengthVal(i, ui, htmlC.getHeight()); // Here the used component is ui and not marginComp, since we're interested in the font size (which will be corrent in Labels not in their containers)
            } else {
                marginPixels=selector.getAttrLengthVal(i, ui, htmlC.getWidth());
            }
            if (marginPixels!=-1) {

                if ((styles & STYLE_SELECTED)!=0) {
                    marginComp.getSelectedStyle().setMargin(i-CSSElement.CSS_MARGIN_TOP, marginPixels);
                    // If this is a link and the selector applies only to Selected, it means this is an 'a:focus'
                    // Since the marginComp is not focusable (as it is the container holding the link), HTMLLink takes care of focusing the
                    // parent when the link focuses
                    if ((ui instanceof HTMLLink) && (styles==STYLE_SELECTED)) {
                        ((HTMLLink)ui).setParentChangesOnFocus();
                    }
                }
                if ((styles & STYLE_UNSELECTED)!=0) {
                    marginComp.getUnselectedStyle().setMargin(i-CSSElement.CSS_MARGIN_TOP, marginPixels);
                    
                }
                // Since we don't apply the margin/padding on the component but rather on its parent
                // There is no point in setting the PRESSED style since we don't have a pressed event from Button, nor do we have a pressedStyle for containers
                // That's why we can't do the same trick as in selected style, and the benefit of this rather "edge" case (That is anyway not implemented in all browsers) seems rather small
                //    if ((styles & STYLE_PRESSED)!=0) {
                //        ((HTMLLink)ui).getPressedStyle().setMargin(i-CSSElement.CSS_MARGIN_TOP, marginPixels);
                //    }
            }
        }

        Component padComp=ui;
        if (ui instanceof Label) {
            padComp=ui.getParent();
        } else if ((element.getId()==Element.TAG_LI) && (ui.getParent().getLayout() instanceof BorderLayout)) {
            padComp=ui.getParent();
        }

        for(int i=CSSElement.CSS_PADDING_TOP;i<=CSSElement.CSS_PADDING_RIGHT;i++) {
            int padPixels=-1;

            if ((i==CSSElement.CSS_PADDING_TOP) || (i==CSSElement.CSS_PADDING_BOTTOM)) {
                padPixels=selector.getAttrLengthVal(i, ui, htmlC.getHeight());
            } else {
                padPixels=selector.getAttrLengthVal(i, ui, htmlC.getWidth());
            }
            if (padPixels!=-1) {
                if ((styles & STYLE_SELECTED)!=0) {
                    padComp.getSelectedStyle().setPadding(i-CSSElement.CSS_PADDING_TOP, padPixels);
                    if ((ui instanceof HTMLLink) && (styles==STYLE_SELECTED)) { // See comment on margins
                        ((HTMLLink)ui).setParentChangesOnFocus();
                    }
                }
                if ((styles & STYLE_UNSELECTED)!=0) {
                    padComp.getUnselectedStyle().setPadding(i-CSSElement.CSS_PADDING_TOP, padPixels);
                }
                // See comment in margin on why PRESSED was dropped
                //    if ((styles & STYLE_PRESSED)!=0) {
                //        ((HTMLLink)padComp).getPressedStyle().setPadding(i-CSSElement.CSS_PADDING_TOP, padPixels);
                //    }
            }
        }

        //
        // Text
        //
        
        // Text Alignment
        int align=selector.getAttrVal(CSSElement.CSS_TEXT_ALIGN);
        if (align!=-1) {
            switch(element.getId()) {
                case Element.TAG_TD:
                case Element.TAG_TH:
                    setTableCellAlignment(element, ui, align, true);
                    break;
                case Element.TAG_TR:
                    setTableCellAlignmentTR(element, ui, align, true);
                    break;
                case Element.TAG_TABLE:
                    setTableAlignment(ui, align, true);
                    break;
                default:
                    setTextAlignmentRecursive(ui, align); // TODO - this sometimes may collide with the HTML align attribute. If the style of the same tag has alignment it overrides the align attribute, but if it is inherited, the align tag prevails
            }
        }

        // Vertical align
            int valign=selector.getAttrVal(CSSElement.CSS_VERTICAL_ALIGN);
            if (valign!=-1) {
            switch(element.getId()) {
                case Element.TAG_TD:
                case Element.TAG_TH:
                    setTableCellAlignment(element, ui, valign, false);
                    break;
                case Element.TAG_TR:
                    setTableCellAlignmentTR(element, ui, valign, false);
                    break;
//                case Element.TAG_TABLE: // vertical alignment denoted in the table tag doesn't affect it in most browsers
//                     setTableAlignment(element, ui, valign, false);
//                     break;
                default:
                   //TODO - implement vertical alignment for non-table elements
            }
        }

        // Text Transform
        int transform=selector.getAttrVal(CSSElement.CSS_TEXT_TRANSFORM);
        if (transform!=-1) {
            setTextTransformRecursive(ui, transform);
        }

        // Text indentation
        int indent=selector.getAttrLengthVal(CSSElement.CSS_TEXT_INDENT, ui, htmlC.getWidth());
        if (indent!=-1) {
            setTextIndentationRecursive(ui, indent);
        }

        //
        // Font
        //
        
        // Font family
        String fontFamily=selector.getAttributeById(CSSElement.CSS_FONT_FAMILY);
        if (fontFamily!=null) {
            int index=fontFamily.indexOf(',');
            if (index!=-1) { // Currently we ignore font families fall back (i.e. Arial,Helvetica,Sans-serif) since even finding a match for one font is quite expensive performance-wise
                fontFamily=fontFamily.substring(0, index);
            }
        }
        // Font Style
        int fontStyle=selector.getAttrVal(CSSElement.CSS_FONT_STYLE);


        // Font Weight
        int fontWeight=selector.getAttrVal(CSSElement.CSS_FONT_WEIGHT);

        int fontSize=selector.getAttrLengthVal(CSSElement.CSS_FONT_SIZE,ui, ui.getStyle().getFont().getHeight());
        if (fontSize<-1) {
            int curSize=ui.getStyle().getFont().getHeight();
            if (fontSize==CSSElement.FONT_SIZE_LARGER) {
                fontSize=curSize+2;
            } else if (fontSize==CSSElement.FONT_SIZE_SMALLER) {
                fontSize=curSize-2;
            }
        }

        // Since LWUIT/J2ME doesn't support small-caps fonts, when a small-caps font varinat is requested
        // the font-family is changed to "smallcaps" which should be loaded to HTMLComponent and the theme as a bitmap font
        // If no smallcaps font is found at all, then the family stays the same, but if even only one is found - the best match will be used.
        int fontVariant=selector.getAttrVal(CSSElement.CSS_FONT_VARIANT);
        if ((fontVariant==FONT_VARIANT_SMALLCAPS) && (htmlC.isSmallCapsFontAvailable())) {
            fontFamily=CSSElement.SMALL_CAPS_STRING;
        }

        // Process font only if once of the font CSS properties was mentioned and valid
        if ((fontFamily!=null) || (fontSize!=-1) || (fontStyle!=-1) || (fontWeight!=-1)) {
            setFontRecursive(htmlC, ui, fontFamily, fontSize, fontStyle, fontWeight,selector);
        }

        // List style
        int listType=-1;

        String listImg=null;
        Component borderUi=ui;

        if ((element.getId()==Element.TAG_LI) || (element.getId()==Element.TAG_UL) || (element.getId()==Element.TAG_OL)) {
            int listPos=selector.getAttrVal(CSSElement.CSS_LIST_STYLE_POSITION);
            if (listPos==LIST_STYLE_POSITION_INSIDE) {
                // Padding and not margin since background color should affect also the indented space
                ui.getStyle().setPadding(Component.LEFT, ui.getStyle().getMargin(Component.LEFT)+INDENT_LIST_STYLE_POSITION);
                Container parent=ui.getParent();

                if (parent.getLayout() instanceof BorderLayout) {
                    borderUi=parent;
                }
            }

            listType=selector.getAttrVal(CSSElement.CSS_LIST_STYLE_TYPE);
            listImg=getCSSUrl(selector.getAttributeById(CSSElement.CSS_LIST_STYLE_IMAGE));
        }

        // Border
        Border[] borders = new Border[4];
        for(int i=Component.TOP;i<=Component.RIGHT;i++) {
            borders[i]=createBorder(selector, borderUi, i,styles);
        }
        Border curBorder=borderUi.getUnselectedStyle().getBorder();
        if (((styles & STYLE_SELECTED)!=0) && ((styles & STYLE_UNSELECTED)==0)) {
            curBorder=borderUi.getSelectedStyle().getBorder();
        }
        if ((styles & STYLE_PRESSED)!=0) {
            curBorder=((HTMLLink)borderUi).getSelectedStyle().getBorder();
        }

        // In case this element was assigned a top border for instance, and then by belonging to another tag/class/id it has also a bottom border - this merges the two (and gives priority to the new one)
        if ((curBorder!=null) && (curBorder instanceof CSSBorder)) { // TODO - This doesn't cover the case of having another border (i.e. table/fieldset?) - Can also assign the non-CSS border to the other corners?
            ((CSSBorder)curBorder).mergeBorder(borders);
            borderUi.getParent().revalidate();
        } else {
            Border border=CSSBorder.createCSSBorder(borders,borderUi);
            if (border!=null) {
                if ((styles & STYLE_SELECTED)!=0) {
                    borderUi.getSelectedStyle().setBorder(border);
                }
                if ((styles & STYLE_UNSELECTED)!=0) {
                    borderUi.getUnselectedStyle().setBorder(border);
                }
                if ((styles & STYLE_PRESSED)!=0) {
                    ((HTMLLink)borderUi).getPressedStyle().setBorder(border);
                }
                borderUi.getParent().revalidate();
            }
        }

        //
        // Specific elements styling
        //
        
        // Access keys
        v=selector.getAttributeById(CSSElement.CSS_WAP_ACCESSKEY);
        if ((v!=null) && (v.length()>=1) &&
                ((element.getId()==Element.TAG_INPUT) ||  // These are the only tags that can accpet an access key
                 (element.getId()==Element.TAG_TEXTAREA) || (element.getId()==Element.TAG_LABEL) ||
                 ((element.getId()==Element.TAG_A) && (ui instanceof HTMLLink) && ((HTMLLink)ui).parentLink==null)) // For A tags this is applied only to the first word, no need to apply it to each word of the link
                ) {

            // The accesskey string may consist fallback assignments (comma seperated) and multiple assignments (space seperated) and any combination of those
            // For example: "send *, #" (meaning: assign both the send and * keys, and if failed to assign one of those assign the # key instead)
            int index=v.indexOf(',');
            boolean assigned=false;
            while (index!=-1) { // Handle fallback access keys
                String key=v.substring(0,index).trim();
                v=v.substring(index+1);
                assigned=processAccessKeys(key, htmlC, ui);
                if (assigned) {
                    break; // comma denotes fallback, and once we succeeded assigning the accesskey, the others are irrelevant
                }
                index=v.indexOf(',');
            }
            if (!assigned) {
                processAccessKeys(v.trim(), htmlC, ui);
            }

        }

        // Note that we carefully check the structure of the LI/UL/OL element with instanceof and checking component count
        // This is since in some cases other elements can come between a OL/UL and its LI items (Though illegal in HTML, it can occur)
        if ((listType!=-1) || (listImg!=null)) {
            if (element.getId()==Element.TAG_LI) {
                if (ui instanceof Container) {
                Container liCont=(Container)ui;
                    Container liParent=liCont.getParent();
                    Component firstComp=liParent.getComponentAt(0);
                    if (firstComp instanceof Container) {
                        Container bulletCont=(Container)firstComp;
                        if (bulletCont.getComponentCount()>0) {
                            Component listItemCmp=bulletCont.getComponentAt(0);
                            if (listItemCmp instanceof Component) {
                                HTMLListItem listItem=((HTMLListItem)listItemCmp);
                                listItem.setStyleType(listType);
                                listItem.setImage(listImg);
                            }
                        }
                    }
                }
            } else if ((element.getId()==Element.TAG_UL) || (element.getId()==Element.TAG_OL)) {
                Container ulCont = (Container)ui;
                for(int i=0;i<ulCont.getComponentCount();i++) {
                    Component cmp=ulCont.getComponentAt(i);
                    if (cmp instanceof Container) {
                        Container liCont=(Container)cmp;
                        if (liCont.getComponentCount()>=1) {
                            cmp=liCont.getComponentAt(0);
                            if (cmp instanceof Container) {
                                Container liContFirstLine=(Container)cmp;
                                if (liContFirstLine.getComponentCount()>=1) {
                                    cmp=liContFirstLine.getComponentAt(0);
                                    if (cmp instanceof HTMLListItem) {
                                        HTMLListItem listItem=(HTMLListItem)cmp;
                                        listItem.setStyleType(listType);
                                        listItem.setImage(listImg);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    /**
     * Sets the alignment of all cells in the table 
     * 
     * @param ui The component representing the table (a HTMLTable)
     * @param align The alignment
     * @param isHorizontal true for horizontal alignment, false for vertical alignment
     */
    private void setTableAlignment(Component ui,int align,boolean isHorizontal) {
        HTMLTable table=(HTMLTable)ui; 
        HTMLTableModel model= ((HTMLTableModel)table.getModel());
        model.setAlignToAll(isHorizontal, align);
        table.setModel(model);
    }
    
    /**
     * Sets the table cell 'ui' to the requested alignment
     * 
     * @param tdTag The element representing the table cell (TD/TH tag)
     * @param ui The component representing the table (a HTMLTable)
     * @param align The alignment
     * @param isHorizontal true for horizontal alignment, false for vertical alignment
     */
    private void setTableCellAlignment(Element tdTag,Component ui,int align,boolean isHorizontal) {
        Element trTag=tdTag.getParent();
        while ((trTag!=null) && (trTag.getId()!=Element.TAG_TR)) { // Though in strict XHTML TR can only contain TD/TH - in some HTMLs TR doesn't have to be the direct parent of the tdTag, i.e.: <tr><b><td>...</td>... </b></tr>
            trTag=trTag.getParent();
        }
        setTableCellAlignmentTR(trTag, ui, align, isHorizontal);
    }

    /**
     * Sets the table cell 'ui' to the requested alignment
     * Note that when called directly (on TAG_TR) this is actually called multiple times, each with a different cell of the row as 'ui'.
     * This happens since TR elements contain all their cells as their UI and as such, applyStyle will call applyToUIElement each time with another cell
     *
     * @param trTag The element representing the table row (TR tag) who is the parent of the cell we want to modify
     * @param ui The component representing the table (a HTMLTable)
     * @param align The alignment
     * @param isHorizontal true for horizontal alignment, false for vertical alignment
     */
    private void setTableCellAlignmentTR(Element trTag,Component ui,int align,boolean isHorizontal) {
        if ((trTag!=null) && (trTag.getId()==Element.TAG_TR)) {
            Element tableTag=trTag.getParent();
            while ((tableTag!=null) && (tableTag.getId()!=Element.TAG_TABLE)) { // Though in strict XHTML TABLE can only contain TR - in some HTMLs it might be different
                tableTag=tableTag.getParent();
            }
            if ((tableTag!=null) && (tableTag.getId()==Element.TAG_TABLE)) {
                final HTMLTable table=(HTMLTable)tableTag.getUi().elementAt(0);
                final HTMLTableModel model= ((HTMLTableModel)table.getModel());
                CellConstraint cConstraint=model.getConstraint(ui);

                if (isHorizontal) {
                    cConstraint.setHorizontalAlign(align);
                } else {
                    cConstraint.setVerticalAlign(align);
                }
                table.setModel(model); // Setting the same model again causes re-evaluation of the constraints

            }
        }

    }

    /**
     * Tries to assign the given key string as an access key to the specified component
     * The key string given here may consist of a multiple key assignment, i.e. several keys seperated with space
     *
     * @param keyStr The string representing the key (either a character, a unicode escape sequence or a special key name
     * @param htmlC The HTMLComponent
     * @param ui The component to set the access key on
     * @return true if successful, false otherwise
     */
    private boolean processAccessKeys(String keyStr,HTMLComponent htmlC,Component ui) {
        int index=keyStr.indexOf(' ');
        boolean isFirstKey=true; // Keeps track of whether this is the first key we are adding (In order to override XHTML accesskey or failed multiple assignments)
        while (index!=-1) { // Handle multiple/fallback access keys
            String key=keyStr.substring(0,index).trim();
            keyStr=keyStr.substring(index+1);
            if (!processAccessKey(key, htmlC, ui, isFirstKey)) {
                return false; // If failing to set one of the keys - we return a failure
            }
            isFirstKey=false;
            index=keyStr.indexOf(' ');
        }
        return processAccessKey(keyStr, htmlC, ui, isFirstKey);
    }

    /**
     * Tries to assign the given key string as an access key to the specified component
     * The key string given here is a single key
     *
     * @param keyStr The string representing the key (either a character, a unicode escape sequence or a special key name
     * @param htmlC The HTMLComponent
     * @param ui The component to set the access key on
     * @param override If true overrides other keys assigned previously for this component
     * @return true if successful, false otherwise
     */
    private boolean processAccessKey(String keyStr,HTMLComponent htmlC,Component ui,boolean override) {
        if (keyStr.startsWith("\\")) { // Unicode escape sequence, may be used to denote * and # which technically are illegal as values
            try {
                int keyCode=Integer.parseInt(keyStr.substring(1), 16);
                htmlC.addAccessKey((char)keyCode, ui, override);
                return true;
            } catch (NumberFormatException nfe) {
                return false;

            }
        } else if (keyStr.length()==1) {
            htmlC.addAccessKey(keyStr.charAt(0), ui, override);
            return true;
        } else { //special key shortcut
            if (specialKeys!=null) {
                Integer key=(Integer)specialKeys.get(keyStr);
                if (key!=null) {
                    htmlC.addAccessKey(key.intValue(), ui, override);
                    return true;
                }
            }
            return false;
        }
    }

    /**
     *  Returns a border for a specific side of the component
     *
     * @param styleAttributes The style attributes element containing the border directives
     * @param ui The component we want to set the border on
     * @param location One of Component.TOP/BOTTOM/LEFT/RIGHT
     * @return
     */
    Border createBorder(CSSElement styleAttributes,Component ui,int location,int styles) {
//        String borderStyle=styleAttributes.getAttributeById(CSSElement.CSS_BORDER_TOP_STYLE+location);
//        if (borderStyle==null) {
//            return null;
//        }
        int borderStyle=styleAttributes.getAttrVal(CSSElement.CSS_BORDER_TOP_STYLE+location);
        if ((borderStyle==-1) || (borderStyle==BORDER_STYLE_NONE)) {
            return null;
        }

        int borderColor=styleAttributes.getAttrVal(CSSElement.CSS_BORDER_TOP_COLOR+location);
        int borderWidth=styleAttributes.getAttrLengthVal(CSSElement.CSS_BORDER_TOP_WIDTH+location, ui,0);
        if (borderWidth==-1) {
            borderWidth=CSSElement.BORDER_DEFAULT_WIDTH; // Default value
        }
        if ((styles & STYLE_SELECTED)!=0) {
            ui.getSelectedStyle().setPadding(location, ui.getStyle().getPadding(location)+borderWidth);
        }
        if ((styles & STYLE_UNSELECTED)!=0) {
            ui.getUnselectedStyle().setPadding(location, ui.getStyle().getPadding(location)+borderWidth);
        }
        if ((styles & STYLE_PRESSED)!=0) {
            ((HTMLLink)ui).getPressedStyle().setPadding(location, ui.getStyle().getPadding(location)+borderWidth);
        }

            Border border=null;
            // Note: The only mandatory border styles according to WCSS spec are solid & none all the others are optional, we support what we already have in LWUIT
            // Note that due to that 'border -width' is not supported in non-solid borders, since only the line border supports thickness.
            switch(borderStyle) {
                case BORDER_STYLE_DOUBLE: // No support for this type of background, so instead a solid background is used
                case BORDER_STYLE_DOTTED: // No support for this type of background, so instead a solid background is used
                case BORDER_STYLE_DASHED: // No support for this type of background, so instead a solid background is used
                case BORDER_STYLE_SOLID:
                    if (borderColor!=-1) {
                        border=Border.createLineBorder(borderWidth, borderColor);
                    } else {
                        border=Border.createLineBorder(borderWidth);
                    }
                    break;
                case BORDER_STYLE_INSET:
                    if (borderColor!=-1) {
                        border=Border.createBevelRaised(0,0,borderColor, borderColor);
                    } else {
                        border=Border.createBevelLowered();
                    }
                    break;
                case BORDER_STYLE_OUTSET:
                    if (borderColor!=-1) {
                        border=Border.createBevelRaised(borderColor, borderColor,0,0);
                    } else {
                        border=Border.createBevelRaised();
                    }
                    break;
                case BORDER_STYLE_GROOVE:
                    if (borderColor!=-1) {
                        border=Border.createEtchedRaised(borderColor,0);
                    } else {
                        border=Border.createEtchedRaised();
                    }
                    break;
                case BORDER_STYLE_RIDGE:
                    if (borderColor!=-1) {
                        border=Border.createEtchedLowered(borderColor, 0);
                    } else {
                        border=Border.createEtchedLowered();
                    }
                    break;
                
            }

            return border;

    }

    /**
     * Omits quotes of all kinds if they exist in the string
     * 
     * @param str The string to check
     * @return A quoteless string
     */
    static String omitQuotesIfExist(String str) {
            if (str==null) {
                return null;
            }
            if (((str.charAt(0)=='\"') || (str.charAt(0)=='\'')) && (str.length()>=2)) {
                str=str.substring(1, str.length()-1); // omit quotes from both sides
            }
            return str;
    }

    /**
     * Sets the font of the component to the closest font that can be found according to the specified properties
     * Note that system fonts will be matched only with system fonts and same goes for bitmap fonts
     *
     * @param htmlC The HTMLComponent this component belongs to (For the available bitmap fonts table)
     * @param cmp The component to work on
     * @param fontFamily Teh font family
     * @param fontSize The font size in pixels
     * @param fontStyle The font style - either Font.STYLE_PLAIN or Font.STYLE_ITALIC
     * @param fontWeight The font weight - either Font.STYLE_PLAIN ot Font.STYLE_BOLD
     */
    private void setMatchingFont(HTMLComponent htmlC, Component cmp,String fontFamily,int fontSize,int fontStyle,int fontWeight,CSSElement selector) {

        int styles=getApplicableStyles(cmp, selector);
        Font curFont=cmp.getUnselectedStyle().getFont();

        if (((styles & STYLE_SELECTED)!=0) && ((styles & STYLE_UNSELECTED)==0)) { // Focus
            curFont=cmp.getSelectedStyle().getFont();
        }
        if ((styles & STYLE_PRESSED)!=0) { // Active
            curFont=((HTMLLink)cmp).getPressedStyle().getFont();
        }

            int curSize=0;
            boolean isBold=false;
            boolean isItalic=false;
            String curFamily=null;
            if (curFont.getCharset()==null) { //system font
                // The family string in system fonts is just used to index the font in the matchingFonts cache hashtable
                switch (curFont.getFace()) {
                    case Font.FACE_SYSTEM:
                        curFamily="system";
                        break;
                    case Font.FACE_PROPORTIONAL:
                        curFamily="proportional";
                        break;
                    default:
                        curFamily="monospace";
                }
                curSize=curFont.getHeight()-2; // Font height is roughly 2-3 pixels above the font size, and is the best indicator we have to what the system font size is
                isBold=((curFont.getStyle() & Font.STYLE_BOLD)!=0);
                isItalic=((curFont.getStyle() & Font.STYLE_ITALIC)!=0);
            } else { // bitmap font
                HTMLFont hFont=htmlC.getHTMLFont(curFont);
                
                if (hFont!=null) {
                    curSize=hFont.getSize();
                    isBold=hFont.isBold();
                    isItalic=hFont.isItalic();
                    curFamily=hFont.getFamily();
                }
            }

            if (((fontFamily!=null) && (!fontFamily.equalsIgnoreCase(curFamily))) ||
                (fontSize!=curSize) ||
                ((isBold)!=(fontWeight==Font.STYLE_BOLD)) ||
                ((isItalic)!=(fontWeight==Font.STYLE_ITALIC))) { // This checks if there's a need to set the font, or if the current font matches the properties of the current one

                // Set the unspecified attributes of the requested font to match those of the current one
                if (fontFamily==null) {
                        fontFamily=curFamily.toLowerCase();
                    }
                    if (fontSize==-1) {
                        fontSize=curSize;
                    }
                    if (fontStyle==-1) {
                        if (isItalic) {
                            fontStyle=Font.STYLE_ITALIC;
                        } else {
                            fontStyle=0;
                        }
                    }
                    if (fontWeight==-1) {
                        if (isBold) {
                            fontWeight=Font.STYLE_BOLD;
                        } else {
                            fontWeight=0;
                        }
                    }

                    String fontKey=fontFamily+"."+fontSize+"."+fontStyle+"."+fontWeight;
                    Object obj=matchingFonts.get(fontKey);
                    if (obj!=null) {
                        Font font=(Font)obj;
                        setFontForStyles(styles, cmp, font);
                        return;
                    }

                    Font font=null;
                    if (curFont.getCharset()==null) { //system font
                        int systemFontSize=curFont.getSize();
                        if (fontSize>curSize) { //bigger font
                            if (systemFontSize==Font.SIZE_SMALL) {
                                systemFontSize=Font.SIZE_MEDIUM;
                            } else if (systemFontSize==Font.SIZE_MEDIUM) {
                                systemFontSize=Font.SIZE_LARGE;
                            }
                        } else if (fontSize<curSize) { //smaller font
                            if (systemFontSize==Font.SIZE_LARGE) {
                                systemFontSize=Font.SIZE_MEDIUM;
                            } else if (systemFontSize==Font.SIZE_MEDIUM) {
                                systemFontSize=Font.SIZE_SMALL;
                            }
                        }
                        font=Font.createSystemFont(curFont.getFace(), fontStyle+fontWeight, systemFontSize);
                    } else {
                        font=htmlC.getClosestFont(fontFamily, fontSize, fontStyle,fontWeight);
                    }
                    if (font!=null) {
                        matchingFonts.put(fontKey, font);
                        setFontForStyles(styles, cmp, font);
                    }
            }

    }

    private void setFontForStyles(int styles,Component cmp,Font font) {
        if ((styles & STYLE_UNSELECTED)!=0) {
            cmp.getUnselectedStyle().setFont(font);
        }
        if ((styles & STYLE_SELECTED)!=0) {
            cmp.getSelectedStyle().setFont(font);
        }
        if ((styles & STYLE_PRESSED)!=0) {
            ((HTMLLink)cmp).getPressedStyle().setFont(font);
        }

    }

    /**
     * Extracts a url from a CSS URL value
     *
     * @param cssURL the CSS formatted URL - url(someurl)
     * @return A regular URL - someurl
     */
    static String getCSSUrl(String cssURL) {
        if ((cssURL!=null) && (cssURL.toLowerCase().startsWith("url("))) {
            int index=cssURL.indexOf(')');
            if (index!=-1) {
                cssURL=cssURL.substring(4,index);
                cssURL=cssURL.trim(); // According to the CSS spec, a URL can have white space before/after the quotes
                cssURL=omitQuotesIfExist(cssURL);
                return cssURL;
            }
        }
        return null;
    }

}
