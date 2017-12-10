package com.durafficpark.Traffic;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import Jama.Matrix;
import com.durafficpark.road.Node;
import com.durafficpark.road.Road;

public class Controller {

    private List<Car> cars;
    private Map map;
    private final Matrix M;

    private float a,b,c;
    private float dt;

    private double maxOffset = 150;
    private int choiceLimit = 3;

    public Controller(float a, float b, float c, float dt){
        cars = new ArrayList<>();
        this.a = a;
        this.b = b;
        this.c = c;
        M = new Matrix(new double[][]{  {1, dt, dt*dt/2},
                                        {0, 1, dt},
                                        {0, 0, 0}});
    }

    private void runStep(){
        cars.parallelStream().forEach(car -> {
            Pair carInfront = getCarInfront(car);
            double s = carInfront.offset + carInfront.car.pos.get(0, 0) - car.pos.get(0, 0) - carInfront.car.length;
            car.pos.set(2, 0, F(car.pos.get(1,0), carInfront.car.pos.get(1,0), s));
        });
        applyMatrix();
    }

    private Pair getCarInfront(Car car){
        Road road = car.getRoad();
        return getNextCarOnRoad(road, 0, car, new int[]{0});
    }

    private Pair getNextCarOnRoad(Road road, double offset, Car currentCar, int[] choiceCount){
        if(offset > maxOffset){
            return null;
        }
        List<Car> cars = road.getCars();
        double shortestDist = Double.MAX_VALUE;
        Car closestCar = null;
        for(Car car : cars){
            double dist = offset + car.pos.get(1,0);
            if(dist > currentCar.pos.get(1,0) && dist < shortestDist){
                shortestDist = dist;
                closestCar = car;
            }
        }
        if(closestCar == null){
            Road nextRoad = getNextRoad(road, currentCar, choiceCount);
            if(nextRoad == null){
                return new Pair(new Car(currentCar.length, 0, currentCar.pos.get(1,0)), offset + road.getDistance());
            }
            return getNextCarOnRoad(nextRoad, offset + road.getDistance(), currentCar, choiceCount);
        }
        return new Pair(closestCar, offset);
    }

    private Road getNextRoad(Road road, Car currentCar, int[] choiceCount){
        List<Road> roads = road.getEndNode().getAdjacentRoads();
        if(roads.size() == 1){
            return roads.get(0);
        }else if(roads.size() == 0){
            return null;
        }
        if(choiceCount[0] != 0){
            return null;
        }
        choiceCount[0]++;
        int choice = currentCar.getChoice();
        if(choice == -1) {
            choice = makeChoice(roads, road);
            currentCar.setChoice(choice);
        }
        return roads.get(choice);
    }

    private int makeChoice(List<Road> roads, Road road){
        Node startNode = road.getStartNode();
        int size = roads.size();
        int randomElement = 0;
        for(int i = 0; i < choiceLimit; i++){
            randomElement = (int)(Math.random() * size);
            if(!roads.get(randomElement).getEndNode().equals(startNode)){
                return randomElement;
            }
        }
        return randomElement;
    }

    private double F(double v, double vFollowing, double s){
        return a*(Math.abs(s) - (c * v)) + b*(vFollowing - v);
    }

    private void applyMatrix(){
        cars.parallelStream().forEach(this::applyMatrixInd);
        cars.parallelStream().forEach(Car::completePosTransfer);
    }

    private void applyMatrixInd(Car car){
        car.pos2 = M.times(car.pos);
    }

    private class Pair{
        private double offset;
        private Car car;
        private Pair(Car car, double offset){
            this.car = car;
            this.offset = offset;
        }
    }
}