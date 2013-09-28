package com.breakoutjs;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.TooManyListenersException;

import org.webbitserver.WebServer;
import org.webbitserver.WebServers;
import org.webbitserver.WebSocketConnection;
import org.webbitserver.WebSocketHandler;
import org.webbitserver.handler.StaticFileHandler;

public class SerialBridge implements WebSocketHandler, SerialPortEventListener {

    private BreakoutServer parent;
    private WebServer webServer;
    private WebSocketConnection singleConnection = null;
    protected SerialPort port;
    protected int netPort;
    protected InputStream input;
    protected OutputStream output;
    protected final int rate = 57600;
    protected final int parity = SerialPort.PARITY_NONE;
    protected final int databits = 8;
    protected final int stopbits = SerialPort.STOPBITS_1;
    private final String MULTI_CLIENT = "multiClient";
    private Set<WebSocketConnection> connections;
    private boolean isConnected = false;
    private boolean isMultiClientEnabled = false;
    private int count = 1;
    private int numConnections = 0;
    private ArduinoOSC ardOSC;
    /**
     *
     * @param port The network port number to connect on
     * @param parent A reference to the BreakoutServer instance
     * @param webRoot A relative path to the webserver root (default = "../")
     * @param isEnabled True if multi-client mode is enabled
     */
    public SerialBridge(int port, BreakoutServer parent, String webRoot, boolean isMultiClientEnabled) {
        //
        connections = new HashSet<WebSocketConnection>();

// this isn't too smart, but it works for now... need to refactor
        this.parent = parent;
        this.netPort = port;
        this.isMultiClientEnabled = isMultiClientEnabled;

        if (isMultiClientEnabled) {
            parent.printMessage("Modo Multi-cliente habilitado");
        }

        webServer = WebServers.createWebServer(port)
                .add("/websocket", this)
                .add(new StaticFileHandler(webRoot));
        ardOSC = new ArduinoOSC();
        ardOSC.conectar(webRoot, port);
        this.start();

    }

    /**
     * Start the web server
     */
    public void start() {

        webServer.start();
        parent.printMessage("Servidor ejecutandose en: " + webServer.getUri());

    }

    /**
     * Stop the web server
     */
    public void stop() {

        webServer.stop();


    }

    public void begin(String serialPortName, int baudRate) {
        try {
            Enumeration<?> portList = CommPortIdentifier.getPortIdentifiers();

            while (portList.hasMoreElements()) {
                CommPortIdentifier portId = (CommPortIdentifier) portList.nextElement();

                if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {

                    if (portId.getName().equals(serialPortName)) {
                        port = (SerialPort) portId.open("breakout server", 2000);
                        input = port.getInputStream();
                        output = port.getOutputStream();
                        port.setSerialPortParams(baudRate, databits, stopbits, parity);
                        port.notifyOnDataAvailable(true);
                        parent.printMessage("Conectado a IOBoard en: " + serialPortName);
                    }
                }
            }
            if (port == null) {
                System.out.println("Error: puerto no encontrado");
                parent.printMessage("Error: Puerto serial no encontrado.");
            }
        } catch (Exception e) {
            System.out.println("Error interno (Puerto Serial)");
            e.printStackTrace();
            port = null;
            input = null;
            output = null;
        }

        try {
            port.addEventListener(this);
        } catch (TooManyListenersException e) {
            e.printStackTrace();
        }
    }

    /**
     * Forward data from the serial input stream to the WebSocket output stream
     *
     * @param inputData
     */
    private void processInput(int inputData) {

        String value = Integer.toString(inputData);

// relay serial data to websocket
        if (isMultiClientEnabled) {
            broadcast(value);
        } else {
            this.singleConnection.send(value);
        }
    }

    synchronized public void serialEvent(SerialPortEvent serialEvent) {
        switch (serialEvent.getEventType()) {
            case SerialPortEvent.DATA_AVAILABLE:
                dataAvailable(serialEvent);
                break;
            default:
//System.out.println("other serial event: " + serialEvent);
                break;
        }

    }

    private void dataAvailable(SerialPortEvent serialEvent) {
        try {
            while (input.available() > 0) {
                int inputData = input.read();

// only send serial data when at least one client is connected
                if (isConnected) {
                    processInput(inputData);
                }
            }
        } catch (IOException e) {
        }
    }

    public void dispose() {
        try {
            if (input != null) {
                input.close();
            }
            if (output != null) {
                output.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        input = null;
        output = null;

// why is a thread necessary here?
        new Thread() {
            @Override
            public void run() {
                port.removeEventListener();
                port.close();
                port = null;
            }
        }.start();
    }

    /**
     * Write a byte to the serial output stream.
     *
     * @param data
     */
    public void writeByte(int data) {

        if (output == null) {
            return;
        }

        try {
            output.write((byte) data);
            //parent.printMessage(data+"");
            
            output.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Send data from the IOBoard to all connected clients
     *
     * @param data
     */
    private void broadcast(String data) {
        for (WebSocketConnection connection : connections) {
            connection.send(data);
        }
    }

    @Override
    public void onClose(WebSocketConnection connection) {
        parent.printMessage("Cliente " + connection.data("id") + " cerrado");
//parent.printMessage("Client closed");
        numConnections--;

        if (isMultiClientEnabled) {
            connections.remove(connection);
            if (connections.isEmpty()) {
                isConnected = false;
                numConnections = 0;
                count = 1;
            }
            parent.printMessage("Número de conexiones activas = " + numConnections);
        } else {
            this.singleConnection = null;
            isConnected = false;
            count = 1;
        }
    }

    @Override
    public void onMessage(WebSocketConnection connection, String message) {

        if (message.indexOf(',') > -1) {
            String data[] = message.split(",");
            for (int i = 0; i < data.length; i++) {
                writeByte(Integer.parseInt(data[i]));
            }
        } else {
            writeByte(Integer.parseInt(message));
        }
        parent.printMessage(message);
        ardOSC.enviar(message);

    }

    public void onMessage(WebSocketConnection connection, byte[] message) {
    }

    @Override
    public void onOpen(WebSocketConnection connection) {
        connection.data("id", count++);
        parent.printMessage("Cliente " + connection.data("id") + " conectado");
//parent.printMessage("Client connected");

        numConnections++;

// if multi-client connection is enabled, report status to client
        if (isMultiClientEnabled) {
            connection.send("configuración: " + MULTI_CLIENT);
            connections.add(connection);
            parent.printMessage("Número de conexiones activas = " + numConnections);
        } else {
            this.singleConnection = connection;
        }

        isConnected = true;
    }

    public void onPong(WebSocketConnection connection, String message) {
    }

    /**
     * The network port number.
     *
     * @return
     */
    public int getPort() {
        return netPort;
    }

    @Override
    public void onPing(WebSocketConnection wsc, byte[] bytes) throws Throwable {
        throw new UnsupportedOperationException("No soportado."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void onPong(WebSocketConnection wsc, byte[] bytes) throws Throwable {
        throw new UnsupportedOperationException("No soportado."); //To change body of generated methods, choose Tools | Templates.
    }
}