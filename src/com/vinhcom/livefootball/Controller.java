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
    public String root_url = "http://localhost:4001/menu";
    public Vector href_list = new Vector();
    private Form form;
    private Command mBackCommand = new Command("Quay lại");
    private Command selectCommand = new Command("Chọn");
    private Command exitCommand = new Command("Thoát");

    private void list_display(String json_string) {
        String form_title = Utilities.get_text(json_string, "\"form_title\": \"", "\"");
        form = new Form(form_title);
        form.setLayout(new BorderLayout());
        List list = new List();
        String items_string = Utilities.get_text(json_string, "\"items\": [", "]");
        String items[];
        items = Utilities.split(items_string, "},");
        for (int i = 0; i < items.length; i++) {
            String item = Utilities.get_text(items[i], "\"name\": \"", "\"");
            list.addItem(item);
            String href = root_url + "/"
                        + Utilities.get_text(items[i], "\"href\": \"", "\"");
            System.out.println(href);
            href_list.addElement(href);
        }
        form.addComponent(BorderLayout.CENTER, list);
        form.show();
        
        form.addCommand(selectCommand);
        form.addCommand(exitCommand);
        form.setCommandListener(this);
    }

    private void tree_display(String json_string) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void actionPerformed(ActionEvent ae) {
//      System.out.println(ae.getCommand());
      if (ae.getCommand() == selectCommand) {
        System.out.println("Vừa bấm chọn");
//        form.show();
      }
    }

    public void startApp() {
      if (form == null) {
        Display.init(this);
        try {
            Resources r = Resources.open("/com/vinhcom/livefootball/default.res");
            UIManager.getInstance().setThemeProps(r.getTheme("default"));
        } catch (IOException ioe) {
            System.out.println("Couldn't load theme.");
        }
        String json_string = Utilities.urlopen(root_url);
        String type = Utilities.get_text(json_string, "\"type\": \"", "\"");
        if (type.equals("list")) {
            list_display(json_string);
        } else if (type.equals("tree")) {
            tree_display(json_string);
        }
      }
      else {
      form.show();
      }
    }
    public void pauseApp() {
    }

    public void destroyApp(boolean unconditional) {
    }
}
