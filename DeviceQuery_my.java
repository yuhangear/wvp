package com.genersoft.iot.vmp.vmanager.gb28181.device;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.InviteInfo;
import com.genersoft.iot.vmp.common.InviteSessionStatus;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.StreamInfo2;
import com.genersoft.iot.vmp.common.StreamURL;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.MediaConfig;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.exception.ServiceException;
import com.genersoft.iot.vmp.conf.exception.SsrcTransactionNotFoundException;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.RecordInfo;
import com.genersoft.iot.vmp.gb28181.bean.SyncStatus;
import com.genersoft.iot.vmp.gb28181.task.ISubscribeTask;
import com.genersoft.iot.vmp.gb28181.task.impl.CatalogSubscribeTask;
import com.genersoft.iot.vmp.gb28181.task.impl.MobilePositionSubscribeTask;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.IDeviceChannelService;
import com.genersoft.iot.vmp.service.IDeviceService;
import com.genersoft.iot.vmp.service.IInviteStreamService;
import com.genersoft.iot.vmp.service.IPlayService;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.BaseTree;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.StreamContent;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import com.genersoft.iot.vmp.vmanager.gb28181.record.GBRecordController;
import com.genersoft.iot.vmp.web.gb28181.dto.DeviceChannelExtend;
import com.github.pagehelper.PageInfo;
import com.mysql.cj.xdevapi.JsonArray;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.ibatis.annotations.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import javax.servlet.http.HttpServletResponse;
import javax.sip.InvalidArgumentException;
import javax.sip.SipException;

import static org.junit.jupiter.api.DynamicTest.stream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.springframework.boot.context.properties.ConfigurationProperties;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;


import javax.servlet.http.HttpServletRequest;

@Controller
@ResponseBody
public class DeviceQuery_my {
	
	private final static Logger logger = LoggerFactory.getLogger(GBRecordController.class);
	@Autowired
	private IPlayService playService;

	@Autowired
	private SIPCommander cmder;

	@Autowired
	private IVideoManagerStorage storager;

	@Autowired
    private MediaConfig mediaConfig;

	@Autowired
	private UserSetting userSetting;

	@Autowired
	private DeferredResultHolder resultHolder;


	@Autowired
	private IInviteStreamService inviteStreamService;



	@Operation(summary = "获取所有设备信息")
	@ResponseBody
	@GetMapping("/api/device/query/all_devices")



	public List<JSONObject> all_devices(){


		
		List<JSONObject> dev_list = new ArrayList<>();

		List<Device> devices=storager.queryVideoDeviceList(1, Integer.MAX_VALUE,null).getList();
		for (int i =0;i<devices.size();i++){
			String deviceId=devices.get(i).getDeviceId();
			String name_ = devices.get(i).getName();
			DeviceChannel target_dev = storager.queryChannelsByDeviceId(deviceId, null, null, null, null, 1, Integer.MAX_VALUE).getList().get(0);
			String StreamId = target_dev.getChannelId();

			JSONObject jsonObject = new JSONObject();

			jsonObject.put("name",name_);
			jsonObject.put("channelId",StreamId);
			jsonObject.put("deviceId",deviceId);
			jsonObject.put("type","枪机");
			jsonObject.put("status","在线");
			jsonObject.put("alarmtime",null);
			jsonObject.put("location","446实验室");

			
			dev_list.add( jsonObject);
		

		}


			
	
		return dev_list;


	}





	@Operation(summary = "get_video_url")
	@ResponseBody
	@GetMapping("/api/device/query/get_video_url")
	@Parameter(name = "deviceId", description = "设备id", required = true)
	@Parameter(name = "channelId", description = "通道id", required = true)

	public JSONObject get_video_url(String deviceId, String channelId) {


	


		

		String ip_add = mediaConfig.getIp();
		String port = String.valueOf(mediaConfig.getHttpPort());
		String port_ssl = String.valueOf(mediaConfig.getHttpSSlPort());
			
		///rtp/3402000000320000003_34020000001320000001.live.flv

		//ws://123.57.67.33:50306/rtp/3402000000320000003_34020000001320000001.live.flv
		String add_http = ":"+port+"/rtp/" + deviceId+"_"+channelId +".live.flv";
		// String add_https = "wss://"+ip_add+":"+port_ssl+"/rtp/" + deviceId+"_"+channelId +".live.flv";


		JSONObject jsonObject = new JSONObject();
		jsonObject.put("ws",add_http);

		return jsonObject;

			


	}


