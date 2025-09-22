package com.network.example;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

@Component
public class TcpUdpServer {
    private static final Logger log = LoggerFactory.getLogger(TcpUdpServer.class);
    private final int tcpPort = 9000;
    private final int udpPort = 9001;

    @PostConstruct
    public void startServers() {
        new Thread(this::startTcpServer, "tcp-server-thread").start();
        new Thread(this::startUdpServer, "udp-server-thread").start();
    }

    private void startTcpServer() {
        try (ServerSocket serverSocket = new ServerSocket(tcpPort)) {
            log.info("TCP: Server listening on port {}", tcpPort);
            while(true) {
                Socket clientSocket = serverSocket.accept();
                log.info("TCP: client connected from {}", clientSocket.getRemoteSocketAddress());

                new Thread(() -> handleTcpClient(clientSocket)).start();
            }
        } catch (Exception e) {
            log.error("TCP: Problem to start server. Message: {}", e.getMessage());
        }
    }

    private void handleTcpClient(Socket clientSocket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            OutputStream outputStream = clientSocket.getOutputStream()) {
            String tip = "Inform <size>|<unit> - e.g: 4|MB\nUnits available (GB, MB, KB, B)\n\n";
            sendMessageToTCPClient(tip, outputStream);

            String[] input = reader.readLine().split("\\|");
            int size = Integer.parseInt(input[0].trim());
            String unit = input[1].trim().toUpperCase();

            log.info("TCP: client requested {} {}", size, unit);
            long totalBytes = quantityOfData(unit, size);
            byte[] buffer = "X".repeat(totalBytes >= 1024L ? 1024 : (int) totalBytes).getBytes(StandardCharsets.UTF_8);

            long sent = 0;
            while (sent < totalBytes) {
                outputStream.write(buffer);
                sent += buffer.length;
            }
            outputStream.flush();

            log.info("TCP: Sent {} {} to {}", size, unit, clientSocket.getRemoteSocketAddress());
        } catch (Exception e) {
            log.error("TCP: Error client. Message: {}", e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                log.error("TCP: Error to closing socket. Message: {}", e.getMessage());
            }
        }
    }

    private void startUdpServer() {
        try (DatagramSocket socket = new DatagramSocket(udpPort)) {
            log.info("UDP: Server listening on port {}", udpPort);
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while(true) {
                socket.receive(packet);
                String received = new String(packet.getData(), 0, packet.getLength());

                if (received.trim().equals("tip")) {
                    String tip = "Inform <size>|<unit> - e.g: 4|MB\nUnits available (GB, MB, KB, B)\n\n";
                    sendUdpData(socket, packet, tip);
                } else {
                    try {
                        String[] input = received.split("\\|");

                        int size = Integer.parseInt(input[0].trim());
                        String unit = input[1].trim().toUpperCase();

                        log.info("UDP: Message received from {}: {}", packet.getAddress(), received);
                        sendUdpData(socket, packet, size, unit);
                    } catch (NumberFormatException | IndexOutOfBoundsException e) {
                        String messageError = "Commands available: 'tip' or '<size>|<unit>'\n";
                        sendUdpData(socket, packet, messageError);
                        log.error("UDP: invalid input ({}). message: {}", received, e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("UDP: Error client. Message: {}", e.getMessage());
        }
    }

    private void sendUdpData(DatagramSocket socket, DatagramPacket packet, int size, String unit) {
        try {
            long totalBytes = quantityOfData(unit, size);
            byte[] buffer = "X".repeat(totalBytes >= 1024L ? 1024 : (int) totalBytes).getBytes(StandardCharsets.UTF_8);

            long sent = 0;
            while (sent < totalBytes) {
                DatagramPacket sendPacket = new DatagramPacket(buffer, buffer.length, packet.getAddress(), packet.getPort());
                socket.send(sendPacket);
                sent += buffer.length;
            }

            log.info("UDP: Sent {} {} to {}", size, unit, packet.getAddress());
        } catch (Exception e) {
            log.error("UDP: Problem to send data. Message: {}", e.getMessage());
        }
    }

    private void sendUdpData(DatagramSocket socket, DatagramPacket packet, String tip) {
        try {
            byte[] buffer = tip.getBytes(StandardCharsets.UTF_8);
            DatagramPacket sendPacket = new DatagramPacket(buffer, buffer.length, packet.getAddress(), packet.getPort());
            socket.send(sendPacket);

            log.info("UDP: Tip sent to {}", packet.getAddress());
        } catch (Exception e) {
            log.error("UDP: Problem to send tip. Message: {}", e.getMessage());
        }
    }

    private void sendMessageToTCPClient(String message, OutputStream outputStream) throws IOException {
        byte[] buffer = message.getBytes(StandardCharsets.UTF_8);
        outputStream.write(buffer);
        outputStream.flush();
    }

    private long quantityOfData(String unit, int size) {
        return switch (unit) {
            case "GB" -> size * 1024L * 1024L * 1024L;
            case "MB" -> size * 1024L * 1024L;
            case "KB" -> size * 1024L;
            case "B" -> size;
            default -> 0L;
        };
    }
}
