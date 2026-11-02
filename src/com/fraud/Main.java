package com.fraud;

import com.fraud.ui.ConsoleApp;
import com.fraud.ui.FraudDetectionGUI;
import com.fraud.web.FraudWebServer;

import java.awt.GraphicsEnvironment;

/**
 * Main application launcher for the Credit Card Fraud Detection System.
 * Supports launching:
 *  - Web Browser Portal (--web or -w)
 *  - Desktop Swing GUI   (--gui or -g)
 *  - Interactive Console (--cli or -c)
 */
public class Main {
    public static void main(String[] args) {
        boolean forceCli = false;
        boolean forceGui = false;
        boolean forceWeb = false;

        for (String arg : args) {
            if ("--cli".equalsIgnoreCase(arg) || "-c".equalsIgnoreCase(arg)) {
                forceCli = true;
            } else if ("--gui".equalsIgnoreCase(arg) || "-g".equalsIgnoreCase(arg)) {
                forceGui = true;
            } else if ("--web".equalsIgnoreCase(arg) || "-w".equalsIgnoreCase(arg)) {
                forceWeb = true;
            }
        }

        if (forceWeb) {
            launchWeb();
        } else if (forceCli) {
            new ConsoleApp().start();
        } else if (forceGui) {
            launchGui();
        } else {
            // Default to Web Server for browser viewing
            launchWeb();
        }
    }

    private static void launchWeb() {
        try {
            new FraudWebServer(8080).start();
        } catch (Exception e) {
            System.err.println("Failed to start Web Server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void launchGui() {
        FraudDetectionGUI.main(new String[0]);
    }
}
