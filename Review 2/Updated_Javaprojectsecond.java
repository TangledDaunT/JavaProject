import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.*;

public class Javaprojectsecond {    
    // ===================== SENSORS =====================
    interface Sensor {
        double getValue();
        void setValue(double value);
    }

    static class TemperatureSensor implements Sensor {
        private double temperature;
        private final Random random = new Random();
        public TemperatureSensor(double baseTemp) { this.temperature = baseTemp; }
        public double getValue() {
            double change = (random.nextDouble() * 5) - 5;
            temperature += change;
            return Math.round(temperature * 5) / 10.0;
        }
        public void setValue(double temp) { this.temperature = temp; }
    }

    static class HumiditySensor implements Sensor {
        private double humidity;
        private final Random random = new Random();
        public HumiditySensor(double baseHumidity) { this.humidity = baseHumidity; }
        public double getValue() {
            double change = (random.nextDouble() * 30) - 15;
            humidity += change;
            return Math.round(humidity * 10.0) / 10.0;
        }
        public void setValue(double humidity) { this.humidity = humidity; }
    }

    // ===================== ACTUATORS =====================
    interface Actuator {
        double calculatePower(double currentValue);
    }

    static class AirConditioner implements Actuator {
        public double getDesiredTemp() { return ThermostatDashboard.desiredTempControl; }
        public double calculatePower(double currentTemp) {
            double tempDiff = Math.abs(currentTemp - getDesiredTemp());
            double power = (tempDiff / 1.0) * 80;
            return Math.min(1000.0, Math.round(power * 10.0) / 10.0);
        }
    }

    static class SecondaryAirConditioner extends AirConditioner {
        @Override
        public double calculatePower(double currentTemp) {
            double tempDiff = Math.abs(currentTemp - getDesiredTemp());
            double power = (tempDiff / 1.0) * 60;
            return Math.min(750.0, Math.round(power * 10.0) / 10.0);
        }
    }

    static class Heater implements Actuator {
        public double getDesiredTemp() { return 21.0; }
        public double calculatePower(double currentTemp) {
            if (currentTemp >= getDesiredTemp()) return 0.0;
            double tempDiff = getDesiredTemp() - currentTemp;
            double power = (tempDiff / 1.0) * 150;
            return Math.round(power * 10.0) / 10.0;
        }
    }

    static class Humidifier implements Actuator {
        public double getDesiredHumidity() { return 50.0; }
        public double calculatePower(double currentHumidity) {
            double diff = Math.abs(currentHumidity - getDesiredHumidity());
            double power = (diff / 1.0) * 25;
            return Math.min(300.0, Math.round(power * 10.0) / 10.0);
        }
    }

    static class SmartFan implements Actuator {
        public double calculatePower(double temp) { return (temp > 26.0) ? 60.0 : 0.0; }
    }

    // ===================== UTILITY =====================
    static class ElectricityBill {
        private double totalWattSeconds = 0.0;
        private final double costPerUnit = 8.0;
        public void addConsumption(double watts, int seconds) { totalWattSeconds += watts * seconds; }
        public double getTotalCost() {
            double kWh = totalWattSeconds / 3600000.0;
            return Math.round(kWh * costPerUnit * 100.0) / 100.0;
        }
    }

    // ===================== SIMULATION =====================
    static class SimulationResult {
        int people;
        double temp, humidity, acPower, secondaryACPower, humidifierPower, fanPower, heaterPower, totalPower;
        SimulationResult(int people, double temp, double humidity, double acPower, double secondaryACPower,
                         double humidifierPower, double fanPower, double heaterPower, double totalPower) {
            this.people = people;
            this.temp = temp;
            this.humidity = humidity;
            this.acPower = acPower;
            this.secondaryACPower = secondaryACPower;
            this.humidifierPower = humidifierPower;
            this.fanPower = fanPower;
            this.heaterPower = heaterPower;
            this.totalPower = totalPower;
        }
    }

