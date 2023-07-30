package com.genersoft.iot.vmp.vmanager.gb28181.device;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.MediaConfig;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.SyncStatus;
import com.genersoft.iot.vmp.gb28181.task.ISubscribeTask;
import com.genersoft.iot.vmp.gb28181.task.impl.CatalogSubscribeTask;
import com.genersoft.iot.vmp.gb28181.task.impl.MobilePositionSubscribeTask;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.service.IDeviceChannelService;
import com.genersoft.iot.vmp.service.IDeviceService;
import com.genersoft.iot.vmp.service.IInviteStreamService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.vmanager.bean.BaseTree;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.ParseException;
import java.util.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.springframework.boot.context.properties.ConfigurationProperties;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;




@Controller
@ResponseBody
public class DeviceQuery_my {
	



	@Autowired
	private IVideoManagerStorage storager;

	@Autowired
    private MediaConfig mediaConfig;



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


	


		JSONObject jsonObject = new JSONObject();

		String ip_add = mediaConfig.getIp();
		String port = String.valueOf(mediaConfig.getHttpPort());
		String port_ssl = String.valueOf(mediaConfig.getHttpSSlPort());
			
		///rtp/3402000000320000003_34020000001320000001.live.flv

		//ws://123.57.67.33:50306/rtp/3402000000320000003_34020000001320000001.live.flv
		String add_http = ":"+port+"/rtp/" + deviceId+"_"+channelId +".live.flv";
		// String add_https = "wss://"+ip_add+":"+port_ssl+"/rtp/" + deviceId+"_"+channelId +".live.flv";



		jsonObject.put("ws",add_http);

		return jsonObject;

			


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



}
