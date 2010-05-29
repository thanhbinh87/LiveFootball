package com.vinhcom.livefootball;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.wireless.messaging.MessageConnection;

public class Utilities {

  public static String replace(String text, String searchString, String replacementString) {
    StringBuffer sBuffer = new StringBuffer();
    int pos = 0;
    while ((pos = text.indexOf(searchString)) != -1) {
      sBuffer.append(text.substring(0, pos) + replacementString);
      text = text.substring(pos + searchString.length());
    }
    sBuffer.append(text);
    return sBuffer.toString();
  }

  public static String get_text(String str, String tag) {
    return get_text(str, tag, tag);
  }

  public static String get_text(String str, String open, String close) {
    if (str == null || open == null || close == null) {
      return null;
    }
    int start = str.indexOf(open);
    if (start != -1) {
      int end = str.indexOf(close, start + open.length());
      if (end != -1) {
        return str.substring(start + open.length(), end);
      }
    }
    return null;
  }

  public static boolean is_empty(String str) {
    return str == null || str.length() == 0;
  }

  public static String[] split(String original, String sep) {
    Vector nodes = new Vector();
    String separator = sep;
    int index = original.indexOf(separator);
    while (index >= 0) {
      nodes.addElement(original.substring(0, index));
      original = original.substring(index + separator.length());
      index = original.indexOf(separator);
    }
    nodes.addElement(original);

    String[] result = new String[nodes.size()];
    if (nodes.size() > 0) {
      for (int loop = 0; loop < nodes.size(); loop++) {
        result[loop] = (String) nodes.elementAt(loop);
      }
    }
    return result;
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
      } else {
        throw new Exception("Gặp lỗi trong quá trình kết nối");
      }
    } catch (Exception e) {
      return null;
    } finally {
      try {
        if (str != null) {
          str.close();
        }
        if (cn != null) {
          cn.close();
        }
      } catch (Exception e) {
      }
    }
  }//end urlopen(String)

  public static String get_parent(String url) {
    if ((url == null) || url.equals("") || url.equals("/")) {
      return "";
    }
    if (url.endsWith("/index.html")) {
      url = replace(url, "/index.html", "");
    }
    int lastSlashPos = url.lastIndexOf('/');

    if (lastSlashPos >= 0) {
      return url.substring(0, lastSlashPos); //strip off the slash
    } else {
      return ""; //we expect people to add  + "/somedir on their own
    }
  }

}
