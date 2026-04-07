package com.feijimiao.xianyuassistant.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ItemDetailUtilsTest {

    @Test
    void testExtractDescFromDetailJson() {
        // 测试正常情况
        String detailJson = "{\"itemDO\":{\"desc\":\"测试描述内容\"}}";
        String expectedDesc = "测试描述内容";
        String actualDesc = ItemDetailUtils.extractDescFromDetailJson(detailJson);
        assertEquals(expectedDesc, actualDesc);
    }
    
    @Test
    void testExtractDescFromDetailJsonWithoutItemDO() {
        // 测试没有itemDO字段的情况
        String detailJson = "{\"b2cBuyerDO\":{\"field1\":\"value1\"}}";
        String actualDesc = ItemDetailUtils.extractDescFromDetailJson(detailJson);
        // 应该返回原始JSON
        assertEquals(detailJson, actualDesc);
    }
    
    @Test
    void testExtractDescFromDetailJsonWithoutDesc() {
        // 测试有itemDO但没有desc字段的情况
        String detailJson = "{\"itemDO\":{\"otherField\":\"otherValue\"}}";
        String actualDesc = ItemDetailUtils.extractDescFromDetailJson(detailJson);
        // 应该返回原始JSON
        assertEquals(detailJson, actualDesc);
    }
    
    @Test
    void testExtractDescFromDetailJsonWithNullInput() {
        // 测试null输入
        String actualDesc = ItemDetailUtils.extractDescFromDetailJson(null);
        // 应该返回null
        assertNull(actualDesc);
    }
    
    @Test
    void testExtractDescFromDetailJsonWithEmptyInput() {
        // 测试空字符串输入
        String actualDesc = ItemDetailUtils.extractDescFromDetailJson("");
        // 应该返回空字符串
        assertEquals("", actualDesc);
    }
    
    @Test
    void testExtractDescFromDetailJsonWithInvalidJson() {
        // 测试无效JSON
        String detailJson = "{ invalid json }";
        String actualDesc = ItemDetailUtils.extractDescFromDetailJson(detailJson);
        // 应该返回原始JSON
        assertEquals(detailJson, actualDesc);
    }
}