/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.breakoutjs;

import com.illposed.osc.*;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author root
 */
public class testosc {

    InetAddress ia;

    public testosc() {
        try {
            ia = InetAddress.getByName("localhost");
        } catch (UnknownHostException ex) {
            Logger.getLogger(testosc.class.getName()).log(Level.SEVERE, null, ex);
        }



        OSCPortOut oscs = null;
        try {
            oscs = new OSCPortOut(ia, 9001);
        } catch (SocketException ex) {
            Logger.getLogger(testosc.class.getName()).log(Level.SEVERE, null, ex);
        }
        Object args[] = new Object[2];
        args[0] = new Integer(3);
        args[1] = "hello";
        OSCMessage msg = new OSCMessage("/sayhello", args);
        try {
            oscs.send(msg);


            //        try {
            //            receiver = new OSCPortIn(OSCPort.defaultSCOSCPort());
            //        } catch (SocketException ex) {
            //            Logger.getLogger(testosc.class.getName()).log(Level.SEVERE, null, ex);
            //        }
            //        OSCListener listener = new OSCListener() {
            //            public void acceptMessage(java.util.Date time, OSCMessage message) {
            //                System.out.println("Message received!");
            //            }
            //        };
            //        receiver.startListening();
            //        receiver.startListening();
        } catch (IOException ex) {
            Logger.getLogger(testosc.class.getName()).log(Level.SEVERE, null, ex);
        }




    }

    public static void main(String[] args) {

        testosc t = new testosc();

    }
}
