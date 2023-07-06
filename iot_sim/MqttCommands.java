package ece448.iot_sim;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.TreeMap;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MqttCommands {
    private final TreeMap<String, PlugSim> plugs = new TreeMap<>();
    private final String topicPrefix;
    public MqttCommands(List<PlugSim> plugs, String topicPrefix) {
        for (PlugSim plug: plugs)
            this.plugs.put(plug.getName(), plug);
        this.topicPrefix = topicPrefix;
    }
    public String getTopic() {
        return topicPrefix+"/action/#";
    }
    public void handleMessage(String topic, MqttMessage msg) {
        try {
            logger.info("MqttCmd1 {}", topic);
           
            String[] topicArr = topic.split("/");
            String plugName = topicArr[topicArr.length-2];
            String plugAction = topicArr[topicArr.length-1];
            System.out.println("plugName:" + plugName);
            System.out.println("plugAction:" + plugAction);
            
            PlugSim plug = plugs.get(plugName);
            if ("on".equals(plugAction)) {
                plug.switchOn();
            }
            if ("off".equals(plugAction)) {
                plug.switchOff();
            }
            if ("toggle".equals(plugAction)) {
                plug.toggle();
            }
       

        } catch (Throwable th) {

            logger.error("MqttCmd1 {}", topic, th.getMessage(), th);
        }
    }
    
    private static final Logger logger = LoggerFactory.getLogger(HTTPCommands.class);
}
