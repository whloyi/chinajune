package com.ChargePoint.controller.mobile;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.ChargePoint.Utils.TextUtils;
import com.ChargePoint.Utils.TimeFormatUtil;
import com.ChargePoint.bean.ChargeMoneyRecords2;
import com.ChargePoint.bean.MobileUser;
import com.ChargePoint.services.ChargeMoneyRecordsService;
import com.ChargePoint.services.MobileUserService;
import com.ChargePoint.services.TradeService;
import com.tenpay.PrepayIdRequestHandler;
import com.tenpay.util.XMLUtil;

@Controller
@RequestMapping("/mobile/")
public class TradeHandle {
	
	private ChargeMoneyRecords2 chargeMoneyRecord;
	private boolean payResponse;
	
	public boolean isPayResponse() {
		return payResponse;
	}

	public void setPayResponse(boolean payResponse) {
		this.payResponse = payResponse;
	}

	@RequestMapping(value="getPrepayId", method= RequestMethod.GET)
	@ResponseBody//返回json对象
	public Map<String,Object> getPrepayId(@RequestParam("jsonStr")String jsonStr,HttpServletRequest request,HttpServletResponse response){
		Map<String, Object> modelMap = new HashMap<String, Object>();
		String prepayId = "";
		try {
//			jsonStr = URLDecoder.decode(jsonStr,"UTF-8");
			PrepayIdRequestHandler prepayIdRequestHandler = new PrepayIdRequestHandler(request, response);
			prepayId = prepayIdRequestHandler.sendPrepay(jsonStr);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		modelMap.put("prePayId", prepayId);
		return modelMap;
	}
	
	@RequestMapping(value="payCallback", method= RequestMethod.POST)
	public void payCallback(HttpServletRequest request,HttpServletResponse response){
//	Map<String, Object> modelMap = new HashMap<String, Object>();
	String userName = null;
	userName = TextUtils.TOUTF8(request.getParameter("userName"));
	MobileUser user = MobileUserService.getMobileUser(userName);
	int uID = user.getId();
	this.chargeMoneyRecord = new ChargeMoneyRecords2();
	this.chargeMoneyRecord.setTable_name(String.valueOf(uID));
	this.chargeMoneyRecord.setUser_name(userName);
	this.chargeMoneyRecord.setType("微信支付");
	}
	
	@RequestMapping(value="notifyUrl", method= RequestMethod.POST)
	public void notifyUrl(HttpServletRequest request,HttpServletResponse response){
		String total_fee = "";
		String out_trade_no = "";
		String responseXml = null;
		// 获取收到的报文
		Map m=new HashMap();
		String inputContent = null;
		try {
        BufferedReader reader = request.getReader();
         String line = "";
        StringBuffer inputString = new StringBuffer();

        while ((line = reader.readLine()) != null) {
    	        inputString.append(line);
    	        }
        inputContent = inputString.toString();
    	        request.getReader().close();
    	        System.out.println("----接收到的报文---"+inputContent);
			m=XMLUtil.doXMLParse(inputString.toString());
			 for(Object keyValue : m.keySet()){
	    	        System.out.println(keyValue+"="+m.get(keyValue));
	    	    }
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		if(m.get("result_code")!=null){
			if (m.get("result_code").toString().equalsIgnoreCase("SUCCESS")) {
				total_fee = m.get("total_fee").toString();
				out_trade_no = m.get("out_trade_no").toString();
				System.out.println("--------回调成功，本次消费 ： "+total_fee);
				
				//------------------------------
				//处理业务开始
//--------------账号充值
				boolean increaceMoney = TradeService.moneyIn(this.chargeMoneyRecord.getUser_name(), total_fee);
				if(increaceMoney){
//				获取回调金额并记录
				this.chargeMoneyRecord.setPass(Float.parseFloat(total_fee));
				this.chargeMoneyRecord.setTrade_no(out_trade_no);
				this.chargeMoneyRecord.setTime(TimeFormatUtil.getFormattedNow());
				this.setPayResponse(ChargeMoneyRecordsService.addChargeMoneyRecords(chargeMoneyRecord));
				System.out.println("操作结果 : "+this.payResponse);
				}
				//处理数据库逻辑
				//注意交易单不要重复处理
				//注意判断返回金额
				
				//------------------------------
				//处理业务完毕
				//------------------------------
				
					//支付成功，返回微信服务器结果
					responseXml = "<xml>" + "<return_code><![CDATA[SUCCESS]]></return_code>"
					+ "<return_msg><![CDATA[OK]]></return_msg>" + "</xml> ";
					}else{
						responseXml = "<xml>" + "<return_code><![CDATA[FAIL]]></return_code>"
					+ "<return_msg><![CDATA[报文为空]]></return_msg>" + "</xml> ";
					}
//					微信支付回调数据结束,返回微信服务器告诉不再通知

					BufferedOutputStream out = null;
					try {
						out = new BufferedOutputStream(response.getOutputStream());
						out.write(responseXml.getBytes());
						out.flush();
						out.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
//				response.getWriter().write(XMLUtil.setXML("SUCCESS", ""));   //告诉微信服务器，我收到信息了，不要在调用回调action了
//    	        System.out.println("-------------"+XMLUtil.setXML("SUCCESS", ""));
		}
		try {
			inputContent = new String(inputContent.getBytes("GBK"),"UTF-8");
			System.out.println(inputContent);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	@RequestMapping(value="notifyCallBack", method= RequestMethod.POST)
	@ResponseBody//返回json对象
	public Map<String,Object> notifyCallBack(HttpServletRequest request,HttpServletResponse response){
		Map<String, Object> modelMap = new HashMap<String, Object>();
		modelMap.put("success", this.isPayResponse());
		return modelMap;
	}
	
}
