import java.util.Date;
import java.text.DateFormat;
import com.pi4j.wiringpi.Gpio;
import com.pi4j.wiringpi.GpioUtil;
import java.text.SimpleDateFormat;

public class DHT11 {
	private static final int MAXTIMINGS = 85;
	private final int[] dht11_dat = {0,0,0,0,0};

	private double temperature;
	private double humidity;

	private CommunicationModule comsDHT11 = new CommunicationModule();

	/*
	* Creates a DHT11 object.
	* @params - None
	* @return - None
	*/
	public DHT11() {

		System.out.println("[Launcher] Configuring DHT11 temperature sensor.");

		if (Gpio.wiringPiSetup() == -1) {
			this.comsDHT11.sendEmail("[ERROR] GPIO setup failed.", "An error occured when creating a DHT11 object", this.comsDHT11.getToEmail());
			return;
		}
		GpioUtil.export(3, GpioUtil.DIRECTION_OUT);
	}

	/*
	* Updates the temperature and humidity data from the sensor.
	* @params - int - Raspberry Pi pin sensor is connected to.
	* @return - None
	*/
	public void updateTemperature(final int pin) {
		do {
			int laststate = Gpio.HIGH;
			int j = 0;
			dht11_dat[0] = dht11_dat[1] = dht11_dat[2] = dht11_dat[3] = dht11_dat[4] = 0;

			Gpio.pinMode(pin, Gpio.OUTPUT);
			Gpio.digitalWrite(pin, Gpio.LOW);
			Gpio.delay(18);

			Gpio.digitalWrite(pin, Gpio.HIGH);
			Gpio.pinMode(pin, Gpio.INPUT);

			for (int i = 0; i < MAXTIMINGS; i++) {
				int counter = 0;
				while (Gpio.digitalRead(pin) == laststate) {
					counter++;
					Gpio.delayMicroseconds(1);
					if (counter == 255) {
						break;
					}
				}

				laststate = Gpio.digitalRead(pin);

				if (counter == 255) {
					break;
				}

				try {
					//Ignore first 3 transitions.
					if (i >= 4 && i % 2 == 0) {
					
						dht11_dat[j / 8] <<= 1; //Shove each bit into the storage bytes.
					
						if (counter > 16) {
							dht11_dat[j / 8] |= 1;
						}

						j++;
					}
				} catch(Exception  e) {
					this.comsDHT11.sendEmail("[ERROR] DHT11 ArrayIndexOutOfBoundsException.", "An error occured when getting the temperature from the DHT11 temperature sensor. " + e, this.comsDHT11.getToEmail());
					DHT11 tmp = new DHT11();
					tmp.updateTemperature(7);
					this.temperature = tmp.getTemperature();
					this.humidity = tmp.gethumidity();
					break;
				}
				
			}
			// check we read 40 bits (8bit x 5 ) + verify checksum in the last byte.
			if (j >= 40 && checkParity()) {
				float h = (float)((dht11_dat[0] << 8) + dht11_dat[1]) / 10;
				if (h > 100) {
					h = dht11_dat[0]; // for DHT11
				}
				float c = (float)(((dht11_dat[2] & 0x7F) << 8) + dht11_dat[3]) / 10;
				if (c > 125) {
					c = dht11_dat[2]; // for DHT11
				}
				if ((dht11_dat[2] & 0x80) != 0) {
					c = -c;
				}
				final float f = c * 1.8f + 32;
				this.temperature = f;
				this.humidity = h;
			} else {
				this.temperature = Double.MAX_VALUE;
				this.humidity = Double.MAX_VALUE;
			}
		} while(this.temperature ==  Double.MAX_VALUE || this.humidity == Double.MAX_VALUE);
	}

	/*
	* Returns the temperature from the sensor.
	* @params - None
	* @return - Double - temperature as a double.
	*/
	public double getTemperature() {
		return this.temperature;
	}

	/*
	* Returns the humidity from the sensor.
	* @params - None
	* @return - Double - humidity as a double.
	*/
	public double gethumidity() {
		return this.humidity;
	}

	private boolean checkParity() {
		return dht11_dat[4] == (dht11_dat[0] + dht11_dat[1] + dht11_dat[2] + dht11_dat[3] & 0xFF);
	}
}
