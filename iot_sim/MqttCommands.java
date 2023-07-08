package ece448.iot_sim;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ece448.iot_sim.PlugSim;
import ece448.iot_sim.MqttUpdates;



public class MqttCommands{
    private final Logger logger = LoggerFactory.getLogger(MqttCommands.class);

    private TreeMap<String, PlugSim> plugMap=new TreeMap<>();
    private String BROKER;
    private String ID;
    private String topicPrefix;
    private String postPrefix;
    private MqttClient client;
    private MqttMessage message;

	public MqttCommands(ArrayList<PlugSim> plugs, String mqttBroker, String mqttClientId, String mqttTopicPrefix) throws Exception{
        this.ID=mqttClientId;
        logger.info(String.format("[MqttServer/%s]: Initiating", ID));
        for(PlugSim plug:plugs){
            plugMap.put(plug.getName(), plug);
        }
        this.BROKER=mqttBroker;
        this.topicPrefix=mqttTopicPrefix+"/action";
        this.postPrefix=mqttTopicPrefix+"/update";
        logger.info(String.format("[MqttServer/%s]: Creating mqtt control on: %s", ID, BROKER));
        this.client=new MqttClient(BROKER, ID, new MemoryPersistence());
        this.message=new MqttMessage();
        this.message.setQos(2);
        logger.info(String.format("[MqttServer/%s]: Registering eventbuss", ID));
        MqttUpdates.register(this);
    }
    
	public void start() throws Exception{
        logger.info(String.format("[MqttServer/%s]: Starting", ID));
        MqttConnectOptions options=new MqttConnectOptions();
        options.setCleanSession(true);
        client.connect(options);
        logger.info(String.format("[MqttServer/%s]: subscribing topic: %s", ID, topicPrefix+"/#"));
        client.subscribe(topicPrefix+"/#", 2, this::handleMessage);
        postAllPlugs();
    }
    private void postAllPlugs() throws Exception{
        logger.info(String.format("[MqttServer/%s]: posting plugs", ID));
        for(Map.Entry<String,PlugSim> entry:plugMap.entrySet()){
            postState(entry.getValue().getName(), entry.getValue().isOn());
            postPower(entry.getValue().getName(), entry.getValue().getPower());
        }
    }
	public void close() throws Exception{
        client.disconnect();
        client.close();
    }
    
    synchronized protected void handleMessage(String topic, MqttMessage msg) throws Exception{
        logger.info(String.format("[MqttServer/%s]: Received topic: %s, message: %s", ID, topic, msg.toString()));
        topic=topic.substring(topicPrefix.length()+1);
        String[] params=topic.split("/");
        if(params.length!=2||!checkAction(params[1])){
            //Ignore unexpected message
            logger.error(String.format("[MqttServer/%s]: Unexpected early return", ID));
            return;
        }
        PlugSim plug;
        if(params[0].equals("$")&&params[1].equals("get")){
            postAllPlugs();
            return;
        }else{
            plug=plugMap.get(params[0]);
            if(plug==null){
                //Ignore unknown plug
                logger.warn(String.format("[MqttServer/%s]: Plug %s not connected.", ID, params[0]));
                return;
            }
        }
        switch(params[1]){
            case "on":
                plug.switchOn();
                break;
            case "off":
                plug.switchOff();
                break;
            case "toggle":
                plug.toggle();
                break;
        }
    }
    private static final String[] actions={"on","off","toggle","get"};
    private boolean checkAction(String actionIn){
        boolean contains=false;
        for(String action:actions){
            if(action.equals(actionIn)){
                contains=true;
                break;
            }
        }
        return contains;
    }

    private void postState(String name, boolean isOn) throws Exception{
        String topic=String.format("%s/%s/state", postPrefix, name);
        logger.info(String.format("[MqttServer/%s]: Plug: %s, Topic: %s, Message: %s", ID, name, topic,(isOn?"on":"off")));
        client.publish(topic, (isOn?"on":"off").getBytes(), 2, false);
    }

    private void postPower(String name, double power)throws Exception{
        String topic=String.format("%s/%s/power", postPrefix, name);
        logger.info(String.format("[MqttServer/%s]: Plug: %s, Topic: %s, Message: %.3f", ID, name, topic, power));
        client.publish(topic, String.format("%.3f", power).getBytes(), 2, false);
    }

    // @Override
    public void onEvent(PlugSim plugSim,boolean isState) {
        if(plugMap.containsKey(plugSim.getName())){
            try{
                if(isState){
                    postState(plugSim.getName(), plugSim.isOn());
                }
                else{
                    postPower(plugSim.getName(), plugSim.getPower());
                }
            }catch(Exception e){
                //logger.error(String.format("[MqttServer/%s]: Unknown exception when posting message", ID));
            }
        }
    }
}