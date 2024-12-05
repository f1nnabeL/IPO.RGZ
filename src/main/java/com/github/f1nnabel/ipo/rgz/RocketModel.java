package com.github.f1nnabel.ipo.rgz;

import com.google.gson.Gson;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.io.*;

public class RocketModel implements Serializable {

    public static final double EARTH_RADIUS = 6_371_000; // в метрах
    public static final double GRAVITATIONAL_CONSTANT = 6.67430e-11; // м^3 кг^-1 с^-2
    public static final double EARTH_MASS = 5.972e24; // кг

    private double payloadMass;
    private double[] stageMasses;
    private double[] fuelMasses;
    private double[] initialFuelMasses;
    private double thrustPerKgFuel;

    private double currentMass;
    private double speed;
    private double altitude;
    private double horizontalDistance;

    private int remainingStages;
    private transient boolean paused = false;
    private int cycleDelay = 100;
    private double fuelConsumptionPerCycle = 0.01;
    private double deltaTime = cycleDelay / 1000.0;
    private boolean loadedFromSave = false;
    private transient Object pauseLock = new Object();

    private transient List<RocketObserver> observers = new ArrayList<>();
    private transient boolean running = false;

    private transient DatabaseManager dbManager = new DatabaseManager();

    private transient Thread simulationThread;

    private List<Double> timeData = new ArrayList<>();
    private List<Double> speedData = new ArrayList<>();
    private List<Double> altitudeData = new ArrayList<>();
    private List<Double> massData = new ArrayList<>();

    private double totalTime = 0;

