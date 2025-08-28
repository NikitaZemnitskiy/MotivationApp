package com.buseiny.app.model;

public class ShopItem {
    private String id;
    private String title;
    private int cost;

    public ShopItem(){}
    public ShopItem(String id, String title, int cost){
        this.id=id; this.title=title; this.cost=cost;
    }
    public String getId(){ return id; }
    public void setId(String id){ this.id=id; }
    public String getTitle(){ return title; }
    public void setTitle(String title){ this.title=title; }
    public int getCost(){ return cost; }
    public void setCost(int cost){ this.cost=cost; }
}
