import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Scanner;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.EmptyBorder;
import java.awt.Component;
import java.awt.Dimension;

public class Send {

   

    public static byte[] FileToByte(File file) {
        FileInputStream fis = null;
        byte[] byteArray = new byte[(int) file.length()];
        try {
            fis = new FileInputStream(file);
            fis.read(byteArray);
            fis.close();

        } catch (IOException ioExp) {
            ioExp.printStackTrace();
        }
        return byteArray;
    }

    /**
     * @param socket
     * @param fileByteArray
     * @param address
     * @param port
     * @throws IOException
     */
    public static void sendFile(DatagramSocket socket, byte[] fileByteArray, InetAddress address, int port)
            throws IOException {
        System.out.println("Sending file");

        int sequenceNumber = 0;
        int ackSequence = 0;

        for (int i = 0; i < fileByteArray.length; i = i + 1021) {
            sequenceNumber += 1;
            DatagramPacket sendPacket = createmessage(sequenceNumber, i, fileByteArray, port, address);

            socket.send(sendPacket);

            boolean ackRec;
            printProgress();
            while (true) {
                
                byte[] pack = new byte[2];
                DatagramPacket datagramPacket = new DatagramPacket(pack, pack.length);

                try {
                    socket.setSoTimeout(50);
                    socket.receive(datagramPacket);
                    ackSequence = ((pack[0] & 0xff) << 8) + (pack[1] & 0xff);
                    ackRec = true;
                } catch (SocketTimeoutException e) {
                    System.out.println("Socket timed out waiting for packect received");
                    ackRec = false;
                }

                if ((ackSequence == sequenceNumber) && (ackRec)) {
                    System.out.println("packet received: Sequence Number = " + ackSequence);
                    break;
                } else {
                    socket.send(sendPacket);
                    System.out.println("Resending: Sequence Number = " + sequenceNumber);
                }
            }
        }
    }

    /**
     * @param sequenceNumber
     * @param i
     * @param fileByteArray
     * @param port
     * @param address
     * @return
     */
    private static DatagramPacket createmessage(int sequenceNumber, int i, byte[] fileByteArray, int port,
            InetAddress address) {
        boolean flag;
        byte[] message = new byte[1024];

        message[0] = (byte) (sequenceNumber >> 8);
        message[1] = (byte) (sequenceNumber);

        if ((i + 1021) >= fileByteArray.length) {
            flag = true;
            message[2] = (byte) (1);
        } else {
            flag = false;
            message[2] = (byte) (0);
        }

        if (!flag) {
            System.arraycopy(fileByteArray, i, message, 3, 1021);
        } else {
            System.arraycopy(fileByteArray, i, message, 3, fileByteArray.length - i);
        }
        DatagramPacket sendPacket = new DatagramPacket(message, message.length, address, port);

        return sendPacket;
    }

    /**
     * @param port
     * @param host
     */
    private static void sendUDP(int port, InetAddress host) {
        try {
            DatagramSocket socket = new DatagramSocket();
        //    / InetAddress address = InetAddress.getByName(host);
            String fileName;
            JFileChooser jfc = new JFileChooser();
            jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            if (jfc.isMultiSelectionEnabled()) {
                jfc.setMultiSelectionEnabled(false);
            }
            int r = jfc.showOpenDialog(null);
            if (r == JFileChooser.APPROVE_OPTION) {
                File f = jfc.getSelectedFile();
                fileName = f.getName();
                byte[] fileNameBytes = fileName.getBytes();
                DatagramPacket fileStatPacket = new DatagramPacket(fileNameBytes, fileNameBytes.length, host,
                        port);
                socket.send(fileStatPacket);

                byte[] fileByteArray = FileToByte(f);
                
                sendFile(socket, fileByteArray, host, port);
            }
            socket.close();
        } catch (Exception ex) {
            ex.printStackTrace();

            System.exit(1);
        }

    }

