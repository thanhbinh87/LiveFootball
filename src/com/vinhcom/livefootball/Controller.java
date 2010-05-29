package com.vinhcom.livefootball;

import javax.microedition.midlet.*;

import com.sun.lwuit.Command;
import com.sun.lwuit.Display;
import com.sun.lwuit.Form;
import com.sun.lwuit.List;
import com.sun.lwuit.TextArea;
import com.sun.lwuit.html.DocumentRequestHandler;
import com.sun.lwuit.html.HTMLComponent;
import com.sun.lwuit.layouts.BorderLayout;
import com.sun.lwuit.browser.HttpRequestHandler;
import com.sun.lwuit.events.ActionEvent;
import com.sun.lwuit.events.ActionListener;
import com.sun.lwuit.layouts.BoxLayout;
import com.sun.lwuit.plaf.UIManager;
import com.sun.lwuit.util.Resources;
import java.io.IOException;
import java.util.Vector;
import com.vinhcom.livefootball.Utilities.*;
import java.util.Timer;
import java.util.TimerTask;

public class Controller extends MIDlet implements ActionListener {

  private String VERSION = "1.0";
  private String SELECT = "select";
  private String BACK = "back";
  private String EXIT = "exit";
  private String LIST = "list";
  private String INFO = "info";
  private String HTML = "html";
  private String TYPE_STARTS_WITH = "\"type\": \"";
  private String HREF_STARTS_WITH = "\"href\": \"";
  private String NAME_STARTS_WITH = "\"name\": \"";
  private String FORM_TITLE_STARTS_WITH = "\"form_title\": \"";
  private String LEFT_BUTTON_STARTS_WITH = "\"left_button\": \"";
  private String RIGHT_BUTTON_STARTS_WITH = "\"right_button\": \"";
  private String AUTO_REFRESH_STARTS_WITH = "\"auto_refresh\": \"";
  private String INFO_STARTS_WITH = "\"info\": \"";
  private String INFO_SEPERATE_CHARACTER = "|";
  private String DEFAULT_ENDS_WITH = "\"";
  private String ITEMS_STARTS_WITH = "\"items\": [";
  private String ITEMS_ENDS_WITH = "]";
  private String ITEMS_SPLITS_WITH = "},";
  private String update_path = "http://localhost:4001/latest.jad";
  private String theme_path = "/com/vinhcom/livefootball/default.res";
  private String theme = "default";
  private String root_url = "http://localhost:4001";
  private String url;
  private String last_request;
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

  private void command_handler(String json_string) {
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
//      System.out.println("href when parsing:" + href);
      href_list.addElement(href);
    }
    form.addComponent(BorderLayout.CENTER, list);
    form.show();


    command_handler(json_string);

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
    form = new Form(form_title);
    form.setScrollable(false);
    form.setLayout(new BorderLayout());
    String info = Utilities.get_text(json_string, INFO_STARTS_WITH,
            DEFAULT_ENDS_WITH); //không hiểu sao nếu đề \n ở đây lại không hoạt động :(
    String[] lines = Utilities.split(info, INFO_SEPERATE_CHARACTER);
    info = "";
    for (int i = 0; i < lines.length; i++) {
      info = info + lines[i] + "\n";
    }
    TextArea aboutText = new TextArea(info, 5, 10); //show info
    aboutText.setEditable(false);
    form.addComponent(BorderLayout.CENTER, aboutText);
    form.addCommand(back_command);
    form.show();


    command_handler(json_string);

    auto_refresh = Utilities.get_text(json_string, AUTO_REFRESH_STARTS_WITH,
            DEFAULT_ENDS_WITH);
    if (auto_refresh != null) { // nếu tham số auto_refresh được thiết lập thì tự động refresh mỗi xxx ms định sẵn
      timer = new Timer();
      reload = new RefreshTimerTask();
      timer.schedule(reload, Integer.parseInt(auto_refresh));
    }

  }

  private void html_display(String json_string) {
    String form_title = Utilities.get_text(json_string, FORM_TITLE_STARTS_WITH,
            DEFAULT_ENDS_WITH);
    form = new Form(form_title);
    form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

    DocumentRequestHandler handler;
    handler = new HttpRequestHandler();

    HTMLComponent html = new HTMLComponent(handler);

    String info = Utilities.get_text(json_string, INFO_STARTS_WITH,
            DEFAULT_ENDS_WITH);
    html.setBodyText(info);
    form.addComponent(html);

    form.show();


    command_handler(json_string);

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
      last_request = Utilities.get_parent(last_request) + "/index.html"; //index.html for test only
      display(last_request);
    } else if (ae.getCommand() == select_command) { // nếu bấm nút "Chọn"
      int index = list.getSelectedIndex();  // lấy id của đối tượng được chọn (id bắt đầu từ 0)
//      System.out.println("Href Index: " + index);
      url = (String) href_list.elementAt(index); // lấy url tương ứng với đối tượng được chọn
//      System.out.println("URL to open:" + url);
      display(url);
    }
  }

  private void display(String url) {
    try {
      String json_string = Utilities.urlopen(url);
      String type = Utilities.get_text(json_string, TYPE_STARTS_WITH,
              DEFAULT_ENDS_WITH);

      if (type.equals(LIST)) {
        list_display(json_string);
      } else if (type.equals(INFO)) {
        info_display(json_string);
      } else if (type.equals(HTML)) {
        html_display(json_string);
      }

      last_request = url;
    } catch (Exception e) {
    }
  }

  public void startApp() {
//    try { // kiểm tra phiên bản mới
//      String jad_content = Utilities.urlopen(update_path);
//      String version = Utilities.get_text(jad_content, "MIDlet-Version: ", "\n");
//      System.out.println(version);
//      System.out.println(VERSION);
//      if (version.trim().equals(VERSION.trim())) {
//         System.out.println("không cần nâng cấp");
//      }
//      else {
//        System.out.println("cần nâng cấp");
//        // TODO: hiện thông báo hỏi cập nhật, nếu đồng ý thì tải, nếu không đồng ý chuyển biến VERSION thành phiên bản hiện tại
//        platformRequest(update_path);
//      }
//    } catch (ConnectionNotFoundException ex) {
//    }
    Display.init(this);
    try {
      Resources r = Resources.open(theme_path);
      UIManager.getInstance().setThemeProps(r.getTheme(theme));
    } catch (IOException ioe) {
//      System.out.println("Couldn't load theme.");
    }
    display(root_url);
  }

  public void pauseApp() {
  }

  public void destroyApp(boolean unconditional) {
  }
}
