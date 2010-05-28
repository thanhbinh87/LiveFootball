package com.vinhcom.livefootball;

import javax.microedition.midlet.*;

import com.sun.lwuit.*;
import com.sun.lwuit.events.*;
import com.sun.lwuit.layouts.BorderLayout;
import com.sun.lwuit.plaf.UIManager;
import com.sun.lwuit.util.Resources;
import java.io.IOException;
import java.util.Vector;
import com.vinhcom.livefootball.Utilities.*;

public class Controller extends MIDlet implements ActionListener {
  private String SELECT = "select";
  private String BACK = "back";
  private String EXIT = "exit";
  private String LIST = "list";
  private String TREE = "tree";
  private String TYPE_STARTS_WITH = "\"type\": \"";
  private String HREF_STARTS_WITH = "\"href\": \"";
  private String NAME_STARTS_WITH = "\"name\": \"";
  private String FORM_TITLE_STARTS_WITH = "\"form_title\": \"";
  private String LEFT_BUTTON_STARTS_WITH = "\"left_button\": \"";
  private String RIGHT_BUTTON_STARTS_WITH = "\"right_button\": \"";
  private String DEFAULT_ENDS_WITH = "\"";
  private String ITEMS_STARTS_WITH = "\"items\": [";
  private String ITEMS_ENDS_WITH = "]";
  private String ITEMS_SPLITS_WITH = "},";

  private String theme_path = "/com/vinhcom/livefootball/default.res";
  private String theme = "default";
  

  private String root_url = "http://localhost:4001";
  private Command back_command = new Command("Quay lại");
  private Command select_command = new Command("Chọn");
  private Command exit_command = new Command("Thoát");
  private Vector href_list = new Vector();
  private Form form;
  private List list;

  /*
   *
   * Điều khiển việc hiển thị list bất kỳ
   *
   */
  private void list_display(String json_string) {
    String form_title = Utilities.get_text(json_string, FORM_TITLE_STARTS_WITH,
                                                        DEFAULT_ENDS_WITH);
    form = new Form(form_title);  // tạo form với tên form lấy được trong chuỗi trả về
    list = new List();
    form.setLayout(new BorderLayout());
    String items_string = Utilities.get_text(json_string, ITEMS_STARTS_WITH,
                                                          ITEMS_ENDS_WITH);
    String items[];
    items = Utilities.split(items_string, ITEMS_SPLITS_WITH);
    for (int i = 0; i < items.length; i++) {
      String item = Utilities.get_text(items[i], NAME_STARTS_WITH,
                                                 DEFAULT_ENDS_WITH);
      list.addItem(item);
      String href = root_url + "/" 
                  + Utilities.get_text(items[i], HREF_STARTS_WITH,
                                                 DEFAULT_ENDS_WITH);
      href_list.addElement(href);
    }
    form.addComponent(BorderLayout.CENTER, list);
    form.show();

    // Phân tích chuỗi trả về, lấy ra quy định bố trí nút bấm
    String left_button = Utilities.get_text(json_string, LEFT_BUTTON_STARTS_WITH,
                                                         DEFAULT_ENDS_WITH);
    String right_button = Utilities.get_text(json_string, RIGHT_BUTTON_STARTS_WITH,
                                                          DEFAULT_ENDS_WITH);

    if (left_button.equals(SELECT)) {
      form.addCommand(select_command);
    } else if (left_button.equals(EXIT)) {
      form.addCommand(exit_command);
    } else if (left_button.equals(BACK)) {
      form.addCommand(back_command);
    }

    if (right_button.equals(SELECT)) {
      form.addCommand(select_command);
    } else if (right_button.equals(EXIT)) {
      form.addCommand(exit_command);
    } else if (right_button.equals(BACK)) {
      form.addCommand(back_command);
    }
    form.setCommandListener(this); // Chờ đến khi có một nút được bấm
    }

  private void tree_display(String json_string) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /*
   * Điều khiển việc thực hiện và chuyển hướng các lệnh (nút trái và phải)
   *
   */
  public void actionPerformed(ActionEvent ae) {
    if (ae.getCommand() == exit_command) {
      // thoát
      } else if (ae.getCommand() == back_command) {
      // quay trở lại
      }

    if (ae.getCommand() == select_command) { // nếu bấm nút "Chọn"
      int index = list.getSelectedIndex();  // lấy id của đối tượng được chọn (id bắt đầu từ 0)
      String url = (String) href_list.elementAt(index); // lấy url tương ứng với đối tượng được chọn
      System.out.println(url);
      display(url);
    }
  }

  private void display(String url) {
    String json_string = Utilities.urlopen(url);
    String type = Utilities.get_text(json_string, TYPE_STARTS_WITH,
                                                  DEFAULT_ENDS_WITH);
    
    if (type.equals(LIST)) {
      list_display(json_string);
    } else if (type.equals(TREE)) {
      tree_display(json_string);
    }
  }

  public void startApp() {
      Display.init(this);
      try {
        Resources r = Resources.open(theme_path);
        UIManager.getInstance().setThemeProps(r.getTheme(theme));
      } catch (IOException ioe) {
        System.out.println("Couldn't load theme.");
      }
      display(root_url);
  }

  public void pauseApp() {
  }

  public void destroyApp(boolean unconditional) {
  }
}
