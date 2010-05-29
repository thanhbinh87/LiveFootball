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
import com.sun.lwuit.ButtonGroup;
import com.sun.lwuit.CheckBox;
import com.sun.lwuit.ComboBox;
import com.sun.lwuit.Command;
import com.sun.lwuit.RadioButton;
import com.sun.lwuit.TextArea;
import com.sun.lwuit.events.ActionEvent;
import com.sun.lwuit.plaf.UIManager;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * HTMLForm is an object that holds all the data related to a form within HTML.
 * Note that it is not related in any way to LWUIT's Form.
 *
 * @author Ofir Leitner
 */
class HTMLForm { 

    /**
     * The default text on forms' submit button
     */
    private static final String DEFAULT_SUBMIT_TEXT = "Submit";

    /**
     * The default text on forms' reset button
     */
    private static final String DEFAULT_RESET_TEXT = "Clear";

    Hashtable comps = new Hashtable();
    Hashtable defaultValues = new Hashtable();
    Vector defaultCheckedButtons = new Vector();
    Vector defaultUncheckedButtons = new Vector();
    Hashtable inputFormats = new Hashtable();
    Hashtable buttonGroups = new Hashtable();
    Vector emptyOk = new Vector();
    Vector emptyNotOk = new Vector();

    HTMLComponent htmlC;
    String action;
    String encType;
    boolean isPostMethod;
    boolean hasSubmitButton;

    NamedCommand submitCommand;
    NamedCommand resetCommand;

    /**
     * Constructs the HTMLForm
     * 
     * @param htmlC The HTMLComponent containing this form
     * @param action The action of this form (the URL)
     * @param method The method of this form (get/post)
     */
    HTMLForm(HTMLComponent htmlC,String action,String method,String encType) {
        this.htmlC=htmlC;
        this.action=htmlC.convertURL(action);
        this.encType=encType;
        if (htmlC.getHTMLCallback()!=null) {
            int linkProps=htmlC.getHTMLCallback().getLinkProperties(htmlC, this.action);
            if ((linkProps & HTMLCallback.LINK_FORBIDDEN)!=0) {
                this.action=null;
            }
        }

        this.isPostMethod=((method!=null) && (method.equalsIgnoreCase("post")));
        submitCommand=new NamedCommand(getDefaultSubmitText(), this, true);
        resetCommand=new NamedCommand(getDefaultResetText(), this, false);
    }

    /**
     * Returns the default submit command text
     *
     * @return the default submit command text
     */
    static String getDefaultSubmitText() {
        return UIManager.getInstance().localize("html.submit", DEFAULT_SUBMIT_TEXT);
    }

    /**
     * Returns the default reset command text
     *
     * @return the default reset command text
     */
    static String getDefaultResetText() {
        return UIManager.getInstance().localize("html.reset", DEFAULT_RESET_TEXT);
    }

    /**
     * Returns the submit command of this form
     * 
     * @return the submit command of this form
     */
    Command getSubmitCommand() {
        return submitCommand;
    }

    /**
     * Returns the reset command of this form
     *
     * @return the reset command of this form
     */
    Command getResetCommand() {
        return resetCommand;
    }

    /**
     * Returns the text used for the submit button on this form
     *
     * @return the text used for the submit button on this form
     */
    void setSubmitText(String text) {
        submitCommand.setCommandName(text);
    }

    /**
     * Returns the text used for the reset button on this form
     *
     * @return the text used for the reset button on this form
     */
    void setResetText(String text) {
        resetCommand.setCommandName(text);
    }
    
