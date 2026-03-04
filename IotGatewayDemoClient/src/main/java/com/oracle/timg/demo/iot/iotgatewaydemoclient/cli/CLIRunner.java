package com.oracle.timg.demo.iot.iotgatewaydemoclient.cli;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.oracle.bmc.identity.model.Compartment;
import com.oracle.bmc.iot.model.DigitalTwinInstance;
import com.oracle.bmc.iot.model.IotDomain;
import com.oracle.bmc.iot.model.IotDomainGroup;
import com.oracle.timg.demo.iot.iotgatewaydemoclient.clients.IotGatewayHttpClient;
import com.oracle.timg.oci.authentication.AuthenticationProcessor;
import com.oracle.timg.oci.identity.IdentityProcessor;
import com.oracle.timg.oci.iot.IotProcessor;
import com.oracle.timg.oci.vault.VaultProcessor;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;
import timgutilities.textio.ChoiceDescription;
import timgutilities.textio.ChoiceDescriptionData;
import timgutilities.textio.RunnableCommand;
import timgutilities.textio.TextIOUtils;
import timgutilities.textio.TextIOUtils.NUM_TYPE;

@Singleton
public class CLIRunner implements Runnable {
	private final AuthenticationProcessor authenticationProcessor;
	private final IdentityProcessor identityProcessor;
	private final IotProcessor iotProcessor;
	private final VaultProcessor vaultProcessor;
	private final String compartmentName;
	private final Compartment compartment;
	private final String iotDomainGroupName;
	private final IotDomainGroup iotDomainGroup;
	private final String iotDomainName;
	private final IotDomain iotDomain;

	private final IotGatewayHttpClient iotGatewayHttpClient;

	private final ApplicationContext appCtxt;

	private Thread runThread;
	private final ObjectMapper jsonMapper;
	private final XmlMapper xmlMapper;

	@Inject
	public CLIRunner(ApplicationContext appCtxt,
			@Property(name = "oci.auth.configsection", defaultValue = "DEFAULT") String configSection,
			@Property(name = "oci.iot.compartmentname") String compartmentName,
			@Property(name = "oci.iot.iotdomaingroupname") String iotDomainGroupName,
			@Property(name = "oci.iot.iotdomainname") String iotDomainName, IotGatewayHttpClient iotGatewayHttpClient,
			ObjectMapper jsonMapper, XmlMapper xmlMapper) throws Exception {
		this.appCtxt = appCtxt;
		this.authenticationProcessor = new AuthenticationProcessor(configSection);
		this.identityProcessor = new IdentityProcessor(authenticationProcessor);
		this.iotProcessor = new IotProcessor(authenticationProcessor);
		this.vaultProcessor = new VaultProcessor(authenticationProcessor);
		this.iotGatewayHttpClient = iotGatewayHttpClient;
		this.compartmentName = compartmentName;
		this.iotDomainGroupName = iotDomainGroupName;
		this.iotDomainName = iotDomainName;
		this.jsonMapper = jsonMapper;
		this.xmlMapper = xmlMapper;

		this.compartment = identityProcessor.locateCompartmentByPath(compartmentName);
		if (compartment == null) {
			throw new MissingOciResourceException("Cannot locate compartment " + compartmentName);
		}

		this.iotDomainGroup = iotProcessor.getIotDomainGroup(compartment.getId(), iotDomainGroupName);
		if (iotDomainGroup == null) {
			throw new MissingOciResourceException(
					"Cannot locate iot domain group " + iotDomainGroupName + " in compartment " + compartmentName);
		}
		this.iotDomain = iotProcessor.getIotDomainInDomainGroup(iotDomainGroup, iotDomainName);
		if (iotDomain == null) {
			throw new MissingOciResourceException("Cannot locate iot domain " + iotDomainName + " in iot domain group "
					+ iotDomainGroupName + " in compartment " + compartmentName);
		}
	}

