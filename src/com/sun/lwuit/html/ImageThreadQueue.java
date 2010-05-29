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
package com.sun.lwuit.html;

import com.sun.lwuit.Display;
import com.sun.lwuit.Image;
import com.sun.lwuit.Label;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Vector;




/**
 * ImageThreadQueue is a thread queue used to create and manage threads that download images that were referred from HTML pages
 *
 * @author Ofir Leitner
 */
class ImageThreadQueue {

    /**
     * The default number of maximum threads used for image download
     */
    private static int DEFAULT_MAX_THREADS = 2;

    HTMLComponent htmlC;
    Vector queue = new Vector();
    Vector running = new Vector();
    Vector urls = new Vector();
    static int maxThreads = DEFAULT_MAX_THREADS;
    int threadCount;
    boolean started;

    /**
     * Constructs the queue
     * 
     * @param htmlC The HTMLComponent this queue belongs to
     */
    ImageThreadQueue(HTMLComponent htmlC) {
        this.htmlC=htmlC;
    }

    /**
     * Sets the maximum number of threads to use for image download
     * If startRunning was already called, this will takes effect in the next page loaded.
     *
     * @param threadsNum the maximum number of threads to use for image download
     */
    static void setMaxThreads(int threadsNum) {
        maxThreads=threadsNum;
    }

    /**
     * Adds the image to the queue
     *
     * @param imgLabel The label in which the image should be contained after loaded
     * @param imageUrl The URL this image should be fetched from
     */
    synchronized void add(Label imgLabel,String imageUrl) {
        if (started) {
            throw new IllegalStateException("ImageThreadQueue alreadey started! stop/cancel first");
        }
        int index=urls.indexOf(imageUrl);
        if (index!=-1) {
            ImageThread t=(ImageThread)queue.elementAt(index);
            t.addLabel(imgLabel);
        } else {
            ImageThread t =  new ImageThread(imageUrl, imgLabel, htmlC, this);
            queue.addElement(t);
            urls.addElement(imageUrl);
        }
    }

    /**
     * Returns the queue size
     *
     * @return the queue size
     */
    int getQueueSize() {
        return queue.size();
    }

    /**
     * Notifies the queue that all images have been queues and it can start dequeuing and download the images.
     * The queue isn't started before that to prevent multiple downloads of the same image
     */
    synchronized void startRunning() {
        urls=new Vector(); //reset URL vector
        int threads=Math.min(queue.size(), maxThreads);
        started=(threads>0);
        for(int i=0;i<threads;i++) {
            ImageThread t=(ImageThread)queue.firstElement();
            queue.removeElementAt(0);
            running.addElement(t);
            threadCount++;
            new Thread(t).start();
        }

        if (!started) {
            htmlC.setPageStatus(HTMLCallback.STATUS_COMPLETED);
        }

    }

    /**
     * Called by the ImageThread when it finishes downloading and setting the image.
     * This in turns starts another thread if the queue is not empty
     * 
     * @param finishedThread The calling thread
     * @param success true if the image download was successful, false otherwise
     */
    synchronized void threadFinished(ImageThread finishedThread,boolean success) {
        running.removeElement(finishedThread);

        if (queue.size()>0) {
            ImageThread t=(ImageThread)queue.firstElement();
            queue.removeElementAt(0);
            running.addElement(t);
            new Thread(t).start();
        } else {
            threadCount--;
        }

        started=(threadCount>0);
        if (!started) {
            htmlC.setPageStatus(HTMLCallback.STATUS_COMPLETED);
        }

    }

    /**
     * Discards the entire queue and signals the running threads to cancel.
     * THis will be triggered if the user cancelled the page or moved to another page.
     */
    synchronized void discardQueue() {
        queue.removeAllElements();
        for(Enumeration e=running.elements();e.hasMoreElements();) {
            ImageThread t = (ImageThread)e.nextElement();
            t.cancel();
        }
        running.removeAllElements();
        threadCount=0;
        started=false;

    }

    /**
     * Returns a printout of the threads queue, can be used for debugging
     *
     * @return a printout of the threads queue
     */
    public String toString() {
        String str=("---- Running ----\n");
        int i=1;
        for(Enumeration e=running.elements();e.hasMoreElements();) {
            ImageThread t = (ImageThread)e.nextElement();
            str+="#"+i+": "+t.imageUrl+"\n";
            i++;
        }
        i=1;
        str+="Queue:\n";
        for(Enumeration e=queue.elements();e.hasMoreElements();) {
            ImageThread t = (ImageThread)e.nextElement();
            str+="#"+i+": "+t.imageUrl+"\n";
            i++;
        }
        str+="---- count:"+threadCount+" ----\n";
        return str;
    }



}
/**
 * An ImageThread downloads an Image as requested
 *
 * @author Ofir Leitner
 */