    /**
     * Adds an input field to the form, note that unlike adding to a LWUIT form here the components are added logically only to query them for their value on submit.
     * 
     * @param key The input field's name/id
     * @param input The field itself (either the component or the value for a hidden field)
     * @param defaultValue The default value of the field (or null if none)
     */
    void addInput(String key,Object input,String defaultValue) {
        if (defaultValue!=null) { // Default values are added even before the key is checked, since even if the input component is unnamed, the form still needs to be able to reset it
            defaultValues.put(input,defaultValue);
        }

        if (key==null) {
            return; //no id
        }
        comps.put(key,input);
        if ((htmlC.getHTMLCallback()!=null) && (input instanceof TextArea)) {
            String autoVal=htmlC.getHTMLCallback().getAutoComplete(htmlC, action, key);
            if (autoVal!=null) {
                ((TextArea)input).setText(autoVal);
            }
        }
    }
    /**
     *
     * @param ta The textarea/field
     * @param inputFormat An input format verifier (or null if none)
     */
    void setInputFormat(TextArea ta,HTMLInputFormat inputFormat) {
        inputFormats.put(ta, inputFormat);
    }

    /**
     * Sets whether the specified input field can be left empty or not
     * 
     * @param ta The TextArea
     * @param ok true if can be left empty, false otherwise
     */
    void setEmptyOK(TextArea ta,boolean ok) {
        if (ok) {
            emptyOk.addElement(ta);
        } else {
            emptyNotOk.addElement(ta);
        }
    }


    /**
     * Sets the default value for the specified input field
     *
     * @param input The input field to set the default value to
     * @param defaultValue The default value
     */
    void setDefaultValue(Object input,Object defaultValue) {
        if ((input!=null) &&(defaultValue!=null)) {
            defaultValues.put(input,defaultValue);
        }
    }

    /**
     * Adds the specified CheckBox to the form.
     * Note that unlike adding to a LWUIT form here the components are added logically only to query them for their value on submit.
     *
     * @param key The CheckBox's name/id
     * @param cb The CheckBox
     * @param value The value of the checkbox
     */
    void addCheckBox(String key,CheckBox cb,String value) {
        if (cb.isSelected()) {
            defaultCheckedButtons.addElement(cb);
        } else {
            defaultUncheckedButtons.addElement(cb);
        }

        if (key==null) {
            return; //no id
        }

        Hashtable internal=(Hashtable)comps.get(key);
        if (internal==null) {
            internal=new Hashtable();
            comps.put(key,internal);
        }
        internal.put(cb, value);
    }

    /**
     * Adds the specified RadioButton to the form.
     * Note that unlike adding to a LWUIT form here the components are added logically only to query them for their value on submit.
     *
     * @param key The CheckBox's name/id
     * @param rb The RadioButton to add
     * @param value The value of the checkbox
     */
    void addRadioButton(String key,RadioButton rb,String value) {
        if (rb.isSelected()) {
            defaultCheckedButtons.addElement(rb);
        } else {
            defaultUncheckedButtons.addElement(rb);
        }


        if (key==null) {
            return; //no id
        }

        Hashtable internal=(Hashtable)comps.get(key);
        ButtonGroup group=null;
        if (internal==null) {
            internal=new Hashtable();
            comps.put(key,internal);
            group=new ButtonGroup();
            buttonGroups.put(key, group);

        } else {
            group=(ButtonGroup)buttonGroups.get(key);
        }
        group.add(rb);
        internal.put(rb, value);

    }

    /**
     * Returns the number of fields in the form
     *
     * @return the number of fields in the form
     */
    int getNumFields() {
        return comps.size();
    }


