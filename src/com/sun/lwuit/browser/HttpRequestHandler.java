/*
 * Copyright 2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */
package com.sun.lwuit.browser;

import com.sun.lwuit.html.DocumentInfo;
import com.sun.lwuit.html.DocumentRequestHandler;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;

/**
 * An implementation of DocumentRequestHandler that handles fetching HTML documents both from HTTP and from the JAR.
 * This request handler takes care of cookies, redirects and handles both GET and POST requests
 *
 * @author Ofir Leitner
 */
public class HttpRequestHandler implements DocumentRequestHandler {

    //Hashtable connections = new Hashtable();
    /**
     * A hastable containing all cookies - the table keys are domain names, while the value is another hashtbale containing a pair of cookie name and value.
     */
    static Hashtable cookies = Storage.getCookies();

    /**
     * A hastable containing all history - the table keys are domain names, while the value is a vector containing the visited links.
     */
    static Hashtable visitedLinks = Storage.getHistory();

    /**
     * If true will cache HTML pages, this also means that they will be buffered and read fully and only then passed to HTMLComponent - this can have memory implications.
     * Also note that for the cached HTMLs to be written Storage.RMS_ENABLED[TYPE_CACHE] should be true
     */
    static boolean CACHE_HTML=false;

    /**
     * If true will cache images, this also means that they will be buffered and read fully and only then passed to HTMLComponent - this can have memory implications.
     * Also note that for the cached HTMLs to be written Storage.RMS_ENABLED[TYPE_CACHE] should be true
     */
    static boolean CACHE_IMAGES=true;

    /**
     * Returns the domain string we use to identify visited link.
     * Note that this may be different than the domain name returned by HttpConnection.getHost
     * 
     * @param url The link URL
     * @return The link's domain
     */
    static String getDomainForLinks(String url) {
        String domain=null;
        if (url.startsWith("file:")) {
            return "localhost"; // Just a common name to store local files under
        } 
        int index=-1;
        if (url.startsWith("http://")) {
            index=7;
        } else if (url.startsWith("https://")) {
            index=8;
        }
        if (index!=-1) {
            domain=url.substring(index);
            index=domain.indexOf('/');
            if (index!=-1) {
                domain=domain.substring(0,index);
            }
        }
        return domain;
    }

