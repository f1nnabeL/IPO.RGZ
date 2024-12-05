package com.github.f1nnabel.ipo.rgz;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.io.*;

public class MainWindow extends JFrame implements RocketObserver {
    private RocketController controller;

    private JButton settingsButton;
    private JButton dataButton;

    private JButton startStopButton;
    private JLabel statusLabel;

    private JProgressBar[] fuelBars;
    private JLabel[] fuelLabels;

    private boolean isSimulating = false;

    private List<JFrame> childWindows = new ArrayList<>();
    private JButton resetButton;
    private JButton saveSettingsButton;
    private JButton loadSettingsButton;
    private JButton saveSimulationButton;
    private JButton loadSimulationButton;

    public MainWindow(RocketController controller) {
        this.controller = controller;
        controller.getModel().addObserver(this);
        initUI();
    }

    private void initUI() {
        setTitle("Симуляция Ракеты");
        setSize(400, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        JPanel firstRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        settingsButton = new JButton("Настройки");
        dataButton = new JButton("Данные");
        firstRow.add(settingsButton);
        firstRow.add(dataButton);
        mainPanel.add(firstRow);

        JPanel secondRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        saveSimulationButton = new JButton("Сохранить симуляцию");
        loadSimulationButton = new JButton("Загрузить симуляцию");
        secondRow.add(saveSimulationButton);
        secondRow.add(loadSimulationButton);
        mainPanel.add(secondRow);

        JPanel indicatorsPanel = new JPanel();
        indicatorsPanel.setLayout(new BoxLayout(indicatorsPanel, BoxLayout.Y_AXIS));
        indicatorsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        fuelBars = new JProgressBar[3];
        fuelLabels = new JLabel[3];
        for (int i = 0; i < 3; i++) {
            JPanel stagePanel = new JPanel(new BorderLayout());
            fuelLabels[i] = new JLabel("Ступень " + (i + 1));
            fuelBars[i] = new JProgressBar(0, 100);
            fuelBars[i].setValue(100);
            fuelBars[i].setForeground(Color.GREEN);

            stagePanel.add(fuelLabels[i], BorderLayout.NORTH);
            stagePanel.add(fuelBars[i], BorderLayout.CENTER);
            indicatorsPanel.add(stagePanel);
            indicatorsPanel.add(Box.createVerticalStrut(5));
        }
        mainPanel.add(indicatorsPanel);

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusLabel = new JLabel("Готово к запуску");
        statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
        statusPanel.add(statusLabel);
        mainPanel.add(statusPanel);

        JPanel lastRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        startStopButton = new JButton("Начать симуляцию");
        resetButton = new JButton("Сбросить симуляцию");
        lastRow.add(startStopButton);
        lastRow.add(resetButton);
        mainPanel.add(lastRow);

        add(mainPanel, BorderLayout.CENTER);

        settingsButton.addActionListener(e -> openSettingsWindow());
        dataButton.addActionListener(e -> openDataWindow());
        saveSimulationButton.addActionListener(e -> saveSimulation());
        loadSimulationButton.addActionListener(e -> loadSimulation());
        startStopButton.addActionListener(e -> handleStartStopSimulation());
        resetButton.addActionListener(e -> handleResetSimulation());

        setVisible(true);
    }






    private void openSettingsWindow() {
        SettingsWindow settingsWindow = new SettingsWindow(controller);
        childWindows.add(settingsWindow);
    }

    private void openDataWindow() {
        DataWindow dataWindow = new DataWindow(controller);
        controller.getModel().addObserver(dataWindow);
        childWindows.add(dataWindow);
    }

    private void handleStartStopSimulation() {
        RocketModel model = controller.getModel();
        if (!model.isRunning()) {
            if (controller.isSettingsConfirmed()) {
                boolean resetRequired = !model.isLoadedFromSave();
                controller.startSimulation(resetRequired);
                startStopButton.setText("Пауза");
                statusLabel.setText("Симуляция запущена");
            } else {
                JOptionPane.showMessageDialog(this, "Пожалуйста, подтвердите настройки перед запуском симуляции.", "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        } else if (model.isPaused()) {
            controller.resumeSimulation();
            startStopButton.setText("Пауза");
            statusLabel.setText("Симуляция продолжается");
        } else {
            controller.pauseSimulation();
            startStopButton.setText("Продолжить симуляцию");
            statusLabel.setText("Симуляция на паузе");
        }
    }



    private void handleResetSimulation() {
        controller.resetSimulation();
        startStopButton.setText("Начать симуляцию");
        statusLabel.setText("Симуляция сброшена");

        for (int i = 0; i < 3; i++) {
            fuelBars[i].setValue(100);
            fuelBars[i].setForeground(Color.GREEN);
            fuelLabels[i].setText("Ступень " + (i + 1));
        }
    }


    @Override
    public void onStageSeparation(int stageNumber) {
        System.out.println("Ступень " + stageNumber + " отделилась!");
    }

    @Override
    public void onUpdateStatus(double currentMass, double speed, double altitude, double horizontalDistance,
                               int remainingStages, double[] fuelMasses, double[] initialFuelMasses) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(String.format("Масса: %.2f кг, Скорость: %.2f м/с, Высота: %.2f м", currentMass, speed, altitude));
            for (int i = 0; i < 3; i++) {
                if (i < remainingStages) {
                    fuelLabels[i].setText(String.format("Ступень %d масса: %.2f", i + 1, fuelMasses[i]));
                    int fuelPercentage = (int) (fuelMasses[i] / initialFuelMasses[i] * 100);
                    fuelBars[i].setValue(fuelPercentage);
                    if (fuelPercentage == 0) {
                        fuelBars[i].setForeground(Color.RED);
                    } else {
                        fuelBars[i].setForeground(Color.GREEN);
                    }
                } else {
                    fuelBars[i].setValue(0);
                    fuelBars[i].setForeground(Color.RED);
                    fuelLabels[i].setText(String.format("Ступень %d отделена!", i + 1));
                }
            }
        });

    }

    private void saveSettings() {
        JFileChooser fileChooser = new JFileChooser();
        int option = fileChooser.showSaveDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            controller.saveSettings(file);
        }
    }

    private void loadSettings() {
        JFileChooser fileChooser = new JFileChooser();
        int option = fileChooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            controller.loadSettings(file);
        }
    }

    private void saveSimulation() {
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Simulation Files (*.sim)", "sim");
        fileChooser.setFileFilter(filter);
        int option = fileChooser.showSaveDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".sim")) {
                file = new File(file.getAbsolutePath() + ".sim");
            }
            controller.saveSimulationState(file);
        }
    }

    private void loadSimulation() {
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Simulation Files (*.sim)", "sim");
        fileChooser.setFileFilter(filter);
        int option = fileChooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            controller.loadSimulationState(file);
            controller.setSettingsConfirmed(true);
            startStopButton.setText("Продолжить симуляцию");
            statusLabel.setText("Симуляция загружена");
            controller.getModel().notifyObservers();
        }
    }



}
