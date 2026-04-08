package com.feijimiao.xianyuassistant.event.chatMessageEvent;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsAutoDeliveryConfig;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsAutoDeliveryItem;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsAutoDeliveryRecord;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsConfig;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsInfo;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsAutoDeliveryConfigMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsAutoDeliveryRecordMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsConfigMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsInfoMapper;
import com.feijimiao.xianyuassistant.service.AutoDeliveryInventoryService;
import com.feijimiao.xianyuassistant.service.OrderService;
import com.feijimiao.xianyuassistant.service.WebSocketService;
import com.feijimiao.xianyuassistant.utils.HumanLikeDelayUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 聊天消息自动发货监听器
 */
@Slf4j
@Component
public class ChatMessageEventAutoDeliveryListener {

    @Autowired
    private XianyuGoodsConfigMapper goodsConfigMapper;

    @Autowired
    private XianyuGoodsAutoDeliveryConfigMapper autoDeliveryConfigMapper;

    @Autowired
    private XianyuGoodsAutoDeliveryRecordMapper autoDeliveryRecordMapper;

    @Autowired
    private XianyuGoodsInfoMapper goodsInfoMapper;

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private AutoDeliveryInventoryService autoDeliveryInventoryService;

    @Async
    @EventListener
    public void handleChatMessageReceived(ChatMessageReceivedEvent event) {
        ChatMessageData message = event.getMessageData();

        log.info("【账号{}】[AutoDeliveryListener]收到ChatMessageReceivedEvent事件: pnmId={}, contentType={}, msgContent={}, xyGoodsId={}, sId={}, orderId={}",
                message.getXianyuAccountId(), message.getPnmId(), message.getContentType(),
                message.getMsgContent(), message.getXyGoodsId(), message.getSId(), message.getOrderId());

        try {
            if (message.getContentType() == null || message.getContentType() != 32) {
                return;
            }

            if (message.getMsgContent() == null || !message.getMsgContent().contains("[已付款，待发货]")) {
                return;
            }

            if (message.getXyGoodsId() == null || message.getSId() == null) {
                log.warn("【账号{}】消息缺少商品ID或会话ID，无法触发自动发货: pnmId={}",
                        message.getXianyuAccountId(), message.getPnmId());
                return;
            }

            QueryWrapper<XianyuGoodsInfo> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("xy_good_id", message.getXyGoodsId());
            queryWrapper.eq("xianyu_account_id", message.getXianyuAccountId());
            XianyuGoodsInfo goodsInfo = goodsInfoMapper.selectOne(queryWrapper);

            if (goodsInfo == null) {
                log.warn("【账号{}】未找到商品信息: xyGoodsId={}",
                        message.getXianyuAccountId(), message.getXyGoodsId());
                return;
            }

            XianyuGoodsAutoDeliveryRecord record = new XianyuGoodsAutoDeliveryRecord();
            record.setXianyuAccountId(message.getXianyuAccountId());
            record.setXianyuGoodsId(goodsInfo.getId());
            record.setXyGoodsId(message.getXyGoodsId());
            record.setPnmId(message.getPnmId());
            record.setBuyerUserId(message.getSenderUserId());
            record.setBuyerUserName(message.getSenderUserName());
            record.setOrderId(message.getOrderId());
            record.setContent(null);
            record.setState(0);

            int result;
            try {
                result = autoDeliveryRecordMapper.insert(record);
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("UNIQUE constraint failed")) {
                    log.info("【账号{}】消息已处理过，跳过自动发货: pnmId={}, xyGoodsId={}",
                            message.getXianyuAccountId(), message.getPnmId(), message.getXyGoodsId());
                    return;
                }
                throw e;
            }

            if (result > 0) {
                executeAutoDelivery(record.getId(),
                        message.getXianyuAccountId(),
                        goodsInfo.getId(),
                        message.getXyGoodsId(),
                        message.getSId(),
                        message.getOrderId(),
                        message.getSenderUserId(),
                        message.getSenderUserName());
            }
        } catch (Exception e) {
            log.error("【账号{}】处理自动发货异常: pnmId={}, error={}",
                    message.getXianyuAccountId(), message.getPnmId(), e.getMessage(), e);
        }
    }

    private void executeAutoDelivery(Long recordId,
                                     Long accountId,
                                     Long xianyuGoodsId,
                                     String xyGoodsId,
                                     String sId,
                                     String orderId,
                                     String buyerUserId,
                                     String buyerUserName) {
        XianyuGoodsAutoDeliveryItem reservedItem = null;
        try {
            XianyuGoodsConfig goodsConfig = goodsConfigMapper.selectByAccountAndGoodsId(accountId, xyGoodsId);
            if (goodsConfig == null || goodsConfig.getXianyuAutoDeliveryOn() != 1) {
                updateRecordState(recordId, -1, null);
                return;
            }

            XianyuGoodsAutoDeliveryConfig deliveryConfig = autoDeliveryConfigMapper.findByAccountIdAndGoodsId(accountId, xyGoodsId);
            if (deliveryConfig == null) {
                updateRecordState(recordId, -1, "未配置自动发货");
                return;
            }

            String content;
            Integer deliveryType = deliveryConfig.getType() == null ? 1 : deliveryConfig.getType();
            if (deliveryType == 2) {
                reservedItem = autoDeliveryInventoryService.reserveNextItem(
                        accountId,
                        xianyuGoodsId,
                        xyGoodsId,
                        recordId,
                        orderId,
                        buyerUserId,
                        buyerUserName
                );
                if (reservedItem == null) {
                    log.warn("【账号{}】自动发货库存不足: xyGoodsId={}, recordId={}", accountId, xyGoodsId, recordId);
                    updateRecordState(recordId, -1, "库存不足，未发货");
                    return;
                }
                content = reservedItem.getDeliveryContent();
            } else {
                content = deliveryConfig.getAutoDeliveryContent();
                if (content == null || content.isEmpty()) {
                    updateRecordState(recordId, -1, "未配置固定发货内容");
                    return;
                }
            }

            HumanLikeDelayUtils.mediumDelay();
            HumanLikeDelayUtils.thinkingDelay();
            HumanLikeDelayUtils.typingDelay(content.length());

            String cid = sId.replace("@goofish", "");
            String toId = cid;
            boolean success = webSocketService.sendMessage(accountId, cid, toId, content);

            if (success) {
                if (reservedItem != null) {
                    autoDeliveryInventoryService.markItemUsed(reservedItem.getId());
                }
                updateRecordState(recordId, 1, content);

                if (deliveryConfig.getAutoConfirmShipment() != null && deliveryConfig.getAutoConfirmShipment() == 1) {
                    executeAutoConfirmShipment(accountId, orderId);
                }
            } else {
                if (reservedItem != null) {
                    autoDeliveryInventoryService.releaseReservedItem(reservedItem.getId());
                }
                updateRecordState(recordId, -1, content);
            }
        } catch (Exception e) {
            if (reservedItem != null) {
                autoDeliveryInventoryService.releaseReservedItem(reservedItem.getId());
            }
            log.error("【账号{}】执行自动发货异常: recordId={}, xyGoodsId={}", accountId, recordId, xyGoodsId, e);
            updateRecordState(recordId, -1, null);
        }
    }

    private void executeAutoConfirmShipment(Long accountId, String orderId) {
        try {
            if (orderId == null || orderId.isEmpty()) {
                log.warn("【账号{}】订单ID为空，无法自动确认发货", accountId);
                return;
            }

            HumanLikeDelayUtils.longDelay();
            String result = orderService.confirmShipment(accountId, orderId);
            if (result != null) {
                log.info("【账号{}】自动确认发货成功: orderId={}, result={}", accountId, orderId, result);
            } else {
                log.error("【账号{}】自动确认发货失败: orderId={}", accountId, orderId);
            }
        } catch (Exception e) {
            log.error("【账号{}】自动确认发货异常: orderId={}", accountId, orderId, e);
        }
    }

    private void updateRecordState(Long recordId, Integer state, String content) {
        try {
            autoDeliveryRecordMapper.updateStateAndContent(recordId, state, content);
        } catch (Exception e) {
            log.error("更新发货记录状态和内容失败: recordId={}, state={}, content={}", recordId, state, content, e);
        }
    }
}
