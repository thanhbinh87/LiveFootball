package com.vinhcom.livefootball;





import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.List;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author win7_64
 */

public class Utilities {

    private static String root_url = "http://203.128.246.60:8000/";
    private static String matches_url = "http://203.128.246.60:8000/matches";
    private static String comments_url = "http://203.128.246.60:8000/comments";
    // Main MIDP display
    private Display myDisplay = null;
    // GUI component for submitting request
    private List list;
    private String[] menuItems;
    // GUI for matches
    private List matches_list;
    private String[] matches_menu;
    private List comments_list;
    private String[] comments_menu;
    private List odds_list;
    private String[] odds_menu;
    Vector bet_info_href = new Vector();

    private String[] get_matches() {
        String result;
        result = urlopen(matches_url);
        Vector menu = new Vector();
        String[] items = split(result, "/match>");

        for (int i = 0; i < items.length; i++) {
            String item = get_text(items[i], ">", "<");
            String href_link = get_text(items[i], "href2=\"", "\" ");
            if (item != null) {
                menu.addElement(item.trim());
                bet_info_href.addElement(root_url + href_link.trim());
            }
        }

        String[] _menu = new String[menu.size()];
        if (menu.size() > 0) {
            for (int i = 0; i < menu.size(); i++) {
                _menu[i] = (String) menu.elementAt(i);
            }
        }
        return _menu;
    }

    private String[] bet_info(int match_index) {
        String result;
        Vector odd_info = new Vector();
        String url = (String) bet_info_href.elementAt(match_index);
        url = replace(url, " ", "%20");
        url = replace(url, "|", "%7C");
        result = urlopen(url);
        String match = get_text(result, "<match>", "</match>");
        String source = get_text(result, "<source>", "</source>");
        String date = get_text(result, "<date>", "</date>");
        String hour = get_text(result, "<hour>", "</hour>");
        String team_1 = get_text(result, "<team_1>", "</team_1>");
        String team_2 = get_text(result, "<team_2>", "</team_2>");
        String ah = get_text(result, "<ah>", "</ah>");
        String ah_team_1 = get_text(result, "<ah_team_1>", "</ah_team_1>");
        String ah_team_2 = get_text(result, "<ah_team_2>", "</ah_team_2>");
        String ou = get_text(result, "<ou>", "</ou>");
        String ou_team_1 = get_text(result, "<ou_team_1>", "</ou_team_1>");
        String ou_team_2 = get_text(result, "<ou_team_2>", "</ou_team_2>");
        String ah1st = get_text(result, "<ah1st>", "</ah1st>");
        String ah1st_team_1 = get_text(result, "<ah1st_team_1>", "</ah1st_team_1>");
        String ah1st_team_2 = get_text(result, "<ah1st_team_2>", "</ah1st_team_2>");
        String ou1st = get_text(result, "<ou1st>", "</ou1st>");
        String ou1st_team_1 = get_text(result, "<ou1st_team_1>", "</ou1st_team_1>");
        String ou1st_team_2 = get_text(result, "<ou1st_team_2>", "</ou1st_team_2>");

        odd_info.addElement("Nguồn: " + source);
        odd_info.addElement("Ngày: " + date);
        odd_info.addElement("Giờ: " + hour);
        odd_info.addElement("Trận đấu: " + match);
        odd_info.addElement("Toàn trận");
        odd_info.addElement(" Chấp");
        odd_info.addElement("  " + team_1 + ": +" + ah + "@" + ah_team_1);
        odd_info.addElement("  " + team_2 + ": -" + ah + "@" + ah_team_2);
        odd_info.addElement(" Trên dưới");
        odd_info.addElement("  " + team_1 + ": +" + ou + "@" + ou_team_1);
        odd_info.addElement("  " + team_2 + ": -" + ou + "@" + ou_team_2);
        odd_info.addElement("Hiệp 1");
        odd_info.addElement(" Chấp");
        odd_info.addElement("  " + team_1 + ": +" + ah1st + "@" + ah1st_team_1);
        odd_info.addElement("  " + team_2 + ": -" + ah1st + "@" + ah1st_team_2);
        odd_info.addElement(" Trên dưới");
        odd_info.addElement("  " + team_1 + ": +" + ou1st + "@" + ou1st_team_1);
        odd_info.addElement("  " + team_2 + ": -" + ou1st + "@" + ou1st_team_2);


        String[] _menu = new String[odd_info.size()];
        if (odd_info.size() > 0) {
            for (int i = 0; i < odd_info.size(); i++) {
                _menu[i] = (String) odd_info.elementAt(i);
//                System.out.println(_menu[i]);
            }
        }
        return _menu;

    }

    private String[] get_comments() {
        String result;
        result = urlopen(comments_url);
        Vector menu = new Vector();
        String[] items = split(result, "/comment>");

        for (int i = 0; i < items.length; i++) {
            String item = get_text(items[i], ">", "<");
            if (item != null) {
                menu.addElement(item.trim());
                System.out.println(item);
            }
        }

        String[] _menu = new String[menu.size()];
        if (menu.size() > 0) {
            for (int i = 0; i < menu.size(); i++) {
                _menu[i] = (String) menu.elementAt(i);
            }
        }
        return _menu;

    }

    public static String replace(String text,
            String searchString,
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

    public static String get_text(String str, String tag) {
        return get_text(str, tag, tag);
    }

    public static String get_text(String str,
            String open,
            String close) {
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
        //DataInputStream strd = null;

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
}