    static class ThermostatSimulation {
        private final TemperatureSensor sensor;
        private final HumiditySensor humiditySensor;
        private final AirConditioner ac;
        private final SecondaryAirConditioner secondaryAC;
        private final Humidifier humidifier;
        private final SmartFan smartFan;
        private final Heater heater;
        private final ElectricityBill bill;
        private boolean windowOpen;
        private int totalRuntimeSeconds = 0;
        public ThermostatSimulation(double baseTemp, double baseHumidity, boolean windowOpen) {
            this.sensor = new TemperatureSensor(baseTemp);
            this.humiditySensor = new HumiditySensor(baseHumidity);
            this.ac = new AirConditioner();
            this.secondaryAC = new SecondaryAirConditioner();
            this.humidifier = new Humidifier();
            this.smartFan = new SmartFan();
            this.heater = new Heater();
            this.bill = new ElectricityBill();
            this.windowOpen = windowOpen;
        }
        public SimulationResult simulateStep() {
            Random rand = new Random();
            int people = rand.nextInt(21);
            double temp = sensor.getValue() + (people * 1);
            if (windowOpen) temp -= 1.5;
            if (temp > 45.0) temp = rand.nextInt(40);
            double humidity = humiditySensor.getValue() + (people * 0.1);
            double desiredTemp = ThermostatDashboard.desiredTempControl;
            double tempDiff = temp - desiredTemp;
            double acPower = 0.0, secondaryACPower = 0.0, heaterPower = 0.0;
            if (tempDiff > 0) {
                acPower = (people == 0) ? 0.0 : ac.calculatePower(temp);
                secondaryACPower = (people > 10) ? secondaryAC.calculatePower(temp) : 0.0;
            } else {
                heaterPower = heater.calculatePower(temp);
            }
            double humidifierPower = humidifier.calculatePower(humidity);
            double fanPower = (people >= 6) ? smartFan.calculatePower(temp) : 0.0;
            temp += heaterPower / 600.0;
            double totalPower = acPower + secondaryACPower + humidifierPower + fanPower + heaterPower;
            bill.addConsumption(totalPower, 3);
            totalRuntimeSeconds += 3;
            return new SimulationResult(people, temp, humidity, acPower, secondaryACPower, humidifierPower, fanPower, heaterPower, totalPower);
        }
        public double getTotalCost() { return bill.getTotalCost(); }
        public int getTotalRuntimeSeconds() { return totalRuntimeSeconds; }
    }

    // ===================== GUI =====================
    static class ThermostatDashboard extends JPanel {
        // Add a static field to track the running display
        public static ThermostatDisplay runningDisplay = null;
        // Value labels for live display
        private static JLabel tempValueLabel = new JLabel("Temp: -- °C");
        private static JLabel humidityValueLabel = new JLabel("Humidity: -- %");
        private static JLabel powerValueLabel = new JLabel("Power: -- W");
        private static JLabel peopleValueLabel = new JLabel("People: --");
        private static JLabel acPowerLabel = new JLabel("AC Power: -- W");
        private static JLabel heaterPowerLabel = new JLabel("Heater Power: -- W");
        private static JLabel fanPowerLabel = new JLabel("Fan Power: -- W");
        private static JLabel humidifierPowerLabel = new JLabel("Humidifier Power: -- W");
        private static JLabel billLabel = new JLabel("Bill: $0.00");
        private static JLabel warningLabel = new JLabel("");
        private static JToggleButton beepButton;
        private static boolean beepEnabled = false;

        // Add fields to store user selections
        private JComboBox<String> monthBox;
        private JCheckBox windowCheck;
        private JButton startButton;

        // Store simulation state
        private boolean simulationStarted = false;

        private static JButton stopButton;
        private static JButton resetButton; // Add this line near other button declarations

        public ThermostatDashboard() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(Color.BLACK);

            // Controls panel
            JPanel controlsPanel = new JPanel();
            controlsPanel.setLayout(new GridLayout(0, 1, 2, 2));
            controlsPanel.setBackground(Color.BLACK);

            // Month selection
            JLabel monthLabel = new JLabel("Select Month:");
            monthLabel.setForeground(Color.WHITE);
            controlsPanel.add(monthLabel);

            monthBox = new JComboBox<>(new String[]{
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
            });
            controlsPanel.add(monthBox);

