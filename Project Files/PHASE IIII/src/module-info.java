module ProjectThreeMain {
	requires javafx.controls;
	requires java.sql;
	requires org.junit.jupiter.api;
	requires javafx.graphics;
	
	opens application to javafx.graphics, javafx.fxml;
}
