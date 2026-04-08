package com.feijimiao.xianyuassistant.service;

import com.feijimiao.xianyuassistant.common.ResultObject;
import com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryConfigQueryReqDTO;
import com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryConfigReqDTO;
import com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryConfigRespDTO;
import com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryInventoryImportReqDTO;
import com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryInventorySummaryRespDTO;

import java.util.List;

/**
 * 自动发货配置服务接口
 */
public interface AutoDeliveryConfigService {

    ResultObject<AutoDeliveryConfigRespDTO> saveOrUpdateConfig(AutoDeliveryConfigReqDTO reqDTO);

    ResultObject<AutoDeliveryConfigRespDTO> getConfig(AutoDeliveryConfigQueryReqDTO reqDTO);

    ResultObject<List<AutoDeliveryConfigRespDTO>> getConfigsByAccountId(Long xianyuAccountId);

    ResultObject<AutoDeliveryInventorySummaryRespDTO> importInventory(AutoDeliveryInventoryImportReqDTO reqDTO);

    ResultObject<AutoDeliveryInventorySummaryRespDTO> getInventorySummary(AutoDeliveryConfigQueryReqDTO reqDTO);

    ResultObject<Void> deleteConfig(Long xianyuAccountId, String xyGoodsId);
}
