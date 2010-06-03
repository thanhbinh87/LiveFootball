package com.vinhcom.livefootball;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.wireless.messaging.MessageConnection;
import javax.wireless.messaging.TextMessage;


public class Models {

  public static String replace(String text, String searchString,
                               String replacementString) {
    StringBuffer sBuffer = new StringBuffer();
    int pos = 0;
    while ((pos = text.indexOf(searchString)) != -1) {
      sBuffer.append(text.substring(0, pos) + replacementString);
      text = text.substring(pos + searchString.length());
    }
    sBuffer.append(text);
    return sBuffer.toString();
  }

  public static String urlopen(String url) {
    HttpConnection cn = null;
    InputStream str = null;

    StringBuffer sb = null;
    try {
      int code;
      cn = (HttpConnection) Connector.open(url);
      code = cn.getResponseCode();
      if (code == HttpConnection.HTTP_OK) {
        str = cn.openInputStream();
        sb = new StringBuffer();

        InputStreamReader r = new InputStreamReader(str, "UTF-8");
        int total = 0;
        int read = 0;

        while ((read = r.read()) >= 0) {
          total++;
          sb.append((char) read);
        }
        return sb.toString();
      }
      else {
        throw new Exception("Gặp lỗi trong quá trình kết nối");
      }
    }
    catch (Exception e) {
      return null;
    }
    finally {
      try {
        if (str != null) {
          str.close();
        }
        if (cn != null) {
          cn.close();
        }
      }
      catch (Exception e) {
      }
    }
  }//end urlopen(String)

  public static String get_parent(String url) {
    if ((url == null) || url.equals("") || url.equals("/")) {
      return "";
    }
    if (url.endsWith("/index.txt")) {
      url = replace(url, "/index.txt", "");
    }
    int lastSlashPos = url.lastIndexOf('/');

    if (lastSlashPos >= 0) {
      return url.substring(0, lastSlashPos); //strip off the slash
    }
    else {
      return ""; //we expect people to add  + "/somedir on their own
    }
  }


  /** Sends an SMS message */
  public class SMSender
          implements Runnable {

    private String smsReceiverPort;
    private String message;
    private String phoneNumber;

    public SMSender(String smsReceiverPort) {
      this.smsReceiverPort = smsReceiverPort;
    }

    public void run() {
      StringBuffer addr = new StringBuffer(20);
      addr.append("sms://+");
      if (phoneNumber.length() == 11) {
        addr.append("86");//  china
      }
      addr.append(phoneNumber);
      // String address = "sms://+8613641301055";
      String address = addr.toString();

      MessageConnection smsconn = null;
      try {
        // Open the message connection.
        smsconn = (MessageConnection) Connector.open(address);
        // Create the message.
        TextMessage txtmessage = (TextMessage) smsconn.newMessage(
                MessageConnection.TEXT_MESSAGE);
        txtmessage.setAddress(address);// !!
        txtmessage.setPayloadText(message);
        smsconn.send(txtmessage);
      }
      catch (Exception e) {
        e.printStackTrace();
      }

      if (smsconn != null) {
        try {
          smsconn.close();
        }
        catch (IOException ioe) {
          ioe.printStackTrace();
        }
      }
    }

    public void send(String message, String phoneNumber) {
      this.message = message;
      this.phoneNumber = phoneNumber;
      Thread t = new Thread(this);
      t.start();
    }
  }
}




