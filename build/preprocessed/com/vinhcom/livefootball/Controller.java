package com.vinhcom.livefootball;

import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.midlet.*;

import com.sun.lwuit.*;
import com.sun.lwuit.events.*;
import com.sun.lwuit.layouts.BorderLayout;
import com.sun.lwuit.plaf.UIManager;
import com.sun.lwuit.util.Resources;
import java.io.IOException;
import java.util.Vector;
import com.vinhcom.livefootball.Utilities.*;
import java.util.Timer;
import java.util.TimerTask;

public class Controller extends MIDlet implements ActionListener {

  private String SELECT = "select";
  private String INFO = "info";
  private String BACK = "back";
  private String EXIT = "exit";
  private String LIST = "list";
  private String TYPE_STARTS_WITH = "\"type\": \"";
  private String HREF_STARTS_WITH = "\"href\": \"";
  private String NAME_STARTS_WITH = "\"name\": \"";
  private String FORM_TITLE_STARTS_WITH = "\"form_title\": \"";
  private String LEFT_BUTTON_STARTS_WITH = "\"left_button\": \"";
  private String RIGHT_BUTTON_STARTS_WITH = "\"right_button\": \"";
  private String AUTO_REFRESH_STARTS_WITH = "\"auto_refresh\": \"";
  private String INFO_STARTS_WITH = "\"info\": \"";
  private String DEFAULT_ENDS_WITH = "\"";
  private String ITEMS_STARTS_WITH = "\"items\": [";
  private String ITEMS_ENDS_WITH = "]";
  private String ITEMS_SPLITS_WITH = "},";
  private String theme_path = "/com/vinhcom/livefootball/default.res";
  private String theme = "default";
  private String root_url = "http://localhost:4001";
  private String url;
  private String recent_url;
  private String current_url;
  private String auto_refresh;
  private Command back_command = new Command("Quay lại");
  private Command select_command = new Command("Chọn");
  private Command exit_command = new Command("Thoát");
  private Vector href_list;
  private Form form;
  private List list;
  private Timer timer;
  private RefreshTimerTask reload;
  /*
   *
   * Điều khiển việc hiển thị list bất kỳ
   *
   */
  // TODO: nhấn back 2 lần thì biến recent_url bị sai

  private void list_display(String json_string) {
    String form_title = Utilities.get_text(json_string, FORM_TITLE_STARTS_WITH,
            DEFAULT_ENDS_WITH);
    form = new Form(form_title);  // tạo form với tên form lấy được trong chuỗi trả về
    list = new List();
    href_list = new Vector();
    form.setLayout(new BorderLayout());
    String items_string = Utilities.get_text(json_string, ITEMS_STARTS_WITH,
            ITEMS_ENDS_WITH);
    String items[];
    items = Utilities.split(items_string, ITEMS_SPLITS_WITH);
    for (int i = 0; i < items.length; i++) {
      String item = Utilities.get_text(items[i], NAME_STARTS_WITH,
              DEFAULT_ENDS_WITH);
      list.addItem(item);
      String href = root_url + "/" + Utilities.get_text(items[i], HREF_STARTS_WITH,
              DEFAULT_ENDS_WITH);
      System.out.println("href when parsing:" + href);
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

    auto_refresh = Utilities.get_text(json_string, AUTO_REFRESH_STARTS_WITH,
            DEFAULT_ENDS_WITH);
    if (auto_refresh != null) { // nếu tham số auto_refresh được thiết lập thì tự động refresh mỗi xxx ms định sẵn
      timer = new Timer();
      reload = new RefreshTimerTask();
      timer.schedule(reload, Integer.parseInt(auto_refresh));
    }

  }

  private void info_display(String json_string) {
    String form_title = Utilities.get_text(json_string, FORM_TITLE_STARTS_WITH,
            DEFAULT_ENDS_WITH);
    String info = Utilities.get_text(json_string, INFO_STARTS_WITH,
            DEFAULT_ENDS_WITH);
    form = new Form(form_title);
    form.setScrollable(false);
    form.setLayout(new BorderLayout());
    TextArea aboutText = new TextArea("line1\nline2\n\nline4", 5, 10); //show info
    aboutText.setEditable(false);
    form.addComponent(BorderLayout.CENTER, aboutText);
    form.addCommand(back_command);
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

    auto_refresh = Utilities.get_text(json_string, AUTO_REFRESH_STARTS_WITH,
            DEFAULT_ENDS_WITH);
    if (auto_refresh != null) { // nếu tham số auto_refresh được thiết lập thì tự động refresh mỗi xxx ms định sẵn
      timer = new Timer();
      reload = new RefreshTimerTask();
      timer.schedule(reload, Integer.parseInt(auto_refresh));
    }

  }

  private class RefreshTimerTask extends TimerTask {

    public final void run() {
      display(url);
    }
  }

  /*
   * Điều khiển việc thực hiện và chuyển hướng các lệnh (nút trái và phải)
   *
   */
  public void actionPerformed(ActionEvent ae) {
    if (timer != null) { // dừng tự động refresh (nếu có)
      timer.cancel();
    }
    if (ae.getCommand() == exit_command) {
      notifyDestroyed();
    } else if (ae.getCommand() == back_command) {
      display(recent_url);
    } else if (ae.getCommand() == select_command) { // nếu bấm nút "Chọn"
      int index = list.getSelectedIndex();  // lấy id của đối tượng được chọn (id bắt đầu từ 0)
      System.out.println("Href Index: " + index);
      url = (String) href_list.elementAt(index); // lấy url tương ứng với đối tượng được chọn
      System.out.println("URL to open:" + url);
      recent_url = current_url;
      current_url = url;
      display(url);
    }
  }

  private void display(String url) {
    String json_string = Utilities.urlopen(url);
    String type = Utilities.get_text(json_string, TYPE_STARTS_WITH,
            DEFAULT_ENDS_WITH);

    if (type.equals(LIST)) {
      list_display(json_string);
    } else if (type.equals(INFO)) {
      info_display(json_string);
    }
  }

  public void startApp() {
    try { // kiểm tra phiên bản mới
      platformRequest("http://localhost/latest.jad");
    } catch (ConnectionNotFoundException ex) {
    }
    Display.init(this);
    try {
      Resources r = Resources.open(theme_path);
      UIManager.getInstance().setThemeProps(r.getTheme(theme));
    } catch (IOException ioe) {
      System.out.println("Couldn't load theme.");
    }
    current_url = root_url;
    display(root_url);
  }

  public void pauseApp() {
  }

  public void destroyApp(boolean unconditional) {
  }
}