	@Operation(summary = "停止点播")
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "isSubStream", description = "是否子码流（true-子码流，false-主码流），默认为false", required = true)
	@GetMapping("/api/device/live_stop")
	public JSONObject playStop( String deviceId,  String channelId,boolean isSubStream) {

		logger.debug(String.format("设备预览/回放停止API调用，streamId：%s_%s", deviceId, channelId ));

		if (deviceId == null || channelId == null) {
			throw new ControllerException(ErrorCode.ERROR400);
		}

		Device device = storager.queryVideoDevice(deviceId);
		if (device == null) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "设备[" + deviceId + "]不存在");
		}

		InviteInfo inviteInfo = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, deviceId, channelId);
		if (inviteInfo == null) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "点播未找到");
		}
		if (InviteSessionStatus.ok == inviteInfo.getStatus()) {
			try {
				logger.warn("[停止点播] {}/{}", device.getDeviceId(), channelId);
				cmder.streamByeCmd(device, channelId, inviteInfo.getStream(), null, null);
			} catch (InvalidArgumentException | SipException | ParseException | SsrcTransactionNotFoundException e) {
				logger.error("[命令发送失败] 停止点播， 发送BYE: {}", e.getMessage());
				throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送失败: " + e.getMessage());
			}
		}
		inviteStreamService.removeInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, deviceId, channelId);
		storager.stopPlay(deviceId, channelId);

		JSONObject json = new JSONObject();
		json.put("deviceId", deviceId);
		json.put("channelId", channelId);
		json.put("isSubStream", isSubStream);
		return json;
	}

	@Operation(summary = "开始点播")
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "isSubStream", description = "是否子码流（true-子码流，false-主码流），默认为false", required = true)
	@GetMapping("/api/device/live_start")
	public DeferredResult<WVPResult<StreamContent>>playstart( HttpServletRequest request,String deviceId,  String channelId,boolean isSubStream) {

			// 获取可用的zlm
		Device device = storager.queryVideoDevice(deviceId);
		MediaServerItem newMediaServerItem = playService.getNewMediaServerItem(device);

		RequestMessage requestMessage = new RequestMessage();
		String key = DeferredResultHolder.CALLBACK_CMD_PLAY + deviceId + channelId;
		requestMessage.setKey(key);
		String uuid = UUID.randomUUID().toString();
		requestMessage.setId(uuid);
		DeferredResult<WVPResult<StreamContent>> result = new DeferredResult<>(userSetting.getPlayTimeout().longValue());

		result.onTimeout(()->{
			logger.info("点播接口等待超时");
			// 释放rtpserver
			WVPResult<StreamInfo> wvpResult = new WVPResult<>();
			wvpResult.setCode(ErrorCode.ERROR100.getCode());
			wvpResult.setMsg("点播超时");
			requestMessage.setData(wvpResult);
			resultHolder.invokeResult(requestMessage);
		});

		// 录像查询以channelId作为deviceId查询
		resultHolder.put(key, uuid, result);

		playService.play(newMediaServerItem, deviceId, channelId, null, (code, msg, data) -> {
			WVPResult<StreamContent> wvpResult = new WVPResult<>();
			if (code == InviteErrorCode.SUCCESS.getCode()) {
				wvpResult.setCode(ErrorCode.SUCCESS.getCode());
				wvpResult.setMsg(ErrorCode.SUCCESS.getMsg());

				if (data != null) {
					StreamInfo streamInfo = (StreamInfo)data;
					if (userSetting.getUseSourceIpAsStreamIp()) {
						streamInfo.channgeStreamIp(request.getLocalAddr());
					}
					wvpResult.setData(new StreamContent(streamInfo));
				}
			}else {
				wvpResult.setCode(code);
				wvpResult.setMsg(msg);
			}
			requestMessage.setData(wvpResult);
			resultHolder.invokeResult(requestMessage);
		});
		return result;
	}



	@Operation(summary = "录像记录")
	@ResponseBody
	@GetMapping("/api/device/query/history")
	@Parameter(name = "deviceId", description = "设备id", required = true)
	@Parameter(name = "start_time", description = "start_time", required = true)
	@Parameter(name = "end_time", description = "end_time", required = true)

	public List<JSONObject> get_video_history(String deviceId, String start_time , String end_time) throws JsonProcessingException, ParseException {

	// public DeferredResult<WVPResult<RecordInfo>> get_video_history(String deviceId, String start_date , String end_date) {


	//http://192.168.31.228:50303/api/playback/start/3402000000320000003/34020000001320000001?startTime=2023-07-30%2002:23:35&endTime=2023-07-30%2002:23:39
	// var xhr = new XMLHttpRequest(); 
	// xhr.open('GET', '/api/device/query/history?deviceId=3402000000320000003&start_date=2023-07-30%2002:23:35&end_date=2023-07-30%2012:23:39'); 
	// xhr.onload = function() { 
	//   if (xhr.status === 200) { 
	//    var data = JSON.parse(xhr.responseText); 
	//   // var data = xhr.responseText
	//   console.log(data);
	
	//   } 
	// }; 
	// xhr.send(); 
	
		DeviceChannel target_dev = storager.queryChannelsByDeviceId(deviceId, null, null, null, null, 1, Integer.MAX_VALUE).getList().get(0);
		//deviceId
		String channelId = target_dev.getChannelId();
		String startTime_1 = start_time;
		String endTime_1= end_time;


		String startTime=start_time+":00" ;
		String endTime=end_time+":00" ;



		// Date date = new Date();
		// String startTime=startTime_2.format(date);
		// String endTime=endTime_2.format(date);
		
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("录像信息查询 API调用，deviceId：%s ，startTime：%s， endTime：%s",deviceId, startTime, endTime));
		}
		DeferredResult<WVPResult<RecordInfo>> result = new DeferredResult<>();
		if (!DateUtil.verification(startTime, DateUtil.formatter)){
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "startTime格式为" + DateUtil.PATTERN);
		}
		if (!DateUtil.verification(endTime, DateUtil.formatter)){
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "endTime格式为" + DateUtil.PATTERN);
		}

		Device device = storager.queryVideoDevice(deviceId);
		// 指定超时时间 1分钟30秒
		String uuid = UUID.randomUUID().toString();
		int sn  =  (int)((Math.random()*9+1)*100000);
		String key = DeferredResultHolder.CALLBACK_CMD_RECORDINFO + deviceId + sn;
		RequestMessage msg = new RequestMessage();
		msg.setId(uuid);
		msg.setKey(key);
		try {
			cmder.recordInfoQuery(device, channelId, startTime, endTime, sn, null, null, null, (eventResult -> {
				WVPResult<RecordInfo> wvpResult = new WVPResult<>();
				wvpResult.setCode(ErrorCode.ERROR100.getCode());
				wvpResult.setMsg("查询录像失败, status: " +  eventResult.statusCode + ", message: " + eventResult.msg);
				msg.setData(wvpResult);
				resultHolder.invokeResult(msg);
			}));
		} catch (InvalidArgumentException | SipException | ParseException e) {
			logger.error("[命令发送失败] 查询录像: {}", e.getMessage());
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送失败: " +  e.getMessage());
		}

		// 录像查询以channelId作为deviceId查询
		resultHolder.put(key, uuid, result);


		result.onTimeout(()->{
			msg.setData("timeout");
			WVPResult<RecordInfo> wvpResult = new WVPResult<>();
			wvpResult.setCode(ErrorCode.ERROR100.getCode());
			wvpResult.setMsg("timeout");
			msg.setData(wvpResult);
			resultHolder.invokeResult(msg);
		});





		List<JSONObject> dev_list = new ArrayList<>();



		Object temp = result.getResult();



		// 创建 ObjectMapper 对象
		ObjectMapper objectMapper = new ObjectMapper();
		// 将 DeferredResult 对象转换为 JSON 字符串
		while(result.getResult() == null) {
			// 等待结果...
		}
		String jsonString = objectMapper.writeValueAsString(result.getResult());
		JSONObject jsonObject = JSONObject.parseObject(jsonString);
		
		String recordListStr = jsonObject.getString("recordList");
		JSONArray recordList = JSON.parseArray(recordListStr);


		for (int i = 0; i < recordList.size(); i++) {
			JSONObject record = recordList.getJSONObject(i);
			// String deviceId = record.getString("deviceId");
			String name = record.getString("name");
			String filePath = record.getString("filePath");
			String fileSize = record.getString("fileSize");
			long bytes = Long.parseLong(fileSize);
			double megabytes = (double) bytes / (1024 * 1024);
			String fileSizeInMB = String.format("%.2f MB", megabytes);

			String address = record.getString("address");
			// String startTime = record.getString("startTime");
			// String endTime = record.getString("endTime");
			int secrecy = record.getIntValue("secrecy");
			String type = record.getString("type");

			String name_ = device.getName();
			JSONObject jsonObject1 = new JSONObject();
			jsonObject1.put("name",name_);
			jsonObject1.put("channelId",channelId);
			jsonObject1.put("deviceId",deviceId);
			jsonObject1.put("start_time",record.getString("startTime"));
			jsonObject1.put("end_time",record.getString("endTime"));
			jsonObject1.put("fileSize",fileSizeInMB);
			dev_list.add(jsonObject1);
			// 在这里对获取到的值进行处理
			// ...
		}


		return dev_list;

		

	}







	@Parameter(name = "deviceId", description = "设备id", required = true)
	@Parameter(name = "channelId", description = "通道id", required = true)
	@Parameter(name = "start_time", description = "start_time", required = true)
	@Parameter(name = "end_time", description = "end_time", required = true)

	@ResponseBody
	@GetMapping("/api/device/query/history_url")
	public  DeferredResult<WVPResult<StreamContent>>  history_url2(HttpServletRequest request,  String deviceId,  String channelId,
														  String start_time, String end_time) throws JsonProcessingException {
	// public String history_url2(HttpServletRequest request,  String deviceId,  String channelId,
	// 													  String start_time, String end_time) {
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("设备回放 API调用，deviceId：%s ，channelId：%s", deviceId, channelId));
		}
		String port = String.valueOf(mediaConfig.getHttpPort());
		String uuid = UUID.randomUUID().toString();
		String key = DeferredResultHolder.CALLBACK_CMD_PLAYBACK + deviceId + channelId;
		DeferredResult<WVPResult<StreamContent>> result = new DeferredResult<>(userSetting.getPlayTimeout().longValue());
		resultHolder.put(key, uuid, result);

		RequestMessage requestMessage = new RequestMessage();
		requestMessage.setKey(key);
		requestMessage.setId(uuid);

		playService.playBack(deviceId, channelId, start_time, end_time,
				(code, msg, data)->{

					WVPResult<StreamContent> wvpResult = new WVPResult<>();
					if (code == InviteErrorCode.SUCCESS.getCode()) {
						wvpResult.setCode(ErrorCode.SUCCESS.getCode());
						wvpResult.setMsg(ErrorCode.SUCCESS.getMsg());

						if (data != null) {
							StreamInfo streamInfo = (StreamInfo)data;
							if (userSetting.getUseSourceIpAsStreamIp()) {
								streamInfo.channgeStreamIp(request.getLocalAddr());
							}
							
							// JSONObject jsonObject_ = new JSONObject();
							// jsonObject_.put("ws",);
							String ws_url=streamInfo.getWs_flv().getFile();
							String strean_id=streamInfo.getStream();
							// StreamURL streamURL = new StreamURL(ws_url, msg, code, msg);
							ws_url=":"+port+"/"+ws_url;
							StreamInfo2 streamInfo2 = new StreamInfo2();
							streamInfo2.setWs_flv(ws_url);
							streamInfo2.setStreamId(strean_id);
							wvpResult.setData2(  streamInfo2 );

						}
					}else {
						wvpResult.setCode(code);
						wvpResult.setMsg(msg);
					}
					requestMessage.setData(wvpResult);
					resultHolder.invokeResult(requestMessage);
				});

		// while(result.getResult() == null) {
		// 	// 等待结果...
		// }
		
		// ObjectMapper objectMapper = new ObjectMapper();
		// String jsonString = objectMapper.writeValueAsString(result.getResult());

		// JSONObject jsonObject = JSONObject.parseObject(jsonString);

		// String ws_url=JSONObject.parseObject(jsonObject.getString("data")).getString("ws_flv");


		// JSONObject jsonObject_ = new JSONObject();
		// jsonObject_.put("ws",ws_url);

		return result;



	}









	@Operation(summary = "停止视频回放")
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "streamId", description = "流ID", required = true)
	@ResponseBody
	@GetMapping("/api/device/stop")
	public void playStop(
			String deviceId,
			 String channelId,
			 String streamId) {

		if (ObjectUtils.isEmpty(deviceId) || ObjectUtils.isEmpty(channelId) || ObjectUtils.isEmpty(streamId)) {
			throw new ControllerException(ErrorCode.ERROR400);
		}
		Device device = storager.queryVideoDevice(deviceId);
		if (device == null) {
			throw new ControllerException(ErrorCode.ERROR400.getCode(), "设备：" + deviceId + " 未找到");
		}
		try {
			cmder.streamByeCmd(device, channelId, streamId, null);
		} catch (InvalidArgumentException | ParseException | SipException | SsrcTransactionNotFoundException e) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "发送bye失败： " + e.getMessage());
		}
	}


	@Operation(summary = "回放暂停")
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "streamId", description = "流ID", required = true)
	@ResponseBody
	@GetMapping("/api/device/pause")
	public void playPause_my(String deviceId,
			 String channelId,
			 String streamId) {
		logger.info("playPause: "+streamId);

		try {
			playService.pauseRtp_my(deviceId,channelId,streamId);
		} catch (ServiceException e) {
			throw new ControllerException(ErrorCode.ERROR400.getCode(), e.getMessage());
		} catch (InvalidArgumentException | ParseException | SipException e) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), e.getMessage());
		}
	}


	@Operation(summary = "回放恢复")
	@Parameter(name = "streamId", description = "回放流ID", required = true)
	@ResponseBody
	@GetMapping("/api/device/resume")
	public void playResume_my( String deviceId,
			 String channelId,
			 String streamId) {
		logger.info("playResume: "+streamId);
		try {
			playService.resumeRtp_my(deviceId,channelId,streamId);
		} catch (ServiceException e) {
			throw new ControllerException(ErrorCode.ERROR400.getCode(), e.getMessage());
		} catch (InvalidArgumentException | ParseException | SipException e) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), e.getMessage());
		}
	}

	@Operation(summary = "倍速播放")
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "streamId", description = "流ID", required = true)
	@Parameter(name = "speed", description = "流ID", required = true)
	@ResponseBody
	@GetMapping("/api/device/play_speed")
	public void playspeed_my( String deviceId,
			 String channelId,
			 String streamId,
			 Double speed) {
		logger.info("playR_speed: "+streamId);
		try {
			playService.playspeed_my(deviceId,channelId,streamId,speed);
		} catch (ServiceException e) {
			throw new ControllerException(ErrorCode.ERROR400.getCode(), e.getMessage());
		} catch (InvalidArgumentException | ParseException | SipException e) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), e.getMessage());
		}
	}


	@Operation(summary = "云台控制")
	@ResponseBody
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "command", description = "控制指令,允许值: left, right, up, down, upleft, upright, downleft, downright, zoomin, zoomout, stop", required = true)
	@Parameter(name = "horizonSpeed", description = "水平速度", required = true)
	@Parameter(name = "verticalSpeed", description = "垂直速度", required = true)
	@Parameter(name = "zoomSpeed", description = "缩放速度", required = true)
	@GetMapping("/api/device/ptz_control")
	public void ptz( String deviceId,  String channelId, String command, int horizonSpeed, int verticalSpeed, int zoomSpeed){

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("设备云台控制 API调用，deviceId：%s ，channelId：%s ，command：%s ，horizonSpeed：%d ，verticalSpeed：%d ，zoomSpeed：%d",deviceId, channelId, command, horizonSpeed, verticalSpeed, zoomSpeed));
		}
		Device device = storager.queryVideoDevice(deviceId);
		int cmdCode = 0;
		switch (command){
			case "left":
				cmdCode = 2;
				break;
			case "right":
				cmdCode = 1;
				break;
			case "up":
				cmdCode = 8;
				break;
			case "down":
				cmdCode = 4;
				break;
			case "upleft":
				cmdCode = 10;
				break;
			case "upright":
				cmdCode = 9;
				break;
			case "downleft":
				cmdCode = 6;
				break;
			case "downright":
				cmdCode = 5;
				break;
			case "zoomin":
				cmdCode = 16;
				break;
			case "zoomout":
				cmdCode = 32;
				break;
			case "stop":
				horizonSpeed = 0;
				verticalSpeed = 0;
				zoomSpeed = 0;
				break;
			default:
				break;
		}
		try {
			cmder.frontEndCmd(device, channelId, cmdCode, horizonSpeed, verticalSpeed, zoomSpeed);
		} catch (SipException | InvalidArgumentException | ParseException e) {
			logger.error("[命令发送失败] 云台控制: {}", e.getMessage());
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送失败: " + e.getMessage());
		}
	}


	// @Operation(summary = "history_url")
	// @ResponseBody
	// @GetMapping("/api/device/query/history_url")
	// @Parameter(name = "deviceId", description = "设备id", required = true)
	// @Parameter(name = "channelId", description = "通道id", required = true)
	// @Parameter(name = "start_time", description = "start_time", required = true)
	// @Parameter(name = "end_time", description = "end_time", required = true)
	// public JSONObject get_history_url(HttpServletRequest request,String deviceId, String channelId ,String start_time , String end_time ) throws JsonProcessingException {

	// 	String startTime=start_time;
	// 	String endTime=end_time;

	// 	if (!DateUtil.verification(startTime, DateUtil.formatter)){
	// 		throw new ControllerException(ErrorCode.ERROR100.getCode(), "startTime格式为" + DateUtil.PATTERN);
	// 	}
	// 	if (!DateUtil.verification(endTime, DateUtil.formatter)){
	// 		throw new ControllerException(ErrorCode.ERROR100.getCode(), "endTime格式为" + DateUtil.PATTERN);
	// 	}


	// 	if (logger.isDebugEnabled()) {
	// 		logger.debug(String.format("设备回放 API调用，deviceId：%s ，channelId：%s", deviceId, channelId));
	// 	}

	// 	String uuid = UUID.randomUUID().toString();
	// 	String key = DeferredResultHolder.CALLBACK_CMD_PLAYBACK + deviceId + channelId;
	// 	DeferredResult<WVPResult<StreamContent>> result = new DeferredResult<>(userSetting.getPlayTimeout().longValue());
	// 	resultHolder.put(key, uuid, result);

	// 	RequestMessage requestMessage = new RequestMessage();
	// 	requestMessage.setKey(key);
	// 	requestMessage.setId(uuid);

	// 	playService.playBack(deviceId, channelId, startTime, endTime,
	// 			(code, msg, data)->{

	// 				WVPResult<StreamContent> wvpResult = new WVPResult<>();
	// 				if (code == InviteErrorCode.SUCCESS.getCode()) {
	// 					wvpResult.setCode(ErrorCode.SUCCESS.getCode());
	// 					wvpResult.setMsg(ErrorCode.SUCCESS.getMsg());

	// 					if (data != null) {
	// 						StreamInfo streamInfo = (StreamInfo)data;
	// 						if (userSetting.getUseSourceIpAsStreamIp()) {
	// 							streamInfo.channgeStreamIp(request.getLocalAddr());
	// 						}
	// 						wvpResult.setData(new StreamContent(streamInfo));
	// 					}
	// 				}else {
	// 					wvpResult.setCode(code);
	// 					wvpResult.setMsg(msg);
	// 				}
	// 				requestMessage.setData(wvpResult);
	// 				resultHolder.invokeResult(requestMessage);
	// 			});

		// while(result.getResult() == null) {
		// 	// 等待结果...
		// }


		// ObjectMapper objectMapper = new ObjectMapper();
		// String jsonString = objectMapper.writeValueAsString(result.getResult());
		// JSONObject jsonObject = JSONObject.parseObject(jsonString);
		
	// 	String recordListStr = jsonObject.getString("data");
	// 	JSONArray recordList = JSON.parseArray(recordListStr);


	// 	JSONObject jsonObject_ = new JSONObject();

	// 	String ip_add = mediaConfig.getIp();
	// 	String port = String.valueOf(mediaConfig.getHttpPort());
	// 	String port_ssl = String.valueOf(mediaConfig.getHttpSSlPort());
			
	// 	///rtp/3402000000320000003_34020000001320000001.live.flv

	// 	//ws://123.57.67.33:50306/rtp/3402000000320000003_34020000001320000001.live.flv
	// 	String add_http = ":"+port+"/rtp/" + deviceId+"_"+channelId +".live.flv";
	// 	// String add_https = "wss://"+ip_add+":"+port_ssl+"/rtp/" + deviceId+"_"+channelId +".live.flv";


	// 	jsonObject_.put("ws",add_http);

	// 	return jsonObject_;

			

	// }





}


	// /**
	//  * 分页查询通道数
	//  *
	//  * @param deviceId 设备id
	//  * @param page 当前页
	//  * @param count 每页条数
	//  * @param query 查询内容
	//  * @param online 是否在线  在线 true / 离线 false
	//  * @param channelType 设备 false/子目录 true
	//  * @param catalogUnderDevice 是否直属与设备的目录
	//  * @return 通道列表
	//  */
	// @GetMapping("/devices/{deviceId}/all_channels")
	// @Operation(summary = "分页查询通道")
	// @Parameter(name = "deviceId", description = "设备国标编号", required = true)
	// @Parameter(name = "query", description = "查询内容")
	// @Parameter(name = "online", description = "是否在线")
	// @Parameter(name = "channelType", description = "设备/子目录-> false/true")
	// @Parameter(name = "catalogUnderDevice", description = "是否直属与设备的目录")
	// public String all_channels(@PathVariable String deviceId,
	// 											@RequestParam(required = false) String query,
	// 										   @RequestParam(required = false) Boolean online,
	// 										   @RequestParam(required = false) Boolean channelType,
	// 										   @RequestParam(required = false) Boolean catalogUnderDevice) {
	// 	if (ObjectUtils.isEmpty(query)) {
	// 		query = null;
	// 	}

	// 	DeviceChannel target_dev = storager.queryChannelsByDeviceId(deviceId, null, null, null, null, 1, Integer.MAX_VALUE).getList().get(0);
	// 	String StreamId = target_dev.getChannelId();
	// 	String name_ = target_dev.getName();


	// 	String ip_add = mediaConfig.getIp();
	// 	String port = String.valueOf(mediaConfig.getHttpPort());
	// 	String port_ssl = String.valueOf(mediaConfig.getHttpSSlPort());
			
	// 	///rtp/3402000000320000003_34020000001320000001.live.flv

	// 	//ws://123.57.67.33:50306/rtp/3402000000320000003_34020000001320000001.live.flv
	// 	String add_http = "ws://"+ip_add+":"+port+"/rtp/" + deviceId+"_"+StreamId +".live.flv";
	// 	String add_https = "wss://"+ip_add+":"+port_ssl+"/rtp/" + deviceId+"_"+StreamId +".live.flv";
	// 	List<String> adds=new ArrayList<>();
	// 	adds.add( add_http);
	// 	adds.add( add_https);
	// 	ObjectMapper mapper = new ObjectMapper();
	// 	ObjectNode json = mapper.createObjectNode();
		
	// 	json.put("name",name_);
	// 	json.put("deviceId",deviceId);
		
	// 	json.put("ChannelId",StreamId);
	// 	json.put("ws",add_http);
	// 	json.put("wss",add_https);


	// 	return json.toString();






	// }





	// @Operation(summary = "分页查询国标设备")
	// @GetMapping("/devices_StreamId")
	// @Options()
	// public String devices_StreamId(){


		
	// 	ObjectMapper mapper = new ObjectMapper();
        
	// 	ObjectNode json_all = mapper.createObjectNode();

	// 	List<String> StreamList = new ArrayList<>();
	// 	List<Device> devices=storager.queryVideoDeviceList(1, Integer.MAX_VALUE,null).getList();


	// 	String ip_add = mediaConfig.getIp();
	// 	String port = String.valueOf(mediaConfig.getHttpPort());
	// 	String port_ssl = String.valueOf(mediaConfig.getHttpSSlPort());

	// 	for (int i =0;i<devices.size();i++){
	// 		String name_ = devices.get(i).getName();
	// 		String deviceId=devices.get(i).getDeviceId();
	// 		// storager.queryChannelsByDeviceId(deviceId, null, null, null, null, 1, Integer.MAX_VALUE);

	// 		DeviceChannel target_dev = storager.queryChannelsByDeviceId(deviceId, null, null, null, null, 1, Integer.MAX_VALUE).getList().get(0);
	// 		String StreamId = target_dev.getChannelId();
			


	// 		///rtp/3402000000320000003_34020000001320000001.live.flv

	// 		//ws://123.57.67.33:50306/rtp/3402000000320000003_34020000001320000001.live.flv
	// 		String add_http = "ws://"+ip_add+":"+port+"/rtp/" + deviceId+"_"+StreamId +".live.flv";
	// 		String add_https = "wss://"+ip_add+":"+port_ssl+"/rtp/" + deviceId+"_"+StreamId +".live.flv";
	// 		ObjectNode json = mapper.createObjectNode();
	// 		json.put("name",name_);
	// 		json.put("ChannelId",StreamId);
	// 		json.put("ws",add_http);
	// 		json.put("wss",add_https);

		
	// 		json_all.put( deviceId,json);
		


	// 	}
	// 	return json_all.toString();


	// }



