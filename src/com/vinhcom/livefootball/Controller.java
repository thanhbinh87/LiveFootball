/**
 * TODO:
 * - Khi có nút RELOAD, nhấn liên tục nút này thì thứ tự menu trái/phả bị đảo lộn
 * - Nhấn vào Odd Info rồi back lại, để như vậy khoảng 1 phút thì tự động chuyển lại OddInfo
 * - Add Splash Screen
 * - Display Image in richtext/html
 * - Add icon to list
 */
package com.vinhcom.livefootball;

import javax.microedition.midlet.MIDlet;
import com.sun.lwuit.Command;
import com.sun.lwuit.Display;
import com.sun.lwuit.events.ActionEvent;
import com.sun.lwuit.events.ActionListener;
import com.sun.lwuit.plaf.UIManager;
import com.sun.lwuit.util.Resources;
import java.util.Timer;
import java.util.TimerTask;
import java.io.IOException;
import org.json.me.JSONException;
import org.json.me.JSONObject;


public class Controller
        extends MIDlet
        implements ActionListener {

  /**
   * Global Paramenters
   */
  private String url;
  private String auto_refresh;
  private String button_url;
  private Timer timer;
  private RefreshTimerTask reload;
  private Command hyperlink_command;
  private Command select_command;
  private Command exit_command;
  private Command reload_command;


  /**
   * Timer Refresh Task
   */
  private class RefreshTimerTask
          extends TimerTask {

    public final void run() {
//      System.out.println("Reload with: " + url);
      display(url);
    }
  }

  /**
   * Display Bottom Commands by server config
   */
  private void display_bottom_commands() {
    try {
      /**
       * Button Properties
       */
      JSONObject left_button = Views.json.getJSONObject(Settings.LEFT_BUTTON);
      String left_button_type = left_button.getString(Settings.BUTTON_TYPE);
      String left_button_name = left_button.getString(Settings.BUTTON_NAME);

      JSONObject right_button = Views.json.getJSONObject(Settings.RIGHT_BUTTON);
      String right_button_type = right_button.getString(Settings.BUTTON_TYPE);
      String right_button_name = right_button.getString(Settings.BUTTON_NAME);

      /**
       * Left Command
       */
      if (left_button_type.equals(Settings.SELECT)) {
        select_command = new Command(left_button_name);
        Views.form.addCommand(select_command);
      }
      else if (left_button_type.equals(Settings.EXIT)) {
        exit_command = new Command(left_button_name);
        Views.form.addCommand(exit_command);
      }
      else if (left_button_type.equals(Settings.RELOAD)) {
        reload_command = new Command(left_button_name);
        Views.form.addCommand(reload_command);
      }
      else if (left_button_type.equals(Settings.HYPERLINK)) {
        hyperlink_command = new Command(left_button_name);
        Views.form.addCommand(hyperlink_command);
        button_url = left_button.getString(Settings.BUTTON_URL);
      }

      /**
       * Right Command
       */
      if (right_button_type.equals(Settings.SELECT)) {
        select_command = new Command(right_button_name);
        Views.form.addCommand(select_command);
      }
      else if (right_button_type.equals(Settings.EXIT)) {
        exit_command = new Command(right_button_name);
        Views.form.addCommand(exit_command);
      }
      else if (right_button_type.equals(Settings.RELOAD)) {
        reload_command = new Command(right_button_name);
        Views.form.addCommand(reload_command);
      }
      else if (right_button_type.equals(Settings.HYPERLINK)) {
        hyperlink_command = new Command(right_button_name);
        Views.form.addCommand(hyperlink_command);
        button_url = right_button.getString(Settings.BUTTON_URL);
      }

      /**
       * Command Listener
       */
      Views.form.setCommandListener(this); // Chờ đến khi có một nút được bấm
    }
    catch (JSONException ex) {
      /**
       * If JSON has error
       */
      ex.printStackTrace();
    }
  }

  private void display(final String url) {
    try {
      /**
       * NOTE: Data from server must is UTF-8 without BOM
       * Get Data From Cache. If not exist, get it from Server.
       */
      String content;
      content = (String) Views.cache.get(url);
      if (content != null) {
        System.out.println(
                "Found cache for " + url);
      }
      if (content == null) {
        content = Models.urlopen(url);
        System.out.println("Getting new data from server for url: " + url);
      }

      Views.json = new JSONObject(content);
      String cache_status = Views.json.getString("cache");
      if (cache_status.equals("yes")) {
        Views.cache.put(url, content); //save to cache
      }

      /**
       * Get Display Type
       */
      String type = Views.json.getString(Settings.TYPE);

      /**
       * Diplay Type Parser
       */
      if (type.equals(Settings.LIST)) {
        Views.list_display();
      }
      else if (type.equals(Settings.INFO)) {
        Views.info_display();
      }
      else if (type.equals(Settings.HTML)) {
        Views.html_display();
      }
      else if (type.equals(Settings.RICHTEXT)) {
        Views.loading("Đang tải dữ liệu...").show();
        new Thread() {

          public void run() {
            try {
              Views.richtext_display();
              display_bottom_commands();
              auto_refresh = Views.json.getString(Settings.AUTO_REFRESH);
              if (auto_refresh.equals("0") || (auto_refresh == null)) {
              }
              else {
                if (timer != null) {
                  timer.cancel(); // dừng tự động refresh (nếu có)
                }
                timer = new Timer();
                reload = new RefreshTimerTask();
                timer.schedule(reload, Integer.parseInt(auto_refresh));
              }
            }
            catch (JSONException ex) {
              ex.printStackTrace();
            }
          }
        }.start();

      }
      /**
       * Parse and Display Bottom Commands
       */
      display_bottom_commands();

      /**
       * nếu tham số auto_refresh được thiết lập thì tự động refresh mỗi xxx ms
       * định sẵn
       */
      auto_refresh = Views.json.getString(Settings.AUTO_REFRESH);
      if (auto_refresh.equals("0") || (auto_refresh == null)) {
      }
      else {
        if (timer != null) {
          timer.cancel(); // dừng tự động refresh (nếu có)
        }
        timer = new Timer();
        reload = new RefreshTimerTask();
        timer.schedule(reload, Integer.parseInt(auto_refresh));
      }
    }
    catch (Exception e) {
      Views.alert("Lỗi",
                  "Cấu hình phía server bị lỗi. " +
                  "Bạn không thể truy cập vào mục này" +
                  "cho đến khi việc sửa đổi ở phía server hoàn tất.");
    }
  }

  public void actionPerformed(ActionEvent ae) {
    if (ae.getCommand() == exit_command) {
      if (timer != null) { // dừng tự động refresh (nếu có)
        timer.cancel();
      }
      Views.alert("Tạm biệt",
                  "Cảm ơn bạn đã sử dụng phần mềm :).\n" +
                  "Nếu có bất cứ thắc mắc gì, đừng ngần ngại liên hệ lại" +
                  "với chúng tôi. Rất mong được gặp lại bạn!");
      destroyApp(true);
      notifyDestroyed();
    }
    else if (ae.getCommand() == hyperlink_command) {
      if (timer != null) { // dừng tự động refresh (nếu có)
        timer.cancel();
      }
      if (button_url.equals("/")) {
        display(Settings.ROOT_URL);
      }
      else {
        display(Settings.ROOT_URL + "/" + button_url);
      }
    }
    else if (ae.getCommand() == select_command) { // nếu bấm nút "Chọn"
      if (timer != null) { // dừng tự động refresh (nếu có)
        timer.cancel();
      }
      /* lấy id của đối tượng được chọn (id bắt đầu từ 0) */
      int index = Views.list.getSelectedIndex();

      /* lấy url tương ứng với đối tượng được chọn */
      url = (String) Views.href_list.elementAt(index);
      display(url);
    }
    else if (ae.getCommand() == reload_command) {
      if (timer != null) { // dừng tự động refresh (nếu có)
        timer.cancel();
      }
      /* lấy id của đối tượng được chọn (id bắt đầu từ 0) */
      display(url);
    }
  }

  public void startApp() {
//    try { // kiểm tra phiên bản mới
//      String jad_content = Models.urlopen(update_path);
//      String version = Models.get_text(jad_content, "MIDlet-Version: ", "\n");
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
    Resources r = null;
    try {
      r = Resources.open(Settings.THEME_PATH);
    }
    catch (IOException ex) {
      ex.printStackTrace();
    }
    UIManager.getInstance().setThemeProps(r.getTheme(Settings.THEME));

    display(Settings.ROOT_URL);
  }

  public void pauseApp() {
  }

  public void destroyApp(boolean unconditional) {
    url = null;
    auto_refresh = null;
    button_url = null;
    timer = null;
    reload = null;
    Views.list = null;
    Views.cache = null;
    Views.href_list = null;
    Views.json = null;
    Views.list = null;

  }
}
