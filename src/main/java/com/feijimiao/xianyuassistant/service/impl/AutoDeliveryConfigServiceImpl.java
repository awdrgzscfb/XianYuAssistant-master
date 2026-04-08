package com.feijimiao.xianyuassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.feijimiao.xianyuassistant.common.ResultObject;
import com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryConfigQueryReqDTO;
import com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryConfigReqDTO;
import com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryConfigRespDTO;
import com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryInventoryImportReqDTO;
import com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryInventoryItemDeleteReqDTO;
import com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryInventoryItemRespDTO;
import com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryInventoryItemUpdateReqDTO;
import com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryInventorySummaryRespDTO;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsAutoDeliveryConfig;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsAutoDeliveryConfigMapper;
import com.feijimiao.xianyuassistant.service.AutoDeliveryConfigService;
import com.feijimiao.xianyuassistant.service.AutoDeliveryInventoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AutoDeliveryConfigServiceImpl implements AutoDeliveryConfigService {

    @Autowired
    private XianyuGoodsAutoDeliveryConfigMapper autoDeliveryConfigMapper;

    @Autowired
    private AutoDeliveryInventoryService autoDeliveryInventoryService;

    @Override
    public ResultObject<AutoDeliveryConfigRespDTO> saveOrUpdateConfig(AutoDeliveryConfigReqDTO reqDTO) {
        try {
            XianyuGoodsAutoDeliveryConfig existingConfig = autoDeliveryConfigMapper
                    .findByAccountIdAndGoodsId(reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId());

            XianyuGoodsAutoDeliveryConfig config;
            if (existingConfig != null) {
                config = existingConfig;
            } else {
                config = new XianyuGoodsAutoDeliveryConfig();
                config.setXianyuAccountId(reqDTO.getXianyuAccountId());
                config.setXyGoodsId(reqDTO.getXyGoodsId());
            }

            config.setXianyuGoodsId(reqDTO.getXianyuGoodsId());
            config.setType(reqDTO.getType());
            config.setAutoDeliveryContent(reqDTO.getAutoDeliveryContent());
            config.setAutoConfirmShipment(reqDTO.getAutoConfirmShipment());

            if (existingConfig != null) {
                autoDeliveryConfigMapper.updateById(config);
                log.info("更新自动发货配置成功: id={}", config.getId());
            } else {
                autoDeliveryConfigMapper.insert(config);
                log.info("创建自动发货配置成功: id={}", config.getId());
            }

            return ResultObject.success(toRespDTO(config));
        } catch (Exception e) {
            log.error("保存自动发货配置失败", e);
            return ResultObject.failed("保存自动发货配置失败: " + e.getMessage());
        }
    }

    @Override
    public ResultObject<AutoDeliveryConfigRespDTO> getConfig(AutoDeliveryConfigQueryReqDTO reqDTO) {
        try {
            XianyuGoodsAutoDeliveryConfig config;
            if (reqDTO.getXyGoodsId() != null && !reqDTO.getXyGoodsId().trim().isEmpty()) {
                config = autoDeliveryConfigMapper.findByAccountIdAndGoodsId(reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId());
            } else {
                List<XianyuGoodsAutoDeliveryConfig> configs = autoDeliveryConfigMapper.findByAccountId(reqDTO.getXianyuAccountId());
                config = configs.isEmpty() ? null : configs.get(0);
            }

            if (config == null) {
                return ResultObject.success(null);
            }

            return ResultObject.success(toRespDTO(config));
        } catch (Exception e) {
            log.error("查询自动发货配置失败", e);
            return ResultObject.failed("查询自动发货配置失败: " + e.getMessage());
        }
    }

    @Override
    public ResultObject<List<AutoDeliveryConfigRespDTO>> getConfigsByAccountId(Long xianyuAccountId) {
        try {
            List<AutoDeliveryConfigRespDTO> respDTOs = autoDeliveryConfigMapper.findByAccountId(xianyuAccountId)
                    .stream()
                    .map(this::toRespDTO)
                    .collect(Collectors.toList());
            return ResultObject.success(respDTOs);
        } catch (Exception e) {
            log.error("查询账号自动发货配置列表失败", e);
            return ResultObject.failed("查询账号自动发货配置列表失败: " + e.getMessage());
        }
    }

    @Override
    public ResultObject<AutoDeliveryInventorySummaryRespDTO> importInventory(AutoDeliveryInventoryImportReqDTO reqDTO) {
        try {
            return ResultObject.success(autoDeliveryInventoryService.importInventory(reqDTO));
        } catch (Exception e) {
            log.error("导入自动发货库存失败", e);
            return ResultObject.failed("导入自动发货库存失败: " + e.getMessage());
        }
    }

    @Override
    public ResultObject<AutoDeliveryInventorySummaryRespDTO> getInventorySummary(AutoDeliveryConfigQueryReqDTO reqDTO) {
        try {
            if (reqDTO.getXyGoodsId() == null || reqDTO.getXyGoodsId().trim().isEmpty()) {
                return ResultObject.failed("闲鱼商品ID不能为空");
            }
            return ResultObject.success(autoDeliveryInventoryService.getInventorySummary(reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId()));
        } catch (Exception e) {
            log.error("查询自动发货库存汇总失败", e);
            return ResultObject.failed("查询自动发货库存汇总失败: " + e.getMessage());
        }
    }

    @Override
    public ResultObject<List<AutoDeliveryInventoryItemRespDTO>> getInventoryItems(AutoDeliveryConfigQueryReqDTO reqDTO) {
        try {
            if (reqDTO.getXyGoodsId() == null || reqDTO.getXyGoodsId().trim().isEmpty()) {
                return ResultObject.failed("闲鱼商品ID不能为空");
            }
            return ResultObject.success(autoDeliveryInventoryService.getInventoryItems(reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId()));
        } catch (Exception e) {
            log.error("查询自动发货库存明细失败", e);
            return ResultObject.failed("查询自动发货库存明细失败: " + e.getMessage());
        }
    }

    @Override
    public ResultObject<AutoDeliveryInventoryItemRespDTO> updateInventoryItem(AutoDeliveryInventoryItemUpdateReqDTO reqDTO) {
        try {
            AutoDeliveryInventoryItemRespDTO item = autoDeliveryInventoryService.updatePendingItem(
                    reqDTO.getXianyuAccountId(),
                    reqDTO.getXyGoodsId(),
                    reqDTO.getItemId(),
                    reqDTO.getDeliveryContent()
            );
            return ResultObject.success(item);
        } catch (Exception e) {
            log.error("修改自动发货库存失败", e);
            return ResultObject.failed("修改自动发货库存失败: " + e.getMessage());
        }
    }

    @Override
    public ResultObject<Void> deleteInventoryItem(AutoDeliveryInventoryItemDeleteReqDTO reqDTO) {
        try {
            autoDeliveryInventoryService.deletePendingItem(reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId(), reqDTO.getItemId());
            return ResultObject.success(null);
        } catch (Exception e) {
            log.error("删除自动发货库存失败", e);
            return ResultObject.failed("删除自动发货库存失败: " + e.getMessage());
        }
    }

    @Override
    public ResultObject<Void> deleteConfig(Long xianyuAccountId, String xyGoodsId) {
        try {
            LambdaQueryWrapper<XianyuGoodsAutoDeliveryConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(XianyuGoodsAutoDeliveryConfig::getXianyuAccountId, xianyuAccountId)
                    .eq(XianyuGoodsAutoDeliveryConfig::getXyGoodsId, xyGoodsId);

            int deletedCount = autoDeliveryConfigMapper.delete(wrapper);
            if (deletedCount > 0) {
                return ResultObject.success(null);
            }
            return ResultObject.failed("未找到对应的自动发货配置");
        } catch (Exception e) {
            log.error("删除自动发货配置失败", e);
            return ResultObject.failed("删除自动发货配置失败: " + e.getMessage());
        }
    }

    private AutoDeliveryConfigRespDTO toRespDTO(XianyuGoodsAutoDeliveryConfig config) {
        AutoDeliveryConfigRespDTO respDTO = new AutoDeliveryConfigRespDTO();
        BeanUtils.copyProperties(config, respDTO);

        AutoDeliveryInventorySummaryRespDTO summary = autoDeliveryInventoryService
                .getInventorySummary(config.getXianyuAccountId(), config.getXyGoodsId());
        respDTO.setTotalItemCount(summary.getTotalCount());
        respDTO.setPendingItemCount(summary.getPendingCount());
        respDTO.setUsedItemCount(summary.getUsedCount());
        respDTO.setReservedItemCount(summary.getReservedCount());
        return respDTO;
    }
}
