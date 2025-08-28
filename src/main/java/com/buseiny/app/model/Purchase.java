package com.buseiny.app.model;

import java.time.LocalDateTime;

public class Purchase {
    private String shopItemId;
    private String titleSnapshot;
    private int costSnapshot;
    private LocalDateTime purchasedAt;

    public Purchase(){}
    public Purchase(String id, String title, int cost, LocalDateTime at){
        this.shopItemId=id; this.titleSnapshot=title; this.costSnapshot=cost; this.purchasedAt=at;
    }
    public String getShopItemId(){ return shopItemId; }
    public void setShopItemId(String shopItemId){ this.shopItemId=shopItemId; }
    public String getTitleSnapshot(){ return titleSnapshot; }
    public void setTitleSnapshot(String titleSnapshot){ this.titleSnapshot=titleSnapshot; }
    public int getCostSnapshot(){ return costSnapshot; }
    public void setCostSnapshot(int costSnapshot){ this.costSnapshot=costSnapshot; }
    public LocalDateTime getPurchasedAt(){ return purchasedAt; }
    public void setPurchasedAt(LocalDateTime purchasedAt){ this.purchasedAt=purchasedAt; }
}
