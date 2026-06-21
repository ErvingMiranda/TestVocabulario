package org.Ezone.POO.TestVocabulario.run;

import org.openxava.util.*;

/**
 * Execute this class to start the application.
 */

public class TestVocabulario {

	public static void main(String[] args) throws Exception {
		DBServer.start("TestVocabulario-db"); // To use your own database comment this line and configure src/main/webapp/META-INF/context.xml
		AppServer.run("TestVocabulario"); // Use AppServer.run("") to run in root context
	}

}
