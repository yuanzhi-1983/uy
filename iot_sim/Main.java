package ece448.iot_sim;

import java.io.File;
import java.util.ArrayList;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPublish;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.jboss.logging.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ece448.iot_sim.PlugSim.Observer;
import ece448.iot_sim.http_server.JHTTP;

public class Main implements AutoCloseable {
	public static void main(String[] args) throws Exception {
		// load configuration file
		String configFile = args.length > 0 ? args[0] : "simConfig.json";
		SimConfig config = mapper.readValue(new File(configFile), SimConfig.class);
		logger.info("{}: {}", configFile, mapper.writeValueAsString(config));

		try (Main m = new Main(config))
		{
			// loop forever
			for (;;)
			{
				Thread.sleep(60000);
			}
		}
		// System.exit(0);
	}

	public Main(SimConfig config) throws Exception {
		// create plugs
		ArrayList<PlugSim> plugs = new ArrayList<>();
		for (String plugName: config.getPlugNames()) {
			plugs.add(new PlugSim(plugName));
		}

		// start power measurements
		MeasurePower measurePower = new MeasurePower(plugs);
		measurePower.start();

		// start HTTP commands
		this.http = new JHTTP(config.getHttpPort(), new HTTPCommands(plugs));
		this.http.start();

		// start MQTT commands
		this.mqtt = new MqttClient(config.getMqttBroker(), config.getMqttClientId(),new MemoryPersistence());
		this.mqtt.connect();

		MqttCommands mqttcmd = new MqttCommands(plugs,config.getMqttTopicPrefix());
		logger.info("mqtt subscribe to {}",mqttcmd.getTopic());
		this.mqtt.subscribe(mqttcmd.getTopic(),(topic,msg)->{
				mqttcmd.handleMessage(topic, msg);});
		
		MqttUpdates mqttUpd = new MqttUpdates(config.getMqttTopicPrefix());
		for(PlugSim plug: plugs){
			plug.addObserver((name,key,value) -> {
				try {
					String upd_topic = mqttUpd.getTopic(name,key);
					MqttMessage upd_msg = mqttUpd.getMessage(value);
					mqtt.publish(upd_topic, upd_msg);
				} catch (Exception e) {
					//TODO: handle exception
					logger.error("fail to publish {}{}{}", name,key,value,e);
				}
			});
		// this.mqtt.disconnect();
		}
		// MeasurePower measurePower = new MeasurePower(plugs);
		// measurePower.start();
		

	}

	@Override
	public void close() throws Exception {
		http.close();
		mqtt.disconnect();
		// measurePower.
	}
	private MqttClient mqtt;
	private final JHTTP http;

	private static final ObjectMapper mapper = new ObjectMapper();
	private static final Logger logger = LoggerFactory.getLogger(Main.class);
}
