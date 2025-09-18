package com.sidi.zitter_mao;

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
            log.info("Server TCP listening on port {}", tcpPort);
            while(true) {
                Socket clientSocket = serverSocket.accept();
                log.info("TCP client connected from {}", clientSocket.getRemoteSocketAddress());

                new Thread(() -> handleTcpClient(clientSocket)).start();
            }
        } catch (Exception e) {
            log.error("Problem to start TCP server. Message: {}", e.getMessage());
        }
    }

    private void handleTcpClient(Socket clientSocket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            OutputStream outputStream = clientSocket.getOutputStream()) {
            String input = reader.readLine();
                int size = Integer.parseInt(input.trim());
            log.info("TCP client requested {} MB", size);
            long totalBytes = size * 1024L * 1024L;
            byte[] buffer = "X".repeat(1024).getBytes(StandardCharsets.UTF_8);

            long sent = 0;
            while (sent < totalBytes) {
                outputStream.write(buffer);
                sent += buffer.length;
            }
            outputStream.flush();

            log.info("TCP: Sent {} MB to {}", size, clientSocket.getRemoteSocketAddress());
        } catch (Exception e) {
            log.error("Error TCP client. Message: {}", e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                //TODO
            }
        }
    }

    private void startUdpServer() {
        try (DatagramSocket socket = new DatagramSocket(udpPort)) {
            log.info("Server UDP listening on port {}", udpPort);
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while(true) {
                socket.receive(packet);
                String received = new String(packet.getData(), 0, packet.getLength());
                log.info("UDP Message received from {}: {}", packet.getAddress(), received);
                String response = "Data packet via UDP\n";
                byte[] sendData = response.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packet.getAddress(), packet.getPort());
                socket.send(sendPacket);
            }
        } catch (Exception e) {
            log.error("Problem to start UDP server. Message: {}", e.getMessage());
        }
    }
}
