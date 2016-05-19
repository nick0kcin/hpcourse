package com.ashatta.hps.server;

import com.ashatta.hps.communication.Protocol;
import com.ashatta.hps.server.internal.TaskManager;
import com.ashatta.hps.server.internal.CalculationTask;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/* Receives requests from clients and calls TaskManager to actually handle them. */
public class Server implements Runnable {

    /* Handles a communication with a single client. */
    class SocketThread extends Thread {
        private final Socket socket;

        public SocketThread(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                while (running.get()) {
                    InputStream is = socket.getInputStream();
                    Protocol.WrapperMessage message = Protocol.WrapperMessage.parseDelimitedFrom(is);
                    if (message != null) {
                        Protocol.ServerRequest request = message.getRequest();
                        String clientId = request.getClientId();
                        long requestId = request.getRequestId();
                        synchronized (requestToSocket) {
                            requestToSocket.put(requestId, socket);
                        }

                        if (request.hasSubmit()) {
                            taskManager.submit(clientId, requestId, request.getSubmit().getTask());
                        } else if (request.hasSubscribe()) {
                            taskManager.subscribe(requestId, request.getSubscribe().getTaskId());
                        } else if (request.hasList()) {
                            taskManager.listAll(requestId);
                        } else {
                            throw new IllegalArgumentException("Unrecognized request type");
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private final int port;
    /* Used for testing so that we are able to stop the server. */
    private final AtomicBoolean running;
    private final TaskManager taskManager;
    /* Maps request id to a corresponding connection, used to send responses. */
    private final Map<Long, Socket> requestToSocket;

    public Server(int port, int threadsNumber) {
        this.port = port;
        this.running = new AtomicBoolean(false);
        this.requestToSocket = new HashMap<>();
        this.taskManager = new TaskManager(this, threadsNumber);
    }

    public void run() {
        if (running.get()) {
            throw new IllegalStateException();
        }

        running.set(true);
        try (ServerSocket listener = new ServerSocket(port)) {
            while (running.get()) {
                Socket socket = listener.accept();
                new SocketThread(socket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendSubmitResponse(long requestId, int taskId, boolean status) {
        Protocol.WrapperMessage message = Protocol.WrapperMessage.newBuilder()
                .setResponse(Protocol.ServerResponse.newBuilder()
                        .setRequestId(requestId)
                        .setSubmitResponse(Protocol.SubmitTaskResponse.newBuilder()
                                .setSubmittedTaskId(taskId)
                                .setStatus(status ? Protocol.Status.OK : Protocol.Status.ERROR))).build();

        sendResponse(requestId, message);
    }

    public void sendSubscribeResponse(long requestId, long result, boolean status) {
        Protocol.WrapperMessage message = Protocol.WrapperMessage.newBuilder()
                .setResponse(Protocol.ServerResponse.newBuilder()
                        .setRequestId(requestId)
                        .setSubscribeResponse(Protocol.SubscribeResponse.newBuilder()
                                .setValue(result)
                                .setStatus(status ? Protocol.Status.OK : Protocol.Status.ERROR))).build();

        sendResponse(requestId, message);
    }

    public void sendListTasksResponse(long requestId, List<CalculationTask> runningTasks, List<CalculationTask> completeTasks,
                                      boolean status) {
        List<Protocol.ListTasksResponse.TaskDescription> taskDescriptions = new ArrayList<>();
        for (CalculationTask task : runningTasks) {
            taskDescriptions.add(Protocol.ListTasksResponse.TaskDescription.newBuilder()
                    .setClientId(task.getClientId())
                    .setTask(task.getProtoTask())
                    .setTaskId(task.getTaskId()).build());
        }
        for (CalculationTask task : completeTasks) {
            Protocol.ListTasksResponse.TaskDescription.Builder taskDescriptionBuilder =
                    Protocol.ListTasksResponse.TaskDescription.newBuilder()
                    .setClientId(task.getClientId())
                    .setTask(task.getProtoTask())
                    .setTaskId(task.getTaskId());
            if (!task.hasError()) {
                taskDescriptionBuilder.setResult(task.getResult());
            }
            taskDescriptions.add(taskDescriptionBuilder.build());
        }
        Protocol.WrapperMessage message = Protocol.WrapperMessage.newBuilder()
                .setResponse(Protocol.ServerResponse.newBuilder()
                        .setRequestId(requestId)
                        .setListResponse(Protocol.ListTasksResponse.newBuilder()
                                .addAllTasks(taskDescriptions)
                                .setStatus(status ? Protocol.Status.OK : Protocol.Status.ERROR))).build();

        sendResponse(requestId, message);
    }

    private void sendResponse(long requestId, Protocol.WrapperMessage message) {
        try {
            Socket socket;
            synchronized (requestToSocket) {
                socket = requestToSocket.get(requestId);
            }
            message.writeDelimitedTo(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* Visible for testing. */
    void stop() {
        running.set(false);
        taskManager.shutdown();
    }

    /* Visible for testing. */
    int getPort() {
        return port;
    }
}

