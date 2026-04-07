package com.feijimiao.xianyuassistant.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsAutoDeliveryRecordMapper;
import com.feijimiao.xianyuassistant.service.AccountService;
import com.feijimiao.xianyuassistant.service.OrderService;
import com.feijimiao.xianyuassistant.utils.XianyuSignUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 订单服务实现类
 * 参考Python代码的secure_confirm_decrypted.py
 */
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private AccountService accountService;
    
    @Autowired
    private XianyuGoodsAutoDeliveryRecordMapper autoDeliveryRecordMapper;
    
    @Autowired
    private com.feijimiao.xianyuassistant.service.TokenRefreshService tokenRefreshService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    /**
     * 确认发货API地址
     */
    private static final String CONFIRM_SHIPMENT_URL = "https://h5api.m.goofish.com/h5/mtop.taobao.idle.logistic.consign.dummy/1.0/";

    @Override
    public String confirmShipment(Long accountId, String orderId) {
        try {
            log.info("【账号{}】开始确认发货: orderId={}", accountId, orderId);

            // 在调用API前先刷新_m_h5_tk token，确保token有效
            log.info("【账号{}】调用API前刷新_m_h5_tk token...", accountId);
            boolean refreshSuccess = tokenRefreshService.refreshMh5tkToken(accountId);
            if (!refreshSuccess) {
                log.warn("【账号{}】⚠️ Token刷新失败，继续使用现有token尝试", accountId);
            }

            // 获取Cookie
            String cookieStr = accountService.getCookieByAccountId(accountId);
            if (cookieStr == null || cookieStr.isEmpty()) {
                log.error("【账号{}】未找到Cookie", accountId);
                return null;
            }

            // 解析Cookie
            Map<String, String> cookies = XianyuSignUtils.parseCookies(cookieStr);

            // 提取token
            String token = XianyuSignUtils.extractToken(cookies);
            if (token.isEmpty()) {
                log.error("【账号{}】Cookie中缺少_m_h5_tk字段", accountId);
                return null;
            }

            // 生成时间戳
            String timestamp = String.valueOf(System.currentTimeMillis());

            // 构造data参数（参考Python代码）
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("orderId", orderId);
            dataMap.put("tradeText", "");
            dataMap.put("picList", new String[0]);
            dataMap.put("newUnconsign", true);
            String dataVal = objectMapper.writeValueAsString(dataMap);

            log.info("【账号{}】data参数: {}", accountId, dataVal);

            // 生成签名
            String sign = XianyuSignUtils.generateSign(timestamp, token, dataVal);

            log.info("【账号{}】签名生成: timestamp={}, token={}, sign={}", 
                    accountId, timestamp, token.substring(0, Math.min(10, token.length())) + "...", sign);

            // 构造URL参数
            Map<String, String> params = new HashMap<>();
            params.put("jsv", "2.7.2");
            params.put("appKey", "34839810");
            params.put("t", timestamp);
            params.put("sign", sign);
            params.put("v", "1.0");
            params.put("type", "originaljson");
            params.put("accountSite", "xianyu");
            params.put("dataType", "json");
            params.put("timeout", "20000");
            params.put("api", "mtop.taobao.idle.logistic.consign.dummy");
            params.put("sessionOption", "AutoLoginOnly");

            // 构造完整URL
            StringBuilder urlBuilder = new StringBuilder(CONFIRM_SHIPMENT_URL);
            urlBuilder.append("?");
            for (Map.Entry<String, String> entry : params.entrySet()) {
                urlBuilder.append(entry.getKey())
                        .append("=")
                        .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                        .append("&");
            }
            String url = urlBuilder.substring(0, urlBuilder.length() - 1);

            log.info("【账号{}】请求URL: {}", accountId, url);

            // 构造POST body
            String postBody = "data=" + URLEncoder.encode(dataVal, StandardCharsets.UTF_8);

            // 构造请求
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Cookie", cookieStr)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36")
                    .header("Referer", "https://market.m.goofish.com/")
                    .header("Origin", "https://market.m.goofish.com")
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Accept-Language", "zh-CN,zh;q=0.9")
                    .POST(HttpRequest.BodyPublishers.ofString(postBody))
                    .timeout(Duration.ofSeconds(20))
                    .build();

            // 发送请求
            log.info("【账号{}】发送确认发货请求...", accountId);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            log.info("【账号{}】响应状态码: {}", accountId, response.statusCode());
            log.info("【账号{}】响应内容: {}", accountId, response.body());

            // 解析响应
            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(response.body(), Map.class);

            // 检查响应
            if (result.containsKey("ret")) {
                @SuppressWarnings("unchecked")
                java.util.List<String> ret = (java.util.List<String>) result.get("ret");
                if (ret != null && !ret.isEmpty()) {
                    String retCode = ret.get(0);
                    
                    // 成功情况
                    if (retCode.contains("SUCCESS")) {
                        log.info("【账号{}】✅ 确认发货成功: orderId={}", accountId, orderId);
                        // 更新确认发货状态为1
                        updateOrderStateToConfirmed(accountId, orderId);
                        return "确认发货成功";
                    }
                    
                    // 已经发货的情况，也视为成功
                    if (retCode.contains("ORDER_ALREADY_DELIVERY")) {
                        log.info("【账号{}】✅ 订单已经发货成功: orderId={}", accountId, orderId);
                        // 更新确认发货状态为1
                        updateOrderStateToConfirmed(accountId, orderId);
                        return "订单已经发货成功";
                    }
                    
                    // Token过期
                    if (retCode.contains("TOKEN_EXOIRED") || retCode.contains("TOKEN_EXPIRED")) {
                        log.warn("【账号{}】⚠️ Token已过期: orderId={}", accountId, orderId);
                        return null; // 返回null表示失败，前端会显示"确认发货失败"
                    }
                }
            }

            log.error("【账号{}】❌ 确认发货失败: {}", accountId, result);
            return null;

        } catch (Exception e) {
            log.error("【账号{}】确认发货异常: orderId={}", accountId, orderId, e);
            return null;
        }
    }
    
    /**
     * 更新确认发货状态为已确认
     */
    private void updateOrderStateToConfirmed(Long accountId, String orderId) {
        try {
            int rows = autoDeliveryRecordMapper.updateOrderState(accountId, orderId, 1);
            if (rows > 0) {
                log.info("【账号{}】✅ 更新确认发货状态成功: orderId={}, orderState=1", accountId, orderId);
            } else {
                log.warn("【账号{}】⚠️ 未找到对应的发货记录: orderId={}", accountId, orderId);
            }
        } catch (Exception e) {
            log.error("【账号{}】❌ 更新确认发货状态失败: orderId={}", accountId, orderId, e);
        }
    }
}
