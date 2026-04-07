package com.feijimiao.xianyuassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.feijimiao.xianyuassistant.entity.XianyuOperationLog;
import com.feijimiao.xianyuassistant.mapper.XianyuOperationLogMapper;
import com.feijimiao.xianyuassistant.service.OperationLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 操作记录服务实现
 */
@Slf4j
@Service
public class OperationLogServiceImpl implements OperationLogService {
    
    @Autowired
    private XianyuOperationLogMapper operationLogMapper;
    
    @Async
    @Override
    public void log(XianyuOperationLog operationLog) {
        try {
            if (operationLog.getCreateTime() == null) {
                operationLog.setCreateTime(System.currentTimeMillis());
            }
            operationLogMapper.insert(operationLog);
        } catch (Exception e) {
            log.error("记录操作日志失败", e);
        }
    }
    
    @Async
    @Override
    public void log(Long accountId, String operationType, String operationDesc, Integer status) {
        XianyuOperationLog operationLog = new XianyuOperationLog();
        operationLog.setXianyuAccountId(accountId);
        operationLog.setOperationType(operationType);
        operationLog.setOperationDesc(operationDesc);
        operationLog.setOperationStatus(status);
        operationLog.setCreateTime(System.currentTimeMillis());
        log(operationLog);
    }
    
    @Async
    @Override
    public void log(Long accountId, String operationType, String operationModule,
                    String operationDesc, Integer status, String targetType, String targetId,
                    String requestParams, String responseResult, String errorMessage, Integer durationMs) {
        XianyuOperationLog operationLog = new XianyuOperationLog();
        operationLog.setXianyuAccountId(accountId);
        operationLog.setOperationType(operationType);
        operationLog.setOperationModule(operationModule);
        operationLog.setOperationDesc(operationDesc);
        operationLog.setOperationStatus(status);
        operationLog.setTargetType(targetType);
        operationLog.setTargetId(targetId);
        operationLog.setRequestParams(requestParams);
        operationLog.setResponseResult(responseResult);
        operationLog.setErrorMessage(errorMessage);
        operationLog.setDurationMs(durationMs);
        operationLog.setCreateTime(System.currentTimeMillis());
        log(operationLog);
    }
    
    @Override
    public Map<String, Object> queryLogs(Long accountId, String operationType, String operationModule,
                                         Integer operationStatus, Integer page, Integer pageSize) {
        try {
            // 计算偏移量
            int offset = (page - 1) * pageSize;
            
            // 查询数据
            List<XianyuOperationLog> logs = operationLogMapper.selectByPage(
                    accountId, operationType, operationModule, operationStatus, pageSize, offset);
            
            // 查询总数
            Integer total = operationLogMapper.countByCondition(
                    accountId, operationType, operationModule, operationStatus);
            
            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("logs", logs);
            result.put("total", total);
            result.put("page", page);
            result.put("pageSize", pageSize);
            result.put("totalPages", (int) Math.ceil((double) total / pageSize));
            
            return result;
            
        } catch (Exception e) {
            log.error("查询操作记录失败", e);
            return new HashMap<>();
        }
    }
    
    @Override
    public int deleteOldLogs(int days) {
        try {
            long cutoffTime = System.currentTimeMillis() - (days * 24L * 60 * 60 * 1000);
            int deleted = operationLogMapper.delete(
                    new LambdaQueryWrapper<XianyuOperationLog>()
                            .lt(XianyuOperationLog::getCreateTime, cutoffTime)
            );
            log.info("删除{}天前的操作记录，共删除{}条", days, deleted);
            return deleted;
        } catch (Exception e) {
            log.error("删除旧操作记录失败", e);
            return 0;
        }
    }
}
