package com.github.f1nnabel.ipo.rgz;

public interface RocketObserver {
    void onStageSeparation(int stageNumber);
    void onUpdateStatus(double currentMass, double speed, double altitude, double horizontalDistance, int remainingStages, double[] fuelMasses, double[] initialFuelMasses);
}
