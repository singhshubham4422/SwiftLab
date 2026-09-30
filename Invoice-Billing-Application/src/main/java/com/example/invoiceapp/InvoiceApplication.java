package com.example.invoiceapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.awt.Desktop;
import java.net.URI;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class InvoiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InvoiceApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void openBrowserOnStartup() {
        // Open browser automatically when running as a desktop app
        if (!java.awt.GraphicsEnvironment.isHeadless()) {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI("http://localhost:8080/invoices"));
                }
            } catch (Exception e) {
                System.out.println("Started server on http://localhost:8080/invoices");
            }
        }
    }
}
