/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vinhcom.livefootball;

import com.sun.lwuit.Component;
import com.sun.lwuit.Container;
import com.sun.lwuit.Font;
import com.sun.lwuit.Label;
import com.sun.lwuit.List;
import com.sun.lwuit.layouts.BorderLayout;
import com.sun.lwuit.layouts.BoxLayout;
import com.sun.lwuit.list.ListCellRenderer;


/**
 *
 * @author win7_64
 */

class Contact {

        private String name;
        private String email;

        public Contact(String name, String email) {
            this.name = name;
            this.email = email;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }
        }

class ContactsRenderer
        extends Container
        implements ListCellRenderer {

  private Label name = new Label("");
  private Label email = new Label("");
  private Label focus = new Label("");

  public ContactsRenderer() {
    setLayout(new BorderLayout());
    Container cnt = new Container(new BoxLayout(BoxLayout.Y_AXIS));
    name.getStyle().setBgTransparency(0);
    name.getStyle().setFont(Font.createSystemFont(Font.FACE_SYSTEM,
                                                  Font.STYLE_BOLD,
                                                  Font.SIZE_MEDIUM));
    email.getStyle().setBgTransparency(0);
    cnt.addComponent(name);
    cnt.addComponent(email);
    addComponent(BorderLayout.CENTER, cnt);

    focus.getStyle().setBgTransparency(100);
  }

  public Component getListCellRendererComponent(List list, Object value,
                                                int index, boolean isSelected) {

    Contact person = (Contact) value;
    name.setText(person.getName());
    email.setText(person.getEmail());
    return this;
  }

  public Component getListFocusComponent(List list) {
    return focus;
  }
}