            // Window open checkbox
            windowCheck = new JCheckBox("Window Open");
            windowCheck.setForeground(Color.WHITE);
            windowCheck.setBackground(Color.BLACK);
            windowCheck.setSelected(windowOpenControl);
            controlsPanel.add(windowCheck);

            // Start button
            startButton = new JButton("Start Simulation");
            controlsPanel.add(startButton);
            stopButton = new JButton("Stop Automation");
            controlsPanel.add(stopButton);

            resetButton = new JButton("Reset & Restart"); // Add reset button
            controlsPanel.add(resetButton);

            JLabel tempLabel = new JLabel("Desired Temp:");
            tempLabel.setForeground(Color.WHITE);
            controlsPanel.add(tempLabel);

            JSlider tempSlider = new JSlider(16, 30, (int) desiredTempControl);
            tempSlider.setMajorTickSpacing(2);
            tempSlider.setPaintTicks(true);
            tempSlider.setPaintLabels(true);
            tempSlider.setBackground(Color.BLACK);
            tempSlider.setForeground(Color.WHITE);
            controlsPanel.add(tempSlider);

            // Status labels (reduce font size for compactness)
            JLabel tempStatus = new JLabel("Temp: " + desiredTempControl + " °C");
            tempStatus.setForeground(Color.WHITE);
            tempStatus.setFont(new Font("Arial", Font.PLAIN, 13)); // Smaller font
            controlsPanel.add(tempStatus);

            // Add beep button
            beepButton = new JToggleButton("Alert Sound: OFF");
            beepButton.setBackground(Color.RED);
            beepButton.setForeground(Color.WHITE);
            beepButton.addActionListener(e -> {
                beepEnabled = beepButton.isSelected();
                beepButton.setText("Alert Sound: " + (beepEnabled ? "ON" : "OFF"));
                beepButton.setBackground(beepEnabled ? Color.GREEN : Color.RED);
            });
            controlsPanel.add(beepButton);

            // Listeners
            tempSlider.addChangeListener(e -> {
                desiredTempControl = tempSlider.getValue();
                tempStatus.setText("Temp: " + desiredTempControl + " °C");
            });

            add(controlsPanel);

            // Live value panel
            JPanel valuePanel = new JPanel();
            valuePanel.setLayout(new GridLayout(0, 1, 2, 2)); // Reduce vertical/horizontal gaps
            valuePanel.setBackground(Color.DARK_GRAY);
            for (JLabel label : new JLabel[]{
                    tempValueLabel, humidityValueLabel, powerValueLabel, peopleValueLabel,
                    acPowerLabel, heaterPowerLabel, fanPowerLabel, humidifierPowerLabel
            }) {
                label.setForeground(Color.WHITE);
                valuePanel.add(label);
            }
            // Add bill and warning labels to the panel
            billLabel.setForeground(Color.YELLOW);
            billLabel.setFont(new Font("Arial", Font.BOLD, 13)); // Slightly smaller font
            billLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            warningLabel.setForeground(Color.ORANGE);
            warningLabel.setFont(new Font("Arial", Font.BOLD, 13)); // Slightly smaller font
            warningLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            // Reduce or remove vertical struts for less spacing
            // valuePanel.add(Box.createVerticalStrut(10)); // Remove or comment out
            valuePanel.add(billLabel);
            // valuePanel.add(Box.createVerticalStrut(5));  // Remove or comment out
            valuePanel.add(warningLabel);

            add(valuePanel);

            setPreferredSize(new Dimension(220, 500)); // Slightly smaller panel