    /**
     * {@inheritDoc}
     */
    public InputStream resourceRequested(DocumentInfo docInfo) {
        InputStream is=null;
        String url=docInfo.getUrl();

        String linkDomain=getDomainForLinks(url);

        // Visited links
        if (docInfo.getExpectedContentType()==DocumentInfo.TYPE_HTML) { // Only mark base documents as visited links
            
            if (linkDomain!=null) {
                Vector hostVisitedLinks=(Vector)visitedLinks.get(linkDomain);
                if (hostVisitedLinks==null) {
                    hostVisitedLinks=new Vector();
                    visitedLinks.put(linkDomain,hostVisitedLinks);
                }
                if (!hostVisitedLinks.contains(url)) {
                    hostVisitedLinks.addElement(url);
                    Storage.addHistory(linkDomain, url);
                }
            } else {
                System.out.println("Link domain null for "+url);
            }
        } 

        String params=docInfo.getParams();
        if ((!docInfo.isPostRequest()) && (params !=null) && (!params.equals(""))) {
            url=url+"?"+params;
        }

        // See if page/image is in the cache
        // caching will be used only if there are no parameters and no cookies (Since if they are this is probably dynamic content)
        boolean useCache=false;
        if (((docInfo.getExpectedContentType()==DocumentInfo.TYPE_HTML) && (CACHE_HTML) && ((params==null) || (params.equals(""))) && (!cookiesExistForDomain(linkDomain) )) ||
            ((docInfo.getExpectedContentType()==DocumentInfo.TYPE_IMAGE) && (CACHE_IMAGES)))
        {
            useCache=true;
            InputStream imageIS=Storage.getResourcefromCache(url);
            if (imageIS!=null) {
                return imageIS;
            }
        }

        // Handle the file protocol
        if (url.startsWith("file://")) {
            return getFileStream(docInfo);
        }

        try {
            HttpConnection hc = (HttpConnection)Connector.open(url);
            String encoding=null;
            if (docInfo.isPostRequest()) {
                encoding="application/x-www-form-urlencoded";
            }
            if (!docInfo.getEncoding().equals(DocumentInfo.ENCODING_ISO)) {
                encoding=docInfo.getEncoding();
            }
            //hc.setRequestProperty("Accept_Language","en-US");

            //String domain=hc.getHost(); // sub.domain.com / sub.domain.co.il
            String domain=linkDomain; // will return one of the following formats: sub.domain.com / sub.domain.co.il
            
            sendCookies(domain, hc);
            domain=domain.substring(domain.indexOf('.')); // .domain.com / .domain.co.il
            if (domain.indexOf('.',1)!=-1) { // Make sure that we didn't get just .com - TODO - however note that if the domain was domain.co.il - it can be here .co.il
                sendCookies(domain, hc);
            }

            if (encoding!=null) {
               hc.setRequestProperty("Content-Type", encoding);
            }

            if (docInfo.isPostRequest()) {
               hc.setRequestMethod(HttpConnection.POST);
               byte[] paramBuf=params.getBytes();
               hc.setRequestProperty("Content-Length", ""+paramBuf.length);
               OutputStream os=hc.openOutputStream();
               os.write(paramBuf);
               os.close();

               //os.flush(); // flush is said to be problematic in some devices, uncomment if it is necessary for your device
            }

            
            if (docInfo.getExpectedContentType()==DocumentInfo.TYPE_HTML) { //We perform these checks only for text (i.e. main page), for images we just send what the server sends and "hope for the best"
                String contentTypeStr=hc.getHeaderField("content-type").toLowerCase();
                if (contentTypeStr!=null) {
                    if ((contentTypeStr.startsWith("text/")) || (contentTypeStr.startsWith("application/xhtml")) || (contentTypeStr.startsWith("application/vnd.wap"))) {
                        docInfo.setExpectedContentType(DocumentInfo.TYPE_HTML);
                    } else if (contentTypeStr.startsWith("image/")) {
                        docInfo.setExpectedContentType(DocumentInfo.TYPE_IMAGE);
                        hc.close();
                        return getStream("<img src=\""+url+"\">",null);
                    } else {
                        hc.close();
                        return getStream("Content type "+contentTypeStr+" is not supported.","Error");
                    }
                }

                int charsetIndex = contentTypeStr.indexOf("charset=");
                String charset=contentTypeStr.substring(charsetIndex+8);
                if ((charset.startsWith("utf-8")) || (charset.startsWith("utf8"))) { //startwith to allow trailing white spaces
                    docInfo.setEncoding(DocumentInfo.ENCODING_UTF8);
                }

            }

            int i=0;
            while (hc.getHeaderFieldKey(i)!=null) {
                if (hc.getHeaderFieldKey(i).equals("set-cookie")) {
                    //addCookie(hc.getHeaderField(i), hc);
                    addCookie(hc.getHeaderField(i), url);
                }


                i++;
            }

            int response=hc.getResponseCode();
            if (response/100==3) { // 30x code is redirect
                String newURL=hc.getHeaderField("Location");
                if (newURL!=null) {
                    hc.close();
                    docInfo.setUrl(newURL);
                    //docInfo.setPostRequest(false); 
                    //docInfo.setParams(null); //reset params
                    return resourceRequested(docInfo);
                }
            }
            is = hc.openInputStream();

            if (useCache) {
                byte[] buf=getBuffer(is);
                Storage.addResourceToCache(url, buf,false);
                ByteArrayInputStream bais=new ByteArrayInputStream(buf);
                is.close();
                hc.close(); //all the data is in the buffer
                return bais;
            }

        } catch (IOException e) {
            System.out.println("HttpRequestHandler->IOException: "+e.getMessage());
        } catch (IllegalArgumentException e) { // For malformed URL
            System.out.println("HttpRequestHandler->IllegalArgumentException: "+e.getMessage());
        }

        return is;

    }

    /**
     * Checks if there are cookies stored on the client for the specified domain
     *
     * @param domain Teh domain to check for cookies
     * @return true if cookies for the specified domain exists, false otherwise
     */
    private boolean cookiesExistForDomain(String domain) {
        Object obj=cookies.get(domain);
        //System.out.println("Cookies for domain "+domain+": "+obj);
        if (obj==null) {
            int index=domain.indexOf('.');
            if (index!=-1) {
                domain=domain.substring(index); // .domain.com / .domain.co.il
                if (domain.indexOf('.',1)!=-1) { // Make sure that we didn't get just .com - TODO - however note that if the domain was domain.co.il - it can be here .co.il
                    obj=cookies.get(domain);
                    //System.out.println("Cookies for domain "+domain+": "+obj);
                }
            }
            
        }
        
        return (obj!=null);
    }

    /**
     * Sends the avaiable cookies for the given domain
     * 
     * @param domain The cookies domain
     * @param hc Teh HTTPConnection
     * @throws IOException
     */
    private void sendCookies(String domain,HttpConnection hc) throws IOException {
        //System.out.println("Sending cookies for "+domain);
        Hashtable hostCookies=(Hashtable)cookies.get(domain);
        if (hostCookies!=null) {
            for (Enumeration e=hostCookies.keys();e.hasMoreElements();) {
                String name = (String)e.nextElement();
                String value = (String)hostCookies.get(name);
                String cookie=name+"="+value;
                //System.out.println("Cookie sent: "+cookie);
                hc.setRequestProperty("cookie", cookie);

            }
        }

    }

