import com.fazecast.jSerialComm.SerialPort;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import com.odyometre.core.algorithm.FrequencyManager;
import com.odyometre.core.algorithm.HughsonWestlakeEngine;
import com.odyometre.core.model.TestConfig;
import com.odyometre.core.model.TestState;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Audiometry test GUI: manual frequency/intensity control, Hughson–Westlake
 * test flow, serial hardware link, and live audiogram plotting.
 */
public class GUI extends Application {

    public enum Ear {
        RIGHT, LEFT
    }

    private static final int[] STANDARD_FREQUENCIES = {250, 500, 1000, 2000, 4000, 8000};

    private final SerialManager serialManager = new SerialManager();
    private final Map<Integer, Integer> rightThresholds = new HashMap<>();
    private final Map<Integer, Integer> leftThresholds = new HashMap<>();

    private TestState currentTestState;
    private TestConfig hwConfig = TestConfig.defaultASHA();
    private Ear currentEar;
    private boolean isFirst1000HzDone = false;
    private boolean testFinished = false;

    private ComboBox<String> portCombo;
    private ToggleGroup modeGroup;
    private ComboBox<Ear> earCombo;
    private ComboBox<Integer> frequencyCombo;
    private Slider intensitySlider;
    private Label intensityValueLabel;
    private Label hwStatusLabel;
    private Label connectionLabel;
    private TextArea logArea;
    private AudiogramGraph audiogramGraph;
    private Button sendStimulusBtn;
    private Button heardBtn;
    private Button notHeardBtn;
    private Button startHwBtn;
    private Button resetBtn;
    private VBox manualPanel;
    private VBox hwPanel;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        serialManager.setResponseCallback(() -> Platform.runLater(() -> {
            appendLog("Hardware RESPONSE — counted as Heard.");
            if (isHughsonWestlakeMode() && !testFinished && currentTestState != null) {
                applyHughsonWestlakeResponse(true);
            }
        }));

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(12));
        root.setTop(buildConnectionBar());
        root.setCenter(buildMainContent());
        root.setBottom(buildLogArea());

        Scene scene = new Scene(root, 960, 720);
        stage.setTitle("Audiometry — Test Management");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> serialManager.disconnect());
        stage.show();

        updateModePanels();
        appendLog("GUI ready. Click Refresh to scan serial ports (requires hardware).");
    }

    private HBox buildConnectionBar() {
        portCombo = new ComboBox<>();
        portCombo.setPrefWidth(180);
        portCombo.setPromptText("Click Refresh to scan ports");

        Button refreshPortsBtn = new Button("Refresh");
        refreshPortsBtn.setOnAction(e -> refreshPorts());

        Button connectBtn = new Button("Connect");
        connectBtn.setOnAction(e -> connectSelectedPort());

        Button disconnectBtn = new Button("Disconnect");
        disconnectBtn.setOnAction(e -> disconnectPort());

        connectionLabel = new Label("Not connected");
        connectionLabel.setStyle("-fx-text-fill: #666;");

        HBox bar = new HBox(10, new Label("Serial:"), portCombo, refreshPortsBtn,
                connectBtn, disconnectBtn, connectionLabel);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 0, 10, 0));
        return bar;
    }

    private VBox buildMainContent() {
        modeGroup = new ToggleGroup();
        RadioButton manualMode = new RadioButton("Manual stimulus");
        RadioButton hwMode = new RadioButton("Hughson–Westlake");
        manualMode.setToggleGroup(modeGroup);
        hwMode.setToggleGroup(modeGroup);
        manualMode.setSelected(true);
        manualMode.setOnAction(e -> updateModePanels());
        hwMode.setOnAction(e -> updateModePanels());

        earCombo = new ComboBox<>();
        earCombo.getItems().addAll(Ear.RIGHT, Ear.LEFT);
        earCombo.setValue(Ear.RIGHT);

        HBox modeRow = new HBox(20,
                new Label("Mode:"), manualMode, hwMode,
                new Separator(),
                new Label("Ear:"), earCombo);
        modeRow.setAlignment(Pos.CENTER_LEFT);
        modeRow.setPadding(new Insets(0, 0, 10, 0));

        manualPanel = buildManualPanel();
        hwPanel = buildHughsonWestlakePanel();

        audiogramGraph = new AudiogramGraph(520, 320);
        audiogramGraph.updateAudiogram(rightThresholds, leftThresholds);

        VBox graphBox = new VBox(6, new Label("Audiogram"), audiogramGraph);
        graphBox.setPadding(new Insets(10, 0, 0, 0));

        VBox content = new VBox(8, modeRow, manualPanel, hwPanel, graphBox);
        return content;
    }

    private VBox buildManualPanel() {
        frequencyCombo = new ComboBox<>();
        for (int f : STANDARD_FREQUENCIES) {
            frequencyCombo.getItems().add(f);
        }
        frequencyCombo.setValue(1000);

        intensitySlider = new Slider(
                hwConfig.minDb(),
                hwConfig.maxDb(),
                30);
        intensitySlider.setShowTickLabels(true);
        intensitySlider.setShowTickMarks(true);
        intensitySlider.setMajorTickUnit(20);
        intensitySlider.setMinorTickCount(1);
        intensitySlider.setBlockIncrement(5);
        intensitySlider.setSnapToTicks(false);

        intensityValueLabel = new Label(formatDb(intensitySlider.getValue()));
        intensitySlider.valueProperty().addListener((obs, oldVal, newVal) ->
                intensityValueLabel.setText(formatDb(newVal.doubleValue())));

        sendStimulusBtn = new Button("Send stimulus to device");
        sendStimulusBtn.setOnAction(e -> sendManualStimulus());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.add(new Label("Frequency (Hz):"), 0, 0);
        grid.add(frequencyCombo, 1, 0);
        grid.add(new Label("Intensity (dB HL):"), 0, 1);
        HBox intensityRow = new HBox(10, intensitySlider, intensityValueLabel);
        intensityRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(intensitySlider, Priority.ALWAYS);
        grid.add(intensityRow, 1, 1);
        grid.add(sendStimulusBtn, 1, 2);

        VBox panel = new VBox(8, new Label("Manual frequency / intensity"), grid);
        panel.setPadding(new Insets(8));
        panel.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 6;");
        return panel;
    }

    private VBox buildHughsonWestlakePanel() {
        hwStatusLabel = new Label("No active test. Press Start to begin.");
        hwStatusLabel.setWrapText(true);

        startHwBtn = new Button("Start Hughson–Westlake test");
        startHwBtn.setOnAction(e -> startHughsonWestlakeTest());

        heardBtn = new Button("Heard");
        heardBtn.setOnAction(e -> applyHughsonWestlakeResponse(true));

        notHeardBtn = new Button("Not heard");
        notHeardBtn.setOnAction(e -> applyHughsonWestlakeResponse(false));

        resetBtn = new Button("Reset thresholds");
        resetBtn.setOnAction(e -> resetAllThresholds());

        HBox buttons = new HBox(10, startHwBtn, heardBtn, notHeardBtn, resetBtn);
        buttons.setAlignment(Pos.CENTER_LEFT);

        VBox panel = new VBox(8,
                new Label("Automated test management (HughsonWestlakeEngine)"),
                hwStatusLabel,
                buttons);
        panel.setPadding(new Insets(8));
        panel.setStyle("-fx-background-color: #eef6ff; -fx-background-radius: 6;");
        return panel;
    }

    private VBox buildLogArea() {
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(4);
        logArea.setPromptText("Status messages…");
        VBox box = new VBox(4, new Label("Log"), logArea);
        box.setPadding(new Insets(10, 0, 0, 0));
        return box;
    }

    private void refreshPorts() {
        String previous = portCombo.getValue();
        portCombo.getItems().clear();
        try {
            portCombo.getItems().addAll(
                    Arrays.stream(SerialPort.getCommPorts())
                            .map(SerialPort::getSystemPortName)
                            .collect(Collectors.toList()));
            if (portCombo.getItems().isEmpty()) {
                appendLog("No serial ports found.");
            } else {
                appendLog("Found " + portCombo.getItems().size() + " port(s).");
            }
        } catch (Exception ex) {
            appendLog("Serial port scan failed: " + ex.getMessage());
            appendLog("On Java 22+, run: mvn javafx:run (pom enables native access).");
            appendLog("Or close other apps using jSerialComm and try again.");
            return;
        }
        if (previous != null && portCombo.getItems().contains(previous)) {
            portCombo.setValue(previous);
        } else if (!portCombo.getItems().isEmpty()) {
            portCombo.getSelectionModel().selectFirst();
        }
    }

    private void connectSelectedPort() {
        String port = portCombo.getValue();
        if (port == null || port.isBlank()) {
            appendLog("Select a serial port first.");
            return;
        }
        if (serialManager.connect(port)) {
            connectionLabel.setText("Connected: " + port);
            connectionLabel.setStyle("-fx-text-fill: #22863a;");
            appendLog("Connected to " + port);
        } else {
            connectionLabel.setText("Connection failed");
            connectionLabel.setStyle("-fx-text-fill: #cb2431;");
            appendLog("Failed to open " + port);
        }
    }

    private void disconnectPort() {
        serialManager.disconnect();
        connectionLabel.setText("Not connected");
        connectionLabel.setStyle("-fx-text-fill: #666;");
        appendLog("Serial port closed.");
    }

    private void updateModePanels() {
        boolean manual = isManualMode();
        manualPanel.setVisible(manual);
        manualPanel.setManaged(manual);
        hwPanel.setVisible(!manual);
        hwPanel.setManaged(!manual);
    }

    private boolean isManualMode() {
        Toggle selected = modeGroup.getSelectedToggle();
        return selected != null && ((RadioButton) selected).getText().startsWith("Manual");
    }

    private boolean isHughsonWestlakeMode() {
        return !isManualMode();
    }

    private void sendManualStimulus() {
        Integer freq = frequencyCombo.getValue();
        if (freq == null) {
            appendLog("Choose a frequency.");
            return;
        }
        int db = (int) Math.round(intensitySlider.getValue());
        serialManager.sendCommand(freq, db);
        appendLog(String.format("Manual stimulus: %d Hz @ %d dB HL", freq, db));
    }

    private void startHughsonWestlakeTest() {
        Ear ear = earCombo.getValue();
        if (ear == null) {
            appendLog("Select an ear.");
            return;
        }
        currentEar = ear;
        isFirst1000HzDone = false;
        testFinished = false;

        if (currentEar == Ear.RIGHT) {
            rightThresholds.clear();
        } else {
            leftThresholds.clear();
        }

        currentTestState = TestState.initial(1000, hwConfig.startIntensityDb());

        refreshAudiogram();
        updateHwStatusLabel();
        sendCurrentHwStimulus();
        appendLog("Hughson–Westlake test started for " + ear + " ear.");
    }

    private void applyHughsonWestlakeResponse(boolean heard) {
        if (testFinished || currentTestState == null) {
            appendLog("Start a Hughson–Westlake test first.");
            return;
        }

        currentTestState = HughsonWestlakeEngine.applyResponse(currentTestState, heard, hwConfig);

        if (currentTestState.threshold().isPresent()) {
            int threshold = currentTestState.threshold().get();
            Map<Integer, Integer> target = currentEar == Ear.RIGHT ? rightThresholds : leftThresholds;
            target.put(currentTestState.frequency(), threshold);

            refreshAudiogram();
            appendLog(currentTestState.frequency() + " Hz threshold found: " + threshold + " dB");

            if (currentTestState.frequency() == 1000) {
                isFirst1000HzDone = true;
            }

            Optional<Integer> nextFreq = FrequencyManager.getNextFrequency(currentTestState.frequency(), isFirst1000HzDone);

            if (nextFreq.isEmpty()) {
                testFinished = true;
                updateHwStatusLabel();
                appendLog("Test finished for " + currentEar + " ear. Thresholds recorded.");
                showInfo("Test complete",
                        currentEar + " ear Hughson–Westlake test is complete.\n"
                                + formatThresholds(target));
                return;
            } else {
                currentTestState = TestState.initial(nextFreq.get(), hwConfig.startIntensityDb());
            }
        }

        updateHwStatusLabel();
        sendCurrentHwStimulus();
    }

    private void sendCurrentHwStimulus() {
        if (testFinished || currentTestState == null) {
            return;
        }
        int freq = currentTestState.frequency();
        int db = currentTestState.currentIntensityDb();
        serialManager.sendCommand(freq, db);
        appendLog(String.format("Stimulus: %d Hz @ %d dB HL (%s ear)",
                freq, db, currentEar));
    }

    private void refreshAudiogram() {
        audiogramGraph.updateAudiogram(
                new HashMap<>(rightThresholds),
                new HashMap<>(leftThresholds));
    }

    private void resetAllThresholds() {
        rightThresholds.clear();
        leftThresholds.clear();
        currentTestState = null;
        testFinished = true;
        refreshAudiogram();
        hwStatusLabel.setText("Thresholds cleared. Press Start to begin a new test.");
        appendLog("Audiogram and test state reset.");
    }

    private void updateHwStatusLabel() {
        if (currentTestState == null) {
            hwStatusLabel.setText("No active test. Press Start to begin.");
            return;
        }
        Map<Integer, Integer> target = currentEar == Ear.RIGHT ? rightThresholds : leftThresholds;

        if (testFinished) {
            hwStatusLabel.setText("Finished — " + currentEar + " ear. " + formatThresholds(target));
            return;
        }
        hwStatusLabel.setText(String.format(
                "%s ear | %d Hz @ %d dB HL | recorded: %s",
                currentEar,
                currentTestState.frequency(),
                currentTestState.currentIntensityDb(),
                formatThresholds(target)));
    }

    private static String formatThresholds(Map<Integer, Integer> thresholds) {
        if (thresholds.isEmpty()) {
            return "none yet";
        }
        return thresholds.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + " Hz → " + e.getValue() + " dB")
                .collect(Collectors.joining(", "));
    }

    private static String formatDb(double value) {
        return (int) Math.round(value) + " dB HL";
    }

    private void appendLog(String message) {
        logArea.appendText(message + System.lineSeparator());
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
