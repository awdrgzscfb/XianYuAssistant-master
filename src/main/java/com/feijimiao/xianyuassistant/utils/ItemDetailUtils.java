package com.feijimiao.xianyuassistant.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * 商品详情工具类
 */
@Slf4j
public class ItemDetailUtils {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 从商品详情JSON中提取desc字段
     * 
     * @param detailJson 商品详情JSON字符串
     * @return 提取的desc字段内容，如果提取失败则返回原始JSON
     */
    public static String extractDescFromDetailJson(String detailJson) {
        if (detailJson == null || detailJson.isEmpty()) {
            return detailJson;
        }
        
        try {
            // 解析JSON
            JsonNode rootNode = objectMapper.readTree(detailJson);
            
            // 提取itemDO.desc字段
            JsonNode itemDONode = rootNode.get("itemDO");
            if (itemDONode != null && !itemDONode.isNull()) {
                JsonNode descNode = itemDONode.get("desc");
                if (descNode != null && !descNode.isNull()) {
                    String desc = descNode.asText();
                    log.info("成功提取desc字段，长度: {}", desc.length());
                    return desc;
                } else {
                    log.warn("itemDO中未找到desc字段");
                }
            } else {
                log.warn("未找到itemDO字段");
            }
            
            // 如果提取失败，返回原始JSON
            log.warn("无法提取desc字段，返回原始JSON，长度: {}", detailJson.length());
            return detailJson;
        } catch (Exception e) {
            log.error("解析商品详情JSON失败，返回原始JSON: {}", e.getMessage());
            return detailJson;
        }
    }
}