    /**
     * Returns an Inputstream of the specified HTML text
     *
     * @param htmlText The text to get the stream from
     * @param title The page's title
     * @return an Inputstream of the specified HTML text
     */
    private InputStream getStream(String htmlText,String title) {
        String titleStr="";
        if (title!=null) {
            titleStr="<head><title>"+title+"</title></head>";
        }
        htmlText="<html>"+titleStr+"<body>"+htmlText+"</body></html>";
        ByteArrayInputStream bais = new ByteArrayInputStream(htmlText.getBytes());
        return bais;

    }

    /**
     * Adds the given cookie to the cookie collection
     * 
     * @param setCookie The cookie to add
     * @param hc The HttpConnection
     */
    private void addCookie(String setCookie,String url/*HttpConnection hc*/) {
        //System.out.println("Adding cookie: "+setCookie);
        String urlDomain=getDomainForLinks(url);

        // Determine cookie domain
        String domain=null;
        int index=setCookie.indexOf("domain=");
        if (index!=-1) {
            domain=setCookie.substring(index+7);
            index=domain.indexOf(';');
            if (index!=-1) {
                domain=domain.substring(0, index);
            }
            
            if (!urlDomain.endsWith(domain)) { //if (!hc.getHost().endsWith(domain)) {
                System.out.println("Warning: Cookie tried to set to another domain");
                domain=null;
            }
        }
        if (domain==null) {
            domain=urlDomain; //domain=hc.getHost();
        }

        // Check cookie expiry
        boolean save=false;
        index=setCookie.indexOf("expires=");
        if (index!=-1) { // Cookies without the expires= property are valid only for the current session and as such are not saved to RMS
            String expire=setCookie.substring(index+8);
            index=expire.indexOf(';');
            if (index!=-1) {
                expire=expire.substring(0, index);
            }
            save=true;
        }

        // Get cookie name and value
        index=setCookie.indexOf(';');
        if (index!=-1) {
            setCookie=setCookie.substring(0, index);
        }
        index=setCookie.indexOf('=');
        String name=setCookie;
        String value="";
        if (index!=-1) {
            name=setCookie.substring(0, index);
            value=setCookie.substring(index+1);
        }

        Hashtable hostCookies=(Hashtable)cookies.get(domain);
        if (hostCookies==null) {
            hostCookies=new Hashtable();
            cookies.put(domain,hostCookies);
        }
        hostCookies.put(name,value);

        if (save) { // Note that we save all cookies with expiry specified, while not checking the specific expiry date
            Storage.addCookie(domain, name, value);
        }

    }

    /**
     * This method is used when the requested document is a file in the JAR
     *
     * @param url The URL of the file
     * @return An InputStream of the specified file
     */
    private InputStream getFileStream(DocumentInfo docInfo) {
        String url=docInfo.getUrl();

        // If a from was submitted on a local file, just display the parameters
        if ((docInfo.getParams()!=null) && (!docInfo.getParams().equals(""))) {
            String method="GET";
            if (docInfo.isPostRequest()) {
                method="POST";
            }
            return getStream("<h2>Form submitted locally.</h2><b>Method:</b> "+method+"<br><br><b>Parameters:</b><br>"+docInfo.getParams()+"<hr><a href=\""+docInfo.getUrl()+"\">Continue to local URL</a>","Form Results");
        }

        url=url.substring(7); // Cut the file://

        int hash=url.indexOf('#'); //trim anchors
        if (hash!=-1) {
           url=url.substring(0,hash);
        }

        int param=url.indexOf('?'); //trim parameters, not relvant for files
        if (param!=-1) {
            url=url.substring(0, param);
        }

        return getClass().getResourceAsStream(url);
    }

    /**
     * Reads an inputstream completely and places it into a buffer
     * 
     * @param is The InputStream to read
     * @return A buffer containing the stream's contents
     * @throws IOException
     */
    private byte[] getBuffer(InputStream is) throws IOException {
            int chunk = 50000;
            byte[] buf = new byte[chunk];
            int i=0;
            int b = is.read();
            while (b!=-1) {
                if (i>=buf.length) {
                    byte[] tempbuf=new byte[buf.length+chunk];
                    for (int j=0;j<buf.length;j++) {
                        tempbuf[i]=buf[i];
                    }
                    buf=tempbuf;
                }
                buf[i]=(byte)b;
                i++;
                b = is.read();
            }
            byte[] tempbuf=new byte[i];
            for (int j=0;j<tempbuf.length;j++) {
                tempbuf[j]=buf[j];
            }

            buf=tempbuf;
            return buf;
    }

}