	@Override
	public void run() {
		TextIOUtils.doOutput("CLI Runner is up, entering loop");
		try {
			executeRunLoop();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		TextIOUtils.doOutput("CLI Run loop finished");
		appCtxt.stop();
		TextIOUtils.doOutput("Application server context shutdown");
	}

	private void executeRunLoop() throws IOException {
		TextIOUtils.doOutput("Starting run loop");
		TextIOUtils.selectAndRunLoop("Please chose from", coreCommands, true, false);
	}

	private RunnableCommand[] coreCommands = { new RunnableCommand("Send event", () -> {
		return sendEventCmd();
	}), new RunnableCommand("Send JSON test model event", () -> {
		return uploadTestData(TestDataFormat.JSON);
	}), new RunnableCommand("Send XML Test model event", () -> {
		return uploadTestData(TestDataFormat.XML);
	}), new RunnableCommand("DigitalTwinInstance latest data", () -> {
		return getDigitalTwinInstanceLatestData();
	}), new RunnableCommand("DigitalTwinInstance details", () -> {
		return getDigitalTwinInstanceDetails();
	}), new RunnableCommand("DigitalTwinInstance auth string", () -> {
		return getDigitalTwinInstanceAuthSecret();
	}), new RunnableCommand("DigitalTwinInstance delete", () -> {
		return deleteDigitalTwinInstance();
	}), new RunnableCommand("Iot Configuration", () -> {
		return iotConnectionDetails();
	}) };

	@EventListener
	public void onStartup(StartupEvent event) {
		TextIOUtils.doOutput("Startup event received, initiating cli runner");
		runThread = new Thread(this);
		runThread.start();
	}

	private String deleteDigitalTwinInstance() throws IOException {
		DigitalTwinInstance dti = selectDigitalTwin("Please chose the instance to delete");
		if (dti == null) {
			return "No instance chosen";
		}
		if (TextIOUtils.getYN("Are you sure you want to delete instance " + dti.getDisplayName()
				+ " note that this will not delete any associated secret or data cached in the gateway (which will need to be restarted to clear it's caches)",
				false)) {
			boolean resp = iotProcessor.deleteDigitalTwinInstance(dti);
			return "Deletion of instance " + dti.getDisplayName() + " requested, the immediate response is " + resp;
		} else {
			return "OK, not proceeding with deletion";
		}
	}

	private String getDigitalTwinInstanceDetails() throws IOException {
		DigitalTwinInstance dti = selectDigitalTwin("Please chose the instance to retrieve");
		if (dti == null) {
			return "No instance chosen";
		}
		return dti.toString();
	}

	private String getDigitalTwinInstanceAuthSecret() throws IOException {
		DigitalTwinInstance dti = selectDigitalTwin("Please chose the instance whose secret you want to retrieve");
		if (dti == null) {
			return "No instance chosen";
		}
		String secretOcid = dti.getAuthId();
		String secretContents = vaultProcessor.getSecretContents(secretOcid);
		return "Secret contens are " + secretContents + " for digital twin named " + dti.getDisplayName()
				+ " which has authocid=" + secretOcid;
	}

	private String getDigitalTwinInstanceLatestData() throws IOException {
		DigitalTwinInstance dti = selectDigitalTwin("Please chose the instance whose data you want to retrieve");
		if (dti == null) {
			return "No instance chosen";
		}
		Map<String, Object> latestData = iotProcessor.getDigitalTwinInstanceContent(dti);
		return "Latest data is " + latestData;
	}

	private String sendEventCmd() throws IOException {
		String sourceId = TextIOUtils.getString("Please enter the source id");
		String payload = TextIOUtils.getString("Please enter the payload");
		iotGatewayHttpClient.processIncommingEvent(sourceId, payload);
		return "Sent using sourceId=" + sourceId;
	}

	private String iotConnectionDetails() {
		return "Compartment=" + compartmentName + "CompartmentOcid=" + compartment.getId() + "\niotDmainGroupName="
				+ iotDomainGroupName + ", iotDmainGroupOcid=" + iotDomainGroup.getId() + ", iotDomainName="
				+ iotDomainName + ", iotDomainOcid=" + iotDomain.getId();
	}

	// general purpose digital twin picker
	private DigitalTwinInstance selectDigitalTwin(String prompt) throws IOException {
		List<DigitalTwinInstance> digitalTwinInstances = iotProcessor.listDigitalTwinInstancesActive(iotDomain);
		if (digitalTwinInstances.isEmpty()) {
			TextIOUtils.doOutput("No Digital Twin Instances found in IotDomain  " + iotDomain.getDisplayName());
			return null;
		} else {
			ChoiceDescriptionData<DigitalTwinInstance> digitalTwinInstancesCdd = new ChoiceDescriptionData<>(
					digitalTwinInstances.stream()
							.map(dti -> new ChoiceDescription<DigitalTwinInstance>(dti.getDisplayName(), dti))
							.toList());
			return TextIOUtils.getParamChoice(prompt, digitalTwinInstancesCdd);
		}
	}

	@AllArgsConstructor
	@NoArgsConstructor
	@ToString
	private class RepeatInfo {
		int repeatCount = 1;
		long repeatDelay = 0;
		double currentDelta = 0.0;
		double reserveDelta = 0.0;

		int getCurrentDelta(int count) {
			return (int) ((count) * currentDelta);
		}

		int getReserveDelta(int count) {
			return (int) ((count) * reserveDelta);
		}
	}

	private RepeatInfo getRepeatInfo(StandardModelTestData testData) throws IOException {
		int count = TextIOUtils.getInt("Please enter how many events you want to send", NUM_TYPE.RANGE, 2, 100);
		double timewindow = TextIOUtils
				.getDouble("Please enter the number of seconds (can be part seconds) over which to send the " + count
						+ " events (this is 1st to last event)", NUM_TYPE.AT_OR_ABOVE, 0.01, 1000);
		long delay = Math.round((timewindow * 1000) / (count - 1));
		int currentEnd = TextIOUtils.getInt(
				"Please enter the current charge percentage at the end of the repeat (the current value is "
						+ testData.currentBatteryCapacityPercentage + ")",
				NUM_TYPE.RANGE, 0, 100, testData.currentBatteryCapacityPercentage);
		double currentDelta = ((double) (currentEnd - testData.currentBatteryCapacityPercentage))
				/ ((double) (count - 1));
		int reserveEnd = TextIOUtils.getInt(
				"Please enter the reserved charge percentage at the end of the repeat (the current value is "
						+ testData.reservedBatteryCapacityPercentage + ")",
				NUM_TYPE.RANGE, 0, 100, testData.reservedBatteryCapacityPercentage);
		double reserveDelta = ((double) (reserveEnd - testData.reservedBatteryCapacityPercentage))
				/ ((double) (count - 1));
		return new RepeatInfo(count, delay, currentDelta, reserveDelta);
	}

	private String uploadTestData(TestDataFormat format) throws Exception {
		switch (format) {
		case JSON:
			if (!TextIOUtils.getYN(
					"This will only work if you have a event data reformatter configured that can accept JSON, or are using the detault to pass the data through unchanged, continue ?",
					true)) {
				return "Operation abandoned";
			}
			break;
		case XML:
			if (!TextIOUtils.getYN(
					"This will only work if you have a event data reformatter configured that can process XML to JSON, continue ?",
					false)) {
				return "Operation abandoned";
			}
			break;
		default:
			return "Unknown format " + format;
		}

		String sourceId = TextIOUtils.getString("Please enter the source id");
		StandardModelTestData testData = getStandardModelTestData("Please enter the test data to be sent in JSON");
		TextIOUtils.doOutput("test data is " + testData);
		RepeatInfo repeatInfo = new RepeatInfo();
		if (TextIOUtils.getYN("Do you want to send multiple items in a row ?", false)) {
			repeatInfo = getRepeatInfo(testData);
		}
		TextIOUtils.doOutput("Repeat data is " + repeatInfo);
		String sentPayload = "Nothing sent, failed to do the repeat loop";
		int lastSend = repeatInfo.repeatCount - 1;
		for (int repeatCount = 0; repeatCount < repeatInfo.repeatCount; repeatCount++) {
			StandardModelTestData updatedStandardModelTestData = testData.getRepeatVersion(repeatInfo, repeatCount);
			sentPayload = sendStandardModelTestData(sourceId, format, updatedStandardModelTestData);
			if (repeatInfo.repeatCount == 1) {
				return "Sent using sourceId=" + sourceId + ", payload=" + sentPayload;
			}
			// if we are at the last one no sleep
			if (repeatCount < lastSend) {
				TextIOUtils.doOutput("Sent repeat " + (repeatCount + 1) + ", pausing " + repeatInfo.repeatDelay + ", "
						+ sentPayload);
				Thread.sleep(repeatInfo.repeatDelay);
			}
		}
		// got to the end
		return "Sent " + repeatInfo.repeatCount + " items using source id " + sourceId + ", final payload="
				+ sentPayload;
	}

	private String sendStandardModelTestData(String sourceId, TestDataFormat format,
			StandardModelTestData updatedStandardModelTestData) throws Exception {
		ObjectNode node = getStandardModelTestData(updatedStandardModelTestData);
		String payloadToSend;
		switch (format) {
		case JSON:
			try {
				payloadToSend = jsonMapper.writeValueAsString(node);
			} catch (JsonProcessingException e) {
				throw new Exception("Problem converting object node to JSON - " + e.getLocalizedMessage());
			}
			break;
		case XML:
			try {
				payloadToSend = xmlMapper.writeValueAsString(node);
			} catch (JsonProcessingException e) {
				throw new Exception("Problem converting object node to XML - " + e.getLocalizedMessage());
			}
			break;
		default:
			return "Problem sending entry as the provided format " + format + " is unknown ";
		}
		iotGatewayHttpClient.processIncommingEvent(sourceId, payloadToSend);
		return payloadToSend;
	}

	@AllArgsConstructor
	@ToString
	private class StandardModelTestData {
		int currentBatteryCapacityPercentage;
		int operatingMode;
		int reservedBatteryCapacityPercentage;
		String comment;

		StandardModelTestData getRepeatVersion(RepeatInfo repeatInfo, int repeatCount) {
			if (repeatInfo.repeatCount == 1) {
				return this;
			}
			int newCurrentBatteryCapacityPercentage = this.currentBatteryCapacityPercentage
					+ repeatInfo.getCurrentDelta(repeatCount);
			int newOperatingMode = this.operatingMode;
			int newReservedBatteryCapacityPercentage = this.reservedBatteryCapacityPercentage
					+ repeatInfo.getReserveDelta(repeatCount);
			String newComment = ((comment == null) || (comment.isBlank())) ? "Repeat " + (repeatCount + 1)
					: comment + ", repeat " + (repeatCount + 1);
			return new StandardModelTestData(newCurrentBatteryCapacityPercentage, newOperatingMode,
					newReservedBatteryCapacityPercentage, newComment);

		}
	}

	/**
	 * this builds the test data that the input model for the iotgateway uses in
	 * it's testing. really it's here to save the hassle of working out the correct
	 * string to input, esp if you're going to tru JSON and XML input to check out
	 * the reformatting process
	 * 
	 * @param prompt
	 * @return
	 * @throws IOException
	 */
	private StandardModelTestData getStandardModelTestData(String prompt) throws IOException {
		TextIOUtils.doOutput(prompt);
		int currentBatteryCapacityPercentage = TextIOUtils.getInt("Please enter the currentBatteryCapacityPercentage",
				NUM_TYPE.RANGE, 0, 100);
		int operatingMode = TextIOUtils.getInt(
				"Please enter the numeric operating mode, supported values are 1 (Manual), 2(Automatic - Self Consumption), 6(Battery-Module-Extension (30%)) and 10(Time-Of-Use) all others will be mapped to unknown in the adapter",
				NUM_TYPE.RANGE, 1, 10);
		int reservedBatteryCapacityPercentage = TextIOUtils.getInt("Please enter the reservedBatteryCapacityPercentage",
				NUM_TYPE.RANGE, 0, 100);
		String comment = TextIOUtils.getString("Please enter your comment on this entry", "");
		return new StandardModelTestData(currentBatteryCapacityPercentage, operatingMode,
				reservedBatteryCapacityPercentage, comment);
	}

	/**
	 * this builds the test data that the input model for the iotgateway uses in
	 * it's testing. really it's here to save the hassle of working out the correct
	 * string to input, esp if you're going to tru JSON and XML input to check out
	 * the reformatting process
	 * 
	 * @param currentBatteryCapacityPercentage
	 * @param operatingMode,                   int reservedBatteryCapacityPercentage
	 * 
	 * @param prompt
	 * @return
	 * @throws IOException
	 */
	private ObjectNode getStandardModelTestData(StandardModelTestData standardModelTestData) throws IOException {
		ObjectNode root = jsonMapper.createObjectNode();
		root.put("currentBatteryCapacityPercentage", standardModelTestData.currentBatteryCapacityPercentage);
		root.put("operatingMode", standardModelTestData.operatingMode);
		root.put("reservedBatteryCapacityPercentage", standardModelTestData.reservedBatteryCapacityPercentage);
		root.put("comment", standardModelTestData.comment);
		root.put("time", System.currentTimeMillis());
		root.put("timestamp", DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now()));
		return root;
	}
}
