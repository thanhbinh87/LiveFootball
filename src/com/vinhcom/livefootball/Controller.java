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
  private String HREF_TEMPLATE = "";
  private String TYPE_TEMPLATE = "";
  private String NAME_TEMPLATE = "";
  

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
    String form_title = Utilities.get_text(json_string, "\"form_title\": \"", "\"");
    form = new Form(form_title);  // tạo form với tên form lấy được trong chuỗi trả về
    list = new List();
    form.setLayout(new BorderLayout());
    String items_string = Utilities.get_text(json_string, "\"items\": [", "]");
    String items[];
    items = Utilities.split(items_string, "},");
    for (int i = 0; i < items.length; i++) {
      String item = Utilities.get_text(items[i], "\"name\": \"", "\"");
      list.addItem(item);
      String href = root_url + "/" + Utilities.get_text(items[i], "\"href\": \"", "\"");
      href_list.addElement(href);
    }
    form.addComponent(BorderLayout.CENTER, list);
    form.show();

    // Phân tích chuỗi trả về, lấy ra quy định bố trí nút bấm
    String left_button = Utilities.get_text(json_string, "\"left_button\": \"", "\"");
    String right_button = Utilities.get_text(json_string, "\"right_button\": \"", "\"");

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
    String type = Utilities.get_text(json_string, "\"type\": \"", "\"");
    
    if (type.equals(LIST)) {
      list_display(json_string);
    } else if (type.equals(TREE)) {
      tree_display(json_string);
    }
  }

  public void startApp() {
      Display.init(this);
      try {
        Resources r = Resources.open("/com/vinhcom/livefootball/default.res");
        UIManager.getInstance().setThemeProps(r.getTheme("default"));
      } catch (IOException ioe) {
        System.out.println("Couldn't load theme.");
      }
      String url = root_url + "/menu";
      display(url);
  }

  public void pauseApp() {
  }

  public void destroyApp(boolean unconditional) {
  }
}
