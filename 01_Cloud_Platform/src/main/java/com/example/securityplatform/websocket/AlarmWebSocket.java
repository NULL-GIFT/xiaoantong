package com.example.securityplatform.websocket;

import com.alibaba.fastjson.JSON;
import org.springframework.stereotype.Component;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@ServerEndpoint("/ws/alarm")
public class AlarmWebSocket {

    // 静态变量，用来记录当前在线连接数
    private static final AtomicInteger onlineCount = new AtomicInteger(0);

    // 线程安全的Map，用来存放每个客户端对应的WebSocket对象
    private static final ConcurrentHashMap<String, AlarmWebSocket> webSocketMap = new ConcurrentHashMap<>();

    // 与某个客户端的连接会话，需要通过它来给客户端发送数据
    private Session session;

    // 连接ID
    private String sid = "";

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        this.sid = session.getId();

        webSocketMap.put(sid, this);
        addOnlineCount(); // 在线数加1

        System.out.println("WebSocket连接建立，连接ID: " + sid + "，当前在线数: " + getOnlineCount());

        // 发送连接成功消息
        try {
            sendMessage("{\"code\":200,\"msg\":\"WebSocket连接成功\"}");
        } catch (IOException e) {
            System.err.println("WebSocket发送消息异常: " + e.getMessage());
        }
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        webSocketMap.remove(sid);
        subOnlineCount(); // 在线数减1

        System.out.println("WebSocket连接关闭，连接ID: " + sid + "，当前在线数: " + getOnlineCount());
    }

    /**
     * 收到客户端消息后调用的方法
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("收到客户端消息: " + message);
        // 可以处理客户端发来的指令，如订阅特定设备等
    }

    /**
     * 发生错误时调用
     */
    @OnError
    public void onError(Session session, Throwable error) {
        System.err.println("WebSocket发生错误: " + error.getMessage());
        error.printStackTrace();
    }

    /**
     * 发送消息给当前客户端
     */
    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }

    /**
     * 向所有连接的客户端广播消息
     */
    public static void broadcast(String message) {
        System.out.println("WebSocket广播消息: " + message);

        for (AlarmWebSocket item : webSocketMap.values()) {
            try {
                item.sendMessage(message);
            } catch (IOException e) {
                System.err.println("广播消息失败: " + e.getMessage());
                continue;
            }
        }
    }

    /**
     * 发送报警消息给所有客户端
     */
    public static void sendAlarm(String deviceId, String alarmType, String imgPath, Long alarmId) {
        try {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("deviceId", deviceId);
            map.put("alarmType", alarmType);
            map.put("imgPath", imgPath);
            map.put("alarmId", alarmId);
            map.put("timestamp", System.currentTimeMillis());
            String message = JSON.toJSONString(map);
            broadcast(message);
        } catch (Exception e) {
            System.err.println("WebSocket消息转换失败: " + e.getMessage());
        }
    }

    

    public static synchronized int getOnlineCount() {
        return onlineCount.get();
    }

    public static synchronized void addOnlineCount() {
        onlineCount.incrementAndGet();
    }

    public static synchronized void subOnlineCount() {
        onlineCount.decrementAndGet();
    }



    // 修改后：增加 Float confidence 参数
    public static void sendAlarm(String deviceId, String alarmType, String imgPath, Long alarmId, Float confidence) {
        try {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("deviceId", deviceId);
            map.put("alarmType", alarmType);
            map.put("imgPath", imgPath);
            map.put("alarmId", alarmId);
            map.put("timestamp", System.currentTimeMillis());
            // 🌟 把置信度装进报文里！
            map.put("confidence", confidence);

            String message = JSON.toJSONString(map);
            broadcast(message);
        } catch (Exception e) {
            System.err.println("WebSocket消息转换失败: " + e.getMessage());
        }
    }
}
