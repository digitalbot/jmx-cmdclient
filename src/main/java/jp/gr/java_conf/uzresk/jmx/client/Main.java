package jp.gr.java_conf.uzresk.jmx.client;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.gr.java_conf.uzresk.jmx.client.util.StringUtils;

public class Main {

	private static final Logger LOG = LoggerFactory.getLogger("default");

	private static final Logger SIMPLE_LOG = LoggerFactory.getLogger("simple");

	private MBeanServerConnection mbsc = null;

	private boolean init = true;

	private List<String> header = new ArrayList<>();

	/**
	 * Main
	 * 
	 * @param args args[0] URL args[1] ObjectName args[2] AttributeName args[3] interval
	 */
	public static void main(String[] args) {

		if (StringUtils.isBlank(args[0])) {
			LOG.error("URL is not null.");
			System.exit(1);
		}
		String url = args[0];

		List<ObjectNameAttribute> metrics = load();

		// Bad only if the file or either argument
		if (!metrics.isEmpty() && args.length >= 3) {
			LOG.error("Bad only if the file or either argument.");
			System.exit(1);
		}

		String intervalStr = null;

		boolean isShowDomains = false;

		if (metrics.isEmpty()) {

			if (args.length == 1) {
				isShowDomains = true;
                        } else if (args.length > 2) {
                            String[] attrs = args[2].split(",");
                            for (String attr: attrs) {
                                metrics.add(new ObjectNameAttribute(args[1], attr));
                            }
                            if (args.length == 4) {
                                intervalStr = args[3];
                            }
			}
		} else {
			if (args.length == 2) {
				intervalStr = args[1];
			}
		}

		int interval = 0;
		if (StringUtils.isNotBlank(intervalStr)) {
			try {
				interval = Integer.parseInt(intervalStr);
			} catch (NumberFormatException e) {
				LOG.error("polling interval must be numeric.");
				System.exit(1);
			}
		}

		try {
			new Main().collect(url, isShowDomains, metrics, interval);
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			System.exit(2);
		}
		System.exit(0);
	}

	private void collect(String url, boolean isShowDomains, List<ObjectNameAttribute> metrics, int interval) {
		JMXServiceURL jmxServiceUrl;
		try {
			jmxServiceUrl = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + url + "/jmxrmi");
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}

		try (JMXConnector connector = JMXConnectorFactory.connect(jmxServiceUrl)) {
			mbsc = connector.getMBeanServerConnection();
			if (isShowDomains) {
				outputObjectNames();
			} else {
				// headerを取るために１回空振りさせる
				outputAttribute(metrics);
				init = false;
				showHeader();

				if (interval != 0) {
					while (true) {
						try {
							LOG.info(outputAttribute(metrics));
							Thread.sleep(interval * 1000);
						} catch (InterruptedException e) {
							throw new RuntimeException(e);
						}
					}
				} else {
					LOG.info(outputAttribute(metrics));
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("cannot connect jmx server [" + url + "]", e);
		}
	}

	private static List<ObjectNameAttribute> load() {
		String path = System.getProperty("path");
		if (StringUtils.isBlank(path)) {
			return new ArrayList<>();
		}

		List<ObjectNameAttribute> values = new ArrayList<>();
		try {
			for (String line : Files.readAllLines(Paths.get(path))) {
				if (StringUtils.isEmpty(line)) {
					continue;
				}

				String[] tokens = line.split("\t");
				if (tokens.length != 2) {
					throw new RuntimeException(
							"Either it has not been specified in the ObjectName or attribute. [" + line + "]");
				}
				values.add(new ObjectNameAttribute(tokens[0], tokens[1]));
			}
		} catch (IOException e) {
			throw new RuntimeException("file can not be read. [" + path + "]", e);
		}
		return values;
	}

	private void showHeader() {
		String headerLine = String.join(",", header);
		SIMPLE_LOG.info(headerLine);
	}

	private void outputObjectNames() {
		Set<ObjectName> names;
		try {
			names = new TreeSet<>(mbsc.queryNames(null, null));
		} catch (IOException e) {
			throw new RuntimeException("can't retrieve mbean.", e);
		}

		for (ObjectName name : names) {
			System.out.println(name.toString());
		}

	}

	private String outputAttribute(List<ObjectNameAttribute> metrics) {
		return metrics.stream()
				.map(this::prettyPrintAttribute)
				.collect(Collectors.joining(","));
	}

	private String prettyPrintAttribute(ObjectNameAttribute objectNameAttribute) {

		ObjectName objectName;
		try {
			objectName = new ObjectName(objectNameAttribute.getObjectName());
		} catch (MalformedObjectNameException e) {
			throw new RuntimeException("cannot find object name [" + objectNameAttribute.getObjectName() + "]", e);
		}

		Object obj;
		String attribute = objectNameAttribute.getAttribute();
		try {
			obj = mbsc.getAttribute(objectName, attribute);
		} catch (Exception e) {
			throw new RuntimeException("cannot find attribute [" + attribute + "]", e);
		}

		if (obj instanceof CompositeDataSupport) {
			CompositeDataSupport data = (CompositeDataSupport) obj;
			Set<String> keys = new TreeSet<>(data.getCompositeType().keySet());
			StringJoiner joiner = new StringJoiner(",");
			for (String key : keys) {
				if (init) {
					header.add(attribute + "@" + key);
				}
				joiner.add(data.get(key).toString());
			}
			return joiner.toString();
		} else if (obj.getClass().isArray()) {
			if (init) {
				header.add(attribute);
			}
			return Arrays.deepToString((Object[]) obj);
		} else {
			if (init) {
				header.add(attribute);
			}
			return obj.toString();
		}
	}
}
