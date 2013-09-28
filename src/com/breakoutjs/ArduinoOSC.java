/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.breakoutjs;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortOut;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author root
 */
public class ArduinoOSC {

    InetAddress ia;
    OSCPortOut oscs;
    String[][] dard1;

    public ArduinoOSC() {
        String[][] dard = {
            {"144", "1", "0"},
            {"144", "2", "0"},
            {"144", "1", "0"},
            {"144", "4", "0"},
            {"227", "127", "1"},
            {"144", "16", "0"},
            {"229", "127", "1"},
            {"230", "127", "1"},
            {"144", "0", "1"},
            {"145", "1", "0"},
            {"233", "127", "1"},
            {"234", "127", "0"},
            {"145", "8", "0"},
            {"145", "16", "0"},
            {"145", "32", "0"},
            {"145", "64", "0"},
            {"146", "1", "0"},
            {"146", "2", "0"},
            {"144", "4", "0"}, 
            {"146", "8", "0"},};
        dard1 = dard;
    }

    public boolean conectar(String servidor, int puerto) {
        try {
            ia = InetAddress.getByName("localhost");

            oscs = null;

            oscs = new OSCPortOut(ia, 9001);
        } catch (SocketException ex) {
            Logger.getLogger(testosc.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (UnknownHostException ex) {
            Logger.getLogger(ArduinoOSC.class.getName()).log(Level.SEVERE, null, ex);
        }
        return true;

    }

    public boolean enviar(String message) {
        String[] msg = message.split(",");
        String mess = "";

        if (msg.length > 2) {
            int n = 0;
            for (String[] dard : dard1) {
                if ((msg[0].equals(dard[0])) && (msg[1].equals(dard[1])) && (msg[2].equals(dard[2]))) {
                    mess = "" + n;
                }
                ++n;
            }
            if (mess != "") {
                Object args[] = new Object[1];
                args[0] = "1";
                OSCMessage msg1 = new OSCMessage("/pin" + mess, args);
                try {
                    oscs.send(msg1);

                } catch (IOException ex) {
                    Logger.getLogger(testosc.class.getName()).log(Level.SEVERE, null, ex);
                    return false;
                }
            }
        }
        return true;
    }
}
