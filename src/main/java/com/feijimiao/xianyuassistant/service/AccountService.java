package com.feijimiao.xianyuassistant.service;

/**
 * 账号服务接口
 */
public interface AccountService {
    
    /**
     * 保存账号和Cookie信息
     *
     * @param accountNote 账号备注
     * @param unb UNB标识
     * @param cookieText Cookie字符串
     * @return 账号ID
     */
    Long saveAccountAndCookie(String accountNote, String unb, String cookieText);
    
    /**
     * 保存账号和Cookie信息（包含m_h5_tk）
     *
     * @param accountNote 账号备注
     * @param unb UNB标识
     * @param cookieText Cookie字符串
     * @param mH5Tk _m_h5_tk token
     * @return 账号ID
     */
    Long saveAccountAndCookie(String accountNote, String unb, String cookieText, String mH5Tk);
    
    /**
     * 根据账号ID获取Cookie
     *
     * @param accountId 账号ID
     * @return Cookie字符串
     */
    String getCookieByAccountId(Long accountId);
    
    /**
     * 根据UNB获取Cookie
     *
     * @param unb UNB标识
     * @return Cookie字符串
     */
    String getCookieByUnb(String unb);
    
    /**
     * 根据账号备注获取Cookie
     *
     * @param accountNote 账号备注
     * @return Cookie字符串
     */
    String getCookieByAccountNote(String accountNote);
    
    /**
     * 更新Cookie
     *
     * @param accountId 账号ID
     * @param cookieText 新的Cookie字符串
     * @return 是否成功
     */
    boolean updateCookie(Long accountId, String cookieText);
    
    /**
     * 根据账号ID获取m_h5_tk
     *
     * @param accountId 账号ID
     * @return m_h5_tk token
     */
    String getMh5tkByAccountId(Long accountId);
    
    /**
     * 根据账号备注获取账号ID
     *
     * @param accountNote 账号备注
     * @return 账号ID
     */
    Long getAccountIdByAccountNote(String accountNote);
    
    /**
     * 根据UNB获取账号ID
     *
     * @param unb UNB标识
     * @return 账号ID
     */
    Long getAccountIdByUnb(String unb);
    
    /**
     * 删除账号及其所有关联数据
     *
     * @param accountId 账号ID
     * @return 是否删除成功
     */
    boolean deleteAccountAndRelatedData(Long accountId);
    
    /**
     * 更新Cookie状态
     *
     * @param accountId 账号ID
     * @param cookieStatus Cookie状态 1:有效 2:过期 3:失效
     * @return 是否更新成功
     */
    boolean updateCookieStatus(Long accountId, Integer cookieStatus);
    
    /**
     * 更新账号Cookie（包含UNB更新）
     *
     * @param accountId 账号ID
     * @param unb UNB标识
     * @param cookieText Cookie字符串
     * @return 是否更新成功
     */
    boolean updateAccountCookie(Long accountId, String unb, String cookieText);
}