class ImageThread implements Runnable {

    Label imgLabel;
    Vector labels;
    String imageUrl;
    DocumentRequestHandler handler;
    ImageThreadQueue threadQueue;
    boolean cancelled;
    HTMLComponent htmlC;
    Image img;

    /**
     * Constructs the ImageThread
     * 
     * @param imgLabel The label in which the image should be contained after loaded
     * @param imageUrl The URL this image should be fetched from
     * @param handler The RequestHandler through which to retrieve the image
     * @param threadQueue The main queue, for callback purposes
     */
    ImageThread(String imageUrl, Label imgLabel,HTMLComponent htmlC,ImageThreadQueue threadQueue) {
        this.imageUrl=imageUrl;
        this.imgLabel=imgLabel;
        this.handler=htmlC.getRequestHandler();
        this.threadQueue=threadQueue;
        this.htmlC=htmlC;
    }

    /**
     * Cancels this thread
     */
    void cancel() {
        cancelled=true;
    }

    /**
     * Adds a label which has the same URL, useful for duplicate images in the same page
     * 
     * @param label A label which has the same image URL
     */
    void addLabel(Label label) {
        if (labels==null) {
            labels=new Vector();
        }
        labels.addElement(label);
    }

    /**
     * {@inheritDoc}
     */
    public void run() {
        try {
            InputStream is = handler.resourceRequested(new DocumentInfo(imageUrl,DocumentInfo.TYPE_IMAGE));
            if (is==null) {
                if (htmlC.getHTMLCallback()!=null) {
                    htmlC.getHTMLCallback().parsingError(HTMLCallback.ERROR_IMAGE_NOT_FOUND, null, null, null, "Image not found at "+imageUrl);
                }
            } else {
                img=Image.createImage(is);
                if (img==null) {
                    if (htmlC.getHTMLCallback()!=null) {
                        htmlC.getHTMLCallback().parsingError(HTMLCallback.ERROR_IMAGE_BAD_FORMAT, null, null, null, "Image could not be created from "+imageUrl);
                    }
                }
            }


            if (img==null) {
                threadQueue.threadFinished(this,false);
                return;
            }
            if (!cancelled) {
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        handleImage(img,imgLabel);
                        if (labels!=null) {
                            for(Enumeration e=labels.elements();e.hasMoreElements();) {
                                Label label=(Label)e.nextElement();
                                handleImage(img,label);
                            }
                        }
                    }
                });
                threadQueue.threadFinished(this,true);
            }
        } catch (IOException ioe) {
            htmlC.getHTMLCallback().parsingError(HTMLCallback.ERROR_IMAGE_BAD_FORMAT, null, null, null, "Image could not be created from "+imageUrl+": "+ioe.getMessage());
            if(!cancelled) {
                threadQueue.threadFinished(this,false);
            }
            //threadQueue.threadFinished(imgLabel,imageUrl,false);
        }

    }

    /**
     * After a successful download, this handles placing the image on the label and resizing if necessary
     *
     * @param img The image
     * @param label The label to apply the image on
     */
    private void handleImage(Image img,Label label) {
        int width=label.getPreferredW();   // Was set in HTMLComponent.handleImage if the width attribute was in the tag
        int height=label.getPreferredH();


        if (width==0) { // Width wasn't specified - get from image
            width=img.getWidth();
            label.setPreferredW(width);
            label.setWidth(width);
            htmlC.revalidate();
        }
        if (height==0) { // Height wasn't specified - get from image
            height=img.getHeight();
            label.setPreferredH(height);
            label.setHeight(height);
            if (label.getParent().getPreferredH()<height) { // An empty newline, or one with 0 height is set to the height of the font height. If one of the components is an image that "grew" it has to be adapted
                label.getParent().setPreferredH(height);
            }
            htmlC.revalidate();
        }
        label.setIcon(img.scaled(width, height)); // If width+height are the same no processing will be done (checked in Image.scaled)
        label.getUnselectedStyle().setBorder(null); //remove the border which is a sign the image is loading

    }

}
