package ArduinoSigGen;

import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class Controller  {

	@FXML
	private ChoiceBox<String> portChoice;
	@FXML
	private ChoiceBox<String> speedChoice;
	@FXML
	private TextField FrequencyField;
	@FXML
	private TextField ClockField;
	@FXML
	private TextField MeasuredFreq;
	@FXML
	private TextField DutyField;
	@FXML
	private Button CalculateButton;
	@FXML
	private SplitPane splitPane;
    @FXML
    private AnchorPane root ;
    
	private	SerialPort[] comPort = SerialPort.getCommPorts();

	private SerialPort serialPort;

	private	byte[] buffer = new byte[3];
	
	private	int duty = 0;
	private	int fine = 0;
	private	int medium = 0;
	private	int course = 0;
	private double clockSpeed = 16000000;
	private double measuredSpeed = 0;
	
	private	ObservableList<String> list = FXCollections.observableArrayList();

	public Controller() 
	{
	}

	@FXML
	private void initialize() 
	{
        
		ClockField.setText(String.format("%,.0f hz", clockSpeed));
		populatePortList();
		
		if (list != null)
			portChoice.setItems(list);

		portChoice.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> arg0, Number arg1, Number index) {
				if (index != null && speedChoice.valueProperty().get() != null) {
					closePort();
					setupComPort(portChoice.getItems().get(index.intValue()), speedChoice.getValue());
				}
			}
		});

		speedChoice.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> arg0, Number arg1, Number index) {
				if (portChoice.valueProperty().get() != null && index != null) {
					closePort();
					setupComPort(portChoice.getValue(), speedChoice.getItems().get(index.intValue()));
				}
			}
		});

		portChoice.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent arg0) {
				populatePortList();
			}
		});
		
		CalculateButton.setOnMouseClicked(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent arg0) {
				try {
					measuredSpeed = Double.parseDouble(MeasuredFreq.getText());
					
					if (measuredSpeed != 0) {
						double temp = 1 / measuredSpeed;
						temp = temp / (fine + (medium * 256) + (course * 65536) + 44);
						clockSpeed = 1 / temp;
						ClockField.setText(String.format("%,.0f hz", clockSpeed));
					}
				}
				catch (NumberFormatException e) {
				}
			}
			
		});
	}

	private void populatePortList() {
		comPort = SerialPort.getCommPorts();
		
		list.clear();
		int j = 0;
		
		for (int i = 0; i < comPort.length; i++){
			String portName = comPort[i].getDescriptivePortName();
			if(!portName.contains("Dial")) {
				list.add(j, portName);
				++j;
			}
		}

	}
	
	public void closePort() {
		if (serialPort != null) {
			try {
				serialPort.removeDataListener();
				Thread.sleep(100);
				serialPort.closePort();
				Thread.sleep(100);
			}
			catch (Exception e) {
				((Stage)(FrequencyField.getScene().getWindow())).close();
			}
		}
	}

	protected void setupComPort(String port, String speed) {
		String systemPortName = Arrays.asList(comPort).stream()
				.filter(p -> p.getDescriptivePortName() == port)
				.findFirst().get().getSystemPortName();
		
		try {
			serialPort = SerialPort.getCommPort(systemPortName);
			serialPort.setBaudRate(Integer.parseInt(speed));
		}
		catch (Exception e) {
			e.printStackTrace();
			((Stage)(FrequencyField.getScene().getWindow())).close();
		}

		if (serialPort.openPort()) {
			try {Thread.sleep(100);} catch(Exception e){}

			serialPort.addDataListener(new SerialPortDataListener() {
				@Override
				public int getListeningEvents() { return SerialPort.LISTENING_EVENT_DATA_AVAILABLE; }
				@Override
				public void serialEvent(SerialPortEvent event)
				{
					if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE)
					    ((Stage)(FrequencyField.getScene().getWindow())).close();

					if (serialPort != null && serialPort.getInputStream() != null) {
						try {
							InputStream inputStream = serialPort.getInputStream();
							
							while(inputStream.available() > 0) {
								processInFromArduino((byte) inputStream.read());
							}
						} catch (IOException e) {
							e.printStackTrace();
							((Stage)(FrequencyField.getScene().getWindow())).close();
						}
					}
					else
					{
						((Stage)(FrequencyField.getScene().getWindow())).close();
					}
				}
			});
		}
		else {
			AlertBox alert = new AlertBox(new Dimension(400,100),"Error Connecting", "Try Another port");
			alert.display();
		}
	}

	private void processInFromArduino(byte inputByte) {

		buffer[0] = buffer[1];
		buffer[1] = buffer[2];
		buffer[2] = inputByte;

		try {
			if (buffer[0] == 68 && buffer[1] == 58) {
				duty = Byte.toUnsignedInt(buffer[2]);
			}
			else if (buffer[0] == 70 && buffer[1] == 58) {
				fine = Byte.toUnsignedInt(buffer[2]);
			}
			else if (buffer[0] == 77 && buffer[1] == 58) {
				medium = Byte.toUnsignedInt(buffer[2]);
			}
			else if (buffer[0] == 67 && buffer[1] == 58) {
				course = Byte.toUnsignedInt(buffer[2]);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			((Stage)(FrequencyField.getScene().getWindow())).close();
		}
	}
	
	public void updateValues() {
		double readTotalTicks = fine + (medium * 256) + (course * 65536);
		
		double calculatedHigh = Math.round(((readTotalTicks * duty) / 256) + 1);
		
		 
		
		try{
			DutyField.setText(String.format("%.2f%%", (calculatedHigh / (readTotalTicks + 44)) * 100));
			FrequencyField.setText(String.format("%,.0f Hz", 1 / ((1 / clockSpeed) * (readTotalTicks + 44)) ));
		}
		catch(Exception e) {
			((Stage)(FrequencyField.getScene().getWindow())).close();
		}
	}
}
