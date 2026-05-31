import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

public class SerialManager {
    
    private SerialPort serialPort;
    private Runnable responseCallback;

    public void setResponseCallback(Runnable callback) {
        this.responseCallback = callback;
    }

    // Portu (orn: "COM3" veya "/dev/ttyUSB0") standart hizda (9600) acar
    public boolean connect(String portName) {
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
        }

        serialPort = SerialPort.getCommPort(portName);
        serialPort.setBaudRate(9600);
        serialPort.setNumDataBits(8);
        serialPort.setNumStopBits(1);
        serialPort.setParity(SerialPort.NO_PARITY);

        if (serialPort.openPort()) {
            System.out.println("[OK] Port acildi: " + portName);
            startListening();
            return true;
        } else {
            System.err.println("[HATA] Port acilamadi!");
            return false;
        }
    }

    //Proteusa cikti gonderir ornek : "F1000:D40\n"
    public void sendCommand(int frequency, int decibel) {
        if (serialPort != null && serialPort.isOpen()) {
            String command = "F" + frequency + ":D" + decibel + "\n";
            byte[] buffer = command.getBytes();
            serialPort.writeBytes(buffer, buffer.length);
            System.out.println("[Gonderildi] -> " + command.trim());
        } else {
            System.err.println("[HATA] Port acik degil!");
        }
    }

    // Arka planda donanimi dineyip RESPONSE bekler
    private void startListening() {
        serialPort.addDataListener(new SerialPortDataListener() {
            private String incomingBuffer = "";

            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
            }

            @Override
            public void serialEvent(SerialPortEvent event) {
                if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE) return;

                byte[] newData = new byte[serialPort.bytesAvailable()];
                int numRead = serialPort.readBytes(newData, newData.length);
                incomingBuffer += new String(newData, 0, numRead);

                // Satir sonu (\n) gelene kadar bekle ve gelen mesaji ayikla
                while (incomingBuffer.contains("\n")) {
                    int newlineIndex = incomingBuffer.indexOf("\n");
                    String message = incomingBuffer.substring(0, newlineIndex).trim();
                    incomingBuffer = incomingBuffer.substring(newlineIndex + 1);

                    if (message.equals("RESPONSE")) {
                        System.out.println("[YAKALANDI] RESPONSE geldi.");
                        if (responseCallback != null) {
                            responseCallback.run();
                        }
                    }
                }
            }
        });
    }

    // Uygulama kapanirken portu guvenlice serbest birakir
    public void disconnect() {
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.removeDataListener();
            serialPort.closePort();
            System.out.println("[OK] Port kapatildi.");
        }
    }
}