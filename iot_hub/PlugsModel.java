package ece448.iot_hub;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.validation.constraints.Null;
import org.apache.log4j.spi.LoggerFactory;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ch.qos.logback.classic.Logger;

@Component
public class PlugsModel implements AutoCloseable  {
      public static class MqttHubClient {
      private final String broker;
      private final String clientId;
      private final String topicPrefix;
      private final MqttClient client;
      private final HashMap<String, String> states = new HashMap<>();
      private final HashMap<String, String> powers = new HashMap<>();
      public MqttHubClient(String broker, String clientId,String topicPrefix) throws Exception {
            this.broker = broker;
            this.clientId = clientId;
            this.topicPrefix = topicPrefix;
            this.client = new MqttClient(broker, clientId, new MemoryPersistence());
      }

      public void start() throws Exception {
            MqttConnectOptions mqttconnect = new MqttConnectOptions();
            mqttconnect.setCleanSession(true);
            client.connect(mqttconnect);
            client.subscribe(topicPrefix+"update/#", this::handleUpdate);
            System.out.println("MqttCtl {"+clientId+"} {"+broker+"} :connected");
      }

      public void close() throws Exception {
            client.disconnect();
            System.out.println("MqttCtl {"+ clientId +"}: disconnected");
      }

      synchronized public void publishAction(String plugName, String action) {
            String topic = topicPrefix+"action/"+plugName+"/"+action;
            System.out.println(topic+"Publishing");
      try{
            client.publish(topic, new MqttMessage());
            System.out.println("MqttCtl {"+clientId+"}: {"+topic+"} published");
      } catch (Exception e){
            System.out.println(e.getMessage());
            System.out.println("MqttCtl {"+clientId+"}: {"+topic+"} fail to publish");
      }
      }

      synchronized public String getState(String plugName) {
            return states.get(plugName);
      }
      
      synchronized public String getPower(String plugName) {
            return powers.get(plugName);
      }

      synchronized public Map<String, String> getStates() {
            return new TreeMap<>(states);
      }

      synchronized public Map<String, String> getPowers() {
            return new TreeMap<>(powers);
      }

      synchronized protected void handleUpdate(String topic, MqttMessage msg){
            System.out.println("MqttCtl : "+clientId+" "+topic+" "+msg);
            String[] nameUpdate =topic.substring(topicPrefix.length()).split("/");
                  if ((nameUpdate.length != 3) || !nameUpdate[0].equals("update"))
                        return; // ignore unknown format
                  System.out.println(nameUpdate[2]);
                  switch(nameUpdate[2]){
                        case "state":
                              states.put(nameUpdate[1],msg.toString());
                              break;
                        case "power":
                              powers.put(nameUpdate[1],msg.toString());
                              break;
                        default:
                              return;
                  }
            }
      }

      private static final String broker = "tcp://127.0.0.1";
      private static final String topicPrefix = "grade_p4/iot_ece448";
      MqttHubClient mqtt;
      private static boolean exe = true;
      public PlugsModel() throws Exception {
            this.mqtt = new MqttHubClient(broker, "grader/iot_hub",topicPrefix);
            mqtt.start();
      }
      private ArrayList<HashMap<String, Object>> states = new ArrayList<>();
      synchronized public ArrayList<String> getPlugs() throws Exception {
            ArrayList<String> plugsList =mqtt.getStates().keySet().stream().collect(Collectors.toCollection(ArrayList::new));
            if(exe){
                  makeplugstate(plugsList);
                  exe=false;
            }
            return plugsList;
      }
          
      synchronized public void makeplugstate(List<String> plugs){
            for(String member: plugs ){
                  HashMap<String, Object> state = new HashMap<>();
                  state.put("name",member);
                  state.put("state",mqtt.getState(member));
                  states.add(state);
            } 
      }

      synchronized public String plugState(String plugName) { String state = "off";
            for(HashMap<String, Object>  plug: states){
                  if(plug.containsKey(plugName)){
                        state = plug.get(plugName).toString();
                  }
            }
            return state;
      }

      synchronized public ArrayList<HashMap<String, Object>>
      plugStates(List<String> plugsgroup, String action)throws Exception {
            ArrayList<HashMap<String, Object>> groupstates = new ArrayList<>();
            if(!plugsgroup.isEmpty()){
                  for(String plug: plugsgroup ){
                        updatePlugs(plug, action);
                        HashMap<String, Object> state = new HashMap<>();
                        state.put("name",plug);
                        state.put("state",plugState(plug));
                        System.out.println(state);
                        groupstates.add(state);
                  }
            }
            return groupstates;
      }


      synchronized void updatePlugs(String plugName,String action) throws Exception {
            System.out.println(plugName+action);
            mqtt.publishAction(plugName, action);
            for( HashMap<String, Object> state : states){
                  System.out.println(state.get("name")+"$");
                  System.out.println(state.get(plugName));
                  if(state.get("name") ==plugName  ){
                        System.out.println(state.get("state")+"State....");
                        if(action.equals("on")){
                              mqtt.publishAction(plugName, action);
                              state.replace("state","on");
                              state.put("state", "on");
                              state.remove("state");
                              System.out.println(state);
                        }
                        else if(action.equals("off")){
                              state.replace("state", "off");
                              System.out.println(state);
                        }
                  else if(action.equals("toggle")){
                        if(state.get(plugName)== "on"){
                              state.replace("state", "off");
                        }else{
                              state.replace("state","on");
                        }
                  }
            }
      }
}     

      synchronized public void removePlug(String plug) {
            // plugs.remove(plug);
}
      @Override
      public void close() throws Exception {
            // mqtt.disconnect();
            //mqtt.close();
            }
}