    /**
     * Called when the a form submit is needed. 
     * This querys all form fields, creates a URL accordingly and sets it to the HTMLComponent
     */
    void submit() {
        if (action==null) {
            return;
        }
        boolean error=false; //If this is turned to true anywhere, the form will not be submitted
        String url=action; 
        String params=null;
        if (comps.size()>0) {
            params="";
            for(Enumeration e=comps.keys();e.hasMoreElements();) {
                String key=(String)e.nextElement();
                Object input=comps.get(key);
                key=encodeString(key);
                String value="";
                if (input instanceof String) { //hidden
                    value=encodeString((String)input);
                    params+=key+"="+value+"&";
                } else if (input instanceof Hashtable) { //checkbox / radiobutton
                    Hashtable options=(Hashtable)input;
                    for(Enumeration e2=options.keys();e2.hasMoreElements();) {
                        Button b = (Button)e2.nextElement();
                        if (b.isSelected()) {
                            params+=key+"="+encodeString((String)options.get(b))+"&";
                        }
                    }
                } else if (input instanceof TextArea) { //catches both textareas and text input fields
                    TextArea tf=((TextArea)input);
                    String text=tf.getText();

                    String errorMsg=null;
                    if (HTMLComponent.SUPPORT_INPUT_FORMAT) {
                        boolean ok=false;
                        if (text.equals("")) { // check empty - Note that emptyok/-wap-input-required overrides input format
                            if (emptyNotOk.contains(tf)) {
                                errorMsg=UIManager.getInstance().localize("html.format.emptynotok", "Field can't be empty");
                                error=true;
                            } else if (emptyOk.contains(tf)) {
                                ok=true;
                            }
                        }

                        if ((!error) && (!ok)) { // If there's already an error or it has been cleared by the emptyOK field, no need to check
                            HTMLInputFormat inputFormat=(HTMLInputFormat)inputFormats.get(tf);
                            if ((inputFormat!=null) && (!inputFormat.verifyString(text))) {
                                String emptyStr="";
                                if (emptyOk.contains(tf)) {
                                    emptyStr=UIManager.getInstance().localize("html.format.oremptyok", " or an empty string");
                                } else if (emptyNotOk.contains(tf)) {
                                    emptyStr=UIManager.getInstance().localize("html.format.andemptynotok", " and cannot be an empty string");
                                }
                                errorMsg=UIManager.getInstance().localize("html.format.errordesc", "Malformed text. Correct value: ")+inputFormat.toString()+emptyStr;
                                error=true;
                            }
                        }
                    }

                    if (htmlC.getHTMLCallback()!=null) {
                        int type=HTMLCallback.FIELD_TEXT;
                        if ((tf.getConstraint() & TextArea.PASSWORD)!=0) {
                            type=HTMLCallback.FIELD_PASSWORD;
                        }
                        text=htmlC.getHTMLCallback().fieldSubmitted(htmlC, tf,url, key, text, type,errorMsg);
                    }
                    if (errorMsg==null) {
                        params+=key+"="+encodeString(text)+"&";
                    }
                } else if (input instanceof ComboBox) { // drop down lists (single selection)
                    Object item=((ComboBox)input).getSelectedItem();
                    if (item instanceof OptionItem) {
                        value=((OptionItem)item).getValue();
                        params+=key+"="+encodeString(value)+"&";
                    } // if not - value may be an OPTGROUP label in an only optgroup combobox
                } else if (input instanceof MultiComboBox) { // drop down lists (multiple selection)
                    Vector selected=((MultiComboBox)input).getSelected();
                    for(int i=0;i<selected.size();i++) {
                        Object item=selected.elementAt(i);
                        if (item instanceof OptionItem) {
                            value=((OptionItem)item).getValue();
                            params+=key+"="+encodeString(value)+"&";
                        } // if not - value may be an OPTGROUP label in an only optgroup combobox
                    }
                }

            }

            if (params.endsWith("&")) { //trim the extra &
                params=params.substring(0, params.length()-1);
            }
        }

        if (!error) {
            DocumentInfo docInfo=new DocumentInfo(url, params, isPostMethod);
            if ((encType!=null) && (!encType.equals(""))) {
                docInfo.setEncoding(encType);
            }
            htmlC.setPage(docInfo);
        }
    }

