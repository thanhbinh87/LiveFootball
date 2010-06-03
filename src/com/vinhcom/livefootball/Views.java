package com.vinhcom.livefootball;

import com.sun.lwuit.Component;
import com.sun.lwuit.Container;
import com.sun.lwuit.Font;
import com.sun.lwuit.Label;
import com.sun.lwuit.List;
import com.sun.lwuit.layouts.BorderLayout;
import com.sun.lwuit.layouts.BoxLayout;
import com.sun.lwuit.list.ListCellRenderer;


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