    /**
     * @param host
     * @param port
     */
    private static void sendTCP(InetAddress host, int port) {
        try {
            Socket sock = new Socket(host, port);
            String fileName;
            JFileChooser jfc = new JFileChooser();
            jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            if (jfc.isMultiSelectionEnabled()) {
                jfc.setMultiSelectionEnabled(false);
            }
            int r = jfc.showOpenDialog(null);

            if (r == JFileChooser.APPROVE_OPTION) {
                File file = jfc.getSelectedFile();
                fileName = file.getName();
                int FileSize = (int) file.length();
                PrintWriter pr = new PrintWriter(sock.getOutputStream(), true);
                Scanner in = new Scanner(sock.getInputStream());

                pr.println(fileName);
                pr.println(FileSize);

                int count;
                byte[] buffer = new byte[FileSize];
                OutputStream out = sock.getOutputStream();
                BufferedInputStream input = new BufferedInputStream(new FileInputStream(file));
                printProgress();

                while ((count = input.read(buffer)) > 0) {

                    out.write(buffer, 0, count);
                    out.flush();

                }
                input.close();
                sock.close();
                in.close();

            }

        } catch (Exception e) {
            System.out.println("IOException Error on SendTCP ");
        }

    }

    /**
     * @param port
     * @param host
     */
    private static void choosefiletosend(int port, InetAddress host) {
        JFrame jframe = new JFrame("FILE SENDER");
        jframe.setSize(450, 450);
        jframe.setLayout(new BoxLayout(jframe.getContentPane(), BoxLayout.Y_AXIS));
        jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JLabel jtitle = new JLabel("Choose your method of data transfer");
        jtitle.setFont(new Font("Arial", Font.BOLD, 25));
        jtitle.setBorder(new EmptyBorder(20, 0, 10, 0));
        jtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel jbutton = new JPanel();
        jbutton.setBorder(new EmptyBorder(75, 0, 10, 0));
        JButton jbsendudp = new JButton("UDP");
        jbsendudp.setPreferredSize(new Dimension(250, 75));
        jbsendudp.setFont(new Font("Arial", Font.BOLD, 20));

        JButton jbsendtpc = new JButton("TPC");
        jbsendtpc.setPreferredSize(new Dimension(250, 75));
        jbsendtpc.setFont(new Font("Arial", Font.BOLD, 20));

        JButton jclose = new JButton("EXIT");
        jclose.setPreferredSize(new Dimension(150, 75));
        jclose.setFont(new Font("Arial", Font.BOLD, 20));
        jbutton.add(jbsendtpc);
        jbutton.add(jbsendudp);
        jbutton.add(jclose);
        jclose.addActionListener((event) -> System.exit(0));

        jclose.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("You have exited!");
            }
        });

        jbsendtpc.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("Choosing a file to send to the server via tcp");
                sendTCP(host, port);
            }
        });

        jbsendudp.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("Choosing a file to send to the server via udp");
                sendUDP(port, host);
            }

        });
        jframe.add(jtitle);
        jframe.add(jbutton);
        jframe.setVisible(true);
    }

    public static void printProgress() {
        JFrame frame;
        JProgressBar bar;
        frame = new JFrame("ProgressBar demo");

        JPanel p = new JPanel();

        bar = new JProgressBar();

        bar.setValue(0);
        bar.setStringPainted(true);
        p.add(bar);
        frame.add(p);
        frame.setSize(500, 500);
        frame.setVisible(true);

        int i = 0;
        try {
            while (i <= 100) {

                if (i > 30 && i < 70)
                    bar.setString("30% - 70% done");
                else if (i > 70)
                    bar.setString("100% done ");
                else
                    bar.setString("0% done");

                bar.setValue(i + 10);

                Thread.sleep(2000);
                i = i + 20;
            }
        } catch (Exception e) {
        }
    }

    /**
     * @param args
     * @throws UnknownHostException
     */
    public static void main(String[] args) throws UnknownHostException {
        int port = 4294;
        InetAddress host = InetAddress.getByName("localhost");

        choosefiletosend(port, host);
    }

}
