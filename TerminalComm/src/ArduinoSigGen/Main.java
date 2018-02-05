package ArduinoSigGen;
	
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.Parent;
import javafx.scene.Scene;


public class Main extends Application {
	private Controller controller;
	
	@Override
	public void start(Stage primaryStage) {
		try {
			FXMLLoader loader = new FXMLLoader(Main.class.getResource( "InterfaceMarkup.fxml" ) );
	        Parent root = loader.load();
	        controller = loader.<Controller>getController();
	        
	        Scene scene = new Scene( root );
	        scene.getStylesheets().add(Main.class.getResource("application.css").toExternalForm());
	        
	        primaryStage.setTitle("Signal Generator Display");
	        primaryStage.setScene(scene);
	        primaryStage.show();
		} catch(Exception e) {
			e.printStackTrace();
			Platform.exit();
		}
		
		Timeline sixtyTimesASecond = new Timeline(new KeyFrame(Duration.millis(17), new EventHandler<ActionEvent>() {

		    @Override
		    public void handle(ActionEvent event) {
		        controller.updateValues();
		    }
		}));
		sixtyTimesASecond.setCycleCount(Timeline.INDEFINITE);
		sixtyTimesASecond.play();
	}
	
	@Override
	public void stop(){
	    controller.closePort();
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}
