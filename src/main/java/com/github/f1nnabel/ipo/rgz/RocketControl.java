package com.github.f1nnabel.ipo.rgz;

public interface RocketControl {
    void startSimulation(boolean reset);
    void stopSimulation();
    void setRocketParameters(double payloadMass, double[] stageMasses, double[] fuelMasses, double thrustPerKgFuel);
    void setCycleDelay(int delay);
    void setFuelConsumptionPerCycle(double fuelConsumption);
}
