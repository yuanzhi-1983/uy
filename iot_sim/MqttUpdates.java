package ece448.iot_sim;

import java.util.ArrayList;
import java.util.List;
import ece448.iot_sim.PlugSim;



import ece448.iot_sim.MqttCommands;

public class MqttUpdates {
    private static List<MqttCommands> subscribers=new ArrayList<>();
    public static void post(PlugSim plugSim,boolean isState){
        for(MqttCommands subscriber:subscribers){
            subscriber.onEvent(plugSim,isState);
        }
    }

	public static void register(MqttCommands mqttServer) {
        subscribers.add(mqttServer);
	}
    
}
