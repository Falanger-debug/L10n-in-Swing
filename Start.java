// Autor: Łukasz Lizak

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.TreeMap;

public class Start extends JFrame {
    private JButton readButton = new JButton("ReadFile");
    public JLabel a = new JLabel("a = ");
    public JLabel b = new JLabel("b = ");
    private final TreeMap<Double, Double> points = new TreeMap<>();
    private final PlotPanel plotPanel = new PlotPanel(0, 0, new TreeMap<>());

    public Start() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            e.printStackTrace();
        }

        Locale selectedLocale = selectLanguage();

        ResourceBundle messages = ResourceBundle.getBundle("myProp", selectedLocale);

        // aktualizacja przycisku na podstawie języka
        readButton = new JButton(messages.getString("read"));
        readButton.setBackground(Color.BLUE);
        readButton.setForeground(Color.WHITE);
        readButton.setFont(new Font("Arial", Font.BOLD, 14));


        JTextArea welcomeTextArea = new JTextArea(messages.getString("welcomeMessage"));
        welcomeTextArea.setLineWrap(true);
        welcomeTextArea.setWrapStyleWord(false);
        welcomeTextArea.setEditable(false);
        welcomeTextArea.setOpaque(false);
        welcomeTextArea.setPreferredSize(new Dimension(400, 85));

        JOptionPane.showMessageDialog(
                this,
                welcomeTextArea,
                messages.getString("welcomeTitle"),
                JOptionPane.PLAIN_MESSAGE
        );

        setSize(700, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        class Dzialaj implements Runnable {
            @Override
            public void run() {
                readButton.setEnabled(false); // wyłączam przycisk na chwile


                // wczytywanie z pliku i wyświetlenie okna wczytania uzytkownikowi
                JFileChooser fileChooser = new JFileChooser();
                int result = fileChooser.showOpenDialog(null);

                if (result == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();

                    // wyczyszczenie poprzedniego zbioru punktów
                    points.clear();

                    try {
                        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
                        int sizeOfData = Integer.parseInt(bufferedReader.readLine());

                        // wczytujemy linia po linii rozdzielając x oraz y
                        for (int i = 0; i < sizeOfData; i++) {
                            String line = bufferedReader.readLine();
                            String[] parts = line.split("\\s+"); // po spacji rozdzielamy linie
                            double x = Double.parseDouble(parts[0]);
                            double y = Double.parseDouble(parts[1]);

                            // dodaje punkty do drzewa przechowującego punkty(od razu posortowane)
                            points.put(x, y);
                        }

                        // obliczanie współczynników a i b
                        double sum_x2 = 0;
                        double sum_x = 0;
                        double sum_y = 0;
                        double sum_xy = 0;

                        double current = points.firstKey();
                        for (int i = 0; i < sizeOfData; i++) {
                            sum_x2 += current * current;
                            sum_x += current;
                            sum_y += points.get(current);
                            sum_xy += current * points.get(current);

                            if (i != sizeOfData - 1)
                                current = points.higherKey(current);
                        }
                        double W = (sizeOfData * sum_x2) - (sum_x * sum_x);
                        double a = (sum_x2 * sum_y - sum_x * sum_xy) / W;
                        double b = (sizeOfData * sum_xy - sum_x * sum_y) / W;

                        // zmiana pól w klasie Start
                        getA().setText("a = " + a);
                        getB().setText("b = " + b);

                        // zamknięcie bufora czytania
                        bufferedReader.close();

                        // rysowanie wykresu
                        plotPanel.updateData(a, b, points);
                        repaint(); // przerysowanie od nowa
                        revalidate(); // odświeżenie widoku

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                readButton.setEnabled(true); // włączam przycisk spowrotem
            }
        }

        JPanel a_b_Panel = new JPanel(new GridBagLayout()); // panel potrzebny do ustawienia a i b oraz przycisku
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.insets = new Insets(0, 10, 0, 10);  // Marginesy dla każdego komponentu

        // ustawianie miejsca w panelu dla przycisku oraz a i b
        gbc.weightx = 1.0; // ustawienie skalowania się elementów

        gbc.gridx = 0;
        gbc.gridy = 0;
        a_b_Panel.add(readButton, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 1;
        a_b_Panel.add(a, gbc);

        gbc.gridx = 2;
        gbc.gridwidth = 1;
        a_b_Panel.add(b, gbc);

        add(a_b_Panel, BorderLayout.SOUTH);

        readButton.addActionListener(e -> new Thread(new Dzialaj()).start());

        add(plotPanel, BorderLayout.CENTER);

        setLocationRelativeTo(null);
        setVisible(true);
    }


    private class PlotPanel extends JPanel {
        private double a;
        private double b;
        private TreeMap<Double, Double> dataPoints;

        public PlotPanel(double aa, double bb, TreeMap<Double, Double> points) {
            this.a = aa;
            this.b = bb;
            this.dataPoints = points;
        }

        public void updateData(double aa, double bb, TreeMap<Double, Double> points) {
            this.a = aa;
            this.b = bb;
            this.dataPoints = points;
        }


        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);

            if (dataPoints == null || dataPoints.isEmpty())
                return;

            int panelWidth = getWidth();
            int panelHeight = getHeight();


            // znalezienie skrajnych wartosci punktów aby móc narysować odpowiednio osie wykresu
            double minX = points.firstKey();
            double maxX = points.lastKey();
            double maxY = points.get(points.firstKey());
            double minY = points.get(points.firstKey());

            for (Double y : points.values()) {
                if (y > maxY)
                    maxY = y;
                if (y < minY)
                    minY = y;
            }

            double margines = 20;
            double wysokosc_ox = 0;
            double szerokosc_oy = 0;

            // obliczanie dlugosci przedziałów
            double x_len = 0;
            double y_len = 0;

            if (minX <= 0 && maxX >= 0)
                x_len = maxX - minX;
            else if (minX >= 0)
                x_len = maxX;
            else if (maxX <= 0)
                x_len = -minX;

            if (minY <= 0 && maxY >= 0)
                y_len = maxY - minY;
            else if (minY >= 0)
                y_len = maxY;
            else if (maxY <= 0)
                y_len = -minY;

            int wysokosc_dla_rysowania_punktow = panelHeight - 3 * (int) margines;
            int szerokosc_dla_rysowania_punktow = panelWidth - 3 * (int) margines;

            double new_max_length = Math.max(x_len, y_len);

            double x_unit = szerokosc_dla_rysowania_punktow / new_max_length;
            double y_unit = wysokosc_dla_rysowania_punktow / new_max_length;


            // ustawianie osi X oraz Y
            if (maxX > 0 && minX < 0)
                szerokosc_oy = 1.5 * margines + (-minX / new_max_length) * szerokosc_dla_rysowania_punktow;
            else if (minX >= 0)
                szerokosc_oy = 1.5 * margines;
            else if (maxX <= 0)
                szerokosc_oy = 1.5 * margines + szerokosc_dla_rysowania_punktow;

            if (maxY > 0 && minY < 0)
                wysokosc_ox = 1.5 * margines + (maxY / new_max_length) * wysokosc_dla_rysowania_punktow;
            else if (minY >= 0)
                wysokosc_ox = 1.5 * margines + wysokosc_dla_rysowania_punktow;
            else if (maxY <= 0)
                wysokosc_ox = 1.5 * margines;

            drawArrowLine(graphics, (int) margines, (int) (wysokosc_ox), panelWidth - (int) margines, (int) (wysokosc_ox), 13);
            drawArrowLine(graphics, (int) (szerokosc_oy), panelHeight - (int) margines, (int) (szerokosc_oy), (int) margines, 13);


            // rysowanie punktów
            int r = 7;
            int x_center = (int) (szerokosc_oy) - r;
            int y_center = (int) (wysokosc_ox) - r;

            for (Double x : points.keySet()) {
                double y = points.get(x);

                x = x_center + x * x_unit;
                y = y_center - y * y_unit;

                int x_int = (int) Math.round(x);
                int y_int = (int) Math.round(y);

                graphics.setColor(Color.GREEN);
                graphics.fillOval(x_int, y_int, 2 * r, 2 * r);
            }

            // oblczanie skrajnych x i y dla wykresu (double)
            double skrajnie_maly_x = 0;
            double skrajnie_maly_y = 0;
            double skrajnie_duzy_x = 0;
            double skrajnie_duzy_y = 0;

            while ((int) (x_center + skrajnie_duzy_x * x_unit) < (int) (panelWidth - 1.5 * margines) - r)
                skrajnie_duzy_x += 0.002;

            while ((int) (x_center + skrajnie_maly_x * x_unit) > (int) (1.5 * margines) - r)
                skrajnie_maly_x -= 0.002;

            while ((int) (y_center - skrajnie_duzy_y * y_unit) > (int) (1.5 * margines) - r)
                skrajnie_duzy_y += 0.002;

            while ((int) (y_center - skrajnie_maly_y * y_unit) < (int) (panelHeight - 1.5 * margines) - r)
                skrajnie_maly_y -= 0.002;


            // rysowanie wykresu funkcji regresji liniowej
            ArrayList<Integer> x_regression = new ArrayList<>();
            ArrayList<Integer> y_regression = new ArrayList<>();

            double x_przeciecie_dolne = (skrajnie_maly_y - a) / b;
            double x_przeciecie_gorne = (skrajnie_duzy_y - a) / b;
            double y_przeciecie_lewe = a + b * skrajnie_maly_x;
            double y_przeciecie_prawe = a + b * skrajnie_duzy_x;

            if (Double.isFinite(x_przeciecie_dolne)) {
                if (x_przeciecie_dolne >= skrajnie_maly_x - 0.1 && x_przeciecie_dolne <= skrajnie_duzy_x + 0.1) {
                    x_regression.add((int) (x_center + x_przeciecie_dolne * x_unit));
                    y_regression.add((int) (y_center - (a + b * x_przeciecie_dolne) * y_unit));
                }
            }

            if (Double.isFinite(x_przeciecie_gorne)) {
                if (x_przeciecie_gorne >= skrajnie_maly_x - 0.1 && x_przeciecie_gorne <= skrajnie_duzy_x + 0.1) {
                    x_regression.add((int) (x_center + x_przeciecie_gorne * x_unit));
                    y_regression.add((int) (y_center - (a + b * x_przeciecie_gorne) * y_unit));
                }
            }

            if (Double.isFinite(y_przeciecie_lewe)) {
                if (y_przeciecie_lewe >= skrajnie_maly_y - 0.1 && y_przeciecie_lewe <= skrajnie_duzy_y + 0.1) {
                    if (x_regression.size() < 2)
                        x_regression.add((int) (x_center + skrajnie_maly_x * x_unit));
                    if (y_regression.size() < 2)
                        y_regression.add((int) (y_center - y_przeciecie_lewe * y_unit));
                }
            }

            if (Double.isFinite(y_przeciecie_prawe)) {
                if (y_przeciecie_prawe >= skrajnie_maly_y - 0.1 && y_przeciecie_prawe <= skrajnie_duzy_y + 0.1) {
                    if (x_regression.size() < 2)
                        x_regression.add((int) (x_center + skrajnie_duzy_x * x_unit));
                    if (y_regression.size() < 2)
                        y_regression.add((int) (y_center - y_przeciecie_prawe * y_unit));
                }
            }
            graphics.setColor(Color.BLUE);
            graphics.drawLine(x_regression.get(0) + r, y_regression.get(0) + r, x_regression.get(1) + r, y_regression.get(1) + r);
        }

        private void drawArrowLine(Graphics g, int x1, int y1, int x2, int y2, int arrowSize) {
            Graphics2D g2 = (Graphics2D) g;
            g2.drawLine(x1, y1, x2, y2);

            double angle = Math.atan2(y2 - y1, x2 - x1);
            int arrowX = (int) (x2 - arrowSize * Math.cos(angle - Math.PI / 6));
            int arrowY = (int) (y2 - arrowSize * Math.sin(angle - Math.PI / 6));

            g2.drawLine(x2, y2, arrowX, arrowY);

            arrowX = (int) (x2 - arrowSize * Math.cos(angle + Math.PI / 6));
            arrowY = (int) (y2 - arrowSize * Math.sin(angle + Math.PI / 6));

            g2.drawLine(x2, y2, arrowX, arrowY);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Start::new);
    }

    public JLabel getA() {
        return a;
    }

    public JLabel getB() {
        return b;
    }

    private Locale selectLanguage() {
        ImageIcon polishIcon = resizeIcon(new ImageIcon("./out/images/polish.png"), 32, 32);
        ImageIcon englishIcon = resizeIcon(new ImageIcon("./out/images/english.png"), 32, 32);
        ImageIcon franceIcon = resizeIcon(new ImageIcon("./out/images/france.png"), 32, 32);

        Object[] options = {polishIcon, englishIcon, franceIcon};

        int choice = JOptionPane.showOptionDialog(
                this,
                "Wybierz język / Select Language / Sélectionner La Langue",
                "Language Selection",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                options,
                options[0]);

        if (choice == 0) {
            return new Locale.Builder().setLanguage("pl").build();
        } else if (choice == 2) {
            return new Locale.Builder().setLanguage("fr").build();
        } else {
            return new Locale.Builder().setLanguage("en").build();
        }
    }


    private ImageIcon resizeIcon(ImageIcon icon, int width, int height){
        Image img = icon.getImage();
        Image newImg = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(newImg);
    }
}