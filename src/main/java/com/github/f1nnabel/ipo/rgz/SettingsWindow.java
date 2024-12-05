package com.github.f1nnabel.ipo.rgz;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;

public class SettingsWindow extends JFrame {
    private RocketController controller;

    private JButton saveSettingsButton;
    private JButton loadSettingsButton;
    private JButton applySettingsButton;

    private JTextField payloadMassField;
    private JTextField[] stageMassFields;
    private JTextField[] fuelMassFields;
    private JTextField thrustField;
    private JTextField cycleDelayField;
    private JTextField fuelConsumptionField;

    public SettingsWindow(RocketController controller) {
        this.controller = controller;
        initUI();
    }

    private void initUI() {
        setTitle("Настройки симуляции");
        setSize(400, 500);
        setLayout(new BorderLayout());

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        payloadMassField = new JTextField("10");
        contentPanel.add(createFieldPanel("Полезная масса (кг):", payloadMassField));

        stageMassFields = new JTextField[3];
        fuelMassFields = new JTextField[3];

        for (int i = 0; i < 3; i++) {
            stageMassFields[i] = new JTextField("5");
            fuelMassFields[i] = new JTextField("3");

            contentPanel.add(createFieldPanel("Масса ступени " + (i + 1) + " (кг):", stageMassFields[i]));
            contentPanel.add(createFieldPanel("Масса топлива ступени " + (i + 1) + " (кг):", fuelMassFields[i]));
        }

        thrustField = new JTextField("3500");
        contentPanel.add(createFieldPanel("Тяга на кг топлива:", thrustField));

        cycleDelayField = new JTextField("100");
        contentPanel.add(createFieldPanel("Задержка цикла симуляции (мс):", cycleDelayField));

        fuelConsumptionField = new JTextField("0.01");
        contentPanel.add(createFieldPanel("Сжигаемое топливо за цикл (кг):", fuelConsumptionField));

        JPanel saveLoadPanel = new JPanel(new FlowLayout());
        saveSettingsButton = new JButton("Сохранить настройки");
        loadSettingsButton = new JButton("Загрузить настройки");
        saveLoadPanel.add(saveSettingsButton);
        saveLoadPanel.add(loadSettingsButton);
        contentPanel.add(saveLoadPanel);

        JPanel applyButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        applySettingsButton = new JButton("Применить настройки");
        applyButtonPanel.add(applySettingsButton);
        contentPanel.add(applyButtonPanel);

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        add(scrollPane, BorderLayout.CENTER);

        applySettingsButton.addActionListener(e -> {
            applySettings() ;
                controller.setSettingsConfirmed(true);
        });

        saveSettingsButton.addActionListener(e -> saveSettings());
        loadSettingsButton.addActionListener(e -> loadSettings());

        setVisible(true);
    }

    private JPanel createFieldPanel(String labelText, JTextField textField) {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        JLabel label = new JLabel(labelText);
        label.setPreferredSize(new Dimension(220, 25));
        panel.add(label, BorderLayout.WEST);
        panel.add(textField, BorderLayout.CENTER);
        textField.setMaximumSize(new Dimension(Integer.MAX_VALUE, textField.getPreferredSize().height));

        return panel;
    }
    private void updateFieldsFromModel() {
        RocketModel model = controller.getModel();
        payloadMassField.setText(String.valueOf(model.getPayloadMass()));
        double[] stageMasses = model.getStageMasses();
        double[] fuelMasses = model.getFuelMasses();
        for (int i = 0; i < 3; i++) {
            stageMassFields[i].setText(String.valueOf(stageMasses[i]));
            fuelMassFields[i].setText(String.valueOf(fuelMasses[i]));
        }
        thrustField.setText(String.valueOf(model.getThrustPerKgFuel()));
        cycleDelayField.setText(String.valueOf(model.getCycleDelay()));
        fuelConsumptionField.setText(String.valueOf(model.getFuelConsumptionPerCycle()));
    }

    private void saveSettings() {
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Configuration Files (*.cfg)", "cfg");
        fileChooser.setFileFilter(filter);
        int option = fileChooser.showSaveDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".cfg")) {
                file = new File(file.getAbsolutePath() + ".cfg");
            }
            controller.saveSettingsToFile(file);
        }
    }


    private void loadSettings() {
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Configuration Files (*.cfg)", "cfg");
        fileChooser.setFileFilter(filter);
        int option = fileChooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            controller.loadSettingsFromFile(file);
            updateFieldsFromModel();
            JOptionPane.showMessageDialog(this, "Настройки успешно загружены. Нажмите 'Применить настройки', чтобы подтвердить.", "Информация", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void applySettings() {
        try {
            double payloadMass = Double.parseDouble(payloadMassField.getText());
            double[] stageMasses = new double[3];
            double[] fuelMasses = new double[3];
            for (int i = 0; i < 3; i++) {
                stageMasses[i] = Double.parseDouble(stageMassFields[i].getText());
                fuelMasses[i] = Double.parseDouble(fuelMassFields[i].getText());
            }
            double thrustPerKgFuel = Double.parseDouble(thrustField.getText());
            int cycleDelay = Integer.parseInt(cycleDelayField.getText());
            double fuelConsumptionPerCycle = Double.parseDouble(fuelConsumptionField.getText());

            controller.getModel().setRocketParameters(payloadMass, stageMasses, fuelMasses, thrustPerKgFuel);
            controller.getModel().setCycleDelay(cycleDelay);
            controller.getModel().setFuelConsumptionPerCycle(fuelConsumptionPerCycle);

            controller.setSettingsConfirmed(true);
            controller.saveSettingsToFile(new File("rocket_settings.json"));
            JOptionPane.showMessageDialog(this, "Настройки успешно применены.", "Информация", JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Ошибка ввода данных. Пожалуйста, введите корректные числовые значения.", "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }
}
