/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.vinhcom.livefootball;

import com.sun.lwuit.Component;
import com.sun.lwuit.Container;
import com.sun.lwuit.Dialog;
import com.sun.lwuit.Display;
import com.sun.lwuit.Form;
import com.sun.lwuit.Graphics;
import com.sun.lwuit.Label;
import com.sun.lwuit.List;
import com.sun.lwuit.TextArea;
import com.sun.lwuit.TextField;
import com.sun.lwuit.animations.Transition3D;
import com.sun.lwuit.browser.HttpRequestHandler;
import com.sun.lwuit.html.DocumentRequestHandler;
import com.sun.lwuit.html.HTMLComponent;
import com.sun.lwuit.layouts.BorderLayout;
import com.sun.lwuit.layouts.BoxLayout;
import com.sun.lwuit.plaf.Style;
import java.util.Hashtable;
import java.util.Vector;
import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;

/**
 *
 * @author win7_64
 */
public class Views {

  public static Vector href_list;
  public static Form form;
  public static List list;
  public static JSONObject json;
  public static Hashtable cache = new Hashtable();
/**
   * Display Elements: List, Info, HTML, Richtext.
   */
  public static void list_display() {
    try {
      String form_title = json.getString(Settings.FORM_TITLE);
//      System.out.println("Form title: " + form_title);
      form = new Form(form_title); // tạo form với tên form lấy được trong chuỗi trả về
      form.setTransitionInAnimator(Transition3D.createRotation(500, true)); // .createCube(1000, true));
      list = new List() {

        /**
         * Tìm kiếm đơn giản (gõ chữ cái đầu sẽ nhảy đến vị trí thỏa mãn)
         */
        private long lastSearchInteraction;
        private TextField search = new TextField(3);

        {
          search.getSelectedStyle().setBgTransparency(255);
          search.setReplaceMenu(false);
          search.setInputModeOrder(new String[]{"Abc"});
          search.setFocus(true);
        }

        public void keyPressed(int code) {
          int game = Display.getInstance().getGameAction(code);
          if (game > 0) {
            super.keyPressed(code);
          }
          else {
            search.keyPressed(code);
            lastSearchInteraction = System.currentTimeMillis();
          }
        }

        public void keyReleased(int code) {
          int game = Display.getInstance().getGameAction(code);
          if (game > 0) {
            super.keyReleased(code);
          }
          else {
            search.keyReleased(code);
            lastSearchInteraction = System.currentTimeMillis();
            String t = search.getText();
            int modelSize = getModel().getSize();
            for (int iter = 0; iter < modelSize; iter++) {
              String v = getModel().getItemAt(iter).toString();
              if (v.startsWith(t)) {
                setSelectedIndex(iter);
                return;
              }
            }
          }
        }

        public void paint(Graphics g) {
          super.paint(g);
          if (System.currentTimeMillis() - 300 < lastSearchInteraction || search.
                  isPendingCommit()) {
            search.setSize(search.getPreferredSize());
            Style s = search.getStyle();
            search.setX(getX() + getWidth() - search.getWidth() - s.getPadding(
                    RIGHT) - s.getMargin(RIGHT));
            search.setY(getScrollY() + getY());
            search.paintComponent(g, true);
          }
        }

        public boolean animate() {
          boolean val = super.animate();
          if (lastSearchInteraction != -1) {
            search.animate();
            if (System.currentTimeMillis() - 300 > lastSearchInteraction && !search.
                    isPendingCommit()) {
              lastSearchInteraction = -1;
              search.clear();
            }
            return true;
          }
          return val;
        }
      };
      href_list = new Vector();
      form.setLayout(new BorderLayout());
      JSONArray items = json.getJSONArray(Settings.ITEMS);
      for (int i = 0; i < items.length(); i++) {
        JSONObject item = items.getJSONObject(i);
        String item_name = item.getString(Settings.ITEM_NAME);
        list.addItem(item_name);
        String href = Settings.ROOT_URL + "/" + item.getString(
                Settings.ITEM_HREF);
        href_list.addElement(href);
      }
      form.addComponent(BorderLayout.CENTER, list);
      form.show();
    }
    catch (JSONException ex) {
      ex.printStackTrace();
    }

  }

  public static void info_display() {
    try {
      String form_title = json.getString(Settings.FORM_TITLE);
      form = new Form(form_title);
      form.setScrollable(true);
      form.setLayout(new BorderLayout());
      String info = json.getString(Settings.INFO);
      info = Utilities.replace(info, Settings.INFO_SEPERATE_CHARACTER, "\n");
      TextArea aboutText = new TextArea(info, 5, 10); //show info
      aboutText.setEditable(false);
      form.addComponent(BorderLayout.CENTER, aboutText);
      form.show();
    }
    catch (JSONException ex) {
      ex.printStackTrace();
    }

  }

  public static void html_display() {
    try {
      String form_title = json.getString(Settings.FORM_TITLE);
      form = new Form(form_title);
      form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));
      DocumentRequestHandler handler;
      handler = new HttpRequestHandler();
      HTMLComponent html = new HTMLComponent(handler);
      html.setPage(json.getString(Settings.CONTENT_URL));
      form.addComponent(html);
      form.show();
    }
    catch (JSONException ex) {
      ex.printStackTrace();
    }
  }

  public static void image_display() {
  }

  public static void richtext_display() {
    try {
      String form_title = json.getString(Settings.FORM_TITLE);
      form = new Form(form_title);
      form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));
      DocumentRequestHandler handler;
      handler = new HttpRequestHandler();
      HTMLComponent html = new HTMLComponent(handler);
      html.setBodyText(json.getString(Settings.INFO));
      form.addComponent(html);
      form.show();
    }
    catch (JSONException ex) {
      ex.printStackTrace();
    }
  }

  public static void alert(String Title, String Message) {
    Dialog.show(Title, Message, "Đóng", null);
  }

  public static Form loading(String message) {
    form = new Form();

    Container cont = new Container();
    cont.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

    Label label = new Label(message);
    label.getStyle().setMargin(Component.TOP, Display.getInstance().
            getDisplayHeight() / 3);
    label.getStyle().setMargin(Component.LEFT, Display.getInstance().
            getDisplayWidth() / 3);
    label.setAlignment(Component.CENTER);

    cont.addComponent(label);

    form.addComponent(cont);

    return form;
  }




}
