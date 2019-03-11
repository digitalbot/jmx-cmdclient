package jp.gr.java_conf.uzresk.jmx.client;

import java.util.Objects;

public class ObjectNameAttribute {

	private final String objectName;

	private final String attribute;

	ObjectNameAttribute(String objectName, String attribute) {
		this.objectName = simplify(Objects.requireNonNull(objectName));
		this.attribute = simplify(Objects.requireNonNull(attribute));
	}

	private String simplify(String value) {
		if (value.startsWith("\"") && value.endsWith("\"")) {
			return value.substring(1, value.length() - 1);
		} else {
			return value;
		}
	}

	String getObjectName() {
		return this.objectName;
	}

	String getAttribute() {
		return this.attribute;
	}

	@Override
	public String toString() {
		return "ObjectNameAttribute [objectName=" + objectName + ", attribute=" + attribute + "]";
	}
}
