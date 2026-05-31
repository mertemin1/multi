import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import java.util.Map;

/**
 * Custom JavaFX Canvas component to plot audiogram threshold values.
 * Complies with standard audiogram representations (Right ear: Red O, Left ear: Blue X).
 */
public class AudiogramGraph extends Canvas {

    // Standard audiometric test frequencies in Hz
    private final int[] frequencies = {250, 500, 1000, 2000, 4000, 8000};

    // Y-axis limits (Intensity in dB HL)
    private final int minDB = -10;
    private final int maxDB = 120;

    // Padding to ensure labels fit within the canvas
    private final double padding = 40.0;

    public AudiogramGraph(double width, double height) {
        super(width, height);
        drawGridAndAxes();
    }

    /**
     * Main API method for the GUI team to call when new data is received.
     * It clears the canvas, redraws the grid, and plots both ears.
     * * @param rightEarData Map containing frequency-dB pairs for the right ear.
     * @param leftEarData  Map containing frequency-dB pairs for the left ear.
     */
    public void updateAudiogram(Map<Integer, Integer> rightEarData, Map<Integer, Integer> leftEarData) {
        // 1. Clear everything and redraw the base grid
        drawGridAndAxes();

        // 2. Plot the data points
        if (rightEarData != null && !rightEarData.isEmpty()) {
            plotRightEar(rightEarData);
        }
        if (leftEarData != null && !leftEarData.isEmpty()) {
            plotLeftEar(leftEarData);
        }
    }

    private void drawGridAndAxes() {
        GraphicsContext gc = this.getGraphicsContext2D();
        double width = this.getWidth();
        double height = this.getHeight();

        // Clear the entire canvas
        gc.clearRect(0, 0, width, height);

        double graphWidth = width - (2 * padding);
        double graphHeight = height - (2 * padding);

        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(1.0);
        gc.setFont(new Font("Arial", 12));

        // Draw Y-Axis (Decibels - Horizontal Lines) from -10 to 120 dB
        for (int db = minDB; db <= maxDB; db += 10) {
            double y = padding + ((db - minDB) / (double)(maxDB - minDB)) * graphHeight;
            gc.strokeLine(padding, y, width - padding, y);

            gc.setFill(Color.BLACK);
            gc.fillText(db + " dB", padding - 35, y + 4);
        }

        // Draw X-Axis (Frequencies - Vertical Lines)
        double xStep = graphWidth / (frequencies.length - 1);
        for (int i = 0; i < frequencies.length; i++) {
            double x = padding + (i * xStep);
            gc.strokeLine(x, padding, x, height - padding);

            gc.setFill(Color.BLACK);
            gc.fillText(frequencies[i] + " Hz", x - 15, padding - 10);
        }
    }

    private void plotRightEar(Map<Integer, Integer> dataPoints) {
        GraphicsContext gc = this.getGraphicsContext2D();
        gc.setStroke(Color.RED);
        gc.setLineWidth(2.0);

        double prevX = -1, prevY = -1;

        for (int i = 0; i < frequencies.length; i++) {
            int freq = frequencies[i];
            if (dataPoints.containsKey(freq)) {
                int db = dataPoints.get(freq);
                double[] coords = calculateCoordinates(i, db);

                // Draw Red 'O' for Right Ear
                gc.strokeOval(coords[0] - 5, coords[1] - 5, 10, 10);

                if (prevX != -1) {
                    gc.strokeLine(prevX, prevY, coords[0], coords[1]);
                }
                prevX = coords[0];
                prevY = coords[1];
            }
        }
    }

    private void plotLeftEar(Map<Integer, Integer> dataPoints) {
        GraphicsContext gc = this.getGraphicsContext2D();
        gc.setStroke(Color.BLUE);
        gc.setLineWidth(2.0);

        double prevX = -1, prevY = -1;

        for (int i = 0; i < frequencies.length; i++) {
            int freq = frequencies[i];
            if (dataPoints.containsKey(freq)) {
                int db = dataPoints.get(freq);
                double[] coords = calculateCoordinates(i, db);

                // Draw Blue 'X' for Left Ear
                gc.strokeLine(coords[0] - 5, coords[1] - 5, coords[0] + 5, coords[1] + 5);
                gc.strokeLine(coords[0] - 5, coords[1] + 5, coords[0] + 5, coords[1] - 5);

                if (prevX != -1) {
                    gc.strokeLine(prevX, prevY, coords[0], coords[1]);
                }
                prevX = coords[0];
                prevY = coords[1];
            }
        }
    }

    private double[] calculateCoordinates(int freqIndex, int db) {
        double graphWidth = getWidth() - (2 * padding);
        double graphHeight = getHeight() - (2 * padding);

        double xStep = graphWidth / (frequencies.length - 1);
        double x = padding + (freqIndex * xStep);
        double y = padding + ((db - minDB) / (double)(maxDB - minDB)) * graphHeight;

        return new double[]{x, y};
    }
}