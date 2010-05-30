package com.vinhcom.livefootball;

import com.sun.lwuit.Command;
import com.sun.lwuit.Dialog;
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
import java.util.Timer;
import java.util.TimerTask;
import javax.microedition.midlet.MIDlet;

public class LiveFootball extends MIDlet implements ActionListener {

  private String url;
  private String last_request;
  private String auto_refresh;
  private Vector href_list;
  private Form form;
  private List list;
  private Timer timer;
  private RefreshTimerTask reload;
  private Command back_command = new Command("Quay lại");
  private Command select_command = new Command("Chọn");
  private Command exit_command = new Command("Thoát");

  /*
   *
   * Display Elements: List, Info, HTML.
   *
   */
  private void list_display(String json_string) {
    String form_title = Utilities.get_text(json_string, Settings.FORM_TITLE_STARTS_WITH,
                                           Settings.DEFAULT_ENDS_WITH);
    form = new Form(form_title);  // tạo form với tên form lấy được trong chuỗi trả về
    list = new List();
    href_list = new Vector();
    form.setLayout(new BorderLayout());
    String items_string = Utilities.get_text(json_string, Settings.ITEMS_STARTS_WITH,
                                             Settings.ITEMS_ENDS_WITH);
    String items[];
    items = Utilities.split(items_string, Settings.ITEMS_SPLITS_WITH);
    for (int i = 0; i < items.length; i++) {
      String item = Utilities.get_text(items[i], Settings.NAME_STARTS_WITH,
                                       Settings.DEFAULT_ENDS_WITH);
      list.addItem(item);
      String href = Settings.ROOT_URL + "/" + Utilities.get_text(items[i], Settings.HREF_STARTS_WITH,
                                                                 Settings.DEFAULT_ENDS_WITH);
      href_list.addElement(href);
    }
    form.addComponent(BorderLayout.CENTER, list);
    form.show();

  }

  private void info_display(String json_string) {
    String form_title = Utilities.get_text(json_string, Settings.FORM_TITLE_STARTS_WITH,
                                           Settings.DEFAULT_ENDS_WITH);
    form = new Form(form_title);
    form.setScrollable(false);
    form.setLayout(new BorderLayout());
    String info = Utilities.get_text(json_string, Settings.INFO_STARTS_WITH,
                                     Settings.DEFAULT_ENDS_WITH); //không hiểu sao nếu đề \n ở đây lại không hoạt động :(
    String[] lines = Utilities.split(info, Settings.INFO_SEPERATE_CHARACTER);
    info = "";
    for (int i = 0; i < lines.length; i++) {
      info = info + lines[i] + "\n";
    }
    TextArea aboutText = new TextArea(info, 5, 10); //show info
    aboutText.setEditable(false);
    form.addComponent(BorderLayout.CENTER, aboutText);
    form.addCommand(back_command);
    form.show();

  }

  private void html_display(String json_string) {
    String form_title = Utilities.get_text(json_string, Settings.FORM_TITLE_STARTS_WITH,
                                           Settings.DEFAULT_ENDS_WITH);
    form = new Form(form_title);
    form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

    DocumentRequestHandler handler;
    handler = new HttpRequestHandler();

    HTMLComponent html = new HTMLComponent(handler);

    String info = Utilities.get_text(json_string, Settings.INFO_STARTS_WITH,
                                     Settings.DEFAULT_ENDS_WITH);
    html.setBodyText(info);
    form.addComponent(html);

    form.show();
  }

  private void alert(String Title, String Message) {
    Dialog.show(Title, Message, "Đồng ý", "Hủy");
  }

  /*
   * Timer Refresh Task
   */
  private class RefreshTimerTask extends TimerTask {

    public final void run() {
      display(url);
    }
  }

  public void actionPerformed(ActionEvent ae) {
    if (timer != null) { // dừng tự động refresh (nếu có)
      timer.cancel();
    }

    if (ae.getCommand() == exit_command) {
      notifyDestroyed();
    }
    else if (ae.getCommand() == back_command) {
      last_request = Utilities.get_parent(last_request) + "/index.html"; //index.html for test only
      display(last_request);
    }
    else if (ae.getCommand() == select_command) { // nếu bấm nút "Chọn"
      /* lấy id của đối tượng được chọn (id bắt đầu từ 0) */
      int index = list.getSelectedIndex();

      /* lấy url tương ứng với đối tượng được chọn */
      url = (String) href_list.elementAt(index);
      display(url);
    }
  }

  /*
   * Display Bottom Commands by server config
   */
  private void command_handler(String json_string) {
    String left_button = Utilities.get_text(json_string, Settings.LEFT_BUTTON_STARTS_WITH,
                                            Settings.DEFAULT_ENDS_WITH);
    String right_button = Utilities.get_text(json_string, Settings.RIGHT_BUTTON_STARTS_WITH,
                                             Settings.DEFAULT_ENDS_WITH);
    if (left_button.equals(Settings.SELECT)) {
      form.addCommand(select_command);
    }
    else if (left_button.equals(Settings.EXIT)) {
      form.addCommand(exit_command);
    }
    else if (left_button.equals(Settings.BACK)) {
      form.addCommand(back_command);
    }

    if (right_button.equals(Settings.SELECT)) {
      form.addCommand(select_command);
    }
    else if (right_button.equals(Settings.EXIT)) {
      form.addCommand(exit_command);
    }
    else if (right_button.equals(Settings.BACK)) {
      form.addCommand(back_command);
    }
    form.setCommandListener(this); // Chờ đến khi có một nút được bấm
  }

  private void display(String url) {
    try {
      String json_string = Utilities.urlopen(url);
      String type = Utilities.get_text(json_string, Settings.TYPE_STARTS_WITH,
                                       Settings.DEFAULT_ENDS_WITH);

      if (type.equals(Settings.LIST)) {
        list_display(json_string);
      }
      else if (type.equals(Settings.INFO)) {
        info_display(json_string);
      }
      else if (type.equals(Settings.HTML)) {
        html_display(json_string);
      }
      command_handler(json_string);
      /*
       * nếu tham số auto_refresh được thiết lập thì tự động refresh mỗi xxx ms
       * định sẵn
       */
      auto_refresh = Utilities.get_text(json_string, Settings.AUTO_REFRESH_STARTS_WITH,
                                        Settings.DEFAULT_ENDS_WITH);
      if (auto_refresh != null) {
        timer = new Timer();
        reload = new RefreshTimerTask();
        timer.schedule(reload, Integer.parseInt(auto_refresh));
      }

      last_request = url;
    }
    catch (Exception e) {
      alert("Lỗi",
            "Cấu hình phía server bị lỗi. Bạn không thể truy cập vào mục này" +
              "cho đến khi việc sửa đổi ở phía server hoàn tất.");
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
    System.out.println(Display.GAME_FIRE);
    System.out.println(Display.KEYBOARD_TYPE_NUMERIC);
    System.out.println(Display.KEYBOARD_TYPE_QWERTY);

    Display.init(this);
    try {
      Resources r = Resources.open(Settings.THEME_PATH);
      UIManager.getInstance().setThemeProps(r.getTheme(Settings.THEME));
    }
    catch (IOException ioe) {
//      System.out.println("Couldn't load theme.");
    }
    display(Settings.ROOT_URL);
  }

  public void pauseApp() {
  }

  public void destroyApp(boolean unconditional) {
  }
}
