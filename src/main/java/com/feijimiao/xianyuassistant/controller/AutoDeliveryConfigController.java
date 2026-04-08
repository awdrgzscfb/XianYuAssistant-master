package com.feijimiao.xianyuassistant.controller;

import com.feijimiao.xianyuassistant.common.ResultObject;
import com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryConfigQueryReqDTO;
import com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryConfigReqDTO;
import com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryConfigRespDTO;
import com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryInventoryImportReqDTO;
import com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryInventoryItemDeleteReqDTO;
import com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryInventoryItemRespDTO;
import com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryInventoryItemUpdateReqDTO;
import com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryInventorySummaryRespDTO;
import com.feijimiao.xianyuassistant.service.AutoDeliveryConfigService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/auto-delivery-config")
@CrossOrigin(origins = "*")
public class AutoDeliveryConfigController {

    @Autowired
    private AutoDeliveryConfigService autoDeliveryConfigService;

    @PostMapping("/save")
    public ResultObject<AutoDeliveryConfigRespDTO> saveOrUpdateConfig(@Valid @RequestBody AutoDeliveryConfigReqDTO reqDTO) {
        try {
            log.info("保存自动发货配置请求: xianyuAccountId={}, xyGoodsId={}, type={}",
                    reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId(), reqDTO.getType());
            return autoDeliveryConfigService.saveOrUpdateConfig(reqDTO);
        } catch (Exception e) {
            log.error("保存自动发货配置失败", e);
            return ResultObject.failed("保存自动发货配置失败: " + e.getMessage());
        }
    }

    @PostMapping("/get")
    public ResultObject<AutoDeliveryConfigRespDTO> getConfig(@Valid @RequestBody AutoDeliveryConfigQueryReqDTO reqDTO) {
        try {
            log.info("查询自动发货配置请求: xianyuAccountId={}, xyGoodsId={}",
                    reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId());
            return autoDeliveryConfigService.getConfig(reqDTO);
        } catch (Exception e) {
            log.error("查询自动发货配置失败", e);
            return ResultObject.failed("查询自动发货配置失败: " + e.getMessage());
        }
    }

    @PostMapping("/inventory/import")
    public ResultObject<AutoDeliveryInventorySummaryRespDTO> importInventory(@Valid @RequestBody AutoDeliveryInventoryImportReqDTO reqDTO) {
        try {
            log.info("导入自动发货库存请求: xianyuAccountId={}, xyGoodsId={}, replacePendingItems={}",
                    reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId(), reqDTO.getReplacePendingItems());
            return autoDeliveryConfigService.importInventory(reqDTO);
        } catch (Exception e) {
            log.error("导入自动发货库存失败", e);
            return ResultObject.failed("导入自动发货库存失败: " + e.getMessage());
        }
    }

    @PostMapping("/inventory/summary")
    public ResultObject<AutoDeliveryInventorySummaryRespDTO> getInventorySummary(@Valid @RequestBody AutoDeliveryConfigQueryReqDTO reqDTO) {
        try {
            log.info("查询自动发货库存汇总请求: xianyuAccountId={}, xyGoodsId={}",
                    reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId());
            return autoDeliveryConfigService.getInventorySummary(reqDTO);
        } catch (Exception e) {
            log.error("查询自动发货库存汇总失败", e);
            return ResultObject.failed("查询自动发货库存汇总失败: " + e.getMessage());
        }
    }

    @PostMapping("/inventory/items")
    public ResultObject<List<AutoDeliveryInventoryItemRespDTO>> getInventoryItems(@Valid @RequestBody AutoDeliveryConfigQueryReqDTO reqDTO) {
        try {
            log.info("查询自动发货库存明细请求: xianyuAccountId={}, xyGoodsId={}",
                    reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId());
            return autoDeliveryConfigService.getInventoryItems(reqDTO);
        } catch (Exception e) {
            log.error("查询自动发货库存明细失败", e);
            return ResultObject.failed("查询自动发货库存明细失败: " + e.getMessage());
        }
    }

    @PostMapping("/inventory/item/update")
    public ResultObject<AutoDeliveryInventoryItemRespDTO> updateInventoryItem(@Valid @RequestBody AutoDeliveryInventoryItemUpdateReqDTO reqDTO) {
        try {
            log.info("修改自动发货库存请求: xianyuAccountId={}, xyGoodsId={}, itemId={}",
                    reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId(), reqDTO.getItemId());
            return autoDeliveryConfigService.updateInventoryItem(reqDTO);
        } catch (Exception e) {
            log.error("修改自动发货库存失败", e);
            return ResultObject.failed("修改自动发货库存失败: " + e.getMessage());
        }
    }

    @PostMapping("/inventory/item/delete")
    public ResultObject<Void> deleteInventoryItem(@Valid @RequestBody AutoDeliveryInventoryItemDeleteReqDTO reqDTO) {
        try {
            log.info("删除自动发货库存请求: xianyuAccountId={}, xyGoodsId={}, itemId={}",
                    reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId(), reqDTO.getItemId());
            return autoDeliveryConfigService.deleteInventoryItem(reqDTO);
        } catch (Exception e) {
            log.error("删除自动发货库存失败", e);
            return ResultObject.failed("删除自动发货库存失败: " + e.getMessage());
        }
    }

    @PostMapping("/list")
    public ResultObject<List<AutoDeliveryConfigRespDTO>> getConfigsByAccountId(@RequestParam("xianyuAccountId") Long xianyuAccountId) {
        try {
            log.info("查询账号自动发货配置列表请求: xianyuAccountId={}", xianyuAccountId);
            return autoDeliveryConfigService.getConfigsByAccountId(xianyuAccountId);
        } catch (Exception e) {
            log.error("查询账号自动发货配置列表失败", e);
            return ResultObject.failed("查询账号自动发货配置列表失败: " + e.getMessage());
        }
    }

    @PostMapping("/delete")
    public ResultObject<Void> deleteConfig(@RequestParam("xianyuAccountId") Long xianyuAccountId,
                                           @RequestParam("xyGoodsId") String xyGoodsId) {
        try {
            log.info("删除自动发货配置请求: xianyuAccountId={}, xyGoodsId={}", xianyuAccountId, xyGoodsId);
            return autoDeliveryConfigService.deleteConfig(xianyuAccountId, xyGoodsId);
        } catch (Exception e) {
            log.error("删除自动发货配置失败", e);
            return ResultObject.failed("删除自动发货配置失败: " + e.getMessage());
        }
    }
}
