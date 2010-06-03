/**
 * TODO:
 * - Add Splash Screen
 * - Display Image in richtext/html
 * - Add icon to list
 * - Ấn Back quay lại list trỏ focus về mục vừa bấm (!= đầu tiên)
 */
package com.vinhcom.livefootball;

import javax.microedition.midlet.MIDlet;
import com.sun.lwuit.Command;
import com.sun.lwuit.Component;
import com.sun.lwuit.Container;
import com.sun.lwuit.Dialog;
import com.sun.lwuit.Display;
import com.sun.lwuit.Form;
import com.sun.lwuit.Label;
import com.sun.lwuit.List;
import com.sun.lwuit.animations.Transition3D;
import com.sun.lwuit.browser.HttpRequestHandler;
import com.sun.lwuit.events.ActionEvent;
import com.sun.lwuit.events.ActionListener;
import com.sun.lwuit.html.DocumentRequestHandler;
import com.sun.lwuit.html.HTMLComponent;
import com.sun.lwuit.layouts.BorderLayout;
import com.sun.lwuit.layouts.BoxLayout;
import com.sun.lwuit.plaf.UIManager;
import com.sun.lwuit.util.Resources;
import java.util.Timer;
import java.util.TimerTask;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;
import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;


public class LiveFootball
        extends MIDlet
        implements ActionListener {

  /**
   * Global Paramenters
   */
  private int status;
  private String url, recent_url, auto_refresh, button_url;
  private Timer timer;
  private RefreshTimerTask reload;
  private Command hyperlink_command, select_command, exit_command, reload_command;
  private Thread thread;
  private Vector href_list;
  private Form form;
  private List list;
  private JSONObject json;
  private Hashtable cache = new Hashtable();

  /**
   * Display Elements:
   * - List
   * - Two-lines List
   * - Richtext: Support HTML Format
   * - HTML: Display a WAP page
   * - Alert: Display Popup Notifications
   * - Loading: Display Loading Screen
   */
  public void list_display() {
    try {

      String form_title = json.getString(Settings.FORM_TITLE);
      // font.drawString(form_title, status, status, status, null);
      form = new Form(form_title) {

        public void keyPressed(int key_code) {
          System.out.println("Pressed keycode: " + key_code);

          if (key_code == -5) { // Thực hiện hành động SELECT
            if (timer != null) { // dừng tự động refresh (nếu có)
              timer.cancel();
            }
            /* lấy id của đối tượng được chọn (id bắt đầu từ 0) */
            int index = list.getSelectedIndex();

            /* lấy url tương ứng với đối tượng được chọn */
            url = (String) href_list.elementAt(index);
            progress_controller(url);
          }
          else {
            super.keyPressed(key_code);
          }
        }
      };

      form.setTransitionInAnimator(Transition3D.createCube(200, true));
      form.setLayout(new BorderLayout());
      href_list = new Vector();


      list = new List();

      JSONArray items = json.getJSONArray(Settings.ITEMS);
      for (int i = 0; i < items.length(); i++) {
        JSONObject item = items.getJSONObject(i);
        String item_name = item.getString(Settings.ITEM_NAME);
        list.addItem(item_name);
        list.isSmoothScrolling();
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

  private void richtext_display() {
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

  private void html_display() {
    try {
      String form_title = json.getString(Settings.FORM_TITLE);
      System.out.println(form_title);
      form = new Form(form_title);
      form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));
      DocumentRequestHandler handler;
      handler = new HttpRequestHandler();
      HTMLComponent html = new HTMLComponent(handler);
      html.setPage(json.getString(Settings.CONTENT_URL));
      System.out.println(json.getString(Settings.CONTENT_URL));
      form.addComponent(html);
      form.show();
    }
    catch (JSONException ex) {
      ex.printStackTrace();
    }
  }

  public static void image_display() {
  }

  private void alert(String Title, String Message) {
    Dialog.show(Title, Message, "Đóng", null);
  }

  private void loading(String message) {
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
    form.show();
  }


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
      JSONObject left_button = json.getJSONObject(Settings.LEFT_BUTTON);
      String left_button_type = left_button.getString(Settings.BUTTON_TYPE);
      String left_button_name = left_button.getString(Settings.BUTTON_NAME);

      JSONObject right_button = json.getJSONObject(Settings.RIGHT_BUTTON);
      String right_button_type = right_button.getString(Settings.BUTTON_TYPE);
      String right_button_name = right_button.getString(Settings.BUTTON_NAME);


      /**
       * Left Command
       */
      if (left_button_type.equals(Settings.SELECT)) {
        select_command = new Command(left_button_name);
        form.addCommand(select_command);
      }
      else if (left_button_type.equals(Settings.EXIT)) {
        exit_command = new Command(left_button_name);
        form.addCommand(exit_command);
      }
      else if (left_button_type.equals(Settings.RELOAD)) {
        reload_command = new Command(left_button_name);
        form.addCommand(reload_command);
      }
      else if (left_button_type.equals(Settings.HYPERLINK)) {
        hyperlink_command = new Command(left_button_name);
        form.addCommand(hyperlink_command);
        button_url = left_button.getString(Settings.BUTTON_URL);
      }

      /**
       * Right Command
       */
      if (right_button_type.equals(Settings.SELECT)) {
        select_command = new Command(right_button_name);
        form.addCommand(select_command);
      }
      else if (right_button_type.equals(Settings.EXIT)) {
        exit_command = new Command(right_button_name);
        form.addCommand(exit_command);
      }
      else if (right_button_type.equals(Settings.RELOAD)) {
        reload_command = new Command(right_button_name);
        form.addCommand(reload_command);
      }
      else if (right_button_type.equals(Settings.HYPERLINK)) {
        hyperlink_command = new Command(right_button_name);
        form.addCommand(hyperlink_command);
        button_url = right_button.getString(Settings.BUTTON_URL);
      }

      /**
       * Command Listener
       */
      form.setCommandListener(this); // Chờ đến khi có một nút được bấm
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
      content = (String) cache.get(url);
      System.out.println(content);
      if (content != null) {
      }
      if (content == null) {
        content = Models.urlopen(url);

      }
      json = new JSONObject(content);

      String cache_status = null;
      cache_status = json.getString("cache");
      if (cache_status.equals("yes")) {
        cache.put(url, content); //save to cache
      }

      /**
       * Get Display Type
       */
      String type = null;
      type = json.getString(Settings.TYPE);
      System.out.println(type);

      /**
       * Diplay Type Parser
       */
      if (type.equals(Settings.LIST)) {
        list_display();
        display_bottom_commands();
      }
      else if (type.equals(Settings.HTML)) {
        html_display();
        display_bottom_commands();
      }
      else if (type.equals(Settings.RICHTEXT)) {
        richtext_display();
        display_bottom_commands();

      }
      /**
       * nếu tham số auto_refresh được thiết lập thì tự động refresh mỗi xxx ms
       * định sẵn
       */
      auto_refresh = json.getString(Settings.AUTO_REFRESH);
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

      recent_url = url;

      status = 1;

    }
    catch (Exception e) {
      alert("Lỗi kết nối",
            "Không thể khởi tạo kết nối đến server.");
      System.out.println("Status Code: " + status);
      if (status != 1) {  // status = 1 tương ứng với chương trình đang chạy
        destroyApp(true);
        notifyDestroyed();
      }
      else {
        display(recent_url);
      }
    }
  }

  private void progress_controller(final String url) {
    if (cache.get(url) == null) {
      loading("Đang tải dữ liệu...");
      thread = new Thread() {

        public void run() {
          display(url);
        }
      };
      thread.start();

    }
    else {
      display(url);
    }
  }

  public void actionPerformed(ActionEvent ae) {
    if (ae.getCommand() == exit_command) {
      if (timer != null) { // dừng tự động refresh (nếu có)
        timer.cancel();
      }
      alert("Tạm biệt",
            "Cảm ơn bạn đã sử dụng phần mềm :). \n" +
            "Nếu có bất cứ thắc mắc gì, đừng ngần ngại liên hệ lại " +
            "với chúng tôi. Rất mong được gặp lại bạn!");
      destroyApp(true);
      notifyDestroyed();


    }
    else if (ae.getCommand() == hyperlink_command) {
      if (timer != null) { // dừng tự động refresh (nếu có)
        timer.cancel();
      }
      if (button_url.equals("/")) {
        url = Settings.ROOT_URL + "/index.html";
        progress_controller(url);
      }
      else {
        url = Settings.ROOT_URL + "/" + button_url;
        progress_controller(url);
      }
    }
    else if (ae.getCommand() == select_command) { // nếu bấm nút "Chọn"
      if (timer != null) { // dừng tự động refresh (nếu có)
        timer.cancel();
      }
      /* lấy id của đối tượng được chọn (id bắt đầu từ 0) */
      int index = list.getSelectedIndex();

      /* lấy url tương ứng với đối tượng được chọn */
      url = (String) href_list.elementAt(index);
      progress_controller(url);
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

    Display.init(this);
    Resources r = null;
    try {
      r = Resources.open(Settings.THEME_PATH);
    }
    catch (IOException ex) {
      ex.printStackTrace();
    }
    UIManager.getInstance().setThemeProps(r.getTheme(Settings.THEME));
    progress_controller(Settings.ROOT_URL + "/index.html");
  }

  public void pauseApp() {
  }

  public void destroyApp(boolean unconditional) {
    url = null;
    recent_url = null;
    auto_refresh = null;
    button_url = null;
    timer = null;
    reload = null;
    list = null;
    cache = null;
    href_list = null;
    json = null;
    list = null;
    status = 0;

  }
}
