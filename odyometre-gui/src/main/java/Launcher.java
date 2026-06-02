public class Launcher {
    public static void main(String[] args) {
        // JavaFX 11+ sürümlerinde Application sınıfından miras alan
        // ana sınıfı direkt çalıştırmak "runtime components missing" hatası verir.
        // Bu yüzden Application'dan miras ALMAYAN bu Launcher sınıfı üzerinden çalıştırılır.
        GUI.main(args);
    }
}