    public void startSimulation(boolean resetRequired) {
        if (!running) {
            if (resetRequired) {
                resetSimulationVariables();
            }
            running = true;
            paused = false;
            simulationThread = new Thread(() -> {
                while (running) {
                    synchronized (pauseLock) {
                        if (paused) {
                            try {
                                pauseLock.wait();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }
                    if (altitude <= 0 && speed <= 0 && remainingStages == 0) {
                        running = false;
                        break;
                    }
                    updateRocketState();
                    notifyObservers();
                    try {
                        Thread.sleep(cycleDelay);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                running = false;
            });
            simulationThread.start();
        } else if (paused) {
            resumeSimulation();
        }
    }



    public void stopSimulation() {
        running = false;
        paused = false;
        if (simulationThread != null) {
            simulationThread.interrupt();
            try {
                simulationThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void resetSimulationVariables() {
        altitude = 0;
        speed = 0;
        horizontalDistance = 0;
        currentMass = payloadMass + Arrays.stream(stageMasses).sum() + Arrays.stream(fuelMasses).sum();
        remainingStages = stageMasses.length;
        fuelMasses = initialFuelMasses.clone();
        timeData.clear();
        speedData.clear();
        altitudeData.clear();
        massData.clear();
        totalTime = 0;
        paused = false;
        running = false;
    }


    private void updateRocketState() {
        if (remainingStages == 0) {
            simulatePayloadFlight();
            return;
        }

        int currentStage = remainingStages - 1;

        double thrust = fuelConsumptionPerCycle / deltaTime * thrustPerKgFuel;

        fuelMasses[currentStage] -= fuelConsumptionPerCycle;

        if (fuelMasses[currentStage] <= 0) {
            fuelMasses[currentStage] = 0;
            separateStage();
            return;
        }

        currentMass = payloadMass;
        for (int i = 0; i < remainingStages; i++) {
            currentMass += stageMasses[i] + fuelMasses[i];
        }

        double gravity = calculateGravity(altitude);

        double acceleration = (thrust / currentMass) - gravity;

        speed += acceleration * deltaTime;

        altitude += speed * deltaTime;

        if (altitude < 0) {
            altitude = 0;
            speed = 0;
        }

        totalTime += deltaTime;
        timeData.add(totalTime);
        speedData.add(speed);
        altitudeData.add(altitude);
        massData.add(currentMass);
        dbManager.saveStatistics(this);

    }

    private void simulatePayloadFlight() {
        currentMass = payloadMass;

        double gravity = calculateGravity(altitude);

        double acceleration = -gravity;

        speed += acceleration * deltaTime;

        altitude += speed * deltaTime;

        if (altitude <= 0) {
            altitude = 0;
            speed = 0;
            stopSimulation();
        }

        dbManager.saveStatistics(this);
    }

    public void addObserver(RocketObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(RocketObserver observer) {
        observers.remove(observer);
    }

    public void notifyObservers() {
        for (RocketObserver observer : observers) {
            observer.onUpdateStatus(currentMass, speed, altitude, horizontalDistance, remainingStages, fuelMasses, initialFuelMasses);
        }
    }

    private void separateStage() {
        remainingStages--;
        for (RocketObserver observer : observers) {
            observer.onStageSeparation(remainingStages + 1);
        }
    }

    public void setRocketParameters(double payloadMass, double[] stageMasses, double[] fuelMasses, double thrustPerKgFuel) {
        this.payloadMass = payloadMass;
        this.stageMasses = stageMasses;
        this.fuelMasses = fuelMasses;
        this.initialFuelMasses = fuelMasses.clone();
        this.thrustPerKgFuel = thrustPerKgFuel;
        this.remainingStages = stageMasses.length;
        this.currentMass = payloadMass + Arrays.stream(stageMasses).sum() + Arrays.stream(fuelMasses).sum();
        this.altitude = 0;
        this.speed = 0;
        this.horizontalDistance = 0;
        dbManager.saveSettings(this);
    }

    public void setCycleDelay(int delay) {
        this.cycleDelay = delay;
        this.deltaTime = delay / 1000.0;
    }

    public void setFuelConsumptionPerCycle(double fuelConsumption) {
        this.fuelConsumptionPerCycle = fuelConsumption;
    }

    public double getDeltaTime() {
        return deltaTime;
    }
    public void pauseSimulation() {
        paused = true;
    }

    public void resumeSimulation() {
        paused = false;
        synchronized (this) {
            notifyAll();
        }
    }

    public boolean isPaused() {
        return paused;
    }
    public double getPayloadMass() { return payloadMass; }
    public double[] getStageMasses() { return stageMasses; }
    public double[] getFuelMasses() { return fuelMasses; }
    public double getThrustPerKgFuel() { return thrustPerKgFuel; }
    public int getCycleDelay() { return cycleDelay; }
    public double getFuelConsumptionPerCycle() { return fuelConsumptionPerCycle; }
    public double getCurrentMass() { return currentMass; }
    public double getSpeed() { return speed; }
    public double getAltitude() { return altitude; }
    public int getRemainingStages() { return remainingStages; }
    public double getHorizontalDistance() {
        return horizontalDistance;
    }
    public List<Double> getTimeData() {
        return timeData;
    }

    public List<Double> getSpeedData() {
        return speedData;
    }

    public List<Double> getAltitudeData() {
        return altitudeData;
    }

    public List<Double> getMassData() {
        return massData;
    }
    public boolean isLoadedFromSave() {
        return loadedFromSave;
    }

    public void setLoadedFromSave(boolean loadedFromSave) {
        this.loadedFromSave = loadedFromSave;
    }

    public double getTotalTime() {
        return totalTime;
    }
    public void setPayloadMass(double payloadMass) { this.payloadMass = payloadMass; }
    public void setStageMasses(double[] stageMasses) { this.stageMasses = stageMasses; }
    public void setFuelMasses(double[] fuelMasses) { this.fuelMasses = fuelMasses; }
    public void setThrustPerKgFuel(double thrustPerKgFuel) { this.thrustPerKgFuel = thrustPerKgFuel; }

    public void saveSettings(File file) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
            out.writeObject(payloadMass);
            out.writeObject(stageMasses);
            out.writeObject(fuelMasses);
            out.writeObject(thrustPerKgFuel);
            out.writeObject(cycleDelay);
            out.writeObject(fuelConsumptionPerCycle);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadSettings(File file) {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            payloadMass = (double) in.readObject();
            stageMasses = (double[]) in.readObject();
            fuelMasses = (double[]) in.readObject();
            thrustPerKgFuel = (double) in.readObject();
            cycleDelay = (int) in.readObject();
            fuelConsumptionPerCycle = (double) in.readObject();

            resetSimulationVariables();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void saveSimulationState(File file) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
            out.writeObject(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void loadSimulationState(File file) {
        try (Reader reader = new FileReader(file)) {
            Gson gson = new Gson();
            RocketModel loadedModel = gson.fromJson(reader, RocketModel.class);
            copyFrom(loadedModel);
            this.loadedFromSave = true;
            notifyObservers();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void copyFrom(RocketModel other) {
        this.payloadMass = other.payloadMass;
        this.stageMasses = other.stageMasses.clone();
        this.fuelMasses = other.fuelMasses.clone();
        this.initialFuelMasses = other.initialFuelMasses.clone();
        this.thrustPerKgFuel = other.thrustPerKgFuel;
        this.currentMass = other.currentMass;
        this.speed = other.speed;
        this.altitude = other.altitude;
        this.horizontalDistance = other.horizontalDistance;
        this.remainingStages = other.remainingStages;
        this.cycleDelay = other.cycleDelay;
        this.fuelConsumptionPerCycle = other.fuelConsumptionPerCycle;
        this.deltaTime = other.deltaTime;
        this.timeData = new ArrayList<>(other.timeData);
        this.speedData = new ArrayList<>(other.speedData);
        this.altitudeData = new ArrayList<>(other.altitudeData);
        this.massData = new ArrayList<>(other.massData);
        this.totalTime = other.totalTime;
        this.running = other.running;
        this.paused = other.paused;
    }



    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        observers = new ArrayList<>();
        dbManager = new DatabaseManager();
    }

    private double calculateGravity(double altitude) {
        double distanceFromEarthCenter = EARTH_RADIUS + altitude;
        return GRAVITATIONAL_CONSTANT * EARTH_MASS / (distanceFromEarthCenter * distanceFromEarthCenter);
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }
}
