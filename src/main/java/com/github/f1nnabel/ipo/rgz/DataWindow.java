package com.github.f1nnabel.ipo.rgz;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DataWindow extends JFrame implements RocketObserver {
    private List<Double> timeData = new ArrayList<>();
    private List<Double> speedData = new ArrayList<>();
    private List<Double> altitudeData = new ArrayList<>();
    private List<Double> massData = new ArrayList<>();

    private double time = 0;

    private GraphPanel speedPanel;
    private GraphPanel altitudePanel;
    private GraphPanel massPanel;

    private double maxTime = 0;
    private double maxSpeed = 0;
    private double maxAltitude = 0;
    private double maxMass = 0;

    private RocketController controller;

    public DataWindow(RocketController controller) {
        this.controller = controller;
        setTitle("Данные");
        setSize(900, 300);
        setLayout(new GridLayout(1, 3));

        timeData = controller.getModel().getTimeData();
        speedData = controller.getModel().getSpeedData();
        altitudeData = controller.getModel().getAltitudeData();
        massData = controller.getModel().getMassData();

        updateMaxValues();

        speedPanel = new GraphPanel(timeData, speedData, "Скорость", "Время (с)", "Скорость (м/с)", maxTime, maxSpeed);
        altitudePanel = new GraphPanel(timeData, altitudeData, "Высота", "Время (с)", "Высота (м)", maxTime, maxAltitude);
        massPanel = new GraphPanel(timeData, massData, "Масса", "Время (с)", "Масса (кг)", maxTime, maxMass);

        add(speedPanel);
        add(altitudePanel);
        add(massPanel);

        controller.getModel().addObserver(this);

        setVisible(true);
    }



    @Override
    public void onUpdateStatus(double currentMass, double speed, double altitude, double horizontalDistance,
                               int remainingStages, double[] fuelMasses, double[] initialFuelMasses) {
        SwingUtilities.invokeLater(() -> {
            timeData = controller.getModel().getTimeData();
            speedData = controller.getModel().getSpeedData();
            altitudeData = controller.getModel().getAltitudeData();
            massData = controller.getModel().getMassData();

            updateMaxValues();

            speedPanel.updateData(timeData, speedData, maxTime, maxSpeed);
            altitudePanel.updateData(timeData, altitudeData, maxTime, maxAltitude);
            massPanel.updateData(timeData, massData, maxTime, maxMass);


            controller.saveSimulationState(new File("rocket_simulation_state.json"));
            speedPanel.repaint();
            altitudePanel.repaint();
            massPanel.repaint();
        });
    }


    @Override
    public void onStageSeparation(int stageNumber) {
    }

    public void resetData() {
        timeData.clear();
        speedData.clear();
        altitudeData.clear();
        massData.clear();
        time = 0;
        maxTime = 0;
        maxSpeed = 0;
        maxAltitude = 0;
        maxMass = 0;
    }
    private void updateMaxValues() {
        maxTime = controller.getModel().getTotalTime();
        maxSpeed = controller.getModel().getSpeedData().stream().max(Double::compare).orElse(0.0);
        maxAltitude = controller.getModel().getAltitudeData().stream().max(Double::compare).orElse(0.0);
        maxMass = controller.getModel().getMassData().stream().max(Double::compare).orElse(0.0);
    }
    private class GraphPanel extends JPanel {
        private List<Double> xData;
        private List<Double> yData;
        private String title;
        private String xLabel;
        private String yLabel;

        private double maxX;
        private double maxY;
        public GraphPanel(List<Double> xData, List<Double> yData, String title, String xLabel, String yLabel, double maxX, double maxY) {
            this.xData = xData;
            this.yData = yData;
            this.title = title;
            this.xLabel = xLabel;
            this.yLabel = yLabel;
            this.maxX = maxX;
            this.maxY = maxY;
        }



        public void updateData(List<Double> xData, List<Double> yData, double maxX, double maxY) {
            this.xData = xData;
            this.yData = yData;
            this.maxX = maxX;
            this.maxY = maxY;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (xData.isEmpty() || yData.isEmpty()) return;

            Graphics2D g2 = (Graphics2D) g;

            int padding = 40;
            int labelPadding = 20;

            int width = getWidth();
            int height = getHeight();

            double xMax = maxTime;
            double yMax = 0;
            if (title.equals("Скорость")) {
                yMax = maxSpeed;
            } else if (title.equals("Высота")) {
                yMax = maxAltitude;
            } else if (title.equals("Масса")) {
                yMax = maxMass;
            }

            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, width, height);

            g2.setColor(Color.BLACK);

            g2.drawLine(padding, height - padding, padding, padding);
            g2.drawLine(padding, height - padding, width - padding, height - padding);

            g2.drawString(xLabel, width / 2, height - labelPadding);
            g2.drawString(yLabel, labelPadding, height / 2);

            g2.drawString(title, width / 2 - g2.getFontMetrics().stringWidth(title) / 2, labelPadding);

            double xScale = (width - 2 * padding) / xMax;
            double yScale = (height - 2 * padding) / yMax;

            g2.setColor(Color.LIGHT_GRAY);
            for (int i = 0; i < 10; i++) {
                int x = padding + i * (width - 2 * padding) / 10;
                g2.drawLine(x, height - padding, x, padding);
                int y = padding + i * (height - 2 * padding) / 10;
                g2.drawLine(padding, y, width - padding, y);
            }

            g2.setColor(Color.BLUE);

            if (xData.size() > 1) {
                Path2D.Double path = new Path2D.Double();
                for (int i = 0; i < xData.size(); i++) {
                    double x = xData.get(i) / maxX * (getWidth() - padding * 2) + padding;
                    double y = getHeight() - padding - (yData.get(i) / maxY * (getHeight() - padding * 2));
                    if (i == 0) {
                        path.moveTo(x, y);
                    } else {
                        path.lineTo(x, y);
                    }
                }
                g2.setColor(Color.RED);
                g2.draw(path);
            }

        }
    }
}
