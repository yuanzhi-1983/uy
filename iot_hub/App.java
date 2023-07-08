package ece448.iot_hub;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ece448.grading.GradeP3.MqttController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@SpringBootApplication
public class App implements DisposableBean{
	private static final Logger logger=LoggerFactory.getLogger(App.class);
	private ObjectMapper mapper = new ObjectMapper();
	private MqttController mqttController;
	private Map<String, Set<String>> groups =new HashMap<String, Set<String>>();

	private String getPlugJsonText(String plugName){
		return String.format("{\"name\":\"%s\",\"state\":\"%s\",\"power\":%s}",plugName,mqttController.getState(plugName),mqttController.getPower(plugName));
	}


	@Autowired
	public App(Environment env) throws Exception {
		logger.info("[App]: Initiating mqttController");
		mqttController=new MqttController(env.getProperty("mqtt.broker"), env.getProperty("mqtt.clientId"), env.getProperty("mqtt.topicPrefix"));
		mqttController.start();
		mqttController.publishAction("$", "get");
	}

	@GetMapping("/api/plugs")
	public String getPlugStates1(){
		logger.info("[App]: Received request for plug list");
		StringBuffer buffer=new StringBuffer();
		for(Map.Entry<String,String> entry:mqttController.getStates().entrySet()){
			buffer.append(String.format(",%s", String.format("{\"name\":\"%s\",\"state\":\"%s\",\"power\":%s}",entry.getKey(),mqttController.getState(entry.getKey()),mqttController.getPower(entry.getKey()))));
		}
		return String.format("[%s]", buffer.substring(1));
	}
	@GetMapping("/api/plugs/{name}")
	public String getPlugState(@PathVariable("name")String plugName,@RequestParam(value = "action", required = false) String action){
		logger.info("[App]: Received request for plug: "+ plugName + ((action!=null&&!action.equals(""))?String.format(",action: %s", action):"."));
		plugName=plugName.replace("_", ".");
		if(action!=null&&!action.equals("")){
			mqttController.publishAction(plugName, action);
		}
		return getPlugJsonText(plugName);
	}

	/////////////////////////////////////////////////////////////////
	////////////////////////////p5 p5 p5 p5 p5 p5
	//////////////////////////////////////////////////////////

	// @GetMapping("/api/groups")
	// public String getGroupStates(){
	// 	logger.info("[App]: Received request for group list");
	// 	List<String> messages =new ArrayList<String>();
	// 	for(Map.Entry<String,Set<String>> entry:groups.entrySet()){
	// 		messages.add(getGroupJsonTest(entry.getKey()));
	// 	}
	// 	String message =String.format("[%s]", String.join(",", messages));
	// 	logger.info("[App]: "+message);
	// 	return message;
	// }

	// @GetMapping("/api/groups/{name}")
	// public String getGroupState(@PathVariable("name")String groupName,@RequestParam(value = "action", required = false) String action){
	// 	logger.info("[App]: Received request for group "+groupName);
	// 	if (action != null && !action.equals("")){
	// 		for(String plugName : groups.get(groupName)){
	// 			mqttController.publishAction(plugName, action);
	// 		}
	// 	}
	// 	String message = getGroupJsonTest(groupName);
	// 	logger.info("[App]: "+message);
	// 	return message;
	// }

	// @PostMapping("/api/groups/{name}")
	// public String addGroupMember(@PathVariable("name")String groupName, @RequestBody String body) throws Exception{
	// 	logger.info("[App]: Receiving add group request. Group: "+groupName+" Body: "+body);
	// 	List<String> plugs = mapper.readValue(body, new TypeReference<List<String>>(){});
	// 	logger.info(String.format("[App]: Adding {%s} to group %s", String.join(", ", plugs), groupName));
	// 	if(!groups.containsKey(groupName)){
	// 		groups.put(groupName, new TreeSet<String>());
	// 	}
	// 	groups.get(groupName).clear();
	// 	groups.get(groupName).addAll(plugs);
	// 	logger.info(String.format("[App]: %s", getGroupJsonTest(groupName)));
	// 	return "";
	// }

	// @DeleteMapping("/api/groups/{name}")
	// public String deleteGroup(@PathVariable("name")String groupName){
	// 	logger.info("[App]: Recceiving delete group request. Group: "+groupName);
	// 	if(groups.containsKey(groupName)){
	// 		groups.get(groupName).clear();
	// 		groups.remove(groupName);
	// 	}
	// 	return "";
	// }

	
	// private String getGroupJsonTest(String groupName){
	// 	String memberText="";
	// 	if(groups.containsKey(groupName)){
	// 		List<String> members = new ArrayList<String>();
	// 		for (String member:groups.get(groupName)){
	// 			members.add(getPlugJsonText(member));
	// 		}
	// 		if(members.size()>0){
	// 			memberText = String.join(",", members);
	// 		}
	// 	}
	// 	return String.format("{\"name\":\"%s\",\"members\":[%s]}", groupName, memberText);
	// }

	////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public void destroy() throws Exception {
		try {
			mqttController.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
