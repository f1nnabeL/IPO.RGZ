package com.github.f1nnabel.ipo.rgz;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;

public class RocketController implements RocketControl {
    private RocketModel model;
    private boolean settingsConfirmed = false;

    public RocketController(RocketModel model) {
        this.model = model;
    }

    @Override
    public void startSimulation(boolean reset) {
        model.startSimulation(reset);
    }

    @Override
    public void stopSimulation() {
        model.stopSimulation();
    }

    public void pauseSimulation() {
        model.pauseSimulation();
    }

    public void resumeSimulation() {
        model.resumeSimulation();
    }

    public void resetSimulation() {
        model.stopSimulation();
        model.resetSimulationVariables();
        model.setLoadedFromSave(false);
    }


    @Override
    public void setRocketParameters(double payloadMass, double[] stageMasses, double[] fuelMasses, double thrustPerKgFuel) {
        model.setRocketParameters(payloadMass, stageMasses, fuelMasses, thrustPerKgFuel);
    }

    @Override
    public void setCycleDelay(int delay) {
        model.setCycleDelay(delay);
    }

    @Override
    public void setFuelConsumptionPerCycle(double fuelConsumption) {
        model.setFuelConsumptionPerCycle(fuelConsumption);
    }

    public RocketModel getModel() {
        return model;
    }

    public void setSettingsConfirmed(boolean confirmed) {
        this.settingsConfirmed = confirmed;
    }

    public boolean isSettingsConfirmed() {
        return settingsConfirmed;
    }

    public void saveSettings(File file) {
        model.saveSettings(file);
    }

    public void loadSettings(File file) {
        model.loadSettings(file);
    }

    public void saveSimulationState(File file) {
        try (Writer writer = new FileWriter(file)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(model, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadSimulationState(File file) {
        try (Reader reader = new FileReader(file)) {
            Gson gson = new Gson();
            RocketModel loadedModel = gson.fromJson(reader, RocketModel.class);
            model.copyFrom(loadedModel);
            model.notifyObservers();
            model.setLoadedFromSave(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveSettingsToFile(File file) {
        try (Writer writer = new FileWriter(file)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(model, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadSettingsFromFile(File file) {
        try (Reader reader = new FileReader(file)) {
            Gson gson = new Gson();
            RocketModel loadedModel = gson.fromJson(reader, RocketModel.class);
            model.setRocketParameters(
                    loadedModel.getPayloadMass(),
                    loadedModel.getStageMasses(),
                    loadedModel.getFuelMasses(),
                    loadedModel.getThrustPerKgFuel()
            );
            model.setCycleDelay(loadedModel.getCycleDelay());
            model.setFuelConsumptionPerCycle(loadedModel.getFuelConsumptionPerCycle());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
