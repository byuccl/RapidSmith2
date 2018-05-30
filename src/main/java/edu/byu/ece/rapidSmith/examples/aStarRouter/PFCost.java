package edu.byu.ece.rapidSmith.examples.aStarRouter;


public class PFCost {
    private int wireCost;

    // usage
    private int history;

    public int getWireCost() {
        return wireCost;
    }

    public void setWireCost(int wireCost) {
        this.wireCost = wireCost;
    }

    public int getHistory() {
        return history;
    }

    public void setHistory(int history) {
        this.history = history;
    }

    public int getOccupancy() {
        return occupancy;
    }

    public void setOccupancy(int occupancy) {
        this.occupancy = occupancy;
    }

    private int occupancy;


    public PFCost() {
        wireCost = 1;
        history = 0;
        occupancy = 0;
    }

    public int getPFCost() {
        return (history + wireCost) * occupancy;
    }
}

