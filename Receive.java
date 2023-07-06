import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class Receive {

    private static void receiveFileGUI(String name) {
        JOptionPane.showMessageDialog(null, name, "FILE RECEIVED", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * @param port
     * @param serverRoute
     * @throws IOException
     */
    private static void createFile(int port, String serverRoute) throws IOException {
        byte[] receiveFileName = new byte[1024];
        DatagramPacket receiveFileNamePacket = new DatagramPacket(receiveFileName, receiveFileName.length);
        InetAddress address = InetAddress.getByName("127.0.0.1");
        DatagramSocket socket = new DatagramSocket(port, address);

        socket.receive(receiveFileNamePacket);
        System.out.println("Receiving file name");
        byte[] data = receiveFileNamePacket.getData();
        String fileName = new String(data, 0, receiveFileNamePacket.getLength());

        System.out.println("Creating file");
        File f = new File(serverRoute + "\\" + fileName);
        FileOutputStream outToFile = new FileOutputStream(f);

        receiveFileUDP(outToFile, socket);
        receiveFileGUI(fileName);
    }

    /**
     * @param port
     * @throws IOException
     */
    public static void receiveTCP(int port) throws IOException {

        ServerSocket serverSocket = new ServerSocket(port);
        Socket socket = serverSocket.accept();

        Scanner in = new Scanner(socket.getInputStream());
        PrintWriter printWrite = new PrintWriter(socket.getOutputStream(), true);
        String FileName = in.nextLine();
        int FileSize = in.nextInt();

        FileOutputStream fileoutput = new FileOutputStream(FileName);
        BufferedOutputStream out = new BufferedOutputStream(fileoutput);
        byte[] buffer = new byte[FileSize];
        int count;
        InputStream is = socket.getInputStream();
        while ((count = is.read(buffer, 0, FileSize)) > 0) {

            fileoutput.write(buffer, 0, count);
        }

        receiveFileGUI(FileName);
        closeeverything(fileoutput, socket, out, serverSocket, printWrite);

    }

    private static void closeeverything(FileOutputStream fileoutput, Socket socket, BufferedOutputStream out,
            ServerSocket serverSocket, PrintWriter printWrite) throws IOException {
        fileoutput.close();
        socket.close();
        serverSocket.close();
        out.close();
        printWrite.close();
    }

    /**
     * @param outToFile
     * @param socket
     * @throws IOException
     */
    private static void receiveFileUDP(FileOutputStream outToFile, DatagramSocket socket) throws IOException {
        System.out.println("Receiving file");
        int seqnum = 0;
        int atlast = 0;
        storingFileBytes(seqnum, atlast, socket, outToFile);

    }

    /**
     * @param seqnum
     * @param atlast
     * @param socket
     * @param outToFile
     * @throws IOException
     */
    private static void storingFileBytes(int seqnum, int atlast, DatagramSocket socket, FileOutputStream outToFile)
            throws IOException {
        boolean flag;
        byte[] message = new byte[1024];
        byte[] fileByteArray = new byte[1021];
        while (true) {

            DatagramPacket receivedPacket = new DatagramPacket(message, message.length);
            socket.receive(receivedPacket);
            message = receivedPacket.getData();

            InetAddress address = receivedPacket.getAddress();
            int port = receivedPacket.getPort();
            seqnum = ((message[0] & 0xff) << 8) + (message[1] & 0xff);
            flag = (message[2] & 0xff) == 1;

            if (seqnum == (atlast + 1)) {
                atlast = seqnum;

                System.arraycopy(message, 3, fileByteArray, 0, 1021);

                outToFile.write(fileByteArray);

                System.out.println("Received packet: Sequence number:" + atlast);

                packetReceived(atlast, socket, address, port);

            } else {
                System.out.println("Expected sequence number: " + (atlast + 1) + " but received " + seqnum
                        + ". DISCARDING");
                packetReceived(atlast, socket, address, port);
            }
            if (flag) {
                outToFile.close();
                break;
            }
        }

    }

    /**
     * @param atlast
     * @param socket
     * @param address
     * @param port
     * @throws IOException
     */
    private static void packetReceived(int atlast, DatagramSocket socket, InetAddress address, int port)
            throws IOException {
        byte[] ackPacket = new byte[2];
        ackPacket[0] = (byte) (atlast >> 8);
        ackPacket[1] = (byte) (atlast);
        DatagramPacket acknowledgement = new DatagramPacket(ackPacket, ackPacket.length, address, port);
        socket.send(acknowledgement);
        System.out.println("Send last packet: Sequence Number = " + atlast);
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        int port = 4294;
        String message = "Ready to receive files on port " + port;
        String serverRoute = "/home/charles/Desktop/cs354/gitrepo2/group_52/";
        JOptionPane.showMessageDialog(new JFrame(), message, "Dialog", JOptionPane.INFORMATION_MESSAGE);

        Scanner in = new Scanner(System.in);
        System.out.println("Receiving using [TCP] Enter:1, Receiving using [UDP} Enter: 2");
        int state = in.nextInt();

        if (state == 1) {
            try {
                receiveTCP(port);
            } catch (IOException e1) {
                System.out.println("Error on receiveTCP!");
                e1.printStackTrace();
            }
        } else if (state == 2) {
            try {
                createFile(port, serverRoute);
                // receiveFileGUI();
            } catch (IOException e) {
                System.out.println("Error on receiveUDP!");
            }
        } else {
            System.out.println("PLEASE!! Receiving using [TCP] Enter:1, Receiving using [UDP} Enter: 2");
            System.exit(0);
        }
        in.close();
    }

}