            // --- Reset Button Action ---
            resetButton.addActionListener(e -> {
                // Stop current simulation if running
                if (runningDisplay != null) {
                    runningDisplay.stopDisplay();
                    runningDisplay = null;
                }
                simulationStarted = false;
                startButton.setEnabled(true);

                // Clear graph data
                timeData.clear();
                tempData.clear();
                humidityData.clear();
                powerData.clear();

                // Optionally, reset live value labels
                tempValueLabel.setText("Temp: -- °C");
                humidityValueLabel.setText("Humidity: -- %");
                powerValueLabel.setText("Power: -- W");
                peopleValueLabel.setText("People: --");
                acPowerLabel.setText("AC Power: -- W");
                heaterPowerLabel.setText("Heater Power: -- W");
                fanPowerLabel.setText("Fan Power: -- W");
                humidifierPowerLabel.setText("Humidifier Power: -- W");

                // Start new simulation with current settings
                String selectedMonth = (String) monthBox.getSelectedItem();
                boolean windowOpen = windowCheck.isSelected();
                double[] base = getBaseTempHumidity(selectedMonth);
                final double[] baseTemp = {base[0]};
                final double[] baseHumidity = {base[1]};
                final boolean[] windowOpenHolder = {windowOpen};

                // Listen for changes to monthBox and windowCheck
                monthBox.addActionListener(ev -> {
                    String newMonth = (String) monthBox.getSelectedItem();
                    double[] newBase = getBaseTempHumidity(newMonth);
                    baseTemp[0] = newBase[0];
                    baseHumidity[0] = newBase[1];
                });
                windowCheck.addActionListener(ev -> {
                    windowOpenHolder[0] = windowCheck.isSelected();
                });

                new Thread(() -> runSimulationDynamic(baseTemp, baseHumidity, windowOpenHolder)).start();
                simulationStarted = true;
                startButton.setEnabled(false);
            });
        }

        public static boolean windowOpenControl = false;
        public static double desiredTempControl = 21.0;
        public static int simulationSpeedMs = 3000;
        private static final List<Integer> timeData = new ArrayList<>();
        private static final List<Double> tempData = new ArrayList<>();
        private static final List<Double> humidityData = new ArrayList<>();
        private static final List<Double> powerData = new ArrayList<>();

        public static void updateData(int time, double temp, double humidity, double power) {
            timeData.add(time);
            tempData.add(temp);
            humidityData.add(humidity);
            powerData.add(power / 1000.0); // Store power in kW for graph
        }

        // Call this from simulation to update the live values
        public static void updateLiveValues(double temp, double humidity, double power, int people,
                                            double acPower, double heaterPower, double fanPower, double humidifierPower,
                                            double bill, String warning) {
            SwingUtilities.invokeLater(() -> {
                tempValueLabel.setText("Temp: " + String.format("%.1f", temp) + " °C");
                humidityValueLabel.setText("Humidity: " + String.format("%.1f", humidity) + " %");
                powerValueLabel.setText("Power: " + String.format("%.2f", power / 1000.0) + " kW");
                peopleValueLabel.setText("People: " + people);
                acPowerLabel.setText("AC Power: " + String.format("%.2f", acPower / 1000.0) + " kW");
                heaterPowerLabel.setText("Heater Power: " + String.format("%.1f", heaterPower) + " W");
                fanPowerLabel.setText("Fan Power: " + String.format("%.1f", fanPower) + " W");
                humidifierPowerLabel.setText("Humidifier Power: " + String.format("%.1f", humidifierPower) + " W");
                billLabel.setText("Bill: $" + String.format("%.2f", bill));
                warningLabel.setText(warning);
                
                // Play beep if there's a warning and beep is enabled
                if (!warning.isEmpty() && beepEnabled) {
                    playBeep();
                }
            });
        }

        // Helper to get base temp/humidity for a month
        private static double[] getBaseTempHumidity(String month) {
            return switch (month.toLowerCase()) {
                case "january" -> new double[]{14, 65};
                case "february" -> new double[]{17, 60};
                case "march" -> new double[]{22, 50};
                case "april" -> new double[]{28, 35};
                case "may" -> new double[]{33, 38};
                case "june" -> new double[]{34, 58};
                case "july" -> new double[]{31, 75};
                case "august" -> new double[]{30, 80};
                case "september" -> new double[]{29, 75};
                case "october" -> new double[]{26, 60};
                case "november" -> new double[]{20, 55};
                case "december" -> new double[]{15, 65};
                default -> new double[]{22, 50};
            };
        }

