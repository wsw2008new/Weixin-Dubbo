package com.cheng.weixin.web.mobile.service;

import com.cheng.weixin.common.utils.ServletUtils;
import com.cheng.weixin.common.utils.SystemUtils;
import com.cheng.weixin.rabbitmq.model.SmsModel;
import com.cheng.weixin.rpc.message.entity.SmsHistory;
import com.cheng.weixin.rpc.message.enums.MsgType;
import com.cheng.weixin.rpc.message.service.RpcSmsService;
import com.cheng.weixin.rpc.rabbitmq.service.RpcRabbitSmsService;
import com.cheng.weixin.rpc.user.service.RpcUserService;
import com.cheng.weixin.web.mobile.exception.BusinessException;
import com.cheng.weixin.web.mobile.param.RegDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Desc: 登录
 * Author: cheng
 * Date: 2016/7/8
 */
@Service("loginService")
public class LoginService {

    @Autowired
    private RpcRabbitSmsService rabbitService;
    @Autowired
    private RpcSmsService smsService;
    @Autowired
    private RpcUserService userService;

    /**
     * 发送验证码
     * @param phone
     */
    public void sendRegMsgCode(String phone) {
        String userIp = SystemUtils.getRemoteAddr(ServletUtils.getRequest());
        int countByDay = smsService.getCountByDay(phone);
        if (countByDay >= 4) {
            throw new BusinessException("当前手机号"+phone+"发送次数太多");
        }
        int countByIp = smsService.getCountByIp(userIp);
        if (countByIp >= 4) {
            throw new BusinessException("当前IP"+userIp+"发送次数太多");
        }

        SmsModel smsModel = new SmsModel();
        smsModel.setUserIp(userIp);
        smsModel.setPhone(phone);
        rabbitService.sendValidate(smsModel);
    }

    /**
     * 验证验证码
     * @param phone
     * @param code
     */
    public boolean checkCode(String phone, String code) {
        SmsHistory smsHistory = smsService.getInfoByPhoneAndType(phone, MsgType.VALIDATE);
        if (code.equals(smsHistory.getValidate())) {
            return true;
        }
        return false;
    }

    /**
     * 保存注册信息
     * @param regDto
     */
    public void saveAccess(RegDto regDto) {
        String userIp = SystemUtils.getRemoteAddr(ServletUtils.getRequest());
        userService.saveAccess(regDto.getPhone(),regDto.getPassword(), regDto.getNickname(),userIp);
    }

}
