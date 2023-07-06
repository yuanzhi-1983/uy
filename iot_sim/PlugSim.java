package ece448.iot_sim;
/////////////////////////
// import java.lang.Thread.State;
import java.util.ArrayList;
// import ch.qos.logback.core.status.OnConsoleStatusListener;
///////////////////////////////////////
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simulate a smart plug with power monitoring.
 */
public class PlugSim {

	private final String name;
	private boolean on = false;
	private double power = 0; // in watts
	private final ArrayList<Observer> observers =  new ArrayList<>();

	public PlugSim(String name) {
		this.name = name;
	}
	synchronized void addObserver(Observer observer){
		observers.add(observer);
		observer.update(name, "state", on? "on":"off");
		observer.update(name, "power", String.format("%.3f", power));
	}

	public static interface Observer {
		void update(String name, String key, String value);
	}
	protected void updateState(boolean o){
		on = o;
		logger.info("plug{}:state{}",name,on?"on":"off");
		for(Observer observer :observers){
			observer.update(name, "state", on? "on":"off");
			observer.update(name, "power", String.format("%.3f", power));
		}
	}

	/**
	 * No need to synchronize if read a final field.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Switch the plug on.
	 */
	synchronized public void switchOn() {
		this.on=true;
		this.measurePower();
		updateState(this.on);
	}

	/**
	 * Switch the plug off.
	 */
	synchronized public void switchOff() {
		this.on=false;
		this.measurePower();
		updateState(this.on);
	}

	/**
	 * Toggle the plug.
	 */
	synchronized public void toggle() {
		
		this.on=!this.on;
		this.measurePower();
		updateState(this.on);
	}

	/**
	 * Measure power.
	 */
	synchronized public void measurePower() {
		if (!on) {
			updatePower(0);
			return;
		}

		// a trick to help testing
		if (name.indexOf(".") != -1)
		{
			updatePower(Integer.parseInt(name.split("\\.")[1]));
		}
		// do some random walk
		else if (power < 100)
		{
			updatePower(power + Math.random() * 100);
		}
		else if (power > 300)
		{
			updatePower(power - Math.random() * 100);
		}
		else
		{
			updatePower(power + Math.random() * 40 - 20);
		}
	}

	protected void updatePower(double p) {
		power = p;
		logger.debug("Plug {}: power {}", name, power);
	}

	/**
	 * Getter: current state
	 */
	synchronized public boolean isOn() {
		return on;
	}

	/**
	 * Getter: last power reading
	 */
	synchronized public double getPower() {
		return power;
	}

	private static final Logger logger = LoggerFactory.getLogger(PlugSim.class);
}
