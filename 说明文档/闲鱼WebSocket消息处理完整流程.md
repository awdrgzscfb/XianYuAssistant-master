# 闲鱼 WebSocket 消息处理完整流程（详细到类）

## 目录

1. [架构概览](#架构概览)
2. [连接建立流程](#连接建立流程)
3. [消息接收流程](#消息接收流程)
4. [消息解析流程](#消息解析流程)
5. [事件驱动处理](#事件驱动处理)
6. [消息发送流程](#消息发送流程)
7. [类职责详解](#类职责详解)

---

## 架构概览

### 整体架构图（简化后）

```
┌─────────────────────────────────────────────────────────────────┐
│                        闲鱼 WebSocket 服务器                      │
└─────────────────────────────────────────────────────────────────┘
                              ↕ WebSocket 连接
┌─────────────────────────────────────────────────────────────────┐
│              XianyuWebSocketClient (客户端层)                    │
│              - 接收原始消息                                       │
│              - 解密同步包                                         │
│              - 发送ACK确认                                        │
└─────────────────────────────────────────────────────────────────┘
                              ↓ 调用 messageHandler
┌─────────────────────────────────────────────────────────────────┐
│         DefaultWebSocketMessageHandler (分发层)                  │
│         - 调用路由器分发消息                                      │
└─────────────────────────────────────────────────────────────────┘
                              ↓ 调用 messageRouter
┌─────────────────────────────────────────────────────────────────┐
│            WebSocketMessageRouter (路由层)                       │
│            - 根据 lwp 字段路由到对应 Handler                      │
└─────────────────────────────────────────────────────────────────┘
                              ↓ 路由到 SyncMessageHandler
┌─────────────────────────────────────────────────────────────────┐
│            SyncMessageHandler (解析层)                           │
│            - 解析消息字段                                         │
│            - 构建 XianyuChatMessage                              │
│            - 发布 ChatMessageReceivedEvent                       │
└─────────────────────────────────────────────────────────────────┘
                              ↓ 发布事件
┌─────────────────────────────────────────────────────────────────┐
│              Spring ApplicationEventPublisher                    │
│              - 同步发布事件                                       │
│              - 立即返回，不等待监听器                             │
└─────────────────────────────────────────────────────────────────┘
                              ↓ 广播到所有监听器
                    ┌─────────┴─────────┐
                    ↓                   ↓
┌──────────────────────────┐  ┌──────────────────────────┐
│ ChatMessageEventSave     │  │ ChatMessageEventAuto     │
│ Listener (业务层)        │  │ DeliveryListener (业务层)│
│ - 去重检查               │  │ - 判断触发条件           │
│ - 保存到数据库           │  │ - 执行自动发货           │
└──────────────────────────┘  └──────────────────────────┘
            ↓                             ↓
┌──────────────────────────┐  ┌──────────────────────────┐
│ XianyuChatMessageMapper  │  │ WebSocketService         │
│ - insert()               │  │ - sendMessage()          │
└──────────────────────────┘  └──────────────────────────┘
```

### 核心类列表

**连接层**：
- `WebSocketController` - HTTP 接口控制器
- `WebSocketServiceImpl` - WebSocket 服务实现
- `XianyuWebSocketClient` - WebSocket 客户端
- `WebSocketInitializer` - 连接初始化器

**消息处理层**：
- `DefaultWebSocketMessageHandler` - 消息处理器
- `WebSocketMessageRouter` - 消息路由器
- `AbstractLwpHandler` - Handler 抽象基类
- `SyncMessageHandler` - 同步消息处理器

**事件层**：
- `ChatMessageReceivedEvent` - 消息接收事件
- `ChatMessageEventSaveListener` - 消息保存监听器
- `ChatMessageEventAutoDeliveryListener` - 自动发货监听器

**工具类**：
- `MessageDecryptUtils` - 消息解密工具
- `HumanLikeDelayUtils` - 人工延迟模拟工具

---

## 连接建立流程

### 1. 用户触发连接

```
前端页面
    ↓ HTTP POST /api/websocket/start
WebSocketController.startWebSocket(accountId)
    ↓
WebSocketServiceImpl.startWebSocket(accountId)
```

### 2. WebSocketController 类

**包名**：`com.feijimiao.xianyuassistant.controller`

**方法**：`startWebSocket(Long accountId)`

```java
@PostMapping("/start")
public ResultObject<String> startWebSocket(@RequestParam Long accountId) {
    boolean success = webSocketService.startWebSocket(accountId);
    return success ? ResultObject.success("连接成功") 
                   : ResultObject.failed("连接失败");
}
```

### 3. WebSocketServiceImpl 类

**包名**：`com.feijimiao.xianyuassistant.service.impl`

**核心方法**：`startWebSocket(Long accountId)`

**详细流程**：

```java
@Override
public boolean startWebSocket(Long accountId) {
    // 步骤1: 获取账号 Cookie
    String cookieStr = accountService.getCookieByAccountId(accountId);
    
    // 步骤2: 解析 Cookie，获取 unb
    Map<String, String> cookies = XianyuSignUtils.parseCookies(cookieStr);
    String unb = cookies.get("unb");
    String deviceId = "web_" + unb;
    
    // 步骤3: 获取 accessToken
    String accessToken = tokenService.getAccessToken(accountId, cookieStr, deviceId);
    
    // 步骤4: 调用通用连接方法
    return connectWebSocket(accountId, cookieStr, deviceId, accessToken, unb);
}
```

**connectWebSocket() 方法**：

```java
private boolean connectWebSocket(Long accountId, String cookieStr, 
                                  String deviceId, String accessToken, String unb) {
    // 步骤1: 构建请求头
    Map<String, String> headers = new HashMap<>();
    headers.put("Cookie", cookieStr);
    headers.put("User-Agent", "Mozilla/5.0...");
    // ... 其他请求头
    
    // 步骤2: 创建 WebSocket 客户端
    URI serverUri = new URI("wss://wss-goofish.dingtalk.com/");
    XianyuWebSocketClient client = new XianyuWebSocketClient(
            serverUri, headers, String.valueOf(accountId));
    
    // 步骤3: 设置当前用户ID
    client.setMyUserId(unb);
    
    // 步骤4: 设置消息处理器
    client.setMessageHandler(messageHandler);  // DefaultWebSocketMessageHandler
    
    // 步骤5: 设置注册成功回调
    client.setOnRegistrationSuccess(() -> {
        tokenService.saveToken(accountId, accessToken);
    });
    
    // 步骤6: 设置Token失效回调
    client.setOnTokenExpired(() -> {
        stopWebSocket(accountId);
        startWebSocket(accountId);  // 自动重连
    });
    
    // 步骤7: 连接 WebSocket
    boolean connected = client.connectBlocking(10, TimeUnit.SECONDS);
    
    if (connected) {
        // 步骤8: 保存客户端实例
        webSocketClients.put(accountId, client);
        
        // 步骤9: 初始化（发送注册消息、订阅消息）
        initializer.initialize(client, accessToken, deviceId, String.valueOf(accountId));
        
        // 步骤10: 启动心跳任务
        startHeartbeat(accountId, client);
        
        return true;
    }
    
    return false;
}
```

---

## 消息接收流程

### 1. 完整调用链

```
闲鱼服务器发送消息
    ↓
XianyuWebSocketClient.onMessage(String message)
    ↓
messageExecutor.submit(() -> handleMessageWithSemaphore(message))
    ↓
handleMessageWithSemaphore(String message)
    ↓
handleMessage(String message)
    ↓
messageHandler.handleMessage(accountId, messageData)
    ↓
DefaultWebSocketMessageHandler.handleMessage(accountId, messageData)
    ↓
messageRouter.route(accountId, messageData)
    ↓
WebSocketMessageRouter.route(accountId, messageData)
    ↓
handler.handle(accountId, messageData)
    ↓
SyncMessageHandler.handle(accountId, messageData)
```

### 2. XianyuWebSocketClient 类

**包名**：`com.feijimiao.xianyuassistant.websocket`

**继承**：`extends WebSocketClient`

**核心方法**：

#### onMessage(String message)
```java
@Override
public void onMessage(String message) {
    // 使用线程池异步处理
    messageExecutor.submit(() -> handleMessageWithSemaphore(message));
}
```

#### handleMessageWithSemaphore(String message)
```java
private void handleMessageWithSemaphore(String message) {
    try {
        // 获取信号量（最多100个并发）
        messageSemaphore.acquire();
        
        try {
            handleMessage(message);
        } finally {
            messageSemaphore.release();
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}
```

#### handleMessage(String message)
```java
private void handleMessage(String message) {
    messageCount++;
    
    // 步骤1: 解析 JSON
    Map<String, Object> messageData = objectMapper.readValue(message, Map.class);
    
    // 步骤2: 处理同步包消息 (/s/para 和 /s/sync)
    Object lwp = messageData.get("lwp");
    if (("/s/para".equals(lwp) || "/s/sync".equals(lwp))) {
        // 解密同步包中的加密消息
        Map<String, Object> body = (Map) messageData.get("body");
        Map<String, Object> syncPushPackage = (Map) body.get("syncPushPackage");
        List<Object> dataList = (List) syncPushPackage.get("data");
        
        for (Object item : dataList) {
            Map<String, Object> syncData = (Map) item;
            String encryptedData = syncData.get("data").toString();
            
            // 解密
            String decryptedData = MessageDecryptUtils.decrypt(encryptedData);
            if (decryptedData != null) {
                syncData.put("decryptedData", decryptedData);
            }
        }
    }
    
    // 步骤3: 发送 ACK 确认
    sendAckMessage(messageData);
    
    // 步骤4: 检查特殊响应
    Object code = messageData.get("code");
    if (code != null && code.equals(401)) {
        // Token失效，触发重连
        if (onTokenExpired != null) {
            onTokenExpired.run();
        }
        return;
    }
    
    if (code != null && code.equals(200)) {
        Map<String, Object> headers = (Map) messageData.get("headers");
        if (headers != null && headers.containsKey("reg-sid")) {
            // 注册成功
            if (onRegistrationSuccess != null) {
                onRegistrationSuccess.run();
            }
        }
    }
    
    // 步骤5: 调用消息处理器
    if (messageHandler != null) {
        messageHandler.handleMessage(accountId, messageData);
    }
}
```

### 3. DefaultWebSocketMessageHandler 类

**包名**：`com.feijimiao.xianyuassistant.websocket`

**实现**：`implements WebSocketMessageHandler`

**核心方法**：

```java
@Override
public void handleMessage(String accountId, Map<String, Object> message) {
    try {
        // 使用路由器处理消息
        messageRouter.route(accountId, message);
    } catch (Exception e) {
        log.error("【账号{}】处理消息失败", accountId, e);
    }
}
```

### 4. WebSocketMessageRouter 类

**包名**：`com.feijimiao.xianyuassistant.websocket`

**核心方法**：

```java
public void route(String accountId, Map<String, Object> messageData) {
    // 确保已初始化
    ensureInitialized();
    
    // 获取 lwp 路径
    Object lwpObj = messageData.get("lwp");
    String lwp = lwpObj != null ? lwpObj.toString() : null;
    
    if (lwp == null) {
        // 没有 lwp 字段，可能是响应消息
        handleResponseMessage(accountId, messageData);
        return;
    }
    
    // 查找对应的处理器
    AbstractLwpHandler handler = handlerMap.get(lwp);
    
    if (handler != null) {
        log.debug("【账号{}】路由消息: {} -> {}", 
                accountId, lwp, handler.getClass().getSimpleName());
        handler.handle(accountId, messageData);
    } else {
        log.debug("【账号{}】未找到处理器: {}", accountId, lwp);
    }
}
```

**路由表初始化**：

```java
private void registerHandlers() {
    if (handlers == null || handlers.isEmpty()) {
        return;
    }
    
    for (AbstractLwpHandler handler : handlers) {
        String lwpPath = handler.getLwpPath();
        handlerMap.put(lwpPath, handler);
        log.info("注册WebSocket处理器: {} -> {}", 
                lwpPath, handler.getClass().getSimpleName());
    }
    
    // 同步包消息支持两种路径
    AbstractLwpHandler syncHandler = handlerMap.get("/s/para");
    if (syncHandler != null) {
        handlerMap.put("/s/sync", syncHandler);
    }
}
```

---

## 消息解析流程

### 1. AbstractLwpHandler 类（模板方法）

**包名**：`com.feijimiao.xianyuassistant.websocket.handler`

**核心方法**：

```java
public final void handle(String accountId, Map<String, Object> messageData) {
    try {
        // 步骤1: 解析参数
        Object params = parseParams(accountId, messageData);
        if (params == null) {
            return;
        }
        
        // 步骤2: 核心处理
        Object result = doHandle(accountId, params, messageData);
        
        // 步骤3: 后置处理
        postHandle(accountId, result, messageData);
        
    } catch (Exception e) {
        log.error("【账号{}】处理消息失败", accountId, e);
    }
}

// 子类需要实现的抽象方法
protected abstract String getLwpPath();
protected abstract Object parseParams(String accountId, Map<String, Object> messageData);
protected abstract Object doHandle(String accountId, Object params, Map<String, Object> messageData);
protected void postHandle(String accountId, Object result, Map<String, Object> messageData) {}
```

### 2. SyncMessageHandler 类

**包名**：`com.feijimiao.xianyuassistant.websocket.handler`

**继承**：`extends AbstractLwpHandler`

**依赖注入**：

```java
@Autowired
private ApplicationEventPublisher eventPublisher;

private final ObjectMapper objectMapper = new ObjectMapper();
```

**核心方法**：

#### getLwpPath()
```java
@Override
public String getLwpPath() {
    return "/s/para";  // 也支持 /s/sync
}
```

#### parseParams()
```java
@Override
protected Object parseParams(String accountId, Map<String, Object> messageData) {
    SyncMessageParams params = new SyncMessageParams();
    
    // 获取 body
    Map<String, Object> body = getMap(messageData, "body");
    if (body == null) return null;
    
    // 获取 syncPushPackage
    Map<String, Object> syncPushPackage = getMap(body, "syncPushPackage");
    if (syncPushPackage == null) return null;
    
    // 获取 data 列表
    List<Object> dataList = getList(syncPushPackage, "data");
    if (dataList == null || dataList.isEmpty()) return null;
    
    params.setDataList(dataList);
    params.setMessageCount(dataList.size());
    
    return params;
}
```

#### doHandle()
```java
@Override
protected Object doHandle(String accountId, Object params, Map<String, Object> messageData) {
    SyncMessageParams syncParams = (SyncMessageParams) params;
    List<String> decryptedMessages = new ArrayList<>();
    
    // 获取 lwp 字段
    String lwp = getString(messageData, "lwp");
    
    // 处理每条加密消息
    for (int i = 0; i < syncParams.getDataList().size(); i++) {
        Map<String, Object> syncData = (Map) syncParams.getDataList().get(i);
        
        String encryptedData = getString(syncData, "data");
        if (encryptedData == null) continue;
        
        // 解密消息
        String decryptedData = MessageDecryptUtils.decrypt(encryptedData);
        if (decryptedData != null) {
            decryptedMessages.add(decryptedData);
            
            // 解析并发布事件
            parseAndPublishEvent(accountId, decryptedData, lwp);
        }
    }
    
    return decryptedMessages;
}
```

#### parseAndPublishEvent()
```java
private void parseAndPublishEvent(String accountId, String decryptedData, String lwp) {
    // 步骤1: 解析 JSON
    Map<String, Object> data = objectMapper.readValue(decryptedData, Map.class);
    
    // 步骤2: 检查消息类型
    Object typeObj = data.get("2");
    if (typeObj != null && "2".equals(typeObj.toString())) {
        return; // 已读回执，不处理
    }
    
    // 步骤3: 检查是否是聊天消息
    Object field1 = data.get("1");
    if (!(field1 instanceof Map)) return;
    
    Map<String, Object> messageInfo = (Map) field1;
    
    // 步骤4: 创建消息实体
    XianyuChatMessage message = new XianyuChatMessage();
    message.setXianyuAccountId(Long.parseLong(accountId));
    message.setLwp(lwp);
    
    // 步骤5: 提取各个字段
    message.setPnmId(extractString(messageInfo, "3"));
    message.setSId(extractString(messageInfo, "2"));
    message.setMessageTime(extractLong(messageInfo, "5"));
    
    // 提取 contentType (字段1.6.3.5)
    // ... 详细字段提取逻辑
    
    // 提取字段1.10的内容
    Object field10 = messageInfo.get("10");
    if (field10 instanceof Map) {
        Map<String, Object> field10Map = (Map) field10;
        message.setMsgContent(extractString(field10Map, "reminderContent"));
        message.setSenderUserName(extractString(field10Map, "reminderTitle"));
        message.setSenderUserId(extractString(field10Map, "senderUserId"));
        
        String reminderUrl = extractString(field10Map, "reminderUrl");
        message.setReminderUrl(reminderUrl);
        if (reminderUrl != null) {
            String goodsId = extractItemIdFromUrl(reminderUrl);
            message.setXyGoodsId(goodsId);
        }
    }
    
    // 步骤6: 保存完整消息体
    message.setCompleteMsg(decryptedData);
    
    // 步骤7: 发布事件
    publishChatMessageReceivedEvent(message);
}
```

#### publishChatMessageReceivedEvent()
```java
private void publishChatMessageReceivedEvent(XianyuChatMessage message) {
    try {
        ChatMessageReceivedEvent event = new ChatMessageReceivedEvent(this, message);
        eventPublisher.publishEvent(event);
        // 主线程立即返回，不等待监听器执行
    } catch (Exception e) {
        log.error("【账号{}】发布消息接收事件失败: pnmId={}", 
                message.getXianyuAccountId(), message.getPnmId(), e);
    }
}
```

---

## 事件驱动处理


### 1. ChatMessageReceivedEvent 类

**包名**：`com.feijimiao.xianyuassistant.event.chatMessageEvent`

**继承**：`extends ApplicationEvent`

**定义**：

```java
@Getter
public class ChatMessageReceivedEvent extends ApplicationEvent {
    
    private final XianyuChatMessage chatMessage;
    
    public ChatMessageReceivedEvent(Object source, XianyuChatMessage chatMessage) {
        super(source);
        this.chatMessage = chatMessage;
    }
}
```

### 2. ChatMessageEventSaveListener 类

**包名**：`com.feijimiao.xianyuassistant.event.chatMessageEvent`

**依赖注入**：

```java
@Autowired
private XianyuChatMessageMapper chatMessageMapper;
```

**核心方法**：

```java
@Async  // 异步执行
@EventListener  // 监听事件
public void handleChatMessageReceived(ChatMessageReceivedEvent event) {
    XianyuChatMessage message = event.getChatMessage();
    
    try {
        // 步骤1: 检查消息是否已存在（去重）
        XianyuChatMessage existing = chatMessageMapper.findByPnmId(
                message.getXianyuAccountId(), message.getPnmId());
        
        if (existing != null) {
            log.debug("【账号{}】消息已存在，跳过保存: pnmId={}", 
                    message.getXianyuAccountId(), message.getPnmId());
            return;
        }
        
        // 步骤2: 保存消息到数据库
        int result = chatMessageMapper.insert(message);
        
        if (result <= 0) {
            log.error("【账号{}】保存消息失败: pnmId={}", 
                    message.getXianyuAccountId(), message.getPnmId());
        }
        
    } catch (Exception e) {
        // 检查是否是唯一约束冲突（消息已存在）
        if (e.getMessage() != null && e.getMessage().contains("UNIQUE constraint failed")) {
            log.debug("【账号{}】消息ID冲突，跳过保存: pnmId={}", 
                    message.getXianyuAccountId(), message.getPnmId());
        } else {
            log.error("【账号{}】异步保存消息异常: pnmId={}, error={}", 
                    message.getXianyuAccountId(), message.getPnmId(), e.getMessage());
        }
    }
}
```

### 3. ChatMessageEventAutoDeliveryListener 类

**包名**：`com.feijimiao.xianyuassistant.event.chatMessageEvent`

**依赖注入**：

```java
@Autowired
private XianyuGoodsConfigMapper goodsConfigMapper;

@Autowired
private XianyuGoodsAutoDeliveryConfigMapper autoDeliveryConfigMapper;

@Autowired
private XianyuGoodsAutoDeliveryRecordMapper autoDeliveryRecordMapper;

@Autowired
private WebSocketService webSocketService;
```

**核心方法**：

#### handleChatMessageReceived()
```java
@Async  // 异步执行
@EventListener  // 监听事件
public void handleChatMessageReceived(ChatMessageReceivedEvent event) {
    XianyuChatMessage message = event.getChatMessage();
    
    try {
        // 步骤1: 判断是否需要触发自动发货
        if (message.getContentType() == null || message.getContentType() != 26) {
            return; // 不是已付款待发货消息
        }
        
        if (message.getMsgContent() == null || 
            !message.getMsgContent().contains("[已付款，待发货]")) {
            return; // 消息内容不符合条件
        }
        
        log.info("【账号{}】检测到已付款待发货消息: xyGoodsId={}, sId={}, content={}", 
                message.getXianyuAccountId(), message.getXyGoodsId(), 
                message.getSId(), message.getMsgContent());
        
        // 步骤2: 检查是否有商品ID和会话ID
        if (message.getXyGoodsId() == null || message.getSId() == null) {
            log.warn("【账号{}】消息缺少商品ID或会话ID，无法触发自动发货: pnmId={}", 
                    message.getXianyuAccountId(), message.getPnmId());
            return;
        }
        
        // 步骤3: 从消息内容中提取买家名称
        String buyerUserName = extractBuyerNameFromContent(message.getMsgContent());
        
        log.info("【账号{}】提取买家信息: buyerUserId={}, buyerUserName={}", 
                message.getXianyuAccountId(), message.getSenderUserId(), buyerUserName);
        
        // 步骤4: 创建发货记录（state=0，待发货）
        XianyuGoodsAutoDeliveryRecord record = new XianyuGoodsAutoDeliveryRecord();
        record.setXianyuAccountId(message.getXianyuAccountId());
        record.setXyGoodsId(message.getXyGoodsId());
        record.setBuyerUserId(message.getSenderUserId());
        record.setBuyerUserName(buyerUserName);
        record.setContent(null); // 内容稍后设置
        record.setState(0); // 0=待发货
        
        int result = autoDeliveryRecordMapper.insert(record);
        
        if (result > 0) {
            log.info("【账号{}】创建发货记录成功: recordId={}, xyGoodsId={}, buyerUserName={}, state=0（待发货）", 
                    message.getXianyuAccountId(), record.getId(), 
                    message.getXyGoodsId(), buyerUserName);
            
            // 步骤5: 执行自动发货
            executeAutoDelivery(record.getId(), message.getXianyuAccountId(), 
                    message.getXyGoodsId(), message.getSId());
        } else {
            log.error("【账号{}】创建发货记录失败: xyGoodsId={}", 
                    message.getXianyuAccountId(), message.getXyGoodsId());
        }
        
    } catch (Exception e) {
        log.error("【账号{}】处理自动发货异常: pnmId={}, error={}", 
                message.getXianyuAccountId(), message.getPnmId(), e.getMessage(), e);
    }
}
```

#### executeAutoDelivery()
```java
private void executeAutoDelivery(Long recordId, Long accountId, 
                                  String xyGoodsId, String sId) {
    try {
        log.info("【账号{}】开始执行自动发货: recordId={}, xyGoodsId={}", 
                accountId, recordId, xyGoodsId);
        
        // 步骤1: 检查商品是否开启自动发货
        XianyuGoodsConfig goodsConfig = goodsConfigMapper
                .selectByAccountAndGoodsId(accountId, xyGoodsId);
        if (goodsConfig == null || goodsConfig.getXianyuAutoDeliveryOn() != 1) {
            log.info("【账号{}】商品未开启自动发货: xyGoodsId={}", accountId, xyGoodsId);
            updateRecordState(recordId, -1);
            return;
        }
        
        // 步骤2: 获取自动发货配置
        XianyuGoodsAutoDeliveryConfig deliveryConfig = autoDeliveryConfigMapper
                .findByAccountIdAndGoodsId(accountId, xyGoodsId);
        if (deliveryConfig == null || deliveryConfig.getAutoDeliveryContent() == null || 
                deliveryConfig.getAutoDeliveryContent().isEmpty()) {
            log.warn("【账号{}】商品未配置自动发货内容: xyGoodsId={}", accountId, xyGoodsId);
            updateRecordState(recordId, -1);
            return;
        }
        
        String content = deliveryConfig.getAutoDeliveryContent();
        log.info("【账号{}】准备发送自动发货消息: content={}", accountId, content);
        
        // 步骤3: 模拟人工操作：阅读消息 + 思考 + 打字延迟
        log.info("【账号{}】模拟人工操作延迟...", accountId);
        HumanLikeDelayUtils.mediumDelay();      // 阅读延迟: 1-3秒
        HumanLikeDelayUtils.thinkingDelay();    // 思考延迟: 2-5秒
        HumanLikeDelayUtils.typingDelay(content.length()); // 打字延迟
        
        // 步骤4: 从sId中提取cid和toId
        String cid = sId.replace("@goofish", "");
        String toId = cid;
        
        // 步骤5: 发送消息
        boolean success = webSocketService.sendMessage(accountId, cid, toId, content);
        
        // 步骤6: 更新发货记录状态
        if (success) {
            log.info("【账号{}】自动发货成功: recordId={}, xyGoodsId={}, content={}", 
                    accountId, recordId, xyGoodsId, content);
            updateRecordState(recordId, 1);
        } else {
            log.error("【账号{}】自动发货失败: recordId={}, xyGoodsId={}", 
                    accountId, recordId, xyGoodsId);
            updateRecordState(recordId, -1);
        }
        
    } catch (Exception e) {
        log.error("【账号{}】执行自动发货异常: recordId={}, xyGoodsId={}", 
                accountId, recordId, xyGoodsId, e);
        updateRecordState(recordId, -1);
    }
}
```

#### updateRecordState()
```java
private void updateRecordState(Long recordId, Integer state) {
    try {
        autoDeliveryRecordMapper.updateState(recordId, state);
        log.info("更新发货记录状态: recordId={}, state={}", recordId, state);
    } catch (Exception e) {
        log.error("更新发货记录状态失败: recordId={}, state={}", recordId, state, e);
    }
}
```

---

## 消息发送流程

### 1. 完整调用链

```
用户操作 / 自动发货触发
    ↓
WebSocketService.sendMessage(accountId, cid, toId, text)
    ↓
WebSocketServiceImpl.sendMessage(accountId, cid, toId, text)
    ↓
获取 WebSocket 客户端
    ↓
XianyuWebSocketClient.sendMessage(cid, toId, text)
    ↓
构造消息 JSON
    ↓
client.send(messageJson)
    ↓
WebSocket 发送到闲鱼服务器
```

### 2. WebSocketServiceImpl.sendMessage()

```java
@Override
public boolean sendMessage(Long accountId, String cid, String toId, String text) {
    try {
        log.info("发送消息: accountId={}, cid={}, toId={}, text={}", 
                accountId, cid, toId, text);
        
        // 步骤1: 获取 WebSocket 客户端
        XianyuWebSocketClient client = webSocketClients.get(accountId);
        if (client == null) {
            log.error("WebSocket客户端不存在: accountId={}", accountId);
            return false;
        }
        
        // 步骤2: 检查连接状态
        if (!client.isConnected()) {
            log.error("WebSocket未连接: accountId={}", accountId);
            return false;
        }
        
        // 步骤3: 发送消息
        client.sendMessage(cid, toId, text);
        return true;
        
    } catch (Exception e) {
        log.error("发送消息失败: accountId={}, cid={}, toId={}", 
                accountId, cid, toId, e);
        return false;
    }
}
```

### 3. XianyuWebSocketClient.sendMessage()

```java
public void sendMessage(String cid, String toId, String text) {
    if (!isConnected) {
        log.error("【账号{}】WebSocket未连接，无法发送消息", accountId);
        return;
    }
    
    try {
        // 步骤1: 构造消息内容
        Map<String, Object> textContent = new HashMap<>();
        textContent.put("contentType", 1);
        Map<String, String> textData = new HashMap<>();
        textData.put("text", text);
        textContent.put("text", textData);
        
        // 步骤2: Base64编码消息内容
        String textJson = objectMapper.writeValueAsString(textContent);
        String textBase64 = Base64.getEncoder().encodeToString(
                textJson.getBytes("UTF-8"));
        
        // 步骤3: 构造消息体
        Map<String, Object> messageBody = new HashMap<>();
        messageBody.put("uuid", generateUuid());
        messageBody.put("cid", cid + "@goofish");
        messageBody.put("conversationType", 1);
        
        Map<String, Object> content = new HashMap<>();
        content.put("contentType", 101);
        Map<String, Object> custom = new HashMap<>();
        custom.put("type", 1);
        custom.put("data", textBase64);
        content.put("custom", custom);
        messageBody.put("content", content);
        
        messageBody.put("redPointPolicy", 0);
        
        // 步骤4: 构造接收者列表
        Map<String, Object> receivers = new HashMap<>();
        List<String> actualReceivers = new ArrayList<>();
        actualReceivers.add(toId + "@goofish");
        actualReceivers.add(myUserId + "@goofish");
        receivers.put("actualReceivers", actualReceivers);
        
        // 步骤5: 构造完整消息
        Map<String, Object> message = new HashMap<>();
        message.put("lwp", "/r/MessageSend/sendByReceiverScope");
        
        Map<String, String> headers = new HashMap<>();
        headers.put("mid", generateMid());
        message.put("headers", headers);
        
        List<Object> body = new ArrayList<>();
        body.add(messageBody);
        body.add(receivers);
        message.put("body", body);
        
        // 步骤6: 发送
        String messageJson = objectMapper.writeValueAsString(message);
        send(messageJson);
        
        log.info("【账号{}】发送消息: cid={}, toId={}, text={}", 
                accountId, cid, toId, text);
        
    } catch (Exception e) {
        log.error("【账号{}】发送消息失败: cid={}, toId={}", accountId, cid, toId, e);
    }
}
```

---

## 类职责详解

### 连接层

#### 1. WebSocketController
- **职责**：提供 HTTP 接口
- **方法**：
  - `startWebSocket(accountId)` - 启动连接
  - `stopWebSocket(accountId)` - 停止连接
  - `getStatus(accountId)` - 获取连接状态

#### 2. WebSocketServiceImpl
- **职责**：管理 WebSocket 连接生命周期
- **方法**：
  - `startWebSocket(accountId)` - 启动连接
  - `startWebSocketWithToken(accountId, accessToken)` - 使用手动Token启动
  - `stopWebSocket(accountId)` - 停止连接
  - `isConnected(accountId)` - 检查连接状态
  - `sendMessage(accountId, cid, toId, text)` - 发送消息
  - `connectWebSocket(...)` - 通用连接方法
  - `startHeartbeat(accountId, client)` - 启动心跳
  - `stopHeartbeat(accountId)` - 停止心跳

#### 3. XianyuWebSocketClient
- **职责**：WebSocket 客户端实现
- **方法**：
  - `onOpen(handshake)` - 连接建立回调
  - `onMessage(message)` - 消息接收回调
  - `onClose(code, reason, remote)` - 连接关闭回调
  - `onError(ex)` - 错误处理回调
  - `handleMessage(message)` - 消息处理
  - `handleMessageWithSemaphore(message)` - 带信号量的消息处理
  - `sendMessage(cid, toId, text)` - 发送消息
  - `sendHeartbeat()` - 发送心跳
  - `sendAckMessage(messageData)` - 发送ACK确认
  - `setMessageHandler(handler)` - 设置消息处理器
  - `setMyUserId(userId)` - 设置当前用户ID
  - `setOnRegistrationSuccess(callback)` - 设置注册成功回调
  - `setOnTokenExpired(callback)` - 设置Token失效回调

#### 4. WebSocketInitializer
- **职责**：WebSocket 连接初始化
- **方法**：
  - `initialize(client, accessToken, deviceId, accountId)` - 初始化连接
  - 发送注册消息
  - 发送订阅消息

### 消息处理层

#### 5. DefaultWebSocketMessageHandler
- **职责**：消息处理入口
- **方法**：
  - `handleMessage(accountId, messageData)` - 处理消息
  - `handleHeartbeat(accountId)` - 处理心跳
  - `handleError(accountId, error)` - 处理错误

#### 6. WebSocketMessageRouter
- **职责**：消息路由
- **方法**：
  - `route(accountId, messageData)` - 路由消息
  - `registerHandler(handler)` - 注册Handler
  - `ensureInitialized()` - 确保已初始化
  - `handleResponseMessage(accountId, messageData)` - 处理响应消息
  - `handleUnknownMessage(accountId, messageData)` - 处理未知消息

#### 7. AbstractLwpHandler
- **职责**：Handler 模板方法
- **方法**：
  - `handle(accountId, messageData)` - 模板方法（final）
  - `getLwpPath()` - 获取处理的lwp路径（抽象）
  - `parseParams(accountId, messageData)` - 解析参数（抽象）
  - `doHandle(accountId, params, messageData)` - 核心处理（抽象）
  - `postHandle(accountId, result, messageData)` - 后置处理（可选）

#### 8. SyncMessageHandler
- **职责**：同步消息解析器
- **方法**：
  - `getLwpPath()` - 返回 "/s/para"
  - `parseParams(accountId, messageData)` - 解析同步包参数
  - `doHandle(accountId, params, messageData)` - 解析消息并发布事件
  - `parseAndPublishEvent(accountId, decryptedData, lwp)` - 解析并发布事件
  - `publishChatMessageReceivedEvent(message)` - 发布事件
  - `extractItemIdFromUrl(url)` - 提取商品ID
  - `extractString(map, key)` - 提取字符串
  - `extractInteger(map, key)` - 提取整数
  - `extractLong(map, key)` - 提取长整数

### 事件层

#### 9. ChatMessageReceivedEvent
- **职责**：消息接收事件
- **属性**：
  - `chatMessage` - XianyuChatMessage 对象

#### 10. ChatMessageEventSaveListener
- **职责**：消息保存监听器
- **方法**：
  - `handleChatMessageReceived(event)` - 处理事件，保存消息

#### 11. ChatMessageEventAutoDeliveryListener
- **职责**：自动发货监听器
- **方法**：
  - `handleChatMessageReceived(event)` - 处理事件，判断并执行自动发货
  - `executeAutoDelivery(recordId, accountId, xyGoodsId, sId)` - 执行自动发货
  - `updateRecordState(recordId, state)` - 更新记录状态
  - `extractBuyerNameFromContent(content)` - 提取买家名称

### 工具类

#### 12. MessageDecryptUtils
- **职责**：消息解密
- **方法**：
  - `decrypt(encryptedData)` - 解密消息
  - `tryDecrypt(data)` - 尝试解密

#### 13. HumanLikeDelayUtils
- **职责**：模拟人工延迟
- **方法**：
  - `shortDelay()` - 短延迟 (0.5-1.5秒)
  - `mediumDelay()` - 中等延迟 (1-3秒)
  - `thinkingDelay()` - 思考延迟 (2-5秒)
  - `typingDelay(textLength)` - 打字延迟 (0.1-0.3秒/字)

---

## 完整时序图

```
时间轴 →

T0: 闲鱼服务器发送消息
    ↓
T1: XianyuWebSocketClient.onMessage() 接收
    ↓
T2: messageExecutor 提交到线程池
    ↓
T3: handleMessageWithSemaphore() 获取信号量
    ↓
T4: handleMessage() 解析JSON + 解密
    ↓
T5: sendAckMessage() 发送ACK确认
    ↓
T6: messageHandler.handleMessage() 调用处理器
    ↓
T7: DefaultWebSocketMessageHandler.handleMessage()
    ↓
T8: WebSocketMessageRouter.route() 路由
    ↓
T9: SyncMessageHandler.handle() 开始处理
    ↓
T10: parseParams() 解析参数
    ↓
T11: doHandle() 核心处理
    ↓
T12: parseAndPublishEvent() 解析字段
    ↓
T13: publishChatMessageReceivedEvent() 发布事件
    ↓
T14: eventPublisher.publishEvent() Spring事件机制
    ↓
T15: SyncMessageHandler 返回（主线程完成）
    ↓
    ├─→ [异步线程1] T16: ChatMessageEventSaveListener 开始
    │       ↓
    │   T17: findByPnmId() 检查是否已存在
    │       ↓
    │   T18: insert() 保存到数据库
    │       ↓
    │   T19: ChatMessageEventSaveListener 完成
    │
    └─→ [异步线程2] T20: ChatMessageEventAutoDeliveryListener 开始
            ↓
        T21: 判断是否需要自动发货
            ↓
        T22: insert() 创建发货记录
            ↓
        T23: selectByAccountAndGoodsId() 检查商品配置
            ↓
        T24: findByAccountIdAndGoodsId() 获取发货内容
            ↓
        T25: HumanLikeDelayUtils 模拟延迟 (5-10秒)
            ↓
        T26: webSocketService.sendMessage() 发送消息
            ↓
        T27: updateState() 更新记录状态
            ↓
        T28: ChatMessageEventAutoDeliveryListener 完成

注意：
- T0-T15 在主线程执行，约 10-50ms
- T16-T19 在异步线程1执行，约 10-50ms
- T20-T28 在异步线程2执行，约 5-10秒（包含人工延迟）
- T16 和 T20 同时开始，并发执行
```

---

## 总结

### 核心类数量

- **连接层**：4个类
- **消息处理层**：4个类
- **事件层**：3个类
- **工具类**：2个类
- **总计**：13个核心类

### 设计特点

1. **职责清晰**：每个类只负责一件事
2. **分层明确**：连接、路由、解析、业务分层处理
3. **事件驱动**：业务逻辑通过事件异步执行
4. **易于扩展**：新增业务只需添加EventListener
5. **高性能**：主线程快速返回，业务异步并发

### 关键流程

1. **连接建立**：WebSocketController → WebSocketServiceImpl → XianyuWebSocketClient
2. **消息接收**：XianyuWebSocketClient → DefaultWebSocketMessageHandler → WebSocketMessageRouter → SyncMessageHandler
3. **事件发布**：SyncMessageHandler → ApplicationEventPublisher
4. **业务处理**：ChatMessageEventSaveListener + ChatMessageEventAutoDeliveryListener（并发）
5. **消息发送**：WebSocketService → XianyuWebSocketClient

所有类的职责和调用关系都已详细说明，可以作为系统的技术文档使用！