        public static void launchDashboard() {
            SwingUtilities.invokeLater(() -> {
                JFrame frame = new JFrame("Thermostat Dashboard");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setSize(900, 600);
                frame.setLayout(new BorderLayout());

                ThermostatDashboard dashboardPanel = new ThermostatDashboard();
                dashboardPanel.setPreferredSize(new Dimension(250, 600));

                JPanel graphPanel = new JPanel() {
                    private static final int INITIAL_WIDTH = 800;
                    private static final int POINTS_BEFORE_EXPAND = 50;
                    private int currentWidth = INITIAL_WIDTH;

                    {
                        setBackground(Color.BLACK);
                    }

                    @Override
                    public Dimension getPreferredSize() {
                        return new Dimension(currentWidth, 500);
                    }

                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        Graphics2D g2 = (Graphics2D) g;
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                        int width = getWidth();
                        int height = getHeight();
                        int leftMargin = 60;
                        int rightMargin = 20;
                        int topMargin = 20;
                        int bottomMargin = 60;
                        int graphWidth = width - leftMargin - rightMargin;
                        int graphHeight = height - bottomMargin - topMargin;

                        // Check if we need to expand
                        if (!timeData.isEmpty() && timeData.size() > POINTS_BEFORE_EXPAND) {
                            int neededWidth = leftMargin + rightMargin + timeData.size() * 15; // 15 pixels per data point
                            if (neededWidth > currentWidth) {
                                currentWidth = neededWidth;
                                setPreferredSize(new Dimension(currentWidth, 500));
                                revalidate();
                            }
                        }

                        // Background
                        g2.setColor(Color.BLACK);
                        g2.fillRect(0, 0, width, height);

                        // Grid
                        g2.setColor(new Color(30, 30, 30));
                        for (int i = 0; i <= 10; i++) {
                            int y = topMargin + (i * graphHeight / 10);
                            g2.drawLine(leftMargin, y, width - rightMargin, y);
                        }
                        for (int i = 0; i <= width/50; i++) {
                            int x = leftMargin + (i * 50);
                            g2.drawLine(x, topMargin, x, height - bottomMargin);
                        }

                        // Axes
                        g2.setColor(Color.WHITE);
                        g2.setStroke(new BasicStroke(2));
                        g2.drawLine(leftMargin, height - bottomMargin, width - rightMargin, height - bottomMargin);
                        g2.drawLine(leftMargin, height - bottomMargin, leftMargin, topMargin);

                        // Axis labels
                        g2.drawString("Time (s)", width / 2, height - 20);
                        g2.drawString("Values (°C, %, kW)", 10, height / 2);

                        // Find max Y value
                        double maxY = 100.0;  // Fixed scale for better readability

                        // Draw data
                        if (!timeData.isEmpty()) {
                            drawDataLine(g2, timeData, tempData, Color.RED, maxY, graphWidth, graphHeight, leftMargin, topMargin);
                            drawDataLine(g2, timeData, humidityData, Color.BLUE, maxY, graphWidth, graphHeight, leftMargin, topMargin);
                            drawDataLine(g2, timeData, powerData, Color.GREEN, maxY, graphWidth, graphHeight, leftMargin, topMargin);
                        }

                        // Legend
                        drawLegend(g2);
                    }

                    private void drawLegend(Graphics2D g2) {
                        int x = 80;
                        int y = 30;
                        // Semi-transparent background
                        g2.setColor(new Color(0, 0, 0, 200));
                        g2.fillRect(x - 10, y - 20, 200, 80);

                        // Legend items
                        drawLegendItem(g2, x, y, Color.RED, "Temperature (°C)", 
                            tempData.isEmpty() ? 0 : tempData.get(tempData.size()-1));
                        drawLegendItem(g2, x, y + 20, Color.BLUE, "Humidity (%)", 
                            humidityData.isEmpty() ? 0 : humidityData.get(humidityData.size()-1));
                        drawLegendItem(g2, x, y + 40, Color.GREEN, "Power (kW)", 
                            powerData.isEmpty() ? 0 : powerData.get(powerData.size()-1));
                    }

                    private void drawLegendItem(Graphics2D g2, int x, int y, Color color, String label, double value) {
                        g2.setColor(color);
                        g2.fillRect(x, y, 10, 10);
                        g2.setColor(Color.WHITE);
                        g2.drawString(String.format("%s: %.1f", label, value), x + 15, y + 10);
                    }

                    private void drawDataLine(Graphics2D g2, List<Integer> timeData, List<Double> data, 
                                           Color color, double maxY, int graphWidth, int graphHeight, 
                                           int leftMargin, int topMargin) {
                        if (data.size() < 2) return;

                        g2.setColor(color);
                        g2.setStroke(new BasicStroke(2.0f));

                        int maxTime = timeData.get(timeData.size() - 1);
                        maxTime = Math.max(maxTime, 100);  // Minimum view width

                        for (int i = 1; i < data.size(); i++) {
                            double x1 = leftMargin + ((double)timeData.get(i-1) / maxTime * graphWidth);
                            double x2 = leftMargin + ((double)timeData.get(i) / maxTime * graphWidth);
                            double y1 = topMargin + graphHeight - (data.get(i-1) / maxY * graphHeight);
                            double y2 = topMargin + graphHeight - (data.get(i) / maxY * graphHeight);
                            
                            g2.drawLine((int)x1, (int)y1, (int)x2, (int)y2);
                        }
                    }
                };

                // Wrap in scroll pane
                JScrollPane scrollPane = new JScrollPane(graphPanel);
                scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
                scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
                scrollPane.setBackground(Color.BLACK);
                scrollPane.getViewport().setBackground(Color.BLACK);

                // Auto-scroll timer
                javax.swing.Timer scrollTimer = new javax.swing.Timer(1000, e -> {
                    JScrollBar scrollBar = scrollPane.getHorizontalScrollBar();
                    scrollBar.setValue(scrollBar.getMaximum());
                });
                scrollTimer.start();

                // Repaint timer
                javax.swing.Timer repaintTimer = new javax.swing.Timer(1000, e -> graphPanel.repaint());
                repaintTimer.start();

                frame.add(dashboardPanel, BorderLayout.EAST);
                frame.add(scrollPane, BorderLayout.CENTER);

                // --- Start Simulation Button Action ---
                dashboardPanel.startButton.addActionListener(e -> {
                    if (dashboardPanel.simulationStarted) return; // Prevent multiple starts
                    dashboardPanel.simulationStarted = true;

                    String selectedMonth = (String) dashboardPanel.monthBox.getSelectedItem();
                    boolean windowOpen = dashboardPanel.windowCheck.isSelected();
                    double[] base = getBaseTempHumidity(selectedMonth);

                    // Do NOT disable controls, so user can change them later
                    // dashboardPanel.monthBox.setEnabled(false);
                    // dashboardPanel.windowCheck.setEnabled(false);
                    dashboardPanel.startButton.setEnabled(false);

                    // Use a holder for baseTemp/baseHumidity and windowOpen, so they can be updated
                    final double[] baseTemp = {base[0]};
                    final double[] baseHumidity = {base[1]};
                    final boolean[] windowOpenHolder = {windowOpen};

                    // Listen for changes to monthBox and windowCheck
                    dashboardPanel.monthBox.addActionListener(ev -> {
                        String newMonth = (String) dashboardPanel.monthBox.getSelectedItem();
                        double[] newBase = getBaseTempHumidity(newMonth);
                        baseTemp[0] = newBase[0];
                        baseHumidity[0] = newBase[1];
                    });
                    dashboardPanel.windowCheck.addActionListener(ev -> {
                        windowOpenHolder[0] = dashboardPanel.windowCheck.isSelected();
                    });

                    new Thread(() -> runSimulationDynamic(baseTemp, baseHumidity, windowOpenHolder)).start();
                });

                ThermostatDashboard.stopButton.addActionListener(e -> {
                    if (runningDisplay != null) {
                        runningDisplay.stopDisplay();
                        runningDisplay = null;
                        dashboardPanel.simulationStarted = false;
                        dashboardPanel.startButton.setEnabled(true); // Allow restarting
                    }
                });

                frame.setVisible(true);
            });
        }

        // Main method, decluttered by extracting simulation components
        public static void main(String[] args) {
            ThermostatDashboard.launchDashboard();
        }

        // Helper to run the simulation, instantiating all components
        private static void runSimulation(double baseTemp, double baseHumidity, boolean windowOpen) {
            TemperatureSensor sensor = new TemperatureSensor(baseTemp);
            HumiditySensor humiditySensor = new HumiditySensor(baseHumidity);
            AirConditioner ac = new AirConditioner();
            SecondaryAirConditioner secondaryAC = new SecondaryAirConditioner();
            Humidifier humidifier = new Humidifier();
            SmartFan smartFan = new SmartFan();
            Heater heater = new Heater();
            ElectricityBill bill = new ElectricityBill();
            ThermostatDisplay display = new ThermostatDisplay(sensor, ac, secondaryAC, humiditySensor, humidifier, smartFan, heater, bill, windowOpen);
            display.startDisplay();
        }

        // Add this new simulation runner
        private static void runSimulationDynamic(double[] baseTemp, double[] baseHumidity, boolean[] windowOpenHolder) {
            TemperatureSensor sensor = new TemperatureSensor(baseTemp[0]);
            HumiditySensor humiditySensor = new HumiditySensor(baseHumidity[0]);
            AirConditioner ac = new AirConditioner();
            SecondaryAirConditioner secondaryAC = new SecondaryAirConditioner();
            Humidifier humidifier = new Humidifier();
            SmartFan smartFan = new SmartFan();
            Heater heater = new Heater();
            ElectricityBill bill = new ElectricityBill();

            ThermostatDisplay display = new ThermostatDisplay(sensor, ac, secondaryAC, humiditySensor, humidifier, smartFan, heater, bill, windowOpenHolder[0]) {
                private Timer timer;
                private int lastSpeed = ThermostatDashboard.simulationSpeedMs;
                private boolean windowOpen = windowOpenHolder[0]; // Initialize windowOpen as a field

                @Override
                public void startDisplay() {
                    timer = new Timer();
                    ThermostatDashboard.runningDisplay = this; // <-- Add this line
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            // If simulation speed changed, restart timer with new speed
                            if (lastSpeed != ThermostatDashboard.simulationSpeedMs) {
                                timer.cancel();
                                runSimulationDynamic(baseTemp, baseHumidity, windowOpenHolder);
                                return;
                            }
                            addToTotalRuntimeSeconds(ThermostatDashboard.simulationSpeedMs / 1000);
                            sensor.setValue(baseTemp[0]);
                            humiditySensor.setValue(baseHumidity[0]);
                            windowOpen = windowOpenHolder[0];
                            addToTotalRuntimeSeconds(ThermostatDashboard.simulationSpeedMs / 1000);
                            SimulationResult result = calculateSimulation();
                            bill.addConsumption(result.totalPower, ThermostatDashboard.simulationSpeedMs / 1000);
                            displayOutput(result);
                            logToFile(result);
                        }
                    }, 0, ThermostatDashboard.simulationSpeedMs);
                }

                @Override
                public void stopDisplay() {
                    if (timer != null) timer.cancel();
                }
            };
            display.startDisplay();
        }

        // Add beep method
        private static void playBeep() {
            if (!beepEnabled) return;
            Toolkit.getDefaultToolkit().beep();
        }
    }

    private static ThermostatDisplay runningDisplay = null;
    public void stopDisplay() {
        // Overridden in anonymous class
    }

    // ===================== DISPLAY =====================
    static class ThermostatDisplay {
        private final TemperatureSensor sensor;
        private final AirConditioner ac;
        private final SecondaryAirConditioner secondaryAC;
        private final HumiditySensor humiditySensor;
        private final Humidifier humidifier;
        private final SmartFan smartFan;
        private final Heater heater;
        private final ElectricityBill bill;
        private boolean windowOpen;
        private int totalRuntimeSeconds = 0;
        private Timer timer;

        public ThermostatDisplay(TemperatureSensor sensor, AirConditioner ac, SecondaryAirConditioner secondaryAC,
                                 HumiditySensor humiditySensor, Humidifier humidifier, SmartFan smartFan,
                                 Heater heater, ElectricityBill bill, boolean windowOpen) {
            this.sensor = sensor;
            this.ac = ac;
            this.secondaryAC = secondaryAC;
            this.humiditySensor = humiditySensor;
            this.humidifier = humidifier;
            this.smartFan = smartFan;
            this.heater = heater;
            this.bill = bill;
            this.windowOpen = windowOpen;
        }

        public void startDisplay() {
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    totalRuntimeSeconds += 3;
                    SimulationResult result = calculateSimulation();
                    bill.addConsumption(result.totalPower, 3);
                    displayOutput(result);
                    logToFile(result);
                }
            }, 0, 3000);
        }

        public void stopDisplay() {
            if (timer != null) timer.cancel();
        }

        public void addToTotalRuntimeSeconds(int seconds) {
            this.totalRuntimeSeconds += seconds;
        }

        public SimulationResult calculateSimulation() {
            Random rand = new Random();
            int people = rand.nextInt(21);
            double temp = sensor.getValue() + (people * 1);
            if (windowOpen) temp -= 1.5;
            if (temp > 45.0) temp = rand.nextInt(40);
            double humidity = humiditySensor.getValue() + (people * 0.1);
            double desiredTemp = ThermostatDashboard.desiredTempControl;
            double tempDiff = temp - desiredTemp;
            double acPower = 0.0, secondaryACPower = 0.0, heaterPower = 0.0;
            if (tempDiff > 0) {
                acPower = (people == 0) ? 0.0 : ac.calculatePower(temp);
                secondaryACPower = (people > 10) ? secondaryAC.calculatePower(temp) : 0.0;
            } else {
                heaterPower = heater.calculatePower(temp);
            }
            double humidifierPower = humidifier.calculatePower(humidity);
            double fanPower = (people >= 6) ? smartFan.calculatePower(temp) : 0.0;
            temp += heaterPower / 600.0;
            double totalPower = acPower + secondaryACPower + humidifierPower + fanPower + heaterPower;
            return new SimulationResult(people, temp, humidity, acPower, secondaryACPower, humidifierPower, fanPower, heaterPower, totalPower);
        }

        protected void displayOutput(SimulationResult result) {
            SwingUtilities.invokeLater(() -> {
                String warning = "";
                if (result.temp > ThermostatDashboard.desiredTempControl) {
                    warning = "🔥 ALERT: Room is overheating!";
                } else if (result.people == 0) {
                    warning = "⚠️ No one is in the room. AC is off.";
                } else if (result.totalPower > 2500) {
                    warning = "💡 Tip: Reduce room temp by 1°C to save ~7% energy.";
                }
                ThermostatDashboard.updateData(totalRuntimeSeconds, result.temp, result.humidity, result.totalPower);
                ThermostatDashboard.updateLiveValues(
                    result.temp, result.humidity, result.totalPower, result.people,
                    result.acPower, result.heaterPower, result.fanPower, result.humidifierPower,
                    bill.getTotalCost(), warning
                );
            });
        }

        protected void logToFile(SimulationResult result) {
            try {
                File logFile = new File("thermostat_log.txt");
                if (!logFile.exists()) {
                    logFile.createNewFile();
                }
                try (FileWriter fw = new FileWriter(logFile, true)) {
                    fw.write("People in room: " + result.people + "\n");
                    fw.write("Current Room Temperature: " + result.temp + " °C\n");
                    fw.write("AC Power Consumption: " + result.acPower + " W\n");
                    fw.write("Secondary AC Power Consumption: " + result.secondaryACPower + " W\n");
                    fw.write("Current Room Humidity: " + result.humidity + " %\n");
                    fw.write("Humidifier Power Consumption: " + result.humidifierPower + " W\n");
                    fw.write("Smart Fan Power Consumption: " + result.fanPower + " W\n");
                    fw.write("Heater Power Consumption: " + result.heaterPower + " W\n");
                    fw.write("Total Electricity Cost: $" + bill.getTotalCost() + "\n");
                    fw.write("----------------------------\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}