    /**
     * Encodes the specified string to "percent-encoding" or URL encoding.
     * This encodes reserved, unsafe and unicode characters
     * 
     * @param str The string to be encoded
     * @return A percent-encoding of the string (safe characters remain the same)
     */
    private String encodeString(String str) {
        if (str==null) {
            return "";
        }
        String encodedStr="";
        for(int i=0;i<str.length();i++) {
            char c=str.charAt(i);
            if (
                // Checks for unreserved characters that RFC 3986 defines that shouldn't be encoded
                ((c>='a') && (c<='z')) || ((c>='A') && (c<='Z')) ||
                ((c>='0') && (c<='9')) ||
                (c=='-') || (c=='.') || (c=='_') || (c=='~'))
                
            {
                encodedStr+=c;
            } else if ((c>0x80) && (c<0xffff)) { // UTF encoding - See http://en.wikipedia.org/wiki/UTF-8
                int firstLiteral = c/256;
                int secLiteral = c%256;
                if (c<0x07ff) { // 2 literals unicode
                    firstLiteral=192+(firstLiteral<<2)+(secLiteral>>6);
                    secLiteral=128+secLiteral%192;
                    encodedStr+="%"+Integer.toHexString(firstLiteral).toUpperCase()+"%"+Integer.toHexString(secLiteral).toUpperCase();
                } else { // 3 literals unicode
                    firstLiteral=224+(firstLiteral>>4);
                    secLiteral=128+((firstLiteral%16)<<2)+(secLiteral>>6);
                    int thirdLiteral=128+secLiteral%192;
                    encodedStr+="%"+Integer.toHexString(firstLiteral).toUpperCase()+"%"+Integer.toHexString(secLiteral).toUpperCase()
                            +"%"+Integer.toHexString(thirdLiteral).toUpperCase();
                } // TODO - ? - support for 4 literals code? porbably not even included in UTF8
            } else {
                String prefix="%";
                if (c<16) {
                    prefix+="0"; //For a value lesser than 16, we'd like to get %0F and not %F
                }
                encodedStr+=prefix+Integer.toHexString((int)c).toUpperCase(); 

            }
        }
        return encodedStr;
    }

    /**
     * Called when a form reset is needed and resets all the form fields to their default values.
     */
    void reset() {
            for(Enumeration e=defaultValues.keys();e.hasMoreElements();) {
                Object input=e.nextElement();
                if (input instanceof TextArea) { //catches both textareas and text input fields
                    String defVal=(String)defaultValues.get(input);
                    if (defVal==null) {
                        defVal="";
                    }
                    ((TextArea)input).setText(defVal);
                } else if (input instanceof ComboBox) {
                    OptionItem defVal=(OptionItem)defaultValues.get(input);
                    ComboBox combo=((ComboBox)input);
                    if (defVal!=null) {
                        combo.setSelectedItem(defVal);
                    } else if (combo.size()>0) {
                        combo.setSelectedIndex(0);
                    }
                }
            }

            for (Enumeration e=defaultCheckedButtons.elements();e.hasMoreElements();) {
                Button b = (Button)e.nextElement();
                if (!b.isSelected()) {
                    setButton(b, true);
                }
            }

            for (Enumeration e=defaultUncheckedButtons.elements();e.hasMoreElements();) {
                Button b = (Button)e.nextElement();
                if (b.isSelected()) {
                    setButton(b, false);
                }
            }

    }

    /**
     * A convenience method used in reset()
     *
     * @param button The button to set (CheckBox/RadioButton)
     * @param checked true to check, false to uncheck
     */
    private void setButton(Button button,boolean checked) {
        if (button instanceof RadioButton) {
            ((RadioButton)button).setSelected(checked);
        } else {
            ((CheckBox)button).setSelected(checked);
        }
    }

}

/**
 * A simple class adding the option to change the name of a command
 *
 * @author Ofir Leitner
 */
class NamedCommand extends Command {

        HTMLForm htmlForm;
        String name;
        boolean isSubmit;

        NamedCommand(String name,HTMLForm htmlForm,boolean isSubmit) {
            super(name);
            this.htmlForm=htmlForm;
            this.name=name;
            this.isSubmit=isSubmit;
        }

        public void actionPerformed(ActionEvent evt) {
            super.actionPerformed(evt);
            if (isSubmit) {
                htmlForm.submit();
            } else {
                htmlForm.reset();
            }
        }

        public void setCommandName(String name) {
            this.name=name;
        }

        public String getCommandName() {
            return name;
        }
}
