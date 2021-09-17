package com.peejaygal;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.Dimension;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main extends JFrame implements Runnable, ThreadFactory {

    private static final long serialVersionUID = 6441489157408381878L;

    private Executor executor = Executors.newSingleThreadExecutor(this);

    private JPanel registeredIndicator;
    private Webcam webcam = null;
    private WebcamPanel panel = null;
    private JPanel panelMain;
//    private static String EXCEL_FILE_LOCATION = "C:\\Users\\pjgal\\Desktop\\sample.xlsx";
    private JLabel filePath;
    private static String EXCEL_FILE_LOCATION;
    private JTextArea textarea;
    private JFileChooser jFileChooser;
    private JButton btnFileChooser;
    private boolean isProcessing = false;
    private static final String dateStamp = new SimpleDateFormat("dd-MM-yyyy").format(Calendar.getInstance().getTime());

    public Main() {
        super();

        setLayout(new FlowLayout());
        setTitle("Read QR / Bar Code With Webcam");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//
        Dimension size = WebcamResolution.QVGA.getSize();

        webcam = Webcam.getWebcams().get(0);
        webcam.setViewSize(size);
//
        panel = new WebcamPanel(webcam);
        panel.setPreferredSize(size);
        panel.setFPSDisplayed(true);

        
        textarea = new JTextArea();
        textarea.setEditable(false);
        textarea.setPreferredSize(size);

        jFileChooser = new JFileChooser();
        jFileChooser.setAcceptAllFileFilterUsed(false);
        FileFilter typeXLSX = new FileFilter() {
            @Override
            public boolean accept(File f) {
                if (f.getName().endsWith(".xlsx") || f.getName().endsWith(".xls")) {
                    return true;
                }
                return false;
            }

            @Override
            public String getDescription() {
                return "Excel Files";
            }
        };
        jFileChooser.setFileFilter(typeXLSX);

        registeredIndicator = new JPanel();
        registeredIndicator.setSize(500, 1000);
        registeredIndicator.setMinimumSize(new Dimension(1000, 1000));
        registeredIndicator.setBackground(Color.RED);

        filePath = new JLabel("No file selected");

        btnFileChooser = new JButton();
        btnFileChooser.setText("Choose DB");
        add(panel);
        add(textarea);
        add(btnFileChooser);
        add(filePath);
        add(registeredIndicator);

        btnFileChooser.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int returnVal = jFileChooser.showOpenDialog(Main.this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = jFileChooser.getSelectedFile();
                    //This is where a real application would open the file.
                    System.out.println("File location is at " + file.getAbsolutePath());
                    EXCEL_FILE_LOCATION = file.getAbsolutePath();
                    filePath.setText("Currently saving attendees at " + file.getName());
                } else {

                }
            }
        });

        pack();
        setVisible(true);
//
        executor.execute(this);
    }

    @Override
    public void run() {

        do {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Result result = null;
            BufferedImage image = null;

            if (webcam.isOpen()) {

                if ((image = webcam.getImage()) == null) {
                    continue;
                }

                LuminanceSource source = new BufferedImageLuminanceSource(image);
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

                try {
                    result = new MultiFormatReader().decode(bitmap);
                } catch (NotFoundException e) {
                    // fall thru, it means there is no QR code in image
                }
            }

            if (result != null) {
                textarea.setText(result.getText());
                String timeStamp = getCurrentTimeStamp();
                System.out.println("Scanned: " + result.getText() + " at " + timeStamp);
                if (!isProcessing){
                    isProcessing = true;

                    try {
                        if (!sheetsHelper.isExisting(result.getText(), dateStamp)){
                            registeredIndicator.setBackground(Color.RED);
                            sheetsHelper.appendValue(result.getText(), dateStamp, timeStamp);
                        } else {
                            registeredIndicator.setBackground(Color.GREEN);
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                    isProcessing = false;
                }
            }

        } while (true);
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r, "example-runner");
        t.setDaemon(true);
        return t;
    }

    public static void main(String[] args) {
        new Main();
    }

    public static String getCurrentTimeStamp(){
        return new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());
    }

}