package EDSxADS_Final_Project;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.JPanel;

public class PieChartPanel extends JPanel {

    private String[] labels;
    private double[] values;  
    private Color[] colors;

    // Default colors for each department slot
    private static final Color[] DEFAULT_COLORS = {
        new Color(70, 130, 180),  
        new Color(60, 179, 113),   
        new Color(255, 165, 0),    
        new Color(147, 112, 219),  
        new Color(220, 20, 60),    
        new Color(255, 215, 0)     
    };

    public PieChartPanel(String[] labels, double[] values, Color[] colors) {
        this.labels = labels;
        this.values = values;
        this.colors = colors;
        setOpaque(false);
    }

    public PieChartPanel() {
        loadFromDatabase();
        setOpaque(false);
    }

    private void loadFromDatabase() {
        String[] deptNames  = {"IT", "Engineering", "Business", "Education", "Medical", "Law"};
        double[] counts     = new double[deptNames.length];

        String sql = "SELECT Department, COUNT(*) AS Total "
                   + "FROM Students GROUP BY Department";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                String dept  = rs.getString("Department");
                int    total = rs.getInt("Total");

                for (int i = 0; i < deptNames.length; i++) {
                    if (deptNames[i].equals(dept)) {
                        counts[i] = total;
                        break;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        this.labels = deptNames;
        this.values = counts;
        this.colors = DEFAULT_COLORS;
    }

    public void refresh() {
        loadFromDatabase();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // --- compute total (skip zero-count departments) ---
        double total = 0;
        for (double v : values) total += v;

        if (total == 0) {
            // Nothing in DB yet – draw a placeholder message
            g2.setColor(Color.GRAY);
            g2.setFont(new Font("Arial", Font.ITALIC, 13));
            g2.drawString("No department data yet.", 20, getHeight() / 2);
            return;
        }

        // --- draw pie slices ---
        int arcX   = 10;
        int arcY   = 30;
        int size   = Math.min(getWidth() - 180, getHeight() - 60); // leave room for legend
        int startAngle = 90; // start from top for a natural look

        for (int i = 0; i < values.length; i++) {
            if (values[i] == 0) continue;

            int sweepAngle = (int) Math.round(values[i] / total * 360);

            g2.setColor(colors[i]);
            g2.fillArc(arcX, arcY, size, size, startAngle, sweepAngle);

            // Thin white border between slices
            g2.setColor(Color.WHITE);
            g2.drawArc(arcX, arcY, size, size, startAngle, sweepAngle);

            startAngle += sweepAngle;
        }

        // --- draw legend (right side) ---
        int legendX = size + 30;
        int legendY = 40;

        g2.setFont(new Font("Arial", Font.BOLD, 12));
        g2.setColor(Color.DARK_GRAY);
        g2.drawString("Departments", legendX, legendY - 10);

        g2.setFont(new Font("Arial", Font.PLAIN, 12));

        for (int i = 0; i < labels.length; i++) {
            if (values[i] == 0) continue;

            double pct = values[i] / total * 100.0;

            // color swatch
            g2.setColor(colors[i]);
            g2.fillRoundRect(legendX, legendY, 14, 14, 4, 4);

            // label:  "IT (3 – 50%)"
            g2.setColor(Color.DARK_GRAY);
            String entry = labels[i] + " (" + (int) values[i] + " – " + String.format("%.1f", pct) + "%)";
            g2.drawString(entry, legendX + 20, legendY + 12);

            legendY += 24;
        }

        // --- draw title inside panel ---
        g2.setFont(new Font("Arial", Font.BOLD, 13));
        g2.setColor(new Color(34, 139, 34));
        g2.drawString("Students by Department", arcX, arcY - 8);
    }
}