//
//public class Views {
//
//  public static Vector href_list;
//  public static Form form;
//  public static Form recent_form;
//  public static List list;
//  public static JSONObject json;
//  public static Hashtable cache = new Hashtable();
//
//  /**
//   * Display Elements: List, Info, HTML, Richtext.
//   */
////  public static void list_display() {
////    try {
////      recent_form = form;
////      String form_title = json.getString(Settings.FORM_TITLE);
////      form = new Form(form_title) {
////
////        public void keyPressed(int keyCode) {
////          System.out.println(keyCode);
////          if (keyCode != -5) {
////            super.keyPressed(keyCode);
////          }
////          else {
////            System.out.println("Vừa bấm phím giữa");
////            if (Controller.timer != null) { // dừng tự động refresh (nếu có)
////              Controller.timer.cancel();
////            }
////            /* lấy id của đối tượng được chọn (id bắt đầu từ 0) */
////            int index = Views.list.getSelectedIndex();
////
////            /* lấy url tương ứng với đối tượng được chọn */
////            Controller.url = (String) Views.href_list.elementAt(index);
////            Controller.progress_controller(Controller.url);
////            super.keyPressed(-6); // Bấm nút trái
////          }
////        }
////      }; // tạo form với tên form lấy được trong chuỗi trả về
////      form.setTransitionInAnimator(Transition3D.createCube(300, true));
////      form.setLayout(new BorderLayout());
////      href_list = new Vector();
////      list = new List();
////
////
////      // {
//////
//////        /**
//////         * Tìm kiếm đơn giản (gõ chữ cái đầu sẽ nhảy đến vị trí thỏa mãn)
//////         */
//////        private long lastSearchInteraction;
//////        private TextField search = new TextField(3);
//////
//////        {
//////          search.getSelectedStyle().setBgTransparency(255);
//////          search.setReplaceMenu(false);
//////          search.setInputModeOrder(new String[]{"Abc"});
//////          search.setFocus(true);
//////        }
//////
//////        public void keyPressed(int code) {
//////          int game = Display.getInstance().getGameAction(code);
//////          if (game > 0) {
//////            super.keyPressed(code);
//////          }
//////          else {
//////            search.keyPressed(code);
//////            lastSearchInteraction = System.currentTimeMillis();
//////          }
//////        }
//////
//////        public void keyReleased(int code) {
//////          int game = Display.getInstance().getGameAction(code);
//////          if (game > 0) {
//////            super.keyReleased(code);
//////          }
//////          else {
//////            search.keyReleased(code);
//////            lastSearchInteraction = System.currentTimeMillis();
//////            String t = search.getText();
//////            int modelSize = getModel().getSize();
//////            for (int iter = 0; iter < modelSize; iter++) {
//////              String v = getModel().getItemAt(iter).toString();
//////              if (v.startsWith(t)) {
//////                setSelectedIndex(iter);
//////                return;
//////              }
//////            }
//////          }
//////        }
//////
//////        public void paint(Graphics g) {
//////          super.paint(g);
//////          if (System.currentTimeMillis() - 300 < lastSearchInteraction || search.
//////                  isPendingCommit()) {
//////            search.setSize(search.getPreferredSize());
//////            Style s = search.getStyle();
//////            search.setX(getX() + getWidth() - search.getWidth() - s.getPadding(
//////                    RIGHT) - s.getMargin(RIGHT));
//////            search.setY(getScrollY() + getY());
//////            search.paintComponent(g, true);
//////          }
//////        }
//////
//////        public boolean animate() {
//////          boolean val = super.animate();
//////          if (lastSearchInteraction != -1) {
//////            search.animate();
//////            if (System.currentTimeMillis() - 300 > lastSearchInteraction && !search.
//////                    isPendingCommit()) {
//////              lastSearchInteraction = -1;
//////              search.clear();
//////            }
//////            return true;
//////          }
//////          return val;
//////        }
//////      };
////
////
////      JSONArray items = json.getJSONArray(Settings.ITEMS);
////      for (int i = 0; i < items.length(); i++) {
////        JSONObject item = items.getJSONObject(i);
////        String item_name = item.getString(Settings.ITEM_NAME);
////        list.addItem(item_name);
////        String href = Settings.ROOT_URL + "/" + item.getString(
////                Settings.ITEM_HREF);
////        href_list.addElement(href);
////      }
////
////      form.addComponent(BorderLayout.CENTER, list);
////      form.show();
////    }
////    catch (JSONException ex) {
////      ex.printStackTrace();
////    }
////
////  }
//  public static void info_display() {
//    try {
//      recent_form = form;
//      String form_title = json.getString(Settings.FORM_TITLE);
//      form = new Form(form_title);
//      form.setScrollable(true);
//      form.setLayout(new BorderLayout());
//      String info = json.getString(Settings.INFO);
//      info = Models.replace(info, Settings.INFO_SEPERATE_CHARACTER, "\n");
//      TextArea aboutText = new TextArea(info, 5, 10); //show info
//      aboutText.setEditable(false);
//      form.addComponent(BorderLayout.CENTER, aboutText);
//      form.show();
//    }
//    catch (JSONException ex) {
//      ex.printStackTrace();
//    }
//
//  }
//
//  public static void html_display() {
//    try {
//      recent_form = form;
//      String form_title = json.getString(Settings.FORM_TITLE);
//      form = new Form(form_title);
//      form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));
//      DocumentRequestHandler handler;
//      handler = new HttpRequestHandler();
//      HTMLComponent html = new HTMLComponent(handler);
//      html.setPage(json.getString(Settings.CONTENT_URL));
//      form.addComponent(html);
//      form.show();
//    }
//    catch (JSONException ex) {
//      ex.printStackTrace();
//    }
//  }
//
//  public static void image_display() {
//  }
//
//  public static void richtext_display() {
//    try {
//      recent_form = form;
//      String form_title = json.getString(Settings.FORM_TITLE);
//      form = new Form(form_title);
////      form.setTransitionInAnimator(Transition3D.createCube(300, true));
//      form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));
//      DocumentRequestHandler handler;
//      handler = new HttpRequestHandler();
//      HTMLComponent html = new HTMLComponent(handler);
//      html.setBodyText(json.getString(Settings.INFO));
//      form.addComponent(html);
//      form.show();
//    }
//    catch (JSONException ex) {
//      ex.printStackTrace();
//    }
//  }
//
//  public static void alert(String Title, String Message) {
//    Dialog.show(Title, Message, "Đóng", null);
//  }
//
//  public static void loading(String message) {
//    form = new Form();
//    Container cont = new Container();
//    cont.setLayout(new BoxLayout(BoxLayout.Y_AXIS));
//
//    Label label = new Label(message);
//    label.getStyle().setMargin(Component.TOP, Display.getInstance().
//            getDisplayHeight() / 3);
//    label.getStyle().setMargin(Component.LEFT, Display.getInstance().
//            getDisplayWidth() / 3);
//    label.setAlignment(Component.CENTER);
//
//    cont.addComponent(label);
//    form.addComponent(cont);
//    form.show();
//  }
//}
