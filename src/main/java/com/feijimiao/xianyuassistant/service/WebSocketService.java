package com.feijimiao.xianyuassistant.service;

/**
 * WebSocket服务接口
 */
public interface WebSocketService {
    
    /**
     * 启动WebSocket连接
     *
     * @param accountId 账号ID
     * @return 是否成功
     */
    boolean startWebSocket(Long accountId);
    
    /**
     * 使用手动提供的accessToken启动WebSocket连接
     *
     * @param accountId 账号ID
     * @param accessToken 手动提供的accessToken
     * @return 是否成功
     */
    boolean startWebSocketWithToken(Long accountId, String accessToken);
    
    /**
     * 停止WebSocket连接
     *
     * @param accountId 账号ID
     * @return 是否成功
     */
    boolean stopWebSocket(Long accountId);
    
    /**
     * 检查WebSocket连接状态
     *
     * @param accountId 账号ID
     * @return 是否已连接
     */
    boolean isConnected(Long accountId);
    
    /**
     * 停止所有WebSocket连接
     */
    void stopAllWebSockets();
    
    /**
     * 发送消息
     *
     * @param accountId 账号ID
     * @param cid 会话ID（不带@goofish后缀）
     * @param toId 接收方用户ID（不带@goofish后缀）
     * @param text 消息文本内容
     * @return 是否成功
     */
    boolean sendMessage(Long accountId, String cid, String toId, String text);
}
