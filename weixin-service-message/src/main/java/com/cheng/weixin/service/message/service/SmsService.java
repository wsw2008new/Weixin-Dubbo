package com.cheng.weixin.service.message.service;

import com.cheng.weixin.common.security.CodecUtil;
import com.cheng.weixin.common.utils.StringUtils;
import com.cheng.weixin.rabbitmq.model.SmsModel;
import com.cheng.weixin.rpc.message.entity.SmsHistory;
import com.cheng.weixin.rpc.message.entity.SmsTemplate;
import com.cheng.weixin.rpc.message.enums.MsgType;
import com.cheng.weixin.rpc.message.service.RpcSmsService;
import com.cheng.weixin.service.message.dao.SmsHistoryDaoMapper;
import com.cheng.weixin.service.message.dao.SmsTemplateDaoMapper;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * Desc: 短信服务
 * Author: 光灿
 * Date: 2016/7/10
 */
@Service("smsService")
public class SmsService implements RpcSmsService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private SmsTemplateDaoMapper smsTemplateDao;
    @Autowired
    private SmsHistoryDaoMapper smsHistoryDao;

    @Override
    public int getCountByDay(String phone) {
        SmsHistory smsHistory = new SmsHistory();
        smsHistory.setPhone(phone);
        DateTime startOfDay = new DateTime().withTimeAtStartOfDay();
        smsHistory.setStartOfDay(new Date(startOfDay.getMillis()));
        return smsHistoryDao.loadOneDayCount(smsHistory);
    }
    @Override
    public int getCountByIp(String ip) {
        SmsHistory smsHistory = new SmsHistory();
        smsHistory.setUserIp(ip);
        return smsHistoryDao.loadCurrentIpCount(smsHistory);
    }
    @Override
    public void sendValidate(SmsModel smsModel) {

        int countByDay = getCountByDay(smsModel.getPhone());
        if (countByDay >= 4) {
            logger.warn("当前手机号"+smsModel.getPhone()+"发送次数太多");
            return;
        }
        int countByIp = getCountByIp(smsModel.getUserIp());
        if (countByIp >= 4) {
            logger.warn("当前IP"+smsModel.getUserIp()+"发送次数太多");
            return;
        }

        SmsTemplate smsTemplate = smsTemplateDao.loadRegTemp();
        String code = CodecUtil.createRandomNum(4);
        String content = StringUtils.replace(StringUtils.replace(smsTemplate.getContent(), "[MSGCODE]", code), "[TIMEOUT]", smsTemplate.getTimeout()+"");

        // 发送短信 开始
        logger.info("开始发送短信===> "+content);

        // 保存短息历史纪录
        SmsHistory history = new SmsHistory();
        history.setPhone(smsModel.getPhone());
        history.setUserIp(smsModel.getUserIp());
        history.setContent(content);
        history.setSender("system");
        history.setTimeout(smsTemplate.getTimeout());
        history.setType(MsgType.VALIDATE);
        history.setValidate(code);
        history.preInsert();
        smsHistoryDao.save(history);
    }

    @Override
    public SmsHistory getInfoByPhoneAndType(String phone, MsgType type) {
        SmsHistory history = new SmsHistory();
        history.setPhone(phone);
        history.setType(MsgType.VALIDATE);
        return smsHistoryDao.loadNewByPhoneAndType(history);
    }

    @Override
    public void sendNotice(String msgData) {
        logger.info("==================> "+msgData);
    }

    @Override
    public void sendActivity(String msgData) {
        logger.info("==================> "+msgData);
    }
}
