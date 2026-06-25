package org.Ezone.POO.TestVocabulario.run;

import org.openxava.util.*;

/**
 * Execute this class to start the application.
 */

public class TestVocabulario {

	public static void main(String[] args) throws Exception {
		configurarPostgreSQL();
		AppServer.run("TestVocabulario"); // Use AppServer.run("") to run in root context
	}

	private static void configurarPostgreSQL() {
		setRequired("testvocabulario.db.url", "TESTVOCABULARIO_DB_URL");
		setRequired("testvocabulario.db.user", "TESTVOCABULARIO_DB_USER");
		setRequired("testvocabulario.db.password", "TESTVOCABULARIO_DB_PASSWORD");
	}

	private static void setRequired(String propertyName, String environmentName) {
		if (System.getProperty(propertyName) != null) return;

		String value = System.getenv(environmentName);
		if (value == null || value.isBlank()) {
			throw new IllegalStateException("Configure la variable de entorno " + environmentName + " para usar PostgreSQL");
		}
		System.setProperty(propertyName, value);
	}

